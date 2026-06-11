package com.ttms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * CORS 跨域配置类
 * 允许前端开发服务器跨域访问后端API
 * 开发环境：允许 localhost 所有端口
 * 生产环境：仅允许配置的特定域名（通过 application-prod.yml 中的 cors.allowed-origins 配置）
 */
@Slf4j
@Configuration
public class CorsConfig {

    /**
     * 允许的跨域来源列表，从配置文件读取
     * 开发环境默认: http://localhost:*
     * 生产环境必须显式配置具体域名
     */
    @Value("${cors.allowed-origins:http://localhost:*}")
    private String allowedOrigins;

    /**
     * 配置CORS过滤器
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

        // 从配置读取允许的来源列表（逗号分隔）
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        for (String origin : origins) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                // 支持通配符模式（如 http://localhost:*）
                if (trimmed.contains("*")) {
                    config.addAllowedOriginPattern(trimmed);
                } else {
                    config.addAllowedOrigin(trimmed);
                }
            }
        }

        // 允许常用HTTP方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        // 允许常用请求头
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("X-Requested-With");

        // 允许携带认证信息（Cookie、Authorization头等）
        config.setAllowCredentials(true);

        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        // 将CORS配置应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("CORS配置初始化: allowedOrigins={}, allowCredentials=true", origins);
        return source;
    }
}
