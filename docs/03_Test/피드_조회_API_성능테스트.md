# 피드 조회 성능 테스트

## 1. 목적

현재 피드 조회 API의 성능 특성을 확인하고, 실제 병목 지점을 파악한다.

이번 테스트에서는 아래 두 가지를 함께 확인했다.

- 실제 API 응답 시간
- `EXPLAIN ANALYZE` 기반 SQL 실행 계획

## 2. 테스트 대상

- API: `GET /api/v1/posts?page=0&size=10`
- 서버 주소: `http://localhost:8080`
- 서비스 코드: [`PostFeedService.java`](C:\Users\chlwo\workspace\daily-us\src\main\java\com\jaeychoi\dailyus\post\service\PostFeedService.java)
- 쿼리 정의: [`PostMapper.xml`](C:\Users\chlwo\workspace\daily-us\src\main\resources\mapper\post\PostMapper.xml)

현재 피드 조회는 아래 순서로 동작한다.

1. `existsFeedPosts`
2. `findFeedPosts` 또는 `findRecentFeedPosts`
3. `findImagesByPostIds`

## 3. 테스트 환경

- 애플리케이션: 로컬 실행 서버
- DB: 로컬 MySQL
- 데이터 규모
  - 사용자 `300,000`
  - 게시물 `3,000,000`
  - 사용자당 평균 게시물 `10`
- 인증 계정
  - 일반 사용자: `local-user000001@dailyus.local`
  - 팔로우가 많은 사용자: `local-user000011@dailyus.local`
  - fallback 확인용 사용자: 테스트 중 신규 가입 계정 생성

## 4. 테스트 방식

### 4.1 API 응답 시간 측정

각 사용자로 로그인 후 `GET /api/v1/posts?page=0&size=10` 를 3회 호출했다.

- 측정 기준: `Invoke-RestMethod` 호출 전후 stopwatch 기준 elapsed time
- 측정 목적: 실제 사용자 체감 시간 확인

### 4.2 SQL 실행 계획 분석

아래 쿼리에 대해 로컬 DB에서 실제 `EXPLAIN ANALYZE`를 실행했다.

- `existsFeedPosts`
- `findFeedPosts`
- `findRecentFeedPosts`

## 5. API 테스트 결과

| 구분 | 계정 특성 | 호출 결과 | 1회차 | 평균 | 최소 | 최대 |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| 일반 사용자 | `userId = 1` | 10건 반환 | 248.12ms | 92.27ms | 14.32ms | 248.12ms |
| 팔로우가 많은 사용자 | `userId = 11` | 10건 반환 | 3471.58ms | 3591.97ms | 3471.58ms | 3713.38ms |
| fallback 사용자 | 신규 가입 사용자 | 10건 반환 | 10011.39ms | 10094.17ms | 10011.39ms | 10169.46ms |

- 일반 사용자는 첫 호출 이후 매우 빠르게 응답했다.
- 팔로우가 많은 사용자는 매 호출이 `3.5초` 전후로 지속되었다.
- fallback 사용자는 매 호출이 `10초` 전후로 유지되었다.

## 6. 실행 계획 분석 결과

### 6.1 일반 사용자 `userId = 1`

#### `existsFeedPosts`

- 총 시간: 약 `10.3ms`
- 관련 사용자 집합: `77 rows`

핵심 plan:

```text
Union materialize with deduplication ... (actual time=2.39..2.39 rows=77)
Index lookup on p using idx_posts_user_id_created_at ... (actual time=6.78..6.78 rows=1)
```

평가:

- 일반 사용자에서는 `existsFeedPosts` 자체가 큰 문제는 아니다.

#### `findFeedPosts`

- 총 시간: 약 `246ms`
- 관련 사용자 집합: `77 rows`
- 게시물 후보군: `770 rows`

핵심 plan:

```text
Limit: 10 row(s)  (actual time=246..246 rows=10)
Sort: p.created_at DESC ... (actual time=246..246 rows=10)
Stream results ... (actual time=0.119..245 rows=770)
Index lookup on p using idx_posts_user_id_created_at ... (actual time=0.56..2.83 rows=10 loops=77)
```

### 6.2 팔로우가 많은 사용자 `userId = 11`

#### `existsFeedPosts`

- 총 시간: 약 `332ms`
- 관련 사용자 집합: `90,044 rows`

핵심 plan:

