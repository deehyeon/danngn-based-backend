package backend.daangnbasedbackend.global.adapter;

import backend.daangnbasedbackend.global.application.MemoryMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

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
    public boolean checkExistsValue(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
