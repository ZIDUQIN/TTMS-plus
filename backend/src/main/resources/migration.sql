-- ============================================================
-- TTMS 完整迁移脚本 (MySQL 8.0 兼容)
-- 覆盖: movie/user/seat/hall/order 新增列 + 16张新表 + 种子数据
-- 执行: mysql -u root -p TTMS < migration.sql
-- ============================================================
USE TTMS;

-- 存储过程: 安全添加列（列不存在才加）
DROP PROCEDURE IF EXISTS AddColIfNotExist;
DELIMITER //
CREATE PROCEDURE AddColIfNotExist(IN tbl VARCHAR(64), IN col VARCHAR(64), IN colDef VARCHAR(256))
BEGIN
    IF (SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col) = 0 THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', colDef);
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
        SELECT CONCAT('[OK] ', tbl, '.', col, ' 已添加') AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', tbl, '.', col, ' 已存在') AS result;
    END IF;
END //
DELIMITER ;

-- ========== Movie 新字段 ==========
CALL AddColIfNotExist('movie', 'country', 'VARCHAR(50)');
CALL AddColIfNotExist('movie', 'language', 'VARCHAR(20)');
CALL AddColIfNotExist('movie', 'rating', 'DOUBLE');
CALL AddColIfNotExist('movie', 'trailer_url', 'VARCHAR(500)');
CALL AddColIfNotExist('movie', 'stills', 'TEXT');
CALL AddColIfNotExist('movie', 'content_rating', 'VARCHAR(10)');
CALL AddColIfNotExist('movie', 'douban_rating', 'DOUBLE');
CALL AddColIfNotExist('movie', 'imdb_rating', 'DOUBLE');
CALL AddColIfNotExist('movie', 'tags', 'VARCHAR(500)');

-- ========== User 新字段 ==========
CALL AddColIfNotExist('user', 'real_name', 'VARCHAR(64)');
CALL AddColIfNotExist('user', 'id_card', 'VARCHAR(18)');
CALL AddColIfNotExist('user', 'member_level_id', 'BIGINT');
CALL AddColIfNotExist('user', 'points', 'INT DEFAULT 0');
CALL AddColIfNotExist('user', 'balance', 'DECIMAL(10,2) DEFAULT 0.00');

-- ========== Seat 新字段 ==========
CALL AddColIfNotExist('seat', 'price_adjustment', 'DECIMAL(10,2) DEFAULT 0.00');

-- ========== Hall 新字段 ==========
CALL AddColIfNotExist('hall', 'seat_layout', 'TEXT');
CALL AddColIfNotExist('hall', 'layout_cfg', 'TEXT');

-- ========== Order 新字段 ==========
CALL AddColIfNotExist('order', 'payment_method', 'VARCHAR(20)');
CALL AddColIfNotExist('order', 'cashier_id', 'BIGINT');
CALL AddColIfNotExist('order', 'refund_amount', 'DECIMAL(10,2)');

-- ========== 清理存储过程 ==========
DROP PROCEDURE IF EXISTS AddColIfNotExist;

-- ========== 新表（CREATE TABLE IF NOT EXISTS 安全）==========

CREATE TABLE IF NOT EXISTS `cinema` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `cinema_name` VARCHAR(200) NOT NULL,
    `address` VARCHAR(500), `phone` VARCHAR(20), `status` INT DEFAULT 1,
    `deleted` INT DEFAULT 0, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO `cinema` (`cinema_name`, `address`, `phone`) VALUES ('TTMS总店', '默认地址', '400-888-8888');

CREATE TABLE IF NOT EXISTS `member_level` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `level_name` VARCHAR(50) NOT NULL,
    `min_spending` DECIMAL(10,2) NOT NULL, `discount_rate` DECIMAL(3,2) DEFAULT 1.00,
    `points_rate` DECIMAL(3,2) DEFAULT 1.00, `sort_order` INT DEFAULT 0,
    `deleted` INT DEFAULT 0, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO `member_level` (`level_name`, `min_spending`, `discount_rate`, `points_rate`, `sort_order`) VALUES
('普通会员',0,1.00,1.00,1),('银卡会员',500,0.95,1.20,2),('金卡会员',2000,0.88,1.50,3),('钻石会员',5000,0.80,2.00,4);

