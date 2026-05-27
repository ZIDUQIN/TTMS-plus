package com.ttms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttms.entity.Hall;

/**
 * 影厅服务接口
 * 负责影厅的增删改查、状态管理等业务
 */
public interface HallService {

    /**
     * 分页查询影厅列表
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    Page<Hall> list(int page, int size);

    /**
     * 查询影厅详情
     *
     * @param id 影厅ID
     * @return 影厅实体
     */
    Hall detail(Long id);

    /**
     * 添加影厅
     * 自动计算总容量 = 行数 x 列数
     *
     * @param hall 影厅信息
     * @return 添加后的影厅
     */
    Hall add(Hall hall);

    /**
     * 更新影厅信息
     * 重新计算总容量
     *
     * @param hall 影厅信息（含ID）
     * @return 更新后的影厅
     */
    Hall update(Hall hall);

    /**
     * 删除影厅（逻辑删除）
     * 删除前检查是否有未结束的场次
     *
     * @param id 影厅ID
     */
    void delete(Long id);

    /**
     * 设置影厅状态
     *
     * @param id     影厅ID
     * @param status 状态值（0-维护中 1-正常）
     */
    void setStatus(Long id, Integer status);
}
