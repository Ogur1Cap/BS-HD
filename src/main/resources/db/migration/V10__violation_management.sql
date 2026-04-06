-- 添加用户风控字段
ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE users ADD COLUMN violation_count INT DEFAULT 0 NOT NULL;
ALTER TABLE users ADD COLUMN is_high_risk BOOLEAN DEFAULT FALSE NOT NULL;

-- 创建违规记录表
CREATE TABLE violations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    related_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL, -- PENDING (待处理/预警中), APPEALED (申诉中), RESOLVED (已处理)
    appeal_reason TEXT,
    admin_id BIGINT,
    admin_action VARCHAR(50), -- WARNING (警告), RESTRICT (限制功能), BAN (封号), DISMISS (撤销违规)
    admin_notes TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);