package backend.daangnbasedbackend.global.application.provided;

import java.util.Set;

public interface MemoryMap {
    void setValue(String key, String value, Long timeout);
    String getValue(String key);
    void deleteValue(String key);

    // Atomic 연산
    long increment(String key, long delta);

    // 값을 반환하고 즉시 삭제
    String getAndDelete(String key);

    // Set 연산
    void addToSet(String key, String value);

    /**
     * Set 전체를 원자적으로 추출하고 삭제 (RENAME → SMEMBERS → DEL).
     * key가 존재하지 않으면 빈 Set 반환.
     */
    Set<String> drainSet(String key);
}
