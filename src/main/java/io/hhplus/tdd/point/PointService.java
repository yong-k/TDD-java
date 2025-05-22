package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final int MAX_CHARGE = 2_000_000;    // 1회 최대 충전 가능 포인트
    private static final int MAX_POINT = 2_000_000;     // 최대 보유 가능 포인트

    public UserPoint selectById(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> selectHistoryById(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint charge(long userId, long amount) {
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
