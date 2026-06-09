package backend.daangnbasedbackend.global.annotation.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key(); // 락의 고유 키
    long waitTime() default 5L; // 락을 획득하기 위해 대기하는 시간 (초)
    long leaseTime() default 3L; // 락을 획득한 후 보유하는 최대 시간 (초)
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}