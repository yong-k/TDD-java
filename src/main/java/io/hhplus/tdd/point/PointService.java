package io.hhplus.tdd.point;

import io.hhplus.tdd.exception.DataNotFoundException;
import io.hhplus.tdd.exception.PointPolicyViolationException;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    private static final int MAX_CHARGE = 2_000_000;    // 1회 최대 충전 가능 포인트
    private static final int MAX_POINT = 2_000_000;     // 최대 보유 가능 포인트

    // userId 별로 Lock객체(빈 Object 인스턴스) 만들어서 관리
    private final ConcurrentHashMap<Long, Object> userLock = new ConcurrentHashMap<>();

    private Object getLock(long userId) {
        return userLock.computeIfAbsent(userId, id -> new Object());
    }

    /**
     * 주어진 사용자 ID에 해당하는 포인트 정보를 조회합니다.
     *
     * @param id 조회할 사용자의 고유 식별자
     * @return 해당 사용자의 포인트 데이터를 포함한 UserPoint 객체
     * @throws DataNotFoundException 사용자 포인트 정보가 존재하지 않을 경우 발생
     */
    public UserPoint selectById(long id) {
        UserPoint userPoint = userPointRepository.selectById(id);
        if (userPoint == null)
            throw new DataNotFoundException("해당 유저의 포인트 정보가 존재하지 않습니다. userId[" + id + "]");
        return userPoint;
    }

    /**
     * 특정 사용자의 포인트 충전 및 사용 이력을 조회합니다.
     *
     * @param userId 조회할 사용자의 고유 식별자
     * @return 해당 사용자의 포인트 이력 목록
     */
    public List<PointHistory> selectHistoryById(long userId) {
        return pointHistoryRepository.selectAllByUserId(userId);
    }

    /**
     * 특정 사용자의 포인트를 지정된 금액만큼 충전합니다.
     *
     * 다음 조건을 검증하여 예외를 발생시킵니다:
     *  - 충전 금액은 0보다 커야 합니다.
     *  - 충전 금액은 MAX_CHARGE 이하여야 합니다.
     *  - 충전 후 총 보유 포인트가 MAX_POINT를 초과할 수 없습니다.
     * 충전이 완료되면 사용자 포인트는 업데이트되고, 충전 기록이 저장됩니다.
     *
     * @param userId 충전할 사용자의 고유 식별자
     * @param amount 충전할 포인트 금액
     * @return 업데이트된 사용자 포인트 데이터를 포함한 UserPoint 객체
     * @throws PointPolicyViolationException 정책 위반(금액 제한, 최대치 초과 등)의 경우 발생
     */
    public UserPoint charge(long userId, long amount) {
        // 동시성 처리
        synchronized (getLock(userId)) {
            // 음수 체크
            if (amount <= 0)
                throw new PointPolicyViolationException("충전 금액은 0보다 커야합니다: " + amount);

            // 1회 최대 충전 금액 체크
            if (amount > MAX_CHARGE)
                throw new PointPolicyViolationException("1회 최대 충전 금액은 " + String.format("%,d", MAX_CHARGE) + "원입니다: 요청금액[" + amount + "]");

            UserPoint beforeCharge = userPointRepository.selectById(userId);

            // 최대 보유 포인트 초과여부 체크
            long afterChargePoint = beforeCharge.point() + amount;
            if (afterChargePoint > MAX_POINT)
                throw new PointPolicyViolationException("포인트는 최대 " + String.format("%,d", MAX_POINT) + "원까지 보유할 수 있습니다: 현재 포인트[" + beforeCharge.point() + "], 충전 포인트[" + amount + "]");

            UserPoint afterCharge = userPointRepository.insertOrUpdate(userId, afterChargePoint);
            pointHistoryRepository.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return afterCharge;
        }
    }

    /**
     * 특정 사용자의 포인트를 지정된 금액만큼 차감합니다.
     *
     * 다음 조건을 검증하여 예외를 발생시킵니다:
     *  - 사용 금액은 0보다 커야 합니다.
     *  - 사용 금액은 현재 보유 포인트 이하여야 합니다.
     * 사용이 완료되면 사용자 포인트는 업데이트되고, 사용 기록이 저장됩니다.
     *
     * @param userId 포인트 사용할 사용자의 고유 식별자
     * @param amount 사용할 포인트 금액
     * @return 업데이트된 사용자 포인트 데이터를 포함한 UserPoint 객체
     * @throws PointPolicyViolationException 정책 위반(금액 제한, 잔액 부족 등)의 경우 발생
     */
    public UserPoint use(long userId, long amount) {
        // 동시성 처리
        synchronized (getLock(userId)) {
            // 음수 체크
            if (amount <= 0)
                throw new PointPolicyViolationException("사용 금액은 0보다 커야합니다: " + amount);

            UserPoint beforeUse = userPointRepository.selectById(userId);

            // 보유포인트 < 사용포인트 체크
            if (beforeUse.point() < amount)
                throw new PointPolicyViolationException("포인트가 부족합니다: 현재 포인트[" + beforeUse.point() + "], 사용 포인트[" + amount + "]");

            UserPoint afterUse = userPointRepository.insertOrUpdate(userId, beforeUse.point() - amount);
            pointHistoryRepository.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

            return afterUse;
        }
    }
}
