-- Social feature schema
-- Execute manually on your database (sm_db) before running the new APIs.

CREATE TABLE IF NOT EXISTS social_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    text_content TEXT NULL,
    image_url VARCHAR(500) NULL,
    code_language VARCHAR(64) NULL,
    code_content MEDIUMTEXT NULL,
    like_count INT NOT NULL DEFAULT 0,
    comment_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_social_post_user_id (user_id),
    INDEX idx_social_post_create_time (create_time),
    CONSTRAINT fk_social_post_user FOREIGN KEY (user_id) REFERENCES news_user(uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS social_post_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_social_post_like_post_user (post_id, user_id),
    INDEX idx_social_post_like_user_id (user_id),
    CONSTRAINT fk_social_post_like_post FOREIGN KEY (post_id) REFERENCES social_post(id),
    CONSTRAINT fk_social_post_like_user FOREIGN KEY (user_id) REFERENCES news_user(uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS social_post_reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    content TEXT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_social_post_reply_post_id (post_id),
    INDEX idx_social_post_reply_user_id (user_id),
    CONSTRAINT fk_social_post_reply_post FOREIGN KEY (post_id) REFERENCES social_post(id),
    CONSTRAINT fk_social_post_reply_user FOREIGN KEY (user_id) REFERENCES news_user(uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
