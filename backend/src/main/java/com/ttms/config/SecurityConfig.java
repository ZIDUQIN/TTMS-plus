package com.ttms.config;

import com.ttms.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置类
 * 配置JWT认证过滤器、URL权限规则、密码编码器
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用方法级别安全注解 @PreAuthorize/@PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 安全过滤器链配置
     * Spring Security 6.x 使用 Lambda DSL 风格
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 启用CORS（必须在Spring Security链中配置，否则OPTIONS预检请求会被拦截）
            .cors(cors -> {})

            // 禁用CSRF（前后端分离项目不需要CSRF保护，因为使用JWT令牌认证）
            .csrf(csrf -> csrf.disable())

            // 无状态会话管理（不使用HttpSession存储SecurityContext，每次请求从JWT令牌解析）
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL授权规则配置
            .authorizeHttpRequests(auth -> auth
                // 允许所有用户访问认证相关接口（登录、注册）
                .requestMatchers("/api/auth/**").permitAll()
                // 允许所有用户查看影片信息（公开GET请求）
                .requestMatchers(HttpMethod.GET, "/api/movies/**").permitAll()
                // 影片管理操作需要管理员角色
                .requestMatchers(HttpMethod.POST, "/api/movies/**").hasAnyRole("SUPER_ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/movies/**").hasAnyRole("SUPER_ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/movies/**").hasAnyRole("SUPER_ADMIN", "STAFF")
                // 允许所有用户查询场次信息
                .requestMatchers("/api/schedules/query/**").permitAll()
                // 场次管理操作需要管理员角色
                .requestMatchers("/api/schedules/**").hasAnyRole("SUPER_ADMIN", "STAFF")
                // 允许所有用户访问上传的静态资源文件
                .requestMatchers("/uploads/**").permitAll()
                // 管理端接口需要 SUPER_ADMIN 或 STAFF 角色
                .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "STAFF")
                // 文件上传接口需要管理员角色
                .requestMatchers("/api/upload").hasAnyRole("SUPER_ADMIN", "STAFF")
                // 用户端接口需要认证（所有已登录用户均可访问）
                .requestMatchers("/api/user/**").authenticated()
                // 其他所有请求需要认证
                .anyRequest().authenticated())

            // 将JWT认证过滤器添加到UsernamePasswordAuthenticationFilter之前
            // 这样请求会先经过JWT过滤器解析令牌并设置认证信息
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器 Bean
     * 使用BCrypt加密算法，自动加盐，安全性高
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
