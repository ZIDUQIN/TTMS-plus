package com.ttms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置类
 * 允许前端开发服务器跨域访问后端API
 * 生产环境通过配置文件限制具体的允许来源
 */
@Configuration
public class CorsConfig {

    /**
     * 配置CORS过滤器
     * 开发环境下允许本地开发服务器，生产环境应限制具体的允许来源
     */
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    /**
     * 配置CORS配置源，供Spring Security过滤器链使用
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 开发环境：允许本地前端开发服务器
        config.addAllowedOriginPattern("http://localhost:*");
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

        return source;
    }
}
