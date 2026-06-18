-- dailyus celebrity-problem sample data seed script
-- target: MySQL 8.x
--
-- local scale
--   users                 : 140,001
--   celebrity             : user_id = 1
--   celebrity followers   : 100,000 (user_id 2..100001)
--   general reader pool   : 40,000 (user_id 100002..140001)
--   posts                 : about 111,000
--   user_follow           : about 1,220,000
--
-- purpose
--   1. celebrity fanout write amplification testing
--   2. mixed feed read/write performance testing with nGrinder
--   3. deterministic follower/general reader pools

USE dailyus;

SET SESSION sql_safe_updates = 0;
SET SESSION unique_checks = 0;
SET SESSION foreign_key_checks = 0;

TRUNCATE TABLE hashtag_posts;
TRUNCATE TABLE comment_likes;
TRUNCATE TABLE post_likes;
TRUNCATE TABLE user_follow;
TRUNCATE TABLE group_members;
TRUNCATE TABLE comments;
TRUNCATE TABLE post_images;
TRUNCATE TABLE posts;
TRUNCATE TABLE user_groups;
TRUNCATE TABLE hashtag;
TRUNCATE TABLE users;

DROP TABLE IF EXISTS seed_digits;
DROP TABLE IF EXISTS seed_follow_offsets_8;
DROP TABLE IF EXISTS seed_post_offsets_5;
DROP TABLE IF EXISTS seed_post_offsets_3;
DROP TABLE IF EXISTS seed_seq_100k;
DROP TABLE IF EXISTS seed_seq_140001;

