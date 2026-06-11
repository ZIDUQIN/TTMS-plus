package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问层
 * 继承MyBatis-Plus的BaseMapper，自动获得CRUD方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     * 用于登录校验和重复用户名检查
     *
     * @param username 用户名
     * @return 用户实体，不存在则返回null
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND deleted = 0")
    User findByUsername(@Param("username") String username);

    /**
     * 软删除用户（直接SQL，绕过MyBatis-Plus @TableLogic）
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE user SET deleted = 1 WHERE id = #{userId} AND deleted = 0")
    int softDeleteById(@Param("userId") Long userId);
}
