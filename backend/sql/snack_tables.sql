-- ============================================================
-- TTMS 卖品管理模块 — 数据库建表DDL
-- ============================================================

-- 卖品表（小吃、饮料、周边等）
CREATE TABLE IF NOT EXISTS `snack` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '卖品名称',
    `category` VARCHAR(50) NOT NULL DEFAULT 'OTHER' COMMENT '分类: POPCORN/DRINK/SNACK/OTHER',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价',
    `image_url` VARCHAR(500) COMMENT '图片URL',
    `description` VARCHAR(500) COMMENT '描述',
    `stock` INT DEFAULT -1 COMMENT '库存（-1表示无限）',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卖品表';

-- 套餐表（组合卖品）
CREATE TABLE IF NOT EXISTS `snack_combo` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '套餐价格',
    `original_price` DECIMAL(10,2) DEFAULT 0 COMMENT '原价合计',
    `snack_ids` VARCHAR(500) COMMENT '包含的卖品ID列表（JSON数组）',
    `description` VARCHAR(500) COMMENT '描述',
    `image_url` VARCHAR(500) COMMENT '图片URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套餐表';

-- 卖品订单表（记录卖品销售）
CREATE TABLE IF NOT EXISTS `snack_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
    `movie_order_id` BIGINT COMMENT '关联的电影订单ID（可为空）',
    `user_id` BIGINT COMMENT '购买用户ID',
    `cashier_id` BIGINT COMMENT '收银员ID',
    `items` TEXT COMMENT '购买明细JSON',
    `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    `payment_method` VARCHAR(20) DEFAULT 'CASH' COMMENT '支付方式',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-待支付 1-已完成 2-已退款',
    `pay_time` DATETIME COMMENT '支付时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卖品订单表';

-- Seed data: 默认卖品
INSERT INTO `snack` (`name`, `category`, `price`, `stock`, `status`, `sort_order`) VALUES
('大桶爆米花', 'POPCORN', 25.00, -1, 1, 1),
('中桶爆米花', 'POPCORN', 18.00, -1, 1, 2),
('可口可乐(大)', 'DRINK', 12.00, -1, 1, 3),
('可口可乐(中)', 'DRINK', 8.00, -1, 1, 4),
('矿泉水', 'DRINK', 5.00, -1, 1, 5),
('薯片', 'SNACK', 10.00, -1, 1, 6),
('热狗', 'SNACK', 12.00, -1, 1, 7),
('冰淇淋', 'SNACK', 15.00, 50, 1, 8);

-- Seed data: 默认套餐
INSERT INTO `snack_combo` (`name`, `price`, `original_price`, `snack_ids`, `description`, `status`) VALUES
('双人观影套餐', 45.00, 61.00, '[1,3,3]', '大桶爆米花+2杯大可乐，立省¥16', 1),
('单人观影套餐', 32.00, 43.00, '[2,4]', '中桶爆米花+中可乐，立省¥11', 1),
('家庭欢乐观影套餐', 68.00, 93.00, '[1,3,3,6,7]', '大桶爆米花+2杯大可乐+薯片+热狗，立省¥25', 1);
