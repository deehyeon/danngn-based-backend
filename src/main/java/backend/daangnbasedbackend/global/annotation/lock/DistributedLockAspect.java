package backend.daangnbasedbackend.global.annotation.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {
    private static final String LOCK_PREFIX = "LOCK:";
    private final RedissonClient redissonClient;
    private final AopForTransaction aopForTransaction;

    @Around("@annotation(backend.daangnbasedbackend.global.annotation.lock.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        DistributedLock distributedLock = signature.getMethod().getAnnotation(DistributedLock.class);

        String key = LOCK_PREFIX + CustomSpringELParser.getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());
        RLock rLock = redissonClient.getLock(key);

        boolean acquired = false;
        try {
            acquired = rLock.tryLock(distributedLock.waitTime(), -1L, distributedLock.timeUnit());
            if (!acquired) {
                log.warn("락 획득 실패: {}", key);
                throw new IllegalStateException("분산 락 획득에 실패했습니다: " + key);
            }
            return aopForTransaction.proceed(joinPoint);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (acquired) {
                try {
                    rLock.unlock();
                } catch (IllegalMonitorStateException e) {
                    log.info("이미 반환된 락입니다: {}", key);
                }
            }
        }
    }
}