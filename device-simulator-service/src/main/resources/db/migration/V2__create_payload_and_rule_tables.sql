-- 报文仓库表，用于存储所有可复用的报文内容
CREATE TABLE `payload_repository` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `payload_key` VARCHAR(255) NOT NULL COMMENT '报文的唯一业务标识',
  `group_key` VARCHAR(255) NULL COMMENT '报文所属的组标识，用于周期性上报等场景',
  `payload_type` VARCHAR(50) NOT NULL COMMENT '报文类型, 如: json, hex',
  `content` TEXT NOT NULL COMMENT '报文内容 (JSON字符串或十六进制字符串)',
  `description` VARCHAR(500) NULL COMMENT '描述',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_payload_key` (`payload_key` ASC)
) COMMENT = '可复用报文仓库';

-- 请求响应规则表，用于定义请求与一个或多个响应之间的映射关系
CREATE TABLE `request_response_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_group` VARCHAR(255) NOT NULL COMMENT '规则所属的组标识',
  `request_key` VARCHAR(255) NOT NULL COMMENT '请求报文的key (外键关联到payload_repository)',
  `response_key` VARCHAR(255) NOT NULL COMMENT '响应报文的key (外键关联到payload_repository)',
  `description` VARCHAR(500) NULL COMMENT '描述',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_rule_group` (`rule_group` ASC)
) COMMENT = '请求响应映射规则表';
