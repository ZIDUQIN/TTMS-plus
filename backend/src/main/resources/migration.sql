-- ============================================================
-- TTMS 数据库迁移脚本 (v1.0 → v2.0)
-- 在已有数据库上执行，添加B1-B32新功能的表和列
-- mysql -h <RDS地址> -u <用户名> -p TTMS < migration.sql
-- ============================================================
USE TTMS;

-- ========== 现有表新增列 ==========

-- Movie表: B14富内容 + B28分级
ALTER TABLE `movie`
  ADD COLUMN IF NOT EXISTS `trailer_url` VARCHAR(500) AFTER `rating`,
  ADD COLUMN IF NOT EXISTS `stills` TEXT AFTER `trailer_url`,
  ADD COLUMN IF NOT EXISTS `content_rating` VARCHAR(10) AFTER `stills`,
  ADD COLUMN IF NOT EXISTS `douban_rating` DOUBLE AFTER `content_rating`,
  ADD COLUMN IF NOT EXISTS `imdb_rating` DOUBLE AFTER `douban_rating`,
  ADD COLUMN IF NOT EXISTS `tags` VARCHAR(500) AFTER `imdb_rating`;

-- User表: B12实名制 + B15会员
ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS `real_name` VARCHAR(64) AFTER `nickname`,
  ADD COLUMN IF NOT EXISTS `id_card` VARCHAR(18) AFTER `real_name`,
  ADD COLUMN IF NOT EXISTS `member_level_id` BIGINT AFTER `theme`,
  ADD COLUMN IF NOT EXISTS `points` INT DEFAULT 0 AFTER `member_level_id`,
  ADD COLUMN IF NOT EXISTS `balance` DECIMAL(10,2) DEFAULT 0.00 AFTER `points`;

-- Seat表: B9分区定价
ALTER TABLE `seat`
  ADD COLUMN IF NOT EXISTS `price_adjustment` DECIMAL(10,2) DEFAULT 0.00 AFTER `seat_number`;

-- Hall表: B18增强布局
ALTER TABLE `hall`
  ADD COLUMN IF NOT EXISTS `layout_cfg` TEXT AFTER `seat_layout`;

-- Order表: B1支付方式 + B4退款金额
ALTER TABLE `order`
  ADD COLUMN IF NOT EXISTS `payment_method` VARCHAR(20) AFTER `total_price`,
  ADD COLUMN IF NOT EXISTS `cashier_id` BIGINT AFTER `payment_method`,
  ADD COLUMN IF NOT EXISTS `refund_amount` DECIMAL(10,2) AFTER `cashier_id`;

-- ========== 新表 ==========

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

INSERT IGNORE INTO `cinema` (`cinema_name`, `address`, `phone`) VALUES ('TTMS总店', '默认地址', '400-888-8888');

CREATE TABLE IF NOT EXISTS `member_level` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `level_name` VARCHAR(50) NOT NULL,
    `min_spending` DECIMAL(10,2) NOT NULL,
    `discount_rate` DECIMAL(3,2) DEFAULT 1.00,
    `points_rate` DECIMAL(3,2) DEFAULT 1.00,
    `sort_order` INT DEFAULT 0,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `member_level` (`level_name`, `min_spending`, `discount_rate`, `points_rate`, `sort_order`) VALUES
('普通会员', 0, 1.00, 1.00, 1),
('银卡会员', 500, 0.95, 1.20, 2),
('金卡会员', 2000, 0.88, 1.50, 3),
('钻石会员', 5000, 0.80, 2.00, 4);

CREATE TABLE IF NOT EXISTS `coupon` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `value` DECIMAL(10,2) NOT NULL,
    `min_order_amount` DECIMAL(10,2) DEFAULT 0.00,
    `expire_days` INT DEFAULT 30,
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
    `status` INT DEFAULT 0,
    `used_order_id` BIGINT,
    `obtain_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `use_time` DATETIME,
    `expire_time` DATETIME,
    INDEX `idx_user_coupon_user` (`user_id`),
    INDEX `idx_user_coupon_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
    `status` INT DEFAULT 0,
    `review_notes` VARCHAR(500),
    `reviewer_id` BIGINT,
    `review_time` DATETIME,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `shift` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `employee_id` BIGINT NOT NULL,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME,
    `status` INT DEFAULT 0,
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

CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL,
    `title` VARCHAR(200),
    `content` TEXT,
    `status` INT DEFAULT 0,
    `template_code` VARCHAR(50),
    `send_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_notify_user` (`user_id`),
    INDEX `idx_notify_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `invoice` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `user_id` BIGINT,
    `invoice_type` VARCHAR(20) DEFAULT 'ELECTRONIC',
    `title_type` VARCHAR(20) DEFAULT 'PERSONAL',
    `title` VARCHAR(200),
    `taxpayer_id` VARCHAR(50),
    `amount` DECIMAL(10,2),
    `invoice_no` VARCHAR(50),
    `status` INT DEFAULT 0,
    `issue_time` DATETIME,
    `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `report_type` VARCHAR(20) NOT NULL,
    `report_date` DATE NOT NULL,
    `content` TEXT,
    `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_report_type_date` (`report_type`, `report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `payment_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `transaction_id` VARCHAR(100),
    `amount` DECIMAL(10,2) NOT NULL,
    `method` VARCHAR(20),
    `status` INT DEFAULT 0,
    `callback_time` DATETIME,
    `callback_data` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_payment_order` (`order_id`),
    INDEX `idx_payment_transaction` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `backup_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `backup_type` VARCHAR(20) DEFAULT 'FULL',
    `file_path` VARCHAR(500),
    `file_size` BIGINT,
    `status` INT DEFAULT 0,
    `error_msg` TEXT,
    `start_time` DATETIME,
    `end_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 初始化默认数据 ==========

INSERT IGNORE INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('buffer_minutes', '20', '场次间缓冲时间(分钟)'),
('member_enabled', 'true', '是否启用会员系统'),
('coupon_enabled', 'true', '是否启用优惠券系统');