```text
Union materialize with deduplication ... (actual time=325..325 rows=90044)
Covering index lookup on uf using PRIMARY (follower = 11) ... (actual time=20.3..290 rows=90024)
```

- `user_follow` 절대량이 큰 사용자는 존재 여부 확인만으로도 수백 ms를 사용한다.
- 본 조회 전에 한 번 더 수행한다.

#### `findFeedPosts`

- 총 시간: 약 `3.898s`
- 관련 사용자 집합: `90,044 rows`
- 게시물 후보군: `900,440 rows`

핵심 plan:

```text
Limit: 10 row(s)  (actual time=3898..3898 rows=10)
Sort: p.created_at DESC ... (actual time=3898..3898 rows=10)
Stream results ... (actual time=42.2..3748 rows=900440)
Table scan on r ... (actual time=42..55.6 rows=90044)
Covering index lookup on f using PRIMARY (follower = 11) ... (actual time=0.664..15.6 rows=90024)
Index lookup on p using idx_posts_user_id_created_at ... (actual time=0.00345..0.0321 rows=10 loops=90044)
```

- 관련 사용자 `90,044명`에 대해 `posts` lookup을 `90,044회` 수행하고,
- 그 결과 `900,440건의 게시물`을 모아 정렬한 뒤 상위 10건만 반환한다.
- 팔로우가 많은 사용자는 구조적으로 읽기 비용과 정렬 비용이 커진다.

### 6.3 fallback `findRecentFeedPosts`

- 총 시간: 약 `11.1s`
- 전체 사용자 `300,000 rows` 스캔
- 전체 게시물 `3,000,000 rows` 수준 처리

핵심 plan:

```text
Limit: 10 row(s)  (actual time=11116..11116 rows=10)
Sort: p.created_at DESC ... (actual time=11116..11116 rows=10)
Stream results ... (actual time=15.2..10575 rows=3e+6)
Table scan on u ... (actual time=1.43..584 rows=300000)
Index lookup on p using idx_posts_user_id_created_at ... (actual time=0.00324..0.027 rows=10 loops=300000)
```

- 현재 `posts` 테이블에는 해당 쿼리를 실행하기 위한 적합한 인덱스가 없다.
- 그래서 쿼리 수행 시 많은 시간이 걸린다.

## 7. 종합 분석

### 7.1 가장 느린 쿼리

응답 시간만 놓고 본다면 `findRecentFeedPosts`가 가장 느리다.

하지만, 해당 쿼리는 관계(그룹, 팔로우)가 아예 없는 사용자에게만 수행되는 쿼리다.
즉, 수행 빈도가 낮을 것이다. 그리고 해당 쿼리를 위해 인덱스를 생성한다면 저장 비용 등 고려할 사항이 많아진다.

따라서 별도의 인덱스 생성은 하지 않고 `최신 게시물을 메모리에 캐싱하는 방식`을 적용 시킬 수 있을 것 같다.

### 7.2 개인별 피드 조회

일반 사용자에 비해 팔로우 수가 많은 사용자는 피드 조회 시 더 많은 작업량이 발생한다.
관계된 사용자 집합을 먼저 구한 뒤, 해당 사용자들의 게시물을 조회하고, 최종적으로 이를 정렬하는 과정에서 처리해야 할 데이터 양이 크게 증가하기 때문이다.
즉, 팔로우가 많은 사용자일수록 피드 조회 시 정렬 대상이 되는 게시물 수가 많아진다.

따라서 현재와 같은 `Pull 기반 조회 방식만으로는 팔로우 수가 많은 사용자에 대해 일관된 응답 속도를 보장하기 어렵다.` 
이에 대한 개선 방안으로, 게시글을 조회하는 시점마다 관계된 사용자의 게시물을 수집하는 대신, `게시글 작성 시점에 미리 사용자별 피드 목록을 구성하는 Push 모델로 아키텍처`를 고려할 수 있다. 
이 방식을 통해 조회 시점의 연산 부담을 줄일 수 있다.

또한 조회 전에 별도의 존재 여부를 확인하는 쿼리를 수행하고 있다. 이는 전체 요청당 불필요하게 데이터베이스를 접근 한다. 
따라서 실제 조회 결과를 기반으로 처리할 수 있도록 구조를 단순화하여, 사전 존재 확인 쿼리를 제거하는 방향을 고려할 수 있다.
