package com.ttms.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流器
 * 基于IP+用户名限制登录尝试次数，防止暴力破解
 * 规则：同一IP+用户名连续失败5次后锁定15分钟
 * 内存实现，单机部署适用
 */
@Slf4j
@Component
public class LoginRateLimiter {

    /** 最大失败尝试次数 */
    private static final int MAX_ATTEMPTS = 5;
    /** 锁定时长（秒） */
    private static final long LOCK_DURATION_SECONDS = 15 * 60;

    /** key: "IP:username", value: [失败次数, 锁定过期时间epoch秒] */
    private final Map<String, int[]> attempts = new ConcurrentHashMap<>();

    /**
     * 检查是否被锁定
     * @param ip 客户端IP
     * @param username 用户名
     * @return true=已锁定，拒绝登录
     */
    public boolean isLocked(String ip, String username) {
        String key = buildKey(ip, username);
        int[] record = attempts.get(key);
        if (record == null) return false;
        // 检查锁定是否过期
        if (record[0] >= MAX_ATTEMPTS) {
            if (Instant.now().getEpochSecond() < record[1]) {
                return true; // 仍在锁定期
            } else {
                attempts.remove(key); // 锁定期满，清除记录
            }
        }
        return false;
    }

    /**
     * 记录登录失败
     * @param ip 客户端IP
     * @param username 用户名
     * @return 剩余尝试次数（0=已锁定）
     */
    public int recordFailure(String ip, String username) {
        String key = buildKey(ip, username);
        int[] record = attempts.compute(key, (k, v) -> {
            if (v == null) return new int[]{1, 0};
            v[0]++;
            if (v[0] >= MAX_ATTEMPTS) {
                v[1] = (int) (Instant.now().getEpochSecond() + LOCK_DURATION_SECONDS);
                log.warn("登录限流触发: IP={}, username={}, 锁定{}分钟", ip, username, LOCK_DURATION_SECONDS / 60);
            }
            return v;
        });
        return Math.max(0, MAX_ATTEMPTS - record[0]);
    }

    /**
     * 登录成功后清除失败记录
     */
    public void clearOnSuccess(String ip, String username) {
        attempts.remove(buildKey(ip, username));
    }

    /**
     * 获取锁定的剩余时间（秒），未锁定返回0
     */
    public long getLockRemainingSeconds(String ip, String username) {
        int[] record = attempts.get(buildKey(ip, username));
        if (record == null || record[0] < MAX_ATTEMPTS) return 0;
        return Math.max(0, record[1] - Instant.now().getEpochSecond());
    }

    private String buildKey(String ip, String username) {
        return ip + ":" + (username != null ? username.toLowerCase() : "");
    }

    /**
     * 定期清理过期记录（每10分钟执行）
     */
    @Scheduled(fixedDelay = 600000)
    public void cleanExpired() {
        long now = Instant.now().getEpochSecond();
        attempts.entrySet().removeIf(entry -> {
            int[] v = entry.getValue();
            return v[1] > 0 && v[1] < now;
        });
    }
}
