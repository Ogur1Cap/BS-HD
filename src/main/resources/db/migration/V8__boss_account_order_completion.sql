-- BOSS 账号（user_level=2）与订单「完成申请」说明字段
SET @boss_pass := '$2a$10$la2xR0pgeWvw5P37xJgBy.N49/d7ljqQlJn4MJOaUSc9.PG5umQLS';

ALTER TABLE orders
    ADD COLUMN completion_request_note VARCHAR(500) NULL COMMENT '打手申请完成时填写的说明' AFTER refund_reason;

-- 最高权限演示账号：用户名 BOSS_Delta，密码与演示一致 123456
INSERT INTO users (username, email, password_hash, created_at, user_level, player_profile_id)
SELECT 'BOSS_Delta', 'boss@demo.delta', @boss_pass, NOW(6), 2, NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'BOSS_Delta');

UPDATE users
SET password_hash = @boss_pass,
    user_level     = 2,
    email          = 'boss@demo.delta'
WHERE username = 'BOSS_Delta';

INSERT INTO user_profiles (user_id, avatar, phone, bio, game_preference)
SELECT u.id, '', '', '平台管理员 · BOSS', '全站'
FROM users u
WHERE u.username = 'BOSS_Delta'
  AND NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id);

INSERT INTO user_settings (user_id, nickname, bio, notify_channels, notify_types, wechat, qq, weibo, updated_at)
SELECT u.id, 'BOSS-三角洲', '订单审核与调度', 'site,email', 'order,system,message', '', '', '', NOW(6)
FROM users u
WHERE u.username = 'BOSS_Delta'
  AND NOT EXISTS (SELECT 1 FROM user_settings s WHERE s.user_id = u.id);
