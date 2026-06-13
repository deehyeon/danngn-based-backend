package backend.daangnbasedbackend.global.annotation.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedLockAspectTest {

    @Mock private RedissonClient redissonClient;
    @Mock private AopForTransaction aopForTransaction;
    @Mock private RLock rLock;
    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private MethodSignature signature;

    private DistributedLockAspect aspect;

    // @DistributedLock 어노테이션을 빌려오기 위한 더미 메서드
    @DistributedLock(key = "#productId")
    void dummyTarget(Long productId) {}

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        aspect = new DistributedLockAspect(redissonClient, aopForTransaction);

        Method method = DistributedLockAspectTest.class.getDeclaredMethod("dummyTarget", Long.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getParameterNames()).thenReturn(new String[]{"productId"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{42L});
        when(redissonClient.getLock("LOCK:42")).thenReturn(rLock);
    }

    @Test
    @DisplayName("락 획득 성공 — proceed를 호출하고 finally에서 unlock한다")
    void 락_획득_성공_proceed_호출_후_unlock_호출됨() throws Throwable {
        // given
        when(rLock.tryLock(5L, -1L, TimeUnit.SECONDS)).thenReturn(true);
        when(aopForTransaction.proceed(joinPoint)).thenReturn("ok");

        // when
        Object result = aspect.lock(joinPoint);

        // then
        assertThat(result).isEqualTo("ok");
        verify(aopForTransaction).proceed(joinPoint);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("락 획득 실패 — IllegalStateException을 던지고 proceed를 호출하지 않는다")
    void 락_획득_실패_IllegalStateException_proceed_미호출() throws Throwable {
        // given
        when(rLock.tryLock(5L, -1L, TimeUnit.SECONDS)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> aspect.lock(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("분산 락 획득에 실패했습니다");

        verify(aopForTransaction, never()).proceed(any());
        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("비즈니스 로직 예외 발생 — 예외가 전파되지만 unlock은 반드시 호출된다")
    void 비즈니스_로직_예외_발생시_unlock_호출됨() throws Throwable {
        // given
        when(rLock.tryLock(5L, -1L, TimeUnit.SECONDS)).thenReturn(true);
        when(aopForTransaction.proceed(joinPoint)).thenThrow(new RuntimeException("DB 오류"));

        // when & then
        assertThatThrownBy(() -> aspect.lock(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 오류");

        verify(rLock).unlock();
    }
}
