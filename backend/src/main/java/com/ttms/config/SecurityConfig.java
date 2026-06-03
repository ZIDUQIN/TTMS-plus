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
            // 策略：仅保护API路径，非API请求（静态资源、Vue Router SPA路由）全部放行
            .authorizeHttpRequests(auth -> auth
                // ========== 认证相关接口：所有人可访问 ==========
                .requestMatchers("/api/auth/**").permitAll()

                // ========== 影片信息：GET公开，管理操作需管理员 ==========
                .requestMatchers(HttpMethod.GET, "/api/movies/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/movies/**").hasAnyRole("SUPER_ADMIN", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/movies/**").hasAnyRole("SUPER_ADMIN", "STAFF")
                .requestMatchers(HttpMethod.DELETE, "/api/movies/**").hasAnyRole("SUPER_ADMIN", "STAFF")

                // ========== 场次查询：所有人可查，管理需管理员 ==========
                .requestMatchers("/api/schedules/query/**").permitAll()
                .requestMatchers("/api/schedules/**").hasAnyRole("SUPER_ADMIN", "STAFF")

                // ========== 管理端接口：管理员角色 ==========
                .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "STAFF")

                // ========== 文件上传：管理员 ==========
                .requestMatchers("/api/upload").hasAnyRole("SUPER_ADMIN", "STAFF")

                // ========== 用户端接口：需要登录 ==========
                .requestMatchers("/api/user/**").authenticated()

                // ========== 其他API路径：需要认证 ==========
                .requestMatchers("/api/**").authenticated()

                // ========== 非API请求（静态资源 + SPA路由）：全部放行 ==========
                .anyRequest().permitAll())

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
