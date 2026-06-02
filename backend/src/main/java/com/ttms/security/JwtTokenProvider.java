package com.ttms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT令牌工具类
 * 负责生成、解析和验证JWT令牌
 * 令牌中包含用户ID、用户名、角色等关键信息
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /** JWT签名密钥，从application.yml读取，默认值确保启动不报错 */
    @Value("${jwt.secret:TTMS2024DefaultJWTSecretKeyForDevelopmentOnly}")
    private String jwtSecret;

    /** JWT过期时间（毫秒），从application.yml读取，默认24小时 */
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * 获取用于签名和验证的密钥对象
     * 使用HMAC-SHA算法，密钥至少需要256位(32字节)
     * 如果配置的secret不足32字节，使用HKDF扩展方式补齐
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // 确保密钥长度至少为256位(32字节)
        if (keyBytes.length < 32) {
            // 使用迭代哈希扩展密钥长度，而非零字节填充
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hashed = digest.digest(keyBytes);
                keyBytes = new byte[32];
                System.arraycopy(hashed, 0, keyBytes, 0, Math.min(hashed.length, 32));
            } catch (java.security.NoSuchAlgorithmException e) {
                // SHA-256必然可用，回退到补齐逻辑
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
                keyBytes = paddedKey;
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 根据用户信息生成JWT令牌
     *
     * @param userId   用户ID（可以是User.id或Employee.id）
     * @param username 用户名
     * @param role     角色编码（如ROLE_SUPER_ADMIN / ROLE_STAFF / ROLE_USER）
     * @param loginType 登录类型（USER-用户端 / ADMIN-管理端）
     * @return 生成的JWT令牌字符串
     */
    public String generateToken(Long userId, String username, String role, String loginType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        // 构建JWT的Payload（声明集）
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("loginType", loginType);

        return Jwts.builder()
                .claims(claims)                      // 设置自定义声明
                .subject(username)                   // 设置主题（通常是用户名）
                .issuedAt(now)                       // 签发时间
                .expiration(expiryDate)              // 过期时间
                .signWith(getSigningKey())           // 使用HMAC-SHA256签名
                .compact();                          // 压缩为字符串
    }

    /**
     * 从JWT令牌中解析出所有声明(Claims)
     *
     * @param token JWT令牌字符串
     * @return 解析出的Claims对象
     * @throws JwtException 令牌无效或过期时抛出
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())         // 设置验证密钥
                .build()
                .parseSignedClaims(token)            // 解析签名令牌
                .getPayload();                       // 获取载荷
    }

    /**
     * 验证JWT令牌是否有效
     * 检查令牌是否被篡改、是否在有效期内
     *
     * @param token JWT令牌字符串
     * @return true表示有效，false表示无效
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT令牌已过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.debug("JWT令牌无效: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.debug("JWT令牌为空或格式错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从令牌中获取用户ID
     *
     * @param token JWT令牌字符串
     * @return 用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token JWT令牌字符串
     * @return 用户名
     */
    public String getUsername(String token) {
        Claims claims = parseClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * 从令牌中获取角色编码
     *
     * @param token JWT令牌字符串
     * @return 角色编码，如 ROLE_SUPER_ADMIN
     */
    public String getRole(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * 从令牌中获取登录类型
     *
     * @param token JWT令牌字符串
     * @return 登录类型（USER-用户端 / ADMIN-管理端）
     */
    public String getLoginType(String token) {
        Claims claims = parseClaims(token);
        return claims.get("loginType", String.class);
    }
}
