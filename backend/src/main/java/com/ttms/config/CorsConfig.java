package com.ttms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置类
 * 允许前端开发服务器跨域访问后端API
 */
@Configuration
public class CorsConfig {

    /**
     * 配置CORS过滤器
     * 开发环境下允许所有来源、所有方法和所有请求头
     * 生产环境应限制具体的允许来源
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源（开发环境），生产环境应改为具体域名
        config.addAllowedOriginPattern("*");
        // 允许所有HTTP方法（GET/POST/PUT/DELETE/OPTIONS等）
        config.addAllowedMethod("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许携带认证信息（Cookie、Authorization头等）
        config.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        // 将CORS配置应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
