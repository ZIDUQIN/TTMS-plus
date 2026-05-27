package com.ttms.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类
 * 配置分页插件和自动填充处理器
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器配置
     * 添加分页插件，支持物理分页查询
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页内部拦截器，指定数据库类型为MySQL
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置单页最大限制条数，防止恶意查询
        paginationInnerInterceptor.setMaxLimit(500L);
        // 溢出总页数后是否进行处理（默认不处理，设为true会返回最后页数据）
        paginationInnerInterceptor.setOverflow(true);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }

    /**
     * 元对象自动填充处理器
     * 实现createTime和updateTime字段的自动填充
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 插入数据时自动填充 createTime 和 updateTime
             * @param metaObject 元对象，代表当前正在操作的数据实体
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                // 如果createTime字段为null，则自动填充当前时间
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
                // 如果updateTime字段为null，则自动填充当前时间
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            }

            /**
             * 更新数据时自动填充 updateTime
             * @param metaObject 元对象，代表当前正在操作的数据实体
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                // 自动填充updateTime为当前时间
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
