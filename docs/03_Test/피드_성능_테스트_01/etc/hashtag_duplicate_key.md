# hashtag duplicate key 오류 분석

## 1. 개요

`celebrity_feed_read_write_mix.groovy`로 피드 읽기/쓰기 혼합 성능 테스트를 수행하는 중 발생한 `hashtag` 중복 키 오류

게시글 작성에서 다음 오류 발생

```text
Caused by: java.sql.SQLIntegrityConstraintViolationException:
Duplicate entry '1780902137234' for key 'hashtag.uk_hashtag_name'
```

```sql
CONSTRAINT uk_hashtag_name UNIQUE (name)
```

동일한 `name` 값을 가진 해시태그를 두 개 이상 저장하려고 하면서 유니크 키 충돌

## 2. 원인

### 2.1 해시태그 생성 로직의 동시성 문제

게시글 생성 시 해시태그 저장 흐름

```text
1. content에서 해시태그 추출
2. hashtag 테이블에서 기존 name 조회
3. 조회되지 않은 해시태그를 INSERT
4. hashtag_posts에 게시글-해시태그 관계 INSERT
```

여러 요청이 동시에 같은 해시태그를 처음 생성하면서 race condition 발생

```text
T1: hashtag.name = 'celebrity' 조회 -> 없음
T2: hashtag.name = 'celebrity' 조회 -> 없음
T1: INSERT 성공
T2: INSERT 시도 -> Duplicate key 오류
```

트랜잭션 실행 흐름

```text
Tx A 시작
Tx B 시작

Tx A: SELECT hashtag WHERE name = 'fanout'
      -> 결과 없음

Tx B: SELECT hashtag WHERE name = 'fanout'
      -> 결과 없음

Tx A: INSERT INTO hashtag(name) VALUES ('fanout')
      -> 성공

Tx A: COMMIT

Tx B: INSERT INTO hashtag(name) VALUES ('fanout')
      -> Duplicate key 오류

Tx B: ROLLBACK
```

## 3. 해결 방안

### 3.1 hashtag INSERT 멱등 처리

`hashtag` 저장 로직을 동시성에 안전하게 변경

MySQL에서는 `INSERT ... ON DUPLICATE KEY UPDATE`를 사용해 신규 생성과 기존 조회를 하나의 DB 연산으로 처리

```sql
INSERT INTO hashtag (name)
VALUES (#{name})
ON DUPLICATE KEY UPDATE hashtag_id = LAST_INSERT_ID(hashtag_id)
```

동작 방식
- 해시태그가 없으면 새로 INSERT
- 이미 존재하면 유니크 키 오류 없이 기존 row 사용
- `LAST_INSERT_ID(hashtag_id)`로 기존 row의 `hashtag_id` 반환
- MyBatis의 `useGeneratedKeys`와 함께 신규/기존 케이스 모두 처리

장점:

- 동시성 제어를 DB 유니크 키와 단일 insert 구문에 위임할 수 있음
- 별도 락을 잡지 않아 처리량 저하가 상대적으로 작음
- 충돌이 발생해도 예외 흐름으로 빠지지 않음
- `hashtag.name`은 이미 유니크 제약이 있어 upsert 대상에 적합함

### 3.2 DuplicateKeyException catch 후 재조회

중복 insert가 발생하면 예외를 잡고 다시 `SELECT`해서 기존 `hashtag_id`를 가져오는 방식이다.

장점:

- 현재 `SELECT -> INSERT` 구조를 크게 바꾸지 않아도 됨
- DB 벤더 종속 SQL을 줄일 수 있음

단점:

- 충돌이 예상되는 상황에서 예외를 제어 흐름으로 사용
- 충돌이 많은 부하 테스트에서는 예외 생성, 로그, rollback 비용 증가
- insert 실패 후 재조회까지 DB round trip 증가

### 3.3 `SELECT ... FOR UPDATE` 사용

해시태그 조회 시 row lock을 잡아 다른 트랜잭션의 접근을 막는 방식

장점:

- 이미 존재하는 row에 대해서는 명시적으로 동시 접근 제어 가능

단점:

- 존재하지 않는 해시태그에는 잠글 row가 없음
- gap lock까지 고려
- 락 대기와 deadlock 가능성 증가

### 3.4 트랜잭션 격리 수준 상향

트랜잭션 격리 수준을 `SERIALIZABLE`로 올려 check-then-insert 경쟁을 줄이는 방식

장점:

- 애플리케이션 코드 변경 없이 동시성 이상 현상을 줄일 수 있음

단점:

- 전체 트랜잭션의 락 경합 증가
- 게시글 작성 API 처리량 저하 가능
- lock wait timeout, deadlock 가능성 증가
