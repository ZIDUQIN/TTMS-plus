package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 员工数据访问层
 * 继承MyBatis-Plus的BaseMapper，自动获得CRUD方法
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 根据用户名查询员工
     * 用于管理端登录校验
     *
     * @param username 用户名
     * @return 员工实体，不存在则返回null
     */
    @Select("SELECT * FROM employee WHERE username = #{username} AND deleted = 0")
    Employee findByUsername(@Param("username") String username);
}
