-- dailyus local sample data seed script
-- target: MySQL 8.x
--
-- local scale
--   users         : 300,000
--   posts         : 3,000,000
--   avg posts/user: 10
--   post_images   : about 4,800,000
--   user_groups   : 30,000
--   group_members : about 750,000
--   user_follow   : about 10,800,000 base edges + hub accounts
--
-- purpose
--   1. local/staging feed performance and pagination testing
--   2. deterministic social graph with moderate clustering
--   3. exactly 10 posts per user on average

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

DROP TABLE IF EXISTS seed_local_digits;
DROP TABLE IF EXISTS seed_local_offsets_24;
DROP TABLE IF EXISTS seed_local_offsets_36;
DROP TABLE IF EXISTS seed_local_seq_100k;
DROP TABLE IF EXISTS seed_local_seq_300k;
DROP TABLE IF EXISTS seed_local_seq_3m;

CREATE TABLE seed_local_digits (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_digits (n)
VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

CREATE TABLE seed_local_offsets_24 (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_offsets_24 (n)
VALUES
    (1), (2), (3), (4), (5), (6), (7), (8), (9), (10),
    (11), (12), (13), (14), (15), (16), (17), (18), (19), (20),
    (21), (22), (23), (24);

CREATE TABLE seed_local_offsets_36 (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_offsets_36 (n)
VALUES
    (1), (2), (3), (4), (5), (6), (7), (8), (9), (10),
    (11), (12), (13), (14), (15), (16), (17), (18), (19), (20),
    (21), (22), (23), (24), (25), (26), (27), (28), (29), (30),
    (31), (32), (33), (34), (35), (36);

CREATE TABLE seed_local_seq_100k (
    seq INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_seq_100k (seq)
SELECT ones.n
     + tens.n * 10
     + hundreds.n * 100
     + thousands.n * 1000
     + ten_thousands.n * 10000
     + 1 AS seq
FROM seed_local_digits ones
CROSS JOIN seed_local_digits tens
CROSS JOIN seed_local_digits hundreds
CROSS JOIN seed_local_digits thousands
CROSS JOIN seed_local_digits ten_thousands
WHERE ones.n
    + tens.n * 10
    + hundreds.n * 100
    + thousands.n * 1000
    + ten_thousands.n * 10000 < 100000;

CREATE TABLE seed_local_seq_300k (
    seq INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_seq_300k (seq)
SELECT seq
FROM seed_local_seq_100k
UNION ALL
SELECT seq + 100000
FROM seed_local_seq_100k
UNION ALL
SELECT seq + 200000
FROM seed_local_seq_100k;

CREATE TABLE seed_local_seq_3m (
    seq INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_seq_3m (seq)
SELECT seq
FROM seed_local_seq_300k
UNION ALL
SELECT seq + 300000
FROM seed_local_seq_300k
UNION ALL
SELECT seq + 600000
FROM seed_local_seq_300k
UNION ALL
SELECT seq + 900000
FROM seed_local_seq_300k
UNION ALL
SELECT seq + 1200000
FROM seed_local_seq_300k
UNION ALL
SELECT seq + 1500000
FROM seed_local_seq_300k
UNION ALL
SELECT seq + 1800000
FROM seed_local_seq_300k
UNION ALL
SELECT seq + 2100000
FROM seed_local_seq_300k
UNION ALL
SELECT seq + 2400000
FROM seed_local_seq_300k
UNION ALL
SELECT seq + 2700000
FROM seed_local_seq_300k;

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
SELECT CONCAT('local-user', LPAD(seq, 6, '0'), '@dailyus.local'),
       '$2a$10$N9/gNWwi6iIRM5/xzdAi3u4H8RZCxqSC/b956LzNv1wUwEALulsQK',
       CONCAT('local_user_', LPAD(seq, 6, '0')),
       0,
       0,
       CONCAT('https://cdn.dailyus.local/profile/local-', seq, '.png'),
       TIMESTAMP('2024-01-01 00:00:00') + INTERVAL (seq % 540) DAY + INTERVAL (seq % 86400) SECOND,
       TIMESTAMP('2024-01-01 00:00:00') + INTERVAL (seq % 540) DAY + INTERVAL (seq % 86400) SECOND,
       NULL
FROM seed_local_seq_300k;

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
SELECT CONCAT('local_group_', LPAD(seq, 5, '0')),
       CONCAT('DailyUs local group #', seq),
       CONCAT('https://cdn.dailyus.local/group/local-', seq, '.png'),
       ((seq * 17) % 300000) + 1,
       0,
       TIMESTAMP('2024-06-01 00:00:00') + INTERVAL (seq % 240) DAY + INTERVAL (seq % 86400) SECOND,
       TIMESTAMP('2024-06-01 00:00:00') + INTERVAL (seq % 240) DAY + INTERVAL (seq % 86400) SECOND,
       NULL
FROM seed_local_seq_300k
WHERE seq <= 30000;

-- 25 members per group including owner = about 750,000 memberships
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
       ((g.group_id * 97 + o.n * 1543) % 300000) + 1,
       g.created_at + INTERVAL o.n MINUTE
FROM user_groups g
CROSS JOIN seed_local_offsets_24 o;

INSERT INTO posts (
    content,
    created_at,
    updated_at,
    deleted_at,
    like_count,
    user_id
)
SELECT CONCAT(
           'local sample post #', seq,
           ' by user ', ((seq - 1) % 300000) + 1,
           ' #dailyus #local'
       ),
       TIMESTAMP('2025-01-01 00:00:00')
           + INTERVAL (seq % 180) DAY
           + INTERVAL (((seq * 13) + (((seq - 1) % 300000) * 7)) % 86400) SECOND,
       TIMESTAMP('2025-01-01 00:00:00')
           + INTERVAL (seq % 180) DAY
           + INTERVAL (((seq * 13) + (((seq - 1) % 300000) * 7)) % 86400) SECOND,
       NULL,
       CASE
           WHEN seq <= 3000 THEN 15000 - (seq % 3000)
           WHEN MOD(seq, 50000) = 0 THEN 5000
           WHEN MOD(seq, 5000) = 0 THEN 1200
           ELSE MOD(seq * 17, 600)
       END,
       ((seq - 1) % 300000) + 1
FROM seed_local_seq_3m;

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

INSERT INTO post_images (
    image_url,
    created_at,
    deleted_at,
    post_id
)
SELECT CONCAT('https://cdn.dailyus.local/post/local-', p.post_id, '/3.jpg'),
       p.created_at + INTERVAL 2 SECOND,
       NULL,
       p.post_id
FROM posts p
WHERE MOD(p.post_id, 5) = 0;

-- 36 follows per user = about 10.8M base edges
INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT u.seq,
       ((u.seq + o.n * 7919) % 300000) + 1,
       TIMESTAMP('2025-03-01 00:00:00') + INTERVAL ((u.seq + o.n) % 120) DAY
FROM seed_local_seq_300k u
CROSS JOIN seed_local_offsets_36 o
WHERE ((u.seq + o.n * 7919) % 300000) + 1 <> u.seq;

-- hub users for discoverability-heavy feeds
INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT u.seq,
       hub.hub_user_id,
       TIMESTAMP('2025-04-15 00:00:00') + INTERVAL (u.seq % 45) DAY
FROM seed_local_seq_300k u
JOIN (
    SELECT 1 AS hub_user_id
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
) hub
  ON u.seq BETWEEN hub.hub_user_id + 1 AND 210000 + hub.hub_user_id;

-- a few power users following many accounts
INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT pf.power_user_id,
       target.seq,
       TIMESTAMP('2025-05-01 00:00:00') + INTERVAL (target.seq % 30) DAY
FROM (
    SELECT 11 AS power_user_id
    UNION ALL SELECT 12
    UNION ALL SELECT 13
) pf
JOIN seed_local_seq_300k target
  ON target.seq <= 90000
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

DROP TABLE IF EXISTS seed_local_seq_3m;
DROP TABLE IF EXISTS seed_local_seq_300k;
DROP TABLE IF EXISTS seed_local_seq_100k;
DROP TABLE IF EXISTS seed_local_offsets_36;
DROP TABLE IF EXISTS seed_local_offsets_24;
DROP TABLE IF EXISTS seed_local_digits;

SET SESSION foreign_key_checks = 1;
SET SESSION unique_checks = 1;
