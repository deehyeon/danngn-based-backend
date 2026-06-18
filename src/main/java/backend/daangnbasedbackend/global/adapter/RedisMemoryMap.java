package backend.daangnbasedbackend.global.adapter;

import backend.daangnbasedbackend.global.application.provided.MemoryMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    public void addToSortedSet(String key, String value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    @Override
    public List<String> reverseRangeFromSortedSetByScore(String key, double maxScore, long count) {
        Set<String> result = redisTemplate.opsForZSet().reverseRangeByScore(key, Double.NEGATIVE_INFINITY, maxScore, 0, count);
        return result != null ? new ArrayList<>(result) : List.of();
    }

    @Override
    public void removeFromSortedSet(String key, String value) {
        redisTemplate.opsForZSet().remove(key, value);
    }

    @Override
    public void trimSortedSet(String key, long maxSize) {
        redisTemplate.opsForZSet().removeRange(key, 0, -(maxSize + 1));
    }

    @Override
    public Set<String> popAllFromSet(String key) {
        RedisScript<List> script = RedisScript.of(
                "local m = redis.call('SMEMBERS', KEYS[1]) " +
                "if #m > 0 then redis.call('DEL', KEYS[1]) end " +
                "return m",
                List.class
        );
        List<String> result = redisTemplate.execute(script, List.of(key));
        if (result == null || result.isEmpty()) return Set.of();
        return new HashSet<>(result);
    }
}
