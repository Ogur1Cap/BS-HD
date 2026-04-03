-- 账号分级：0=顾客（默认），1=打手；打手绑定 players 表主键，与订单里的 player_id 字符串对应
ALTER TABLE users
    ADD COLUMN user_level TINYINT NOT NULL DEFAULT 0 COMMENT '0顾客 1打手',
    ADD COLUMN player_profile_id BIGINT NULL COMMENT '打手绑定的展示档案 players.id';

ALTER TABLE users
    ADD CONSTRAINT fk_users_player_profile FOREIGN KEY (player_profile_id) REFERENCES players (id) ON DELETE SET NULL;

-- 打手演示账号（密码 123456，与种子脚本一致），绑定「夜袭者」
INSERT INTO users (username, email, password_hash, created_at, user_level, player_profile_id)
SELECT 'DaShou_YeXi', 'dashou_yexi@demo.delta', '$2a$10$la2xR0pgeWvw5P37xJgBy.N49/d7ljqQlJn4MJOaUSc9.PG5umQLS', NOW(6), 1, p.id
FROM players p
WHERE p.name = '夜袭者'
  AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'DaShou_YeXi')
LIMIT 1;

INSERT INTO user_profiles (user_id, avatar, phone, bio, game_preference)
SELECT u.id, '', '', '认证打手 · 夜袭者', '三角洲行动'
FROM users u
WHERE u.username = 'DaShou_YeXi'
  AND NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id);

INSERT INTO user_settings (user_id, nickname, bio, notify_channels, notify_types, wechat, qq, weibo, updated_at)
SELECT u.id, '打手-夜袭者', '机密局 / 突击专精', 'site,email', 'order,system,message', '', '', '', NOW(6)
FROM users u
WHERE u.username = 'DaShou_YeXi'
  AND NOT EXISTS (SELECT 1 FROM user_settings s WHERE s.user_id = u.id);

-- 将演示顾客 XiaoLiMao 的订单指定给「夜袭者」，便于打手账号登录后能看到待接单
UPDATE orders o
INNER JOIN users u ON o.user_id = u.id AND u.username = 'XiaoLiMao'
SET o.player_id = (SELECT CAST(p.id AS CHAR) FROM players p WHERE p.name = '夜袭者' LIMIT 1),
    o.player_name = '夜袭者'
WHERE EXISTS (SELECT 1 FROM players p2 WHERE p2.name = '夜袭者');

-- 演示单改为「待接单」且状态值为合法枚举，便于打手工作台演示接单流程
UPDATE orders o
INNER JOIN users u ON o.user_id = u.id AND u.username = 'XiaoLiMao'
SET o.status = 'PENDING',
    o.updated_at = NOW(6)
WHERE o.player_id = (SELECT CAST(p.id AS CHAR) FROM players p WHERE p.name = '夜袭者' LIMIT 1);
