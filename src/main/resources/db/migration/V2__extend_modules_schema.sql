CREATE TABLE map_pois (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    floor VARCHAR(20) NOT NULL,
    type VARCHAR(30) NOT NULL,
    modes VARCHAR(100),
    security_levels VARCHAR(100)
);

CREATE TABLE user_map_markers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    label VARCHAR(100) NOT NULL,
    note VARCHAR(255),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_user_map_markers_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE faq_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category VARCHAR(30) NOT NULL,
    question VARCHAR(300) NOT NULL,
    answer VARCHAR(2000) NOT NULL
);

CREATE TABLE players (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    avatar VARCHAR(255),
    rank_name VARCHAR(50),
    skills VARCHAR(255),
    win_rate DECIMAL(5,2),
    completed_orders INT NOT NULL DEFAULT 0,
    rating DECIMAL(3,2),
    price_per_hour DECIMAL(10,2),
    intro VARCHAR(500),
    tags VARCHAR(255)
);

CREATE TABLE support_tickets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    contact VARCHAR(100),
    problem_type VARCHAR(30) NOT NULL,
    emergency_level VARCHAR(30) NOT NULL,
    problem_desc VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_support_tickets_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE user_settings (
    user_id BIGINT PRIMARY KEY,
    nickname VARCHAR(50),
    bio VARCHAR(255),
    notify_channels VARCHAR(100),
    notify_types VARCHAR(100),
    wechat VARCHAR(100),
    qq VARCHAR(30),
    weibo VARCHAR(100),
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users(id)
);
