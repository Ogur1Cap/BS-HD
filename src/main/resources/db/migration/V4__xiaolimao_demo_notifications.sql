-- 演示账号 XiaoLiMao / 123456（若库中已有该用户名则跳过创建，避免覆盖用户自改密码）
INSERT INTO users (username, email, password_hash, created_at)
SELECT 'XiaoLiMao', 'xiaolimao@demo.delta', '$2a$10$la2xR0pgeWvw5P37xJgBy.N49/d7ljqQlJn4MJOaUSc9.PG5umQLS', NOW(6)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'XiaoLiMao');

INSERT INTO user_profiles (user_id, avatar, phone, bio, game_preference)
SELECT u.id, '', '13800138000', '俱乐部演示账号，欢迎体验护航预约、地图工具与通知中心。', '三角洲行动'
FROM users u
WHERE u.username = 'XiaoLiMao'
  AND NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id);

INSERT INTO user_settings (user_id, nickname, bio, notify_channels, notify_types, wechat, qq, weibo, updated_at)
SELECT u.id, '小狸猫', '偏好：机密局护航 · 零号大坝', 'site,email', 'order,system,message', 'XiaoLiMao_Club', '', '', NOW(6)
FROM users u
WHERE u.username = 'XiaoLiMao'
  AND NOT EXISTS (SELECT 1 FROM user_settings s WHERE s.user_id = u.id);

-- 一条进行中的演示订单，供订单类通知 relatedId 跳转
INSERT INTO orders (user_id, game, game_key, game_image, service_type, status, amount, player_id, player_name, start_time, created_at, updated_at)
SELECT u.id, '三角洲行动', 'delta', '', '机密护航', 'ongoing', 168.00, '1', '夜袭者', DATE_ADD(NOW(6), INTERVAL 6 HOUR), NOW(6), NOW(6)
FROM users u
WHERE u.username = 'XiaoLiMao'
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id);

-- 通知：系统 / 订单 / 消息，时间错开便于列表排序与筛选体验
INSERT INTO notifications (user_id, title, content, type, related_id, created_at)
SELECT u.id, '欢迎加入三角洲行动俱乐部', '小狸猫您好！完善资料与绑定联系方式，匹配打手更快、售后更顺畅。', 'system', NULL, DATE_SUB(NOW(6), INTERVAL 3 DAY)
FROM users u WHERE u.username = 'XiaoLiMao';

INSERT INTO notifications (user_id, title, content, type, related_id, created_at)
SELECT u.id, '您的护航订单已被接单', '打手「夜袭者」已接单，请按约定时间上号，如需改期请前往订单页操作。', 'order',
  (SELECT CAST(MAX(o.id) AS CHAR) FROM orders o WHERE o.user_id = u.id),
  DATE_SUB(NOW(6), INTERVAL 1 DAY)
FROM users u WHERE u.username = 'XiaoLiMao';

INSERT INTO notifications (user_id, title, content, type, related_id, created_at)
SELECT u.id, '地图工具数据已同步', '零号大坝官方 POI 与赛季说明已更新，可在「游戏地图」中查看与添加个人标记。', 'system', NULL, DATE_SUB(NOW(6), INTERVAL 10 HOUR)
FROM users u WHERE u.username = 'XiaoLiMao';

INSERT INTO notifications (user_id, title, content, type, related_id, created_at)
SELECT u.id, '活动提醒：周末护航优惠', '本周末指定套餐享折扣，详见护航服务页；有疑问可联系客服中心。', 'message', NULL, DATE_SUB(NOW(6), INTERVAL 3 HOUR)
FROM users u WHERE u.username = 'XiaoLiMao';

INSERT INTO notifications (user_id, title, content, type, related_id, created_at)
SELECT u.id, '订单即将开始', '您的订单将在近期开局，请提前检查账号与语音，避免延误。', 'order',
  (SELECT CAST(MAX(o.id) AS CHAR) FROM orders o WHERE o.user_id = u.id),
  DATE_SUB(NOW(6), INTERVAL 40 MINUTE)
FROM users u WHERE u.username = 'XiaoLiMao';

-- 将最早两条标为已读（欢迎、接单），其余未读便于红点体验
INSERT INTO notification_reads (notification_id, user_id, read_at)
SELECT t.id, t.user_id, DATE_SUB(NOW(6), INTERVAL 2 DAY)
FROM (
  SELECT n.id, n.user_id
  FROM notifications n
  INNER JOIN users u ON u.id = n.user_id AND u.username = 'XiaoLiMao'
  ORDER BY n.created_at ASC
  LIMIT 2
) AS t;
