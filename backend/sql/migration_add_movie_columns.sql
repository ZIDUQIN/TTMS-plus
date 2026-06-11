-- ============================================================
-- TTMS 数据库迁移脚本
-- 在华为云 RDS MySQL 上执行以添加缺失的列
-- mysql -h <RDS地址> -u <用户名> -p TTMS < migration_add_movie_columns.sql
-- 注意: 如提示 "Duplicate column" 说明该列已存在，忽略即可
-- ============================================================

USE TTMS;

ALTER TABLE `movie` ADD COLUMN `trailer_url` VARCHAR(500) COMMENT '预告片链接';
ALTER TABLE `movie` ADD COLUMN `stills` TEXT COMMENT '剧照JSON';
ALTER TABLE `movie` ADD COLUMN `content_rating` VARCHAR(10) COMMENT '内容分级';
ALTER TABLE `movie` ADD COLUMN `douban_rating` DOUBLE COMMENT '豆瓣评分';
ALTER TABLE `movie` ADD COLUMN `imdb_rating` DOUBLE COMMENT 'IMDB评分';
ALTER TABLE `movie` ADD COLUMN `tags` VARCHAR(500) COMMENT '标签';
