CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    follower_count BIGINT NOT NULL DEFAULT 0,
    followee_count BIGINT NOT NULL DEFAULT 0,
    intro VARCHAR(500) NULL,
    profile_image VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_nickname UNIQUE (nickname)
);
CREATE TABLE posts (
    post_id BIGINT NOT NULL AUTO_INCREMENT,
    content TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    like_count BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_posts_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE post_images (
    post_image_id BIGINT NOT NULL AUTO_INCREMENT,
    image_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    post_id BIGINT NOT NULL,
    PRIMARY KEY (post_image_id),
    CONSTRAINT fk_post_images_post_id
        FOREIGN KEY (post_id) REFERENCES posts (post_id)
);

CREATE TABLE hashtag (
    hashtag_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (hashtag_id),
    CONSTRAINT uk_hashtag_name UNIQUE (name)
);

CREATE TABLE hashtag_posts (
    hashtag_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (hashtag_id, post_id),
    CONSTRAINT fk_hashtag_posts_hashtag_id
        FOREIGN KEY (hashtag_id) REFERENCES hashtag (hashtag_id),
    CONSTRAINT fk_hashtag_posts_post_id
        FOREIGN KEY (post_id) REFERENCES posts (post_id)
);

CREATE TABLE user_follow (
    follower BIGINT NOT NULL,
    followee BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower, followee),
    CONSTRAINT fk_user_follow_follower
        FOREIGN KEY (follower) REFERENCES users (user_id),
    CONSTRAINT fk_user_follow_followee
        FOREIGN KEY (followee) REFERENCES users (user_id)
);

CREATE TABLE user_groups (
    group_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    intro VARCHAR(500) NULL,
    group_image VARCHAR(500) NULL,
    owner_id BIGINT NOT NULL,
    member_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (group_id),
    CONSTRAINT fk_groups_owner_id
        FOREIGN KEY (owner_id) REFERENCES users (user_id)
);

CREATE TABLE group_members (
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk_group_members_group_id
        FOREIGN KEY (group_id) REFERENCES user_groups (group_id),
    CONSTRAINT fk_group_members_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);
