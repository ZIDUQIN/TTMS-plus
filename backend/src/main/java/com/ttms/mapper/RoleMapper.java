package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色数据访问层
 * 继承MyBatis-Plus的BaseMapper，自动获得CRUD方法
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    // BaseMapper已提供所有基本CRUD操作
    // 如需自定义查询可在此添加
}
