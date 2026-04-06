-- dailyus local sample data seed script
-- target: MySQL 8.x
--
-- local scale
--   users         : 10,000
--   posts         : 50,000
--   post_images   : about 80,000
--   user_groups   : 2,000
--   group_members : 40,000
--   user_follow   : about 300,000 + hub accounts
--
-- purpose
--   1. fast local seed for API/manual testing
--   2. preserve feed-related data shape without huge disk usage

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
DROP TABLE IF EXISTS seed_local_offsets_19;
DROP TABLE IF EXISTS seed_local_offsets_30;
DROP TABLE IF EXISTS seed_local_seq_10k;
DROP TABLE IF EXISTS seed_local_seq_50k;

CREATE TABLE seed_local_digits (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_digits (n)
VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

CREATE TABLE seed_local_offsets_19 (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_offsets_19 (n)
VALUES
    (1), (2), (3), (4), (5), (6), (7), (8), (9), (10),
    (11), (12), (13), (14), (15), (16), (17), (18), (19);

CREATE TABLE seed_local_offsets_30 (
    n TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_offsets_30 (n)
VALUES
    (1), (2), (3), (4), (5), (6), (7), (8), (9), (10),
    (11), (12), (13), (14), (15), (16), (17), (18), (19), (20),
    (21), (22), (23), (24), (25), (26), (27), (28), (29), (30);

CREATE TABLE seed_local_seq_10k (
    seq INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_seq_10k (seq)
SELECT ones.n
     + tens.n * 10
     + hundreds.n * 100
     + thousands.n * 1000
     + 1 AS seq
FROM seed_local_digits ones
CROSS JOIN seed_local_digits tens
CROSS JOIN seed_local_digits hundreds
CROSS JOIN seed_local_digits thousands
WHERE ones.n + tens.n * 10 + hundreds.n * 100 + thousands.n * 1000 < 10000;

CREATE TABLE seed_local_seq_50k (
    seq INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO seed_local_seq_50k (seq)
SELECT seq
FROM seed_local_seq_10k
UNION ALL
SELECT seq + 10000
FROM seed_local_seq_10k
UNION ALL
SELECT seq + 20000
FROM seed_local_seq_10k
UNION ALL
SELECT seq + 30000
FROM seed_local_seq_10k
UNION ALL
SELECT seq + 40000
FROM seed_local_seq_10k;

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
SELECT CONCAT('local-user', LPAD(seq, 5, '0'), '@dailyus.local'),
       '$2a$10$abcdefghijklmnopqrstuuC0D9Q7zQ8mL7b6Yk6lJY9m8s1Qe2r3W',
       CONCAT('local_user_', LPAD(seq, 5, '0')),
       0,
       0,
       CONCAT('https://cdn.dailyus.local/profile/local-', seq, '.png'),
       TIMESTAMP('2025-01-01 00:00:00') + INTERVAL (seq % 120) DAY + INTERVAL (seq % 86400) SECOND,
       TIMESTAMP('2025-01-01 00:00:00') + INTERVAL (seq % 120) DAY + INTERVAL (seq % 86400) SECOND,
       NULL
FROM seed_local_seq_10k;

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
SELECT CONCAT('local_group_', LPAD(seq, 4, '0')),
       CONCAT('DailyUs local group #', seq),
       CONCAT('https://cdn.dailyus.local/group/local-', seq, '.png'),
       ((seq * 13) % 10000) + 1,
       0,
       TIMESTAMP('2025-02-01 00:00:00') + INTERVAL (seq % 90) DAY + INTERVAL (seq % 86400) SECOND,
       TIMESTAMP('2025-02-01 00:00:00') + INTERVAL (seq % 90) DAY + INTERVAL (seq % 86400) SECOND,
       NULL
FROM seed_local_seq_10k
WHERE seq <= 2000;

-- 20 members per group = 40,000 memberships
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
       ((g.group_id * 97 + o.n * 137) % 10000) + 1,
       g.created_at + INTERVAL o.n MINUTE
FROM user_groups g
CROSS JOIN seed_local_offsets_19 o;

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
           ' by user ', ((seq * 37) % 10000) + 1,
           ' #dailyus #local'
       ),
       TIMESTAMP('2025-03-01 00:00:00') + INTERVAL (seq % 60) DAY + INTERVAL (seq % 86400) SECOND,
       TIMESTAMP('2025-03-01 00:00:00') + INTERVAL (seq % 60) DAY + INTERVAL (seq % 86400) SECOND,
       NULL,
       CASE
           WHEN seq <= 100 THEN 3000 - seq
           WHEN MOD(seq, 1000) = 0 THEN 1000
           ELSE MOD(seq * 17, 300)
       END,
       ((seq * 37) % 10000) + 1
FROM seed_local_seq_50k;

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

-- 30 follows per user = 300,000 base edges
INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT u.seq,
       ((u.seq + o.n * 313) % 10000) + 1,
       TIMESTAMP('2025-04-01 00:00:00') + INTERVAL ((u.seq + o.n) % 45) DAY
FROM seed_local_seq_10k u
CROSS JOIN seed_local_offsets_30 o
WHERE ((u.seq + o.n * 313) % 10000) + 1 <> u.seq;

-- hub users
INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT u.seq,
       hub.hub_user_id,
       TIMESTAMP('2025-04-15 00:00:00') + INTERVAL (u.seq % 30) DAY
FROM seed_local_seq_10k u
JOIN (
    SELECT 1 AS hub_user_id
    UNION ALL SELECT 2
    UNION ALL SELECT 3
) hub
  ON u.seq BETWEEN hub.hub_user_id + 1 AND 7000 + hub.hub_user_id;

-- power followers
INSERT IGNORE INTO user_follow (
    follower,
    followee,
    created_at
)
SELECT pf.power_user_id,
       target.seq,
       TIMESTAMP('2025-05-01 00:00:00') + INTERVAL (target.seq % 20) DAY
FROM (
    SELECT 11 AS power_user_id
    UNION ALL SELECT 12
    UNION ALL SELECT 13
) pf
JOIN seed_local_seq_10k target
  ON target.seq <= 5000
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

DROP TABLE IF EXISTS seed_local_seq_50k;
DROP TABLE IF EXISTS seed_local_seq_10k;
DROP TABLE IF EXISTS seed_local_offsets_30;
DROP TABLE IF EXISTS seed_local_offsets_19;
DROP TABLE IF EXISTS seed_local_digits;

SET SESSION foreign_key_checks = 1;
SET SESSION unique_checks = 1;