CREATE TABLE seed_digits (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_digits (n)
VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

CREATE TABLE seed_follow_offsets_8 (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_follow_offsets_8 (n)
VALUES (1), (2), (3), (4), (5), (6), (7), (8);

CREATE TABLE seed_post_offsets_5 (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_post_offsets_5 (n)
VALUES (1), (2), (3), (4), (5);

CREATE TABLE seed_post_offsets_3 (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_post_offsets_3 (n)
VALUES (1), (2), (3);

CREATE TABLE seed_seq_100k (
    seq INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_seq_100k (seq)
SELECT ones.n
     + tens.n * 10
     + hundreds.n * 100
     + thousands.n * 1000
     + ten_thousands.n * 10000
     + 1 AS seq
FROM seed_digits ones
CROSS JOIN seed_digits tens
CROSS JOIN seed_digits hundreds
CROSS JOIN seed_digits thousands
CROSS JOIN seed_digits ten_thousands
WHERE ones.n
    + tens.n * 10
    + hundreds.n * 100
    + thousands.n * 1000
    + ten_thousands.n * 10000 < 100000;

CREATE TABLE seed_seq_140001 (
    seq INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_seq_140001 (seq)
SELECT seq
FROM seed_seq_100k
UNION ALL
SELECT seq + 100000
FROM seed_seq_100k
WHERE seq <= 40001;

INSERT INTO users (
    email,
    password,
    nickname,
    follower_count,
    followee_count,
    intro,
    profile_image,
    created_at,
    updated_at,
    deleted_at
)
SELECT CONCAT('local-user', LPAD(seq, 6, '0'), '@dailyus.local'),
       '$2a$10$N9/gNWwi6iIRM5/xzdAi3u4H8RZCxqSC/b956LzNv1wUwEALulsQK',
       CASE
           WHEN seq = 1 THEN 'celebrity_000001'
           ELSE CONCAT('local_user_', LPAD(seq, 6, '0'))
       END,
       0,
       0,
       CASE
           WHEN seq = 1 THEN 'celebrity test account with 100k followers'
           ELSE CONCAT('intro ', LPAD(seq, 6, '0'))
       END,
       CONCAT('https://cdn.dailyus.local/profile/local-', seq, '.png'),
       TIMESTAMP('2024-01-01 00:00:00') + INTERVAL (seq % 365) DAY + INTERVAL (seq % 86400) SECOND,
       TIMESTAMP('2024-01-01 00:00:00') + INTERVAL (seq % 365) DAY + INTERVAL (seq % 86400) SECOND,
       NULL
FROM seed_seq_140001;

-- celebrity baseline posts
INSERT INTO posts (
    content,
    created_at,
    updated_at,
    deleted_at,
    like_count,
    user_id
)
SELECT CONCAT('celebrity baseline post #', seq, ' #celebrity #fanout'),
       TIMESTAMP('2025-05-01 00:00:00') + INTERVAL seq MINUTE,
       TIMESTAMP('2025-05-01 00:00:00') + INTERVAL seq MINUTE,
       NULL,
       5000 - (seq % 1000),
       1
FROM seed_seq_100k
WHERE seq <= 1000;

-- follower pool posts: 10,000 follower users x 5 posts
INSERT INTO posts (
    content,
    created_at,
    updated_at,
    deleted_at,
    like_count,
    user_id
)
SELECT CONCAT('follower pool post #', u.seq, '-', o.n, ' #feed #follower'),
       TIMESTAMP('2025-04-01 00:00:00')
           + INTERVAL (u.seq % 45) DAY
           + INTERVAL ((u.seq * 17 + o.n * 97) % 86400) SECOND,
       TIMESTAMP('2025-04-01 00:00:00')
           + INTERVAL (u.seq % 45) DAY
           + INTERVAL ((u.seq * 17 + o.n * 97) % 86400) SECOND,
       NULL,
       (u.seq * o.n) % 300,
       u.seq
FROM seed_seq_140001 u
CROSS JOIN seed_post_offsets_5 o
WHERE u.seq BETWEEN 2 AND 10001;

-- general pool posts: 20,000 general users x 3 posts
INSERT INTO posts (
    content,
    created_at,
    updated_at,
    deleted_at,
    like_count,
    user_id
)
SELECT CONCAT('general pool post #', u.seq, '-', o.n, ' #feed #general'),
       TIMESTAMP('2025-03-01 00:00:00')
           + INTERVAL (u.seq % 60) DAY
           + INTERVAL ((u.seq * 19 + o.n * 131) % 86400) SECOND,
       TIMESTAMP('2025-03-01 00:00:00')
           + INTERVAL (u.seq % 60) DAY
           + INTERVAL ((u.seq * 19 + o.n * 131) % 86400) SECOND,
       NULL,
       (u.seq * o.n) % 150,
       u.seq
FROM seed_seq_140001 u
CROSS JOIN seed_post_offsets_3 o
WHERE u.seq BETWEEN 100002 AND 120001;

INSERT INTO post_images (
    image_url,
    created_at,
    deleted_at,
    post_id
)
SELECT CONCAT('https://cdn.dailyus.local/post/local-', p.post_id, '/1.jpg'),
       p.created_at,
       NULL,
       p.post_id
FROM posts p;

INSERT INTO post_images (
    image_url,
    created_at,
    deleted_at,
    post_id
)
SELECT CONCAT('https://cdn.dailyus.local/post/local-', p.post_id, '/2.jpg'),
       p.created_at + INTERVAL 1 SECOND,
       NULL,
       p.post_id
FROM posts p
WHERE MOD(p.post_id, 2) = 0;

-- 100,000 followers for the celebrity account
INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT seq,
       1,
       TIMESTAMP('2025-05-15 00:00:00') + INTERVAL (seq % 30) DAY
FROM seed_seq_140001
WHERE seq BETWEEN 2 AND 100001;

-- background follow graph excluding the celebrity target
INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT u.seq,
       ((u.seq + o.n * 1543) % 140000) + 2,
       TIMESTAMP('2025-02-01 00:00:00') + INTERVAL ((u.seq + o.n) % 90) DAY
FROM seed_seq_140001 u
CROSS JOIN seed_follow_offsets_8 o
WHERE u.seq BETWEEN 2 AND 140001
  AND ((u.seq + o.n * 1543) % 140000) + 2 <> u.seq;

UPDATE users u
LEFT JOIN (
    SELECT follower AS user_id, COUNT(*) AS followee_count
    FROM user_follow
    GROUP BY follower
) fwee
  ON fwee.user_id = u.user_id
LEFT JOIN (
    SELECT followee AS user_id, COUNT(*) AS follower_count
    FROM user_follow
    GROUP BY followee
) fwer
  ON fwer.user_id = u.user_id
SET u.followee_count = COALESCE(fwee.followee_count, 0),
    u.follower_count = COALESCE(fwer.follower_count, 0);

SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL
SELECT 'posts', COUNT(*) FROM posts
UNION ALL
SELECT 'post_images', COUNT(*) FROM post_images
UNION ALL
SELECT 'user_follow', COUNT(*) FROM user_follow;

SELECT user_id, email, follower_count, followee_count
FROM users
WHERE user_id IN (1, 2, 100001, 100002, 140001)
ORDER BY user_id;

DROP TABLE IF EXISTS seed_seq_140001;
DROP TABLE IF EXISTS seed_seq_100k;
DROP TABLE IF EXISTS seed_post_offsets_3;
DROP TABLE IF EXISTS seed_post_offsets_5;
DROP TABLE IF EXISTS seed_follow_offsets_8;
DROP TABLE IF EXISTS seed_digits;

SET SESSION foreign_key_checks = 1;
SET SESSION unique_checks = 1;
