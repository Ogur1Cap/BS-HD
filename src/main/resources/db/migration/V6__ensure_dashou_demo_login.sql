-- 修复/补全打手演示账号 DaShou_YeXi：确保一定能用 123456 登录，并绑定打手档案
SET @dashou_pass := '$2a$10$la2xR0pgeWvw5P37xJgBy.N49/d7ljqQlJn4MJOaUSc9.PG5umQLS';

-- 优先绑定「夜袭者」，否则退化为 players 表中任意一条，避免 player_profile_id 为空导致工作台不可用
SET @dashou_player_id := (SELECT id FROM players WHERE name = '夜袭者' ORDER BY id LIMIT 1);
SET @dashou_player_id := IFNULL(@dashou_player_id, (SELECT MIN(id) FROM players));

INSERT INTO users (username, email, password_hash, created_at, user_level, player_profile_id)
SELECT 'DaShou_YeXi', 'dashou_yexi@demo.delta', @dashou_pass, NOW(6), 1, @dashou_player_id
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'DaShou_YeXi');

-- 已存在时同步密码与等级（解决手工建号、错误哈希或 V5 未写入等问题）
UPDATE users
SET password_hash = @dashou_pass,
    user_level = 1,
    player_profile_id = COALESCE(@dashou_player_id, player_profile_id),
    email = 'dashou_yexi@demo.delta'
WHERE username = 'DaShou_YeXi';

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
