package com.moksh.kontext.redis.service;

import com.moksh.kontext.redis.exception.RedisOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public void set(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Set key: {}", key);
        } catch (Exception e) {
            log.error("Error setting key: {}", key, e);
            throw new RedisOperationException("Failed to set key: " + key, e);
        }
    }

    public void set(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("Set key: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            log.error("Error setting key: {} with TTL", key, e);
            throw new RedisOperationException("Failed to set key with TTL: " + key, e);
        }
    }

    public String get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            log.debug("Get key: {} -> {}", key, value != null ? "found" : "not found");
            return value;
        } catch (Exception e) {
            log.error("Error getting key: {}", key, e);
            throw new RedisOperationException("Failed to get key: " + key, e);
        }
    }

    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            log.debug("Key exists: {} -> {}", key, exists);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking key existence: {}", key, e);
            throw new RedisOperationException("Failed to check key existence: " + key, e);
        }
    }

    public boolean delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Delete key: {} -> {}", key, deleted);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.error("Error deleting key: {}", key, e);
            throw new RedisOperationException("Failed to delete key: " + key, e);
        }
    }

    public long delete(Collection<String> keys) {
        try {
            Long deleted = redisTemplate.delete(keys);
            log.debug("Delete keys: {} -> {} deleted", keys.size(), deleted);
            return deleted != null ? deleted : 0;
        } catch (Exception e) {
            log.error("Error deleting keys", e);
            throw new RedisOperationException("Failed to delete keys", e);
        }
    }

    public boolean expire(String key, Duration timeout) {
        try {
            Boolean expired = redisTemplate.expire(key, timeout);
            log.debug("Set expiration for key: {} -> {}", key, expired);
            return Boolean.TRUE.equals(expired);
        } catch (Exception e) {
            log.error("Error setting expiration for key: {}", key, e);
            throw new RedisOperationException("Failed to set expiration for key: " + key, e);
        }
    }

    public Duration getExpire(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            log.debug("Get TTL for key: {} -> {}", key, ttl);
            return ttl != null && ttl > 0 ? Duration.ofSeconds(ttl) : Duration.ZERO;
        } catch (Exception e) {
            log.error("Error getting TTL for key: {}", key, e);
            throw new RedisOperationException("Failed to get TTL for key: " + key, e);
        }
    }

    public Set<String> keys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            log.debug("Get keys with pattern: {} -> {} keys found", pattern, keys != null ? keys.size() : 0);
            return keys;
        } catch (Exception e) {
            log.error("Error getting keys with pattern: {}", pattern, e);
            throw new RedisOperationException("Failed to get keys with pattern: " + pattern, e);
        }
    }

    public void setAdd(String key, String... values) {
        try {
            Long added = redisTemplate.opsForSet().add(key, values);
            log.debug("Add to set: {} -> {} added", key, added);
        } catch (Exception e) {
            log.error("Error adding to set: {}", key, e);
            throw new RedisOperationException("Failed to add to set: " + key, e);
        }
    }

    public void setAdd(String key, Duration ttl, String... values) {
        try {
            Long added = redisTemplate.opsForSet().add(key, values);
            redisTemplate.expire(key, ttl);
            log.debug("Add to set: {} with TTL: {} -> {} added", key, ttl, added);
        } catch (Exception e) {
            log.error("Error adding to set with TTL: {}", key, e);
            throw new RedisOperationException("Failed to add to set with TTL: " + key, e);
        }
    }

    public Set<String> setMembers(String key) {
        try {
            Set<String> members = redisTemplate.opsForSet().members(key);
            log.debug("Get set members: {} -> {} members", key, members != null ? members.size() : 0);
            return members;
        } catch (Exception e) {
            log.error("Error getting set members: {}", key, e);
            throw new RedisOperationException("Failed to get set members: " + key, e);
        }
    }

    public boolean setRemove(String key, String... values) {
        try {
            Long removed = redisTemplate.opsForSet().remove(key, (Object[]) values);
            log.debug("Remove from set: {} -> {} removed", key, removed);
            return removed != null && removed > 0;
        } catch (Exception e) {
            log.error("Error removing from set: {}", key, e);
            throw new RedisOperationException("Failed to remove from set: " + key, e);
        }
    }

    public boolean setIsMember(String key, String value) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(key, value);
            log.debug("Is set member: {} -> {}", key, isMember);
            return Boolean.TRUE.equals(isMember);
        } catch (Exception e) {
            log.error("Error checking set membership: {}", key, e);
            throw new RedisOperationException("Failed to check set membership: " + key, e);
        }
    }

    public void hashSet(String key, String field, String value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            log.debug("Hash set: {} field: {}", key, field);
        } catch (Exception e) {
            log.error("Error setting hash field: {} -> {}", key, field, e);
            throw new RedisOperationException("Failed to set hash field: " + key + " -> " + field, e);
        }
    }

    public String hashGet(String key, String field) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            log.debug("Hash get: {} field: {} -> {}", key, field, value != null ? "found" : "not found");
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("Error getting hash field: {} -> {}", key, field, e);
            throw new RedisOperationException("Failed to get hash field: " + key + " -> " + field, e);
        }
    }

    public boolean hashDelete(String key, String field) {
        try {
            Long deleted = redisTemplate.opsForHash().delete(key, field);
            log.debug("Hash delete: {} field: {} -> {}", key, field, deleted);
            return deleted != null && deleted > 0;
        } catch (Exception e) {
            log.error("Error deleting hash field: {} -> {}", key, field, e);
            throw new RedisOperationException("Failed to delete hash field: " + key + " -> " + field, e);
        }
    }

    public long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            log.debug("Increment key: {} -> {}", key, value);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Error incrementing key: {}", key, e);
            throw new RedisOperationException("Failed to increment key: " + key, e);
        }
    }

    public long increment(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            log.debug("Increment key: {} by {} -> {}", key, delta, value);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Error incrementing key: {} by {}", key, delta, e);
            throw new RedisOperationException("Failed to increment key: " + key, e);
        }
    }
}