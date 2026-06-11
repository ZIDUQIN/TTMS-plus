-- ============================================================
-- TTMS 数据库建表脚本
-- 首次部署时在华为云RDS MySQL上手动执行
-- mysql -h <RDS地址> -u <用户名> -p TTMS < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS TTMS DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE TTMS;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(64) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(64),
    `real_name` VARCHAR(64),
    `id_card` VARCHAR(18),
    `phone` VARCHAR(20),
    `email` VARCHAR(100),
    `avatar` VARCHAR(500),
    `status` INT DEFAULT 0,
    `theme` VARCHAR(10) DEFAULT 'white',
    `member_level_id` BIGINT,
    `points` INT DEFAULT 0,
    `balance` DECIMAL(10,2) DEFAULT 0.00,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `role_code` VARCHAR(50) NOT NULL UNIQUE,
    `role_name` VARCHAR(50) NOT NULL,
    `description` VARCHAR(255),
    `permissions` TEXT,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `employee` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `employee_no` VARCHAR(50),
    `username` VARCHAR(64) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `real_name` VARCHAR(64),
    `phone` VARCHAR(20),
    `role_id` BIGINT,
    `status` INT DEFAULT 0,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX `idx_employee_no` (`employee_no`),
    INDEX `idx_employee_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `movie` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `movie_name` VARCHAR(255) NOT NULL,
    `genre` VARCHAR(100),
    `duration` INT,
    `actors` VARCHAR(500),
    `director` VARCHAR(100),
    `description` TEXT,
    `release_date` DATE,
    `base_price` DECIMAL(10,2),
    `poster_url` VARCHAR(500),
    `status` INT DEFAULT 1,
    `is_hot` INT DEFAULT 0,
    `sort_order` INT DEFAULT 0,
    `country` VARCHAR(50),
    `language` VARCHAR(20),
    `rating` DOUBLE,
    `trailer_url` VARCHAR(500),
    `stills` TEXT,
    `content_rating` VARCHAR(10),
    `douban_rating` DOUBLE,
    `imdb_rating` DOUBLE,
    `tags` VARCHAR(500),
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hall` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `hall_name` VARCHAR(100) NOT NULL,
    `row_count` INT NOT NULL,
    `col_count` INT NOT NULL,
    `capacity` INT,
    `hall_type` VARCHAR(50) DEFAULT 'STANDARD',
    `remark` VARCHAR(500),
    `seat_layout` TEXT,
    `layout_cfg` TEXT,
    `status` INT DEFAULT 1,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `schedule` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `movie_id` BIGINT NOT NULL,
    `hall_id` BIGINT NOT NULL,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME,
    `price` DECIMAL(10,2),
    `status` INT DEFAULT 1,
    `sold_count` INT DEFAULT 0,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_schedule_movie` (`movie_id`),
    INDEX `idx_schedule_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 注：`order` 是 MySQL 保留字，需始终用反引号包裹
-- 若新建数据库建议重命名为 t_order 或 ttms_order，此处保持兼容现有代码
CREATE TABLE IF NOT EXISTS `order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(32) NOT NULL UNIQUE,
    `user_id` BIGINT,
    `schedule_id` BIGINT,
    `movie_id` BIGINT,
    `hall_id` BIGINT,
    `seat_numbers` VARCHAR(500),
    `seat_count` INT DEFAULT 0,
    `total_price` DECIMAL(10,2),
    `status` INT DEFAULT 0,
    `pay_time` DATETIME,
    `payment_method` VARCHAR(20),
    `cashier_id` BIGINT,
    `refund_amount` DECIMAL(10,2),
    `original_order_id` BIGINT,
    `reschedule_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_order_user` (`user_id`),
    INDEX `idx_order_schedule` (`schedule_id`),
    INDEX `idx_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT,
    `operation_type` VARCHAR(50),
    `before_content` TEXT,
    `after_content` TEXT,
    `operator_id` BIGINT,
    `operator_type` VARCHAR(20),
    `remark` VARCHAR(255),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_order_log_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `seat` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `schedule_id` BIGINT NOT NULL,
    `seat_row` INT NOT NULL,
    `seat_col` INT NOT NULL,
    `seat_number` VARCHAR(10) NOT NULL,
    `status` INT DEFAULT 0,
    `price_adjustment` DECIMAL(10,2) DEFAULT 0.00,
    `lock_time` DATETIME,
    `order_id` BIGINT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_seat_schedule` (`schedule_id`),
    INDEX `idx_seat_number` (`schedule_id`, `seat_number`),
    INDEX `idx_seat_order` (`order_id`),
    UNIQUE KEY `uk_schedule_seat` (`schedule_id`, `seat_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `config_key` VARCHAR(100) NOT NULL UNIQUE,
    `config_value` TEXT,
    `description` VARCHAR(255),
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- B29: 多影院支持
-- ============================================================
CREATE TABLE IF NOT EXISTS `cinema` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `cinema_name` VARCHAR(200) NOT NULL,
    `address` VARCHAR(500),
    `phone` VARCHAR(20),
    `status` INT DEFAULT 1,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- B15: 会员体系
