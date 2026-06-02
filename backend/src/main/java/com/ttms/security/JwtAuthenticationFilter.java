package com.ttms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT认证过滤器
 * 继承OncePerRequestFilter确保每次请求只执行一次过滤
 * 负责从请求头中提取JWT令牌、验证并设置Spring Security认证上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /** Ant风格的路径匹配器，用于判断请求路径是否需要跳过JWT校验 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 无需JWT令牌校验的路径列表
     * 这些路径对应的请求不会进行令牌解析
     */
    private static final List<String> SKIP_PATHS = List.of(
        "/api/auth/**",              // 认证相关（登录、注册）
        "/api/schedules/query/**",    // 场次查询（公开浏览）
        "/uploads/**"                 // 静态资源文件
    );

    /**
     * 过滤器的核心逻辑
     * 1. 检查是否为公开路径，是则跳过JWT校验
     * 2. 从Authorization请求头提取Bearer令牌
     * 3. 验证令牌有效性
     * 4. 解析令牌中的用户信息，创建认证对象
     * 5. 将认证对象设置到SecurityContext中
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // 检查是否为公开路径，若是则跳过JWT认证
        if (shouldSkip(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头中提取JWT令牌
        String token = extractToken(request);

        try {
            // 验证令牌并设置认证信息
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                String userId = String.valueOf(jwtTokenProvider.getUserId(token));
                String role = jwtTokenProvider.getRole(token);

                // 构建Spring Security的认证对象
                // 将角色编码转换为GrantedAuthority（Spring Security会自动添加ROLE_前缀后匹配）
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(role));
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, token, authorities);
                // 设置认证细节（包含请求的IP、Session等信息）
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

                // 将认证信息存入SecurityContextHolder
                // 后续的@PreAuthorize、SecurityContextHolder.getContext()都可以获取到
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.debug("JWT认证处理跳过: {} | 请求路径: {}", e.getMessage(), requestPath);
            // 不在此处抛出异常，让后面的Spring Security过滤器处理授权
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 判断请求路径是否需要跳过JWT校验
     * 使用Ant风格路径匹配
     *
     * @param requestPath 请求路径
     * @return true表示跳过JWT校验
     */
    private boolean shouldSkip(String requestPath) {
        return SKIP_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    /**
     * 从Authorization请求头中提取Bearer令牌
     * 格式: "Authorization: Bearer eyJhbGciOi..."
     *
     * @param request HTTP请求对象
     * @return JWT令牌字符串，如果没有则返回null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        // 标准Bearer令牌格式: "Bearer <token>"
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // 去掉"Bearer "前缀
        }
        return null;
    }
}
