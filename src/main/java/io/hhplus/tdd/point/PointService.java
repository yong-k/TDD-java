package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final int MAX_CHARGE = 2_000_000;    // 1회 최대 충전 가능 포인트
    private static final int MAX_POINT = 2_000_000;     // 최대 보유 가능 포인트

    // userId 별로 Lock객체(빈 Object 인스턴스) 만들어서 관리
    private final ConcurrentHashMap<Long, Object> userLock = new ConcurrentHashMap<>();

    private Object getLock(long userId) {
        return userLock.computeIfAbsent(userId, id -> new Object());
    }

    public UserPoint selectById(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> selectHistoryById(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint charge(long userId, long amount) {
        // 동시성 처리
        synchronized (getLock(userId)) {

            // 1회 최대 충전 금액 체크
            if (amount > MAX_CHARGE)
                throw new IllegalArgumentException("1회 최대 충전 금액은 " + String.format("%,d", MAX_CHARGE) + "원입니다.");

            UserPoint beforeCharge = userPointTable.selectById(userId);

            // 최대 보유 포인트 초과여부 체크
            long afterChargePoint = beforeCharge.point() + amount;
            if (afterChargePoint > MAX_POINT)
                throw new IllegalArgumentException("포인트는 최대 " + String.format("%,d", MAX_POINT) + "원까지 보유할 수 있습니다.");

            UserPoint afterCharge = userPointTable.insertOrUpdate(userId, afterChargePoint);
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return afterCharge;
        }
    }

    public UserPoint use(long userId, long amount) {
        // 동시성 처리
        synchronized (getLock(userId)) {
            UserPoint beforeUse = userPointTable.selectById(userId);

            // 보유포인트 < 사용포인트 체크
            if (beforeUse.point() < amount)
                throw new IllegalArgumentException("포인트가 부족합니다.");

            UserPoint afterUse = userPointTable.insertOrUpdate(userId, beforeUse.point() - amount);
            pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

            return afterUse;
        }
    }
}
