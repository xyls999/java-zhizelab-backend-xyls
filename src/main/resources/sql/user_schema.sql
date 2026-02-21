-- User table for fresh deployment.
-- If your existing project already has news_user with historical data, do NOT recreate it.

CREATE TABLE IF NOT EXISTS news_user (
    uid INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    user_pwd VARCHAR(64) NOT NULL,
    nick_name VARCHAR(64) DEFAULT NULL,
    version INT NOT NULL DEFAULT 1,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_news_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
