-- Hibernate 将 Java int 映射为 MySQL INT；原 V5 使用 TINYINT 会导致 ddl-auto=validate 失败
ALTER TABLE users
    MODIFY COLUMN user_level INT NOT NULL DEFAULT 0 COMMENT '0顾客 1打手';
