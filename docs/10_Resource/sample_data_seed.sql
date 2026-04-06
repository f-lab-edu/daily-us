-- dailyus sample data seed script
-- target: MySQL 8.x
--
-- balanced scale for feed / social graph testing
--   users         : 1,000,000
--   posts         : 6,000,000
--   post_images   : about 9,600,000
--   user_groups   : 200,000
--   group_members : 6,000,000
--   user_follow   : about 120,000,000 base edges + hub accounts
--
-- notes
--   1. run only on a dedicated local or staging MySQL 8 database.
--   2. this script is deterministic. after TRUNCATE, shape stays stable.
--   3. it is still expensive. expect long execution time and large disk usage.
--   4. optional global tuning such as
--      SET GLOBAL innodb_flush_log_at_trx_commit = 2;
--      should be executed separately by a privileged account if needed.
--   5. this script avoids MySQL TEMPORARY TABLE reopen issues by using helper base tables.

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

DROP TABLE IF EXISTS seed_helper_digits;
DROP TABLE IF EXISTS seed_helper_offsets_29;
DROP TABLE IF EXISTS seed_helper_offsets_120;
DROP TABLE IF EXISTS seed_helper_seq_1m;
DROP TABLE IF EXISTS seed_helper_seq_6m;

CREATE TABLE seed_helper_digits (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_helper_digits (n)
VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

CREATE TABLE seed_helper_offsets_29 (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_helper_offsets_29 (n)
VALUES
    (1), (2), (3), (4), (5), (6), (7), (8), (9), (10),
    (11), (12), (13), (14), (15), (16), (17), (18), (19), (20),
    (21), (22), (23), (24), (25), (26), (27), (28), (29);

CREATE TABLE seed_helper_offsets_120 (
    n SMALLINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_helper_offsets_120 (n)
SELECT ones.n + tens.n * 10 + hundreds.n * 100
FROM seed_helper_digits ones
CROSS JOIN seed_helper_digits tens
CROSS JOIN seed_helper_digits hundreds
WHERE ones.n + tens.n * 10 + hundreds.n * 100 BETWEEN 1 AND 120;

CREATE TABLE seed_helper_seq_1m (
    seq INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_helper_seq_1m (seq)
SELECT ones.n
     + tens.n * 10
     + hundreds.n * 100
     + thousands.n * 1000
     + ten_thousands.n * 10000
     + hundred_thousands.n * 100000
     + 1 AS seq
FROM seed_helper_digits ones
CROSS JOIN seed_helper_digits tens
CROSS JOIN seed_helper_digits hundreds
CROSS JOIN seed_helper_digits thousands
CROSS JOIN seed_helper_digits ten_thousands
CROSS JOIN seed_helper_digits hundred_thousands
WHERE ones.n
    + tens.n * 10
    + hundreds.n * 100
    + thousands.n * 1000
    + ten_thousands.n * 10000
    + hundred_thousands.n * 100000 < 1000000;

CREATE TABLE seed_helper_seq_6m (
    seq INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_helper_seq_6m (seq)
SELECT seq
FROM seed_helper_seq_1m
UNION ALL
SELECT seq + 1000000
FROM seed_helper_seq_1m
UNION ALL
SELECT seq + 2000000
FROM seed_helper_seq_1m
UNION ALL
SELECT seq + 3000000
FROM seed_helper_seq_1m
UNION ALL
SELECT seq + 4000000
FROM seed_helper_seq_1m
UNION ALL
SELECT seq + 5000000
FROM seed_helper_seq_1m;

INSERT INTO users (
    email,
    password,
    nickname,
    follower_count,
    followee_count,
    profile_image,
    created_at,
    updated_at,
    deleted_at
)
SELECT CONCAT('user', LPAD(seq, 7, '0'), '@dailyus.local'),
       '$2a$10$abcdefghijklmnopqrstuuC0D9Q7zQ8mL7b6Yk6lJY9m8s1Qe2r3W',
       CONCAT('user_', LPAD(seq, 7, '0')),
       0,
       0,
       CONCAT('https://cdn.dailyus.local/profile/', seq, '.png'),
       TIMESTAMP('2024-01-01 00:00:00') + INTERVAL (seq % 730) DAY + INTERVAL (seq % 86400) SECOND,
       TIMESTAMP('2024-01-01 00:00:00') + INTERVAL (seq % 730) DAY + INTERVAL (seq % 86400) SECOND,
       NULL
FROM seed_helper_seq_1m;

INSERT INTO user_groups (
    name,
    intro,
    group_image,
    owner_id,
    member_count,
    created_at,
    updated_at,
    deleted_at
)
SELECT CONCAT('group_', LPAD(seq, 6, '0')),
       CONCAT('DailyUs sample group #', seq),
       CONCAT('https://cdn.dailyus.local/group/', seq, '.png'),
       ((seq * 13) % 1000000) + 1,
       0,
       TIMESTAMP('2024-03-01 00:00:00') + INTERVAL (seq % 365) DAY + INTERVAL (seq % 86400) SECOND,
       TIMESTAMP('2024-03-01 00:00:00') + INTERVAL (seq % 365) DAY + INTERVAL (seq % 86400) SECOND,
       NULL
FROM seed_helper_seq_1m
WHERE seq <= 200000;

INSERT INTO group_members (
    group_id,
    user_id,
    created_at
)
SELECT group_id,
       owner_id,
       created_at
FROM user_groups;

INSERT IGNORE INTO group_members (
    group_id,
    user_id,
    created_at
)
SELECT g.group_id,
       ((g.group_id * 97 + o.n * 1543) % 1000000) + 1,
       g.created_at + INTERVAL o.n MINUTE
FROM user_groups g
CROSS JOIN seed_helper_offsets_29 o;

INSERT INTO posts (
    content,
    created_at,
    updated_at,
    deleted_at,
    like_count,
    user_id
)
SELECT CONCAT(
           'sample post #', seq,
           ' by user ', ((seq * 37) % 1000000) + 1,
           ' #dailyus #sample'
       ),
       TIMESTAMP('2025-01-01 00:00:00') + INTERVAL (seq % 365) DAY + INTERVAL (seq % 86400) SECOND,
       TIMESTAMP('2025-01-01 00:00:00') + INTERVAL (seq % 365) DAY + INTERVAL (seq % 86400) SECOND,
       NULL,
       CASE
           WHEN seq <= 5000 THEN 50000 - (seq % 5000)
           WHEN MOD(seq, 10000) = 0 THEN 10000
           WHEN MOD(seq, 1000) = 0 THEN 3000
           ELSE MOD(seq * 17, 900)
       END,
       ((seq * 37) % 1000000) + 1
FROM seed_helper_seq_6m;

INSERT INTO post_images (
    image_url,
    created_at,
    deleted_at,
    post_id
)
SELECT CONCAT('https://cdn.dailyus.local/post/', p.post_id, '/1.jpg'),
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
SELECT CONCAT('https://cdn.dailyus.local/post/', p.post_id, '/2.jpg'),
       p.created_at + INTERVAL 1 SECOND,
       NULL,
       p.post_id
FROM posts p
WHERE MOD(p.post_id, 2) = 0;

INSERT INTO post_images (
    image_url,
    created_at,
    deleted_at,
    post_id
)
SELECT CONCAT('https://cdn.dailyus.local/post/', p.post_id, '/3.jpg'),
       p.created_at + INTERVAL 2 SECOND,
       NULL,
       p.post_id
FROM posts p
WHERE MOD(p.post_id, 5) = 0;

INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT u.seq,
       ((u.seq + o.n * 7919) % 1000000) + 1,
       TIMESTAMP('2025-06-01 00:00:00') + INTERVAL ((u.seq + o.n) % 180) DAY
FROM seed_helper_seq_1m u
CROSS JOIN seed_helper_offsets_120 o
WHERE ((u.seq + o.n * 7919) % 1000000) + 1 <> u.seq;

INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT u.seq,
       hub.hub_user_id,
       TIMESTAMP('2025-07-01 00:00:00') + INTERVAL (u.seq % 90) DAY
FROM seed_helper_seq_1m u
JOIN (
    SELECT 1 AS hub_user_id
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
) hub
  ON u.seq BETWEEN hub.hub_user_id + 1 AND 700000 + hub.hub_user_id;

INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT pf.power_user_id,
       target.seq,
       TIMESTAMP('2025-08-01 00:00:00') + INTERVAL (target.seq % 60) DAY
FROM (
    SELECT 11 AS power_user_id
    UNION ALL SELECT 12
    UNION ALL SELECT 13
    UNION ALL SELECT 14
    UNION ALL SELECT 15
) pf
JOIN seed_helper_seq_1m target
  ON target.seq <= 400000
 AND target.seq <> pf.power_user_id;

UPDATE user_groups g
JOIN (
    SELECT group_id, COUNT(*) AS member_count
    FROM group_members
    GROUP BY group_id
) gm
  ON gm.group_id = g.group_id
SET g.member_count = gm.member_count;

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
SELECT 'user_groups', COUNT(*) FROM user_groups
UNION ALL
SELECT 'group_members', COUNT(*) FROM group_members
UNION ALL
SELECT 'posts', COUNT(*) FROM posts
UNION ALL
SELECT 'post_images', COUNT(*) FROM post_images
UNION ALL
SELECT 'user_follow', COUNT(*) FROM user_follow;

SELECT user_id, follower_count, followee_count
FROM users
WHERE user_id BETWEEN 1 AND 15
ORDER BY user_id;

DROP TABLE IF EXISTS seed_helper_seq_6m;
DROP TABLE IF EXISTS seed_helper_seq_1m;
DROP TABLE IF EXISTS seed_helper_offsets_120;
DROP TABLE IF EXISTS seed_helper_offsets_29;
DROP TABLE IF EXISTS seed_helper_digits;

SET SESSION foreign_key_checks = 1;
SET SESSION unique_checks = 1;
