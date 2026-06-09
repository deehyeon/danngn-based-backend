package backend.daangnbasedbackend.global.adapter;

import backend.daangnbasedbackend.global.application.provided.MemoryMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisMemoryMap implements MemoryMap {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void setValue(String key, String value, Long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public long increment(String key, long delta) {
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result != null ? result : 0L;
    }

    @Override
    public String getAndDelete(String key) {
        return redisTemplate.opsForValue().getAndDelete(key);
    }

    @Override
    public void addToSet(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
    }

    @Override
    public Set<String> drainSet(String key) {
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return Set.of();
        }
        String procKey = key + ":PROC";
        try {
            redisTemplate.rename(key, procKey);
        } catch (Exception e) {
            return Set.of();
        }
        Set<String> members = redisTemplate.opsForSet().members(procKey);
        redisTemplate.delete(procKey);
        return members != null ? members : Set.of();
    }
}
