package io.hhplus.tdd.point;

import io.hhplus.tdd.exception.PointPolicyViolationException;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    private static final int MAX_CHARGE = 2_000_000;    // 1회 최대 충전 가능 포인트
    private static final int MAX_POINT = 2_000_000;     // 최대 보유 가능 포인트

    public UserPoint validateAndCalculateCharge(long amount) {
        // 음수 체크
        if (amount <= 0)
            throw new PointPolicyViolationException("충전 금액은 0보다 커야합니다: " + amount);

        // 1회 최대 충전 금액 체크
        if (amount > MAX_CHARGE)
            throw new PointPolicyViolationException("1회 최대 충전 금액은 " + String.format("%,d", MAX_CHARGE) + "원입니다: 요청금액[" + amount + "]");

        // 최대 보유 포인트 초과여부 체크
        long afterChargePoint = point + amount;
        if (afterChargePoint > MAX_POINT)
            throw new PointPolicyViolationException("포인트는 최대 " + String.format("%,d", MAX_POINT) + "원까지 보유할 수 있습니다: 현재 포인트[" + point + "], 충전 포인트[" + amount + "]");

        return new UserPoint(id, afterChargePoint, System.currentTimeMillis());
    }

    public UserPoint validateAndCalculateUse(long amount) {
        // 음수 체크
        if (amount <= 0)
            throw new PointPolicyViolationException("사용 금액은 0보다 커야합니다: " + amount);

        // 보유포인트 < 사용포인트 체크
        if (point < amount)
            throw new PointPolicyViolationException("포인트가 부족합니다: 현재 포인트[" + point + "], 사용 포인트[" + amount + "]");

        return new UserPoint(id, point - amount, System.currentTimeMillis());
    }

}
