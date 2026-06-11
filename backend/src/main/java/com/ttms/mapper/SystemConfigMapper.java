package com.ttms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 系统配置数据访问层
 * 继承MyBatis-Plus的BaseMapper，自动获得CRUD方法
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查询配置值
     * 用于获取单个系统配置项（如主题色、影院名称等）
     *
     * @param configKey 配置键
     * @return 系统配置实体，不存在则返回null
     */
    @Select("SELECT * FROM system_config WHERE config_key = #{configKey} AND deleted = 0")
    SystemConfig selectByKey(@Param("configKey") String configKey);

    /**
     * 根据配置键更新配置值
     * 用于管理员修改系统设置
     *
     * @param configKey   配置键
     * @param configValue 新的配置值
     * @return 影响行数
     */
    @Update("UPDATE system_config SET config_value = #{configValue} WHERE config_key = #{configKey}")
    int updateByKey(@Param("configKey") String configKey, @Param("configValue") String configValue);
}