CREATE TABLE IF NOT EXISTS `coupon` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(20) NOT NULL, `value` DECIMAL(10,2) NOT NULL,
    `min_order_amount` DECIMAL(10,2) DEFAULT 0.00, `expire_days` INT DEFAULT 30,
    `total_qty` INT DEFAULT 0, `remaining_qty` INT DEFAULT 0,
    `description` VARCHAR(500), `status` INT DEFAULT 1, `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `user_id` BIGINT NOT NULL,
    `coupon_id` BIGINT NOT NULL, `status` INT DEFAULT 0, `used_order_id` BIGINT,
    `obtain_time` DATETIME DEFAULT CURRENT_TIMESTAMP, `use_time` DATETIME, `expire_time` DATETIME,
    INDEX `idx_user_coupon_user` (`user_id`), INDEX `idx_user_coupon_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `group_booking` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `user_id` BIGINT,
    `contact_name` VARCHAR(64), `contact_phone` VARCHAR(20), `company_name` VARCHAR(200),
    `expected_date` DATE, `attendee_count` INT, `movie_id` BIGINT, `hall_id` BIGINT,
    `special_requirements` TEXT, `status` INT DEFAULT 0, `review_notes` VARCHAR(500),
    `reviewer_id` BIGINT, `review_time` DATETIME, `deleted` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `shift` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `employee_id` BIGINT NOT NULL,
    `start_time` DATETIME NOT NULL, `end_time` DATETIME, `status` INT DEFAULT 0,
    `deleted` INT DEFAULT 0, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_shift_employee` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `shift_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `shift_id` BIGINT NOT NULL,
    `cash_collected` DECIMAL(10,2) DEFAULT 0.00, `wechat_collected` DECIMAL(10,2) DEFAULT 0.00,
    `alipay_collected` DECIMAL(10,2) DEFAULT 0.00, `system_total` DECIMAL(10,2) DEFAULT 0.00,
    `difference` DECIMAL(10,2) DEFAULT 0.00, `tickets_sold` INT DEFAULT 0,
    `tickets_refunded` INT DEFAULT 0, `notes` TEXT, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `user_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL, `title` VARCHAR(200), `content` TEXT,
    `status` INT DEFAULT 0, `template_code` VARCHAR(50), `send_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_notify_user` (`user_id`), INDEX `idx_notify_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `invoice` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `order_id` BIGINT NOT NULL, `user_id` BIGINT,
    `invoice_type` VARCHAR(20) DEFAULT 'ELECTRONIC', `title_type` VARCHAR(20) DEFAULT 'PERSONAL',
    `title` VARCHAR(200), `taxpayer_id` VARCHAR(50), `amount` DECIMAL(10,2),
    `invoice_no` VARCHAR(50), `status` INT DEFAULT 0, `issue_time` DATETIME,
    `deleted` INT DEFAULT 0, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `report_type` VARCHAR(20) NOT NULL,
    `report_date` DATE NOT NULL, `content` TEXT, `generated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_report_type_date` (`report_type`, `report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `payment_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `order_id` BIGINT NOT NULL,
    `transaction_id` VARCHAR(100), `amount` DECIMAL(10,2) NOT NULL, `method` VARCHAR(20),
    `status` INT DEFAULT 0, `callback_time` DATETIME, `callback_data` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_payment_order` (`order_id`), INDEX `idx_payment_transaction` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `backup_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `backup_type` VARCHAR(20) DEFAULT 'FULL',
    `file_path` VARCHAR(500), `file_size` BIGINT, `status` INT DEFAULT 0,
    `error_msg` TEXT, `start_time` DATETIME, `end_time` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `snack` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `name` VARCHAR(100) NOT NULL,
    `category` VARCHAR(50) NOT NULL DEFAULT 'OTHER', `price` DECIMAL(10,2) NOT NULL DEFAULT 0,
    `image_url` VARCHAR(500), `description` VARCHAR(500), `stock` INT DEFAULT -1,
    `status` TINYINT DEFAULT 1, `sort_order` INT DEFAULT 0, `deleted` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `snack_combo` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `name` VARCHAR(100) NOT NULL,
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0, `original_price` DECIMAL(10,2) DEFAULT 0,
    `snack_ids` VARCHAR(500), `description` VARCHAR(500), `image_url` VARCHAR(500),
    `status` TINYINT DEFAULT 1, `deleted` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `snack_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY, `order_no` VARCHAR(50) NOT NULL,
    `movie_order_id` BIGINT, `user_id` BIGINT, `cashier_id` BIGINT,
    `items` TEXT, `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0,
    `payment_method` VARCHAR(20) DEFAULT 'CASH', `status` TINYINT DEFAULT 1,
    `pay_time` DATETIME, `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ========== 卖品种子数据 ==========
INSERT IGNORE INTO `snack` (`name`, `category`, `price`, `stock`, `status`, `sort_order`) VALUES
('大桶爆米花','POPCORN',25.00,-1,1,1),('中桶爆米花','POPCORN',18.00,-1,1,2),
('可口可乐(大)','DRINK',12.00,-1,1,3),('可口可乐(中)','DRINK',8.00,-1,1,4),
('矿泉水','DRINK',5.00,-1,1,5),('薯片','SNACK',10.00,-1,1,6),
('热狗','SNACK',12.00,-1,1,7),('冰淇淋','SNACK',15.00,50,1,8);
INSERT IGNORE INTO `snack_combo` (`name`, `price`, `original_price`, `snack_ids`, `description`, `status`) VALUES
('双人观影套餐',45.00,61.00,'[1,3,3]','大桶爆米花+2杯大可乐',1),
('单人观影套餐',32.00,43.00,'[2,4]','中桶爆米花+中可乐',1),
('家庭欢乐观影套餐',68.00,93.00,'[1,3,3,6,7]','大爆+2可乐+薯片+热狗',1);

-- ========== 系统配置 ==========
INSERT IGNORE INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('buffer_minutes','20','场次间缓冲时间(分钟)'),
('member_enabled','true','是否启用会员系统'),
('coupon_enabled','true','是否启用优惠券系统');

SELECT '=== 迁移完成 ===' AS result;
