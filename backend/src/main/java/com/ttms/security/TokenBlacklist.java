package com.ttms.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT Token黑名单
 * 用于实现Token主动失效（登出、修改密码、账号禁用等场景）
 * 内存实现：定期清理过期条目，适合单机部署
 * 多机部署应切换为Redis实现
 */
@Slf4j
@Component
public class TokenBlacklist {

    /** token -> 过期时间戳(epoch秒) */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * 将Token加入黑名单
     * @param token JWT令牌
     * @param expirationSeconds Token原始过期时间戳（epoch秒）
     */
    public void blacklist(String token, long expirationSeconds) {
        blacklist.put(token, expirationSeconds);
        log.debug("Token已加入黑名单, 过期时间: {}", Instant.ofEpochSecond(expirationSeconds));
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    /**
     * 定期清理已过期的黑名单条目（每分钟执行）
     */
    @Scheduled(fixedDelay = 60000)
    public void cleanExpired() {
        long now = Instant.now().getEpochSecond();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
        int removed = before - blacklist.size();
        if (removed > 0) {
            log.debug("清理过期Token黑名单: 移除{}条, 剩余{}条", removed, blacklist.size());
        }
    }

    public int size() {
        return blacklist.size();
    }
}