-- ============================================================
CREATE TABLE IF NOT EXISTS `member_level` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `level_name` VARCHAR(50) NOT NULL,
    `min_spending` DECIMAL(10,2) NOT NULL COMMENT '升级所需累计消费金额',
    `discount_rate` DECIMAL(3,2) DEFAULT 1.00 COMMENT '购票折扣率(0.8=8折)',
    `points_rate` DECIMAL(3,2) DEFAULT 1.00 COMMENT '积分倍数',
    `sort_order` INT DEFAULT 0,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- B16: 优惠券系统
-- ============================================================
CREATE TABLE IF NOT EXISTS `coupon` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(20) NOT NULL COMMENT 'FIXED-满减 PERCENT-折扣',
    `value` DECIMAL(10,2) NOT NULL COMMENT '面值:固定金额或折扣率',
    `min_order_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '最低消费金额',
    `expire_days` INT DEFAULT 30 COMMENT '有效天数',
    `total_qty` INT DEFAULT 0,
    `remaining_qty` INT DEFAULT 0,
    `description` VARCHAR(500),
    `status` INT DEFAULT 1,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `coupon_id` BIGINT NOT NULL,
    `status` INT DEFAULT 0 COMMENT '0-未使用 1-已使用 2-已过期',
    `used_order_id` BIGINT,
    `obtain_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `use_time` DATETIME,
    `expire_time` DATETIME,
    INDEX `idx_user_coupon_user` (`user_id`),
    INDEX `idx_user_coupon_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- B17: 团体/包场预约
-- ============================================================
CREATE TABLE IF NOT EXISTS `group_booking` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT,
    `contact_name` VARCHAR(64),
    `contact_phone` VARCHAR(20),
    `company_name` VARCHAR(200),
    `expected_date` DATE,
    `attendee_count` INT,
    `movie_id` BIGINT,
    `hall_id` BIGINT,
    `special_requirements` TEXT,
    `status` INT DEFAULT 0 COMMENT '0-待审核 1-已通过 2-已拒绝 3-已完成',
    `review_notes` VARCHAR(500),
    `reviewer_id` BIGINT,
    `review_time` DATETIME,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- B22: 交接班
-- ============================================================
CREATE TABLE IF NOT EXISTS `shift` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `employee_id` BIGINT NOT NULL,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME,
    `status` INT DEFAULT 0 COMMENT '0-进行中 1-已交接',
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_shift_employee` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `shift_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `shift_id` BIGINT NOT NULL,
    `cash_collected` DECIMAL(10,2) DEFAULT 0.00,
    `wechat_collected` DECIMAL(10,2) DEFAULT 0.00,
    `alipay_collected` DECIMAL(10,2) DEFAULT 0.00,
    `system_total` DECIMAL(10,2) DEFAULT 0.00,
    `difference` DECIMAL(10,2) DEFAULT 0.00,
    `tickets_sold` INT DEFAULT 0,
    `tickets_refunded` INT DEFAULT 0,
    `notes` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- B20: 消息通知
-- ============================================================
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL COMMENT 'SMS PUSH EMAIL',
    `title` VARCHAR(200),
    `content` TEXT,
    `status` INT DEFAULT 0 COMMENT '0-待发送 1-已发送 2-失败',
    `template_code` VARCHAR(50),
    `send_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_notify_user` (`user_id`),
    INDEX `idx_notify_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- B24: 发票管理
-- ============================================================
CREATE TABLE IF NOT EXISTS `invoice` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `user_id` BIGINT,
    `invoice_type` VARCHAR(20) DEFAULT 'ELECTRONIC' COMMENT 'ELECTRONIC-电子 PAPER-纸质',
    `title_type` VARCHAR(20) DEFAULT 'PERSONAL' COMMENT 'PERSONAL-个人 COMPANY-企业',
    `title` VARCHAR(200),
    `taxpayer_id` VARCHAR(50),
    `amount` DECIMAL(10,2),
    `invoice_no` VARCHAR(50),
    `status` INT DEFAULT 0 COMMENT '0-待开具 1-已开具 2-已作废',
    `issue_time` DATETIME,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- B23: 报表
-- ============================================================
CREATE TABLE IF NOT EXISTS `report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `report_type` VARCHAR(20) NOT NULL COMMENT 'DAILY WEEKLY MONTHLY',
    `report_date` DATE NOT NULL,
    `content` TEXT COMMENT 'JSON格式报表内容',
    `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_report_type_date` (`report_type`, `report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- B2: 支付记录
-- ============================================================
CREATE TABLE IF NOT EXISTS `payment_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `transaction_id` VARCHAR(100),
    `amount` DECIMAL(10,2) NOT NULL,
    `method` VARCHAR(20) COMMENT 'WECHAT ALIPAY CASH',
    `status` INT DEFAULT 0 COMMENT '0-待支付 1-支付成功 2-支付失败 3-已退款',
    `callback_time` DATETIME,
    `callback_data` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_payment_order` (`order_id`),
    INDEX `idx_payment_transaction` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- B32: 数据库备份日志
-- ============================================================
CREATE TABLE IF NOT EXISTS `backup_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `backup_type` VARCHAR(20) DEFAULT 'FULL',
    `file_path` VARCHAR(500),
    `file_size` BIGINT,
    `status` INT DEFAULT 0 COMMENT '0-进行中 1-成功 2-失败',
    `error_msg` TEXT,
    `start_time` DATETIME,
    `end_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
