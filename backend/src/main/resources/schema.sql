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
    `phone` VARCHAR(20),
    `email` VARCHAR(100),
    `status` INT DEFAULT 0,
    `theme` VARCHAR(10) DEFAULT 'white',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `role_code` VARCHAR(50) NOT NULL UNIQUE,
    `role_name` VARCHAR(50) NOT NULL,
    `description` VARCHAR(255),
    `permissions` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
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
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `hall` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `hall_name` VARCHAR(100) NOT NULL,
    `row_count` INT NOT NULL,
    `col_count` INT NOT NULL,
    `seat_layout` TEXT,
    `status` INT DEFAULT 1,
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
    `lock_time` DATETIME,
    `order_id` BIGINT,
    INDEX `idx_seat_schedule` (`schedule_id`),
    INDEX `idx_seat_number` (`schedule_id`, `seat_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `config_key` VARCHAR(100) NOT NULL UNIQUE,
    `config_value` TEXT,
    `description` VARCHAR(255),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
