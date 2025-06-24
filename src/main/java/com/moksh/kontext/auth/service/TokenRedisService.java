package com.moksh.kontext.auth.service;

import com.moksh.kontext.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRedisService {

    private final RedisService redisService;
    
    private static final String ACCESS_TOKEN_PREFIX = "auth:access_token:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
    private static final String USER_TOKENS_PREFIX = "auth:user_tokens:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    public void storeAccessToken(Long userId, String token, Duration ttl) {
        String key = ACCESS_TOKEN_PREFIX + token;
        String userTokensKey = USER_TOKENS_PREFIX + userId + ":access";
        
        redisService.set(key, userId.toString(), ttl);
        redisService.setAdd(userTokensKey, ttl, token);
        
        log.debug("Stored access token for user: {}", userId);
    }

    public void storeRefreshToken(Long userId, String token, Duration ttl) {
        String key = REFRESH_TOKEN_PREFIX + token;
        String userTokensKey = USER_TOKENS_PREFIX + userId + ":refresh";
        
        redisService.set(key, userId.toString(), ttl);
        redisService.setAdd(userTokensKey, ttl, token);
        
        log.debug("Stored refresh token for user: {}", userId);
    }

    public boolean isAccessTokenValid(String token) {
        String key = ACCESS_TOKEN_PREFIX + token;
        return redisService.exists(key) && !isTokenBlacklisted(token);
    }

    public boolean isRefreshTokenValid(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        return redisService.exists(key) && !isTokenBlacklisted(token);
    }

    public Long getUserIdFromAccessToken(String token) {
        String key = ACCESS_TOKEN_PREFIX + token;
        String userId = redisService.get(key);
        return userId != null ? Long.parseLong(userId) : null;
    }

    public Long getUserIdFromRefreshToken(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        String userId = redisService.get(key);
        return userId != null ? Long.parseLong(userId) : null;
    }

    public void blacklistToken(String token, Duration ttl) {
        String key = BLACKLIST_PREFIX + token;
        redisService.set(key, "blacklisted", ttl);
        log.debug("Blacklisted token: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
    }

    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisService.exists(key);
    }

    public void removeAccessToken(String token) {
        String key = ACCESS_TOKEN_PREFIX + token;
        String userId = redisService.get(key);
        
        if (userId != null) {
            String userTokensKey = USER_TOKENS_PREFIX + userId + ":access";
            redisService.setRemove(userTokensKey, token);
        }
        
        redisService.delete(key);
        log.debug("Removed access token");
    }

    public void removeRefreshToken(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        String userId = redisService.get(key);
        
        if (userId != null) {
            String userTokensKey = USER_TOKENS_PREFIX + userId + ":refresh";
            redisService.setRemove(userTokensKey, token);
        }
        
        redisService.delete(key);
        log.debug("Removed refresh token");
    }

    public void revokeAllUserTokens(Long userId) {
        String accessTokensKey = USER_TOKENS_PREFIX + userId + ":access";
        String refreshTokensKey = USER_TOKENS_PREFIX + userId + ":refresh";
        
        Set<String> accessTokens = redisService.setMembers(accessTokensKey);
        Set<String> refreshTokens = redisService.setMembers(refreshTokensKey);
        
        if (accessTokens != null) {
            for (String token : accessTokens) {
                blacklistToken(token, Duration.ofHours(24));
                redisService.delete(ACCESS_TOKEN_PREFIX + token);
            }
        }
        
        if (refreshTokens != null) {
            for (String token : refreshTokens) {
                blacklistToken(token, Duration.ofDays(7));
                redisService.delete(REFRESH_TOKEN_PREFIX + token);
            }
        }
        
        redisService.delete(accessTokensKey);
        redisService.delete(refreshTokensKey);
        
        log.info("Revoked all tokens for user: {}", userId);
    }

    public void extendTokenTTL(String token, boolean isAccessToken, Duration newTtl) {
        String key = (isAccessToken ? ACCESS_TOKEN_PREFIX : REFRESH_TOKEN_PREFIX) + token;
        redisService.expire(key, newTtl);
        log.debug("Extended TTL for {} token", isAccessToken ? "access" : "refresh");
    }

    public Duration getTokenTTL(String token, boolean isAccessToken) {
        String key = (isAccessToken ? ACCESS_TOKEN_PREFIX : REFRESH_TOKEN_PREFIX) + token;
        return redisService.getExpire(key);
    }
}