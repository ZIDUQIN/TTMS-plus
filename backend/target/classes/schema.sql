-- TTMS 数据库表结构 - 自动建表脚本
-- 使用 CREATE TABLE IF NOT EXISTS 确保重复执行安全

-- 角色表
CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) COMMENT '角色描述',
    `permissions` TEXT COMMENT '权限JSON数组',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `status` INT DEFAULT 0 COMMENT '账号状态: 0-正常 1-禁用',
    `theme` VARCHAR(20) DEFAULT 'white' COMMENT '用户偏好主题',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 员工表
CREATE TABLE IF NOT EXISTS `employee` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '员工ID',
    `employee_no` VARCHAR(50) NOT NULL COMMENT '工号',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name` VARCHAR(50) COMMENT '姓名',
    `phone` VARCHAR(20) COMMENT '手机号',
    `role_id` BIGINT COMMENT '角色ID',
    `status` INT DEFAULT 0 COMMENT '账号状态: 0-正常 1-禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    UNIQUE KEY `uk_emp_username` (`username`),
    UNIQUE KEY `uk_employee_no` (`employee_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- 影片表
CREATE TABLE IF NOT EXISTS `movie` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '影片ID',
    `movie_name` VARCHAR(100) NOT NULL COMMENT '片名',
    `genre` VARCHAR(100) COMMENT '类型',
    `duration` INT COMMENT '时长(分钟)',
    `actors` VARCHAR(500) COMMENT '主演',
    `director` VARCHAR(100) COMMENT '导演',
    `description` TEXT COMMENT '简介',
    `poster_url` VARCHAR(500) COMMENT '海报图片URL',
    `release_date` DATE COMMENT '上映日期',
    `base_price` DECIMAL(10,2) COMMENT '基础票价',
    `status` INT DEFAULT 1 COMMENT '状态: 0-下架 1-上架 2-即将上映',
    `is_hot` INT DEFAULT 0 COMMENT '是否热门置顶: 0-否 1-是',
    `sort_order` INT DEFAULT 0 COMMENT '排序权重',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `country` VARCHAR(50) COMMENT '国家/地区',
    `language` VARCHAR(50) COMMENT '语言',
    `rating` DOUBLE DEFAULT 0 COMMENT '评分(1-10)',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
    KEY `idx_status` (`status`),
    KEY `idx_movie_name` (`movie_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影片表';

-- 为已有movie表添加新字段（如果列不存在会报错，continue-on-error=true将忽略错误）
ALTER TABLE `movie` ADD COLUMN `country` VARCHAR(50) COMMENT '国家/地区' AFTER `sort_order`;
ALTER TABLE `movie` ADD COLUMN `language` VARCHAR(50) COMMENT '语言' AFTER `country`;
ALTER TABLE `movie` ADD COLUMN `rating` DOUBLE DEFAULT 0 COMMENT '评分(1-10)' AFTER `language`;


-- 影厅表
CREATE TABLE IF NOT EXISTS `hall` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '影厅ID',
    `hall_name` VARCHAR(50) NOT NULL COMMENT '影厅名称',
    `row_count` INT NOT NULL COMMENT '座位行数',
    `col_count` INT NOT NULL COMMENT '座位列数',
    `capacity` INT COMMENT '总容量',
    `hall_type` VARCHAR(20) DEFAULT 'STANDARD' COMMENT '影厅类型: STANDARD/IMAX/VIP/4DX',
    `status` INT DEFAULT 1 COMMENT '状态: 0-维护中 1-正常',
    `remark` VARCHAR(255) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY `uk_hall_name` (`hall_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='影厅表';

-- 场次表
CREATE TABLE IF NOT EXISTS `schedule` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '场次ID',
    `movie_id` BIGINT NOT NULL COMMENT '影片ID',
    `hall_id` BIGINT NOT NULL COMMENT '影厅ID',
    `start_time` DATETIME NOT NULL COMMENT '放映开始时间',
    `end_time` DATETIME COMMENT '放映结束时间',
    `price` DECIMAL(10,2) COMMENT '票价',
    `status` INT DEFAULT 1 COMMENT '状态: 0-已取消 1-正常放映 2-已结束',
    `sold_count` INT DEFAULT 0 COMMENT '已售座位数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
    KEY `idx_movie_id` (`movie_id`),
    KEY `idx_hall_id` (`hall_id`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场次表';

-- 座位表
CREATE TABLE IF NOT EXISTS `seat` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '座位ID',
    `schedule_id` BIGINT NOT NULL COMMENT '场次ID',
    `seat_row` INT NOT NULL COMMENT '座位行号(从1开始)',
    `seat_col` INT NOT NULL COMMENT '座位列号(从1开始)',
    `seat_number` VARCHAR(20) NOT NULL COMMENT '座位编号: 如A-05',
    `status` INT DEFAULT 0 COMMENT '状态: 0-空闲 1-已锁定 2-已售出',
    `lock_time` DATETIME COMMENT '锁定时间',
    `order_id` BIGINT COMMENT '订单ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_schedule_id` (`schedule_id`),
    KEY `idx_seat_number` (`schedule_id`, `seat_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位表';

-- 订单表（order是MySQL关键字，需用反引号）
CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    `order_no` VARCHAR(30) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `schedule_id` BIGINT NOT NULL COMMENT '场次ID',
    `movie_id` BIGINT COMMENT '影片ID',
    `hall_id` BIGINT COMMENT '影厅ID',
    `seat_numbers` VARCHAR(500) COMMENT '座位编号(逗号分隔)',
    `seat_count` INT DEFAULT 1 COMMENT '座位数量',
    `total_price` DECIMAL(10,2) COMMENT '总金额',
    `status` INT DEFAULT 0 COMMENT '订单状态: 0-待支付 1-待观影 2-已完成 3-已改签 4-已退票 5-已过期',
    `pay_time` DATETIME COMMENT '支付时间',
    `original_order_id` BIGINT COMMENT '原订单ID(改签来源)',
    `reschedule_time` DATETIME COMMENT '改签/退票时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `order_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `order_id` BIGINT COMMENT '订单ID',
    `operation_type` VARCHAR(20) COMMENT '操作类型: CREATE/RESCHEDULE/REFUND/PAY/EXPIRE',
    `before_content` TEXT COMMENT '操作前内容(JSON)',
    `after_content` TEXT COMMENT '操作后内容(JSON)',
    `operator_id` BIGINT COMMENT '操作人ID',
    `operator_type` VARCHAR(20) COMMENT '操作人类型: USER/EMPLOYEE/SYSTEM',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `description` VARCHAR(255) COMMENT '配置描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入示例影厅数据（仅在表为空时）
INSERT INTO `hall` (`hall_name`, `row_count`, `col_count`, `capacity`, `hall_type`, `status`)
SELECT '1号标准厅', 8, 12, 96, 'STANDARD', 1
WHERE NOT EXISTS (SELECT 1 FROM `hall` LIMIT 1);

INSERT INTO `hall` (`hall_name`, `row_count`, `col_count`, `capacity`, `hall_type`, `status`)
SELECT '2号IMAX巨幕厅', 10, 16, 160, 'IMAX', 1
WHERE NOT EXISTS (SELECT 2 FROM `hall` LIMIT 1);

INSERT INTO `hall` (`hall_name`, `row_count`, `col_count`, `capacity`, `hall_type`, `status`)
SELECT '3号VIP豪华厅', 6, 8, 48, 'VIP', 1
WHERE NOT EXISTS (SELECT 3 FROM `hall` LIMIT 1);
