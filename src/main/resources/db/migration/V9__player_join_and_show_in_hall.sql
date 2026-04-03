-- 用户申请成为打手（BOSS 审核）；大厅仅展示 show_in_hall=1 的打手
CREATE TABLE player_join_applications (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    status          VARCHAR(24)  NOT NULL COMMENT 'PENDING/APPROVED/REJECTED',
    display_name    VARCHAR(100) NOT NULL,
    intro           VARCHAR(500) NOT NULL,
    skills          VARCHAR(255) NULL,
    rank_name       VARCHAR(50)  NULL,
    tags            VARCHAR(255) NULL,
    price_per_hour  DECIMAL(10, 2) NULL,
    contact_note    VARCHAR(255) NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    reviewed_at     DATETIME(6)  NULL,
    reviewer_user_id BIGINT      NULL,
    reject_reason   VARCHAR(500) NULL,
    CONSTRAINT fk_pja_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_pja_status ON player_join_applications (status);
CREATE INDEX idx_pja_user ON player_join_applications (user_id);

ALTER TABLE players
    ADD COLUMN show_in_hall TINYINT(1) NOT NULL DEFAULT 1 COMMENT '0=不在大厅展示（如已解除打手资格）' AFTER tags;
