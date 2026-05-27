package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Hall;
import org.apache.ibatis.annotations.Mapper;

/**
 * 影厅数据访问层
 * 继承MyBatis-Plus的BaseMapper，自动获得CRUD方法
 */
@Mapper
public interface HallMapper extends BaseMapper<Hall> {
    // BaseMapper已提供所有基本CRUD操作
    // 如需自定义查询可在此添加
}
