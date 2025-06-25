package com.moksh.kontext.auth.service;

import com.moksh.kontext.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRedisService {

    private final RedisService redisService;
    
    private static final String USER_TOKENS_PREFIX = "auth:user_tokens:";
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    public void storeAccessToken(UUID userId, String token, Duration ttl) {
        String userTokensKey = USER_TOKENS_PREFIX + userId + ":access";
        
        redisService.setAdd(userTokensKey, ttl, token);
        
        log.debug("Stored access token for user: {}", userId);
    }

    public void storeRefreshToken(UUID userId, String token, Duration ttl) {
        String userTokensKey = USER_TOKENS_PREFIX + userId + ":refresh";
        
        redisService.setAdd(userTokensKey, ttl, token);
        
        log.debug("Stored refresh token for user: {}", userId);
    }

    public boolean isAccessTokenValid(String token) {
        return !isTokenBlacklisted(token);
    }

    public boolean isRefreshTokenValid(String token) {
        return !isTokenBlacklisted(token);
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

    public void removeAccessToken(UUID userId, String token) {
        String userTokensKey = USER_TOKENS_PREFIX + userId + ":access";
        redisService.setRemove(userTokensKey, token);
        log.debug("Removed access token for user: {}", userId);
    }

    public void removeRefreshToken(UUID userId, String token) {
        String userTokensKey = USER_TOKENS_PREFIX + userId + ":refresh";
        redisService.setRemove(userTokensKey, token);
        log.debug("Removed refresh token for user: {}", userId);
    }

    public void revokeAllUserTokens(UUID userId) {
        String accessTokensKey = USER_TOKENS_PREFIX + userId + ":access";
        String refreshTokensKey = USER_TOKENS_PREFIX + userId + ":refresh";
        
        Set<String> accessTokens = redisService.setMembers(accessTokensKey);
        Set<String> refreshTokens = redisService.setMembers(refreshTokensKey);
        
        if (accessTokens != null) {
            for (String token : accessTokens) {
                blacklistToken(token, Duration.ofHours(24));
            }
        }
        
        if (refreshTokens != null) {
            for (String token : refreshTokens) {
                blacklistToken(token, Duration.ofDays(7));
            }
        }
        
        redisService.delete(accessTokensKey);
        redisService.delete(refreshTokensKey);
        
        log.info("Revoked all tokens for user: {}", userId);
    }

    public void revokeAllUserAccessTokens(UUID userId) {
        String accessTokensKey = USER_TOKENS_PREFIX + userId + ":access";
        
        Set<String> accessTokens = redisService.setMembers(accessTokensKey);
        
        if (accessTokens != null) {
            for (String token : accessTokens) {
                blacklistToken(token, Duration.ofHours(24));
            }
        }
        
        redisService.delete(accessTokensKey);
        
        log.info("Revoked all access tokens for user: {}", userId);
    }

    public void extendUserTokensTTL(UUID userId, Duration newTtl) {
        String accessTokensKey = USER_TOKENS_PREFIX + userId + ":access";
        String refreshTokensKey = USER_TOKENS_PREFIX + userId + ":refresh";
        
        redisService.expire(accessTokensKey, newTtl);
        redisService.expire(refreshTokensKey, newTtl);
        
        log.debug("Extended TTL for user tokens: {}", userId);
    }
}