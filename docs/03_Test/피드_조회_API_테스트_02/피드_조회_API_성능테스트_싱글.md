# 피드 조회 API 성능 테스트 (싱글, Cursor vs Offset)

## 1. 목적

`cursor` 기반 피드 조회와 기존 `offset` 기반 피드 조회보를 비교한다.

- 실제 API 응답 시간
- 동일 데이터셋에서 `offset` SQL과 `cursor` SQL의 실행 시간 및 `EXPLAIN ANALYZE`

## 2. 비교 대상

### 2.1 이전 Offset 방식

- 컨트롤러: `GET /api/v1/posts?page={page}&size=10`
- 기준 코드: `cba77c9^`, `0cc24c9^`
- 핵심 SQL 차이
  - 개인화 피드: `ORDER BY ... LIMIT #{size} OFFSET #{offset}`
  - fallback 최근 피드: `ORDER BY ... LIMIT #{size} OFFSET #{offset}`

### 2.2 현재 Cursor 방식

- 컨트롤러: `GET /api/v1/posts?createdAt={lastCreatedAt}&postId={lastPostId}&size=10`
- 현재 코드
  - 서비스: `PostFeedService#getFeed`
  - 쿼리: `PostMapper.findFeedPosts`, `PostMapper.findRecentFeedPosts`

## 3. 테스트 환경

- 애플리케이션: `http://localhost:8080`
- DB: 로컬 MySQL
- 데이터 규모
  - 사용자 `300,000`
  - 게시물 `3,000,000`
  - 사용자당 평균 게시물 `10`
- 인증 계정
  - 일반 사용자: `local-user000001@dailyus.local`
  - 팔로우가 많은 사용자: `local-user000011@dailyus.local`
  - fallback 확인용 사용자: 팔로우, 그룹이 없는 사용자

### 인덱스
- 이전
  - `idx_posts_user_id_created_at (user_id, created_at)`
- 리팩토링 후
  - `idx_posts_user_deleted_created_at (user_id, deleted_at, created_at, post_id)`

## 4. 테스트 방식

### 4.1 API 응답 시간

각 사용자로 로그인 후 아래를 3회 호출

- 첫 페이지: `GET /api/v1/posts?size=10`
- 다음 페이지: 첫 페이지 응답의 `lastCreatedAt`, `lastPostId`를 사용해 재호출

### 4.2 SQL 비교

- 이전 `offset` SQL
- 현재 `cursor` SQL
- `EXPLAIN ANALYZE`

## 5. API 테스트 결과

### 5.1 첫 페이지 응답 시간 비교

| 구분 | 계정 특성 | 이전 Offset 평균 | 현재 Cursor 평균 | 변화 |
| --- | --- | ---: | ---: | ---: |
| 일반 사용자 | `userId = 1` | 92.27ms | 109.74ms | +18.9% |
| 팔로우가 많은 사용자 | `userId = 11` | 3591.97ms | 2806.01ms | -21.9% |
| fallback 사용자 | 신규 가입 사용자 | 10094.17ms | 4842.78ms | -52.0% |

### 5.2 Cursor 첫 페이지 응답 시간

| 구분 | 호출 결과 | 1회차 | 평균 | 최소 | 최대 |
| --- | --- | ---: | ---: | ---: | ---: |
| 일반 사용자 | 10건 반환 | 302.64ms | 109.74ms | 11.99ms | 302.64ms |
| 팔로우가 많은 사용자 | 10건 반환 | 2712.53ms | 2806.01ms | 2712.53ms | 2900.78ms |
| fallback 사용자 | 10건 반환 | 3247.73ms | 4842.78ms | 3234.80ms | 8045.80ms |

- 일반 사용자는 이전과 큰 차이가 없다.
- 팔로우가 많은 사용자는 첫 페이지에서도 약 `22%` 개선됐다.
- fallback 사용자는 이전 `10초`에서 `4.8초`로 개선됐다.

### 5.3 다음 페이지 응답 시간

| 구분 | 현재 Cursor 평균 | 최소 | 최대 |
| --- | ---: | ---: | ---: |
| 일반 사용자 | 14.99ms | 11.15ms | 21.89ms |
| 팔로우가 많은 사용자 | 2899.36ms | 2865.08ms | 2942.34ms |
| fallback 사용자 | 6681.29ms | 3463.68ms | 8308.38ms |

## 6. SQL 직접 비교 결과

### 6.1 개인화 피드 (`userId = 11`)

| 위치 | Offset 평균 | Cursor 평균 | 변화 |
| --- | ---: | ---: | ---: |
| 첫 페이지 (`offset 0`) | 2906.56ms | 2702.07ms | -7.0% |
| 다음 페이지 (`offset 10`) | 2733.08ms | 2785.26ms | +1.9% |
| 뒤쪽 페이지 (`offset 10,000`) | 3102.25ms | 2753.96ms | -11.2% |
| 깊은 페이지 (`offset 100,000`) | 3080.67ms | 2645.99ms | -14.1% |

- 개인화 피드는 첫 페이지/다음 페이지에서는 `offset`과 `cursor` 차이가 크지 않다.
- 하지만 뒤로 갈수록 `cursor`가 조금씩 유리해진다.
- 사용자 집합과 게시물 후보를 읽고 정렬하기 때문에 개선 폭이 기대보다 크지 않다.

### 6.2 fallback 최근 피드

| 위치 | Offset 평균 | Cursor 평균 | 변화 |
| --- | ---: | ---: | ---: |
| 첫 페이지 (`offset 0`) | 6447.24ms | 4783.15ms | -25.8% |
| 다음 페이지 (`offset 10`) | 4684.75ms | 6531.14ms | +39.4% |
| 뒤쪽 페이지 (`offset 10,000`) | 4214.72ms | 4891.90ms | +16.1% |
| 깊은 페이지 (`offset 100,000`) | 11990.28ms | 4799.54ms | -60.0% |

- 최근 피드는 측정 편차가 크다.
- 뒤로 갈수록 `cursor`가 확실히 유리했다.

## 7. 종합 결론

- `cursor` 전환 이후, API 기준으로
  - 팔로우가 많은 사용자 첫 페이지는 약 `21.9%` 개선
  - fallback 최근 피드 첫 페이지는 약 `52.0%` 개선
- 뒷 페이지 비교에서는 `offset` 대비 `cursor`가 유리해진다.
  - 개인화 피드 `offset 100,000`: `3098ms -> 2746ms`
  - 최근 피드 `offset 100,000`: `11211ms -> 3154ms`

## 8. 기타
- Offset 기반 Pagination으로 무한 스크롤 구현 시 중복된 페이지가 출력될 수 있다. 
- 단순히 게시글 개수만으로 구분 하는 방식이기 때문에 게시글이 새롭게 추가 되는 상황을 생각해보자.  
-> 따라서 Cursor 기반 방식으로 구현하는 것이 적합하다.
