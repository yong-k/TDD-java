package io.hhplus.tdd.point.lock;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class LockManager {
    // userId 별로 Lock객체(빈 Object 인스턴스) 만들어서 관리
    private final ConcurrentHashMap<Long, Object> userLock = new ConcurrentHashMap<>();

    public Object getLock(long userId) {
        return userLock.computeIfAbsent(userId, id -> new Object());
    }
}
