-- 用户表
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '加密后的密码',
  `roles` varchar(255) NOT NULL COMMENT '用户角色，逗号分隔',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用，1为启用，0为禁用',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 插入一个默认的管理员用户，方便初次使用
-- 用户名: admin, 密码: admin@$$w0rd!Secur3_2025 (密码是被BCrypt加密后的字符串)
INSERT INTO `users` (`username`, `password`, `roles`, `enabled`) VALUES
('admin', '$2a$10$wGM.9BskeHT/p8Msp/0vuuXDCOaIKdJkW51Qg6M7zadfkawelz3O.', 'ROLE_ADMIN,ROLE_USER', 1);
