package io.hhplus.tdd.point;

import io.hhplus.tdd.exception.DataNotFoundException;
import io.hhplus.tdd.exception.PointPolicyViolationException;
import io.hhplus.tdd.point.lock.LockManager;
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
    private final LockManager lockManager;

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
     * 도메인 객체(UserPoint)를 통해 다음 정책을 검증 및 계산합니다:
     *  - 충전 금액은 0보다 커야 합니다.
     *  - 충전 금액은 MAX_CHARGE 이하여야 합니다.
     *  - 충전 후 총 보유 포인트가 MAX_POINT를 초과할 수 없습니다.
     * 정책을 통과하면 사용자 포인트는 업데이트되고, 충전 기록을 저장합니다.
     *
     * @param userId 충전할 사용자의 고유 식별자
     * @param amount 충전할 포인트 금액
     * @return 업데이트된 사용자 포인트 데이터를 포함한 UserPoint 객체
     * @throws PointPolicyViolationException 정책 위반 시 발생
     */
    public UserPoint charge(long userId, long amount) {
        // 동시성 처리
        synchronized (lockManager.getLock(userId)) {
            // 충전 전 UserPoint 객체
            UserPoint beforeCharge = userPointRepository.selectById(userId);

            // 유효성검증 및 포인트계산
            UserPoint updated = beforeCharge.validateAndCalculateCharge(amount);

            // 충전 후 UserPoint 객체
            UserPoint afterCharge = userPointRepository.insertOrUpdate(userId, updated.point());

            // 충전내역 저장
            pointHistoryRepository.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return afterCharge;
        }
    }

    /**
     * 특정 사용자의 포인트를 지정된 금액만큼 차감합니다.
     *
     * 도메인 객체(UserPoint)를 통해 다음 정책을 검증 및 계산합니다:
     *  - 사용 금액은 0보다 커야 합니다.
     *  - 사용 금액은 현재 보유 포인트 이하여야 합니다.
     * 정책을 통과하면 사용자 포인트는 업데이트되고, 사용 기록을 저장합니다.
     *
     * @param userId 포인트 사용할 사용자의 고유 식별자
     * @param amount 사용할 포인트 금액
     * @return 업데이트된 사용자 포인트 데이터를 포함한 UserPoint 객체
     * @throws PointPolicyViolationException 정책 위반 시 발생
     */
    public UserPoint use(long userId, long amount) {
        // 동시성 처리
        synchronized (lockManager.getLock(userId)) {
            // 사용 전 UserPoint 객체
            UserPoint beforeUse = userPointRepository.selectById(userId);

            // 유효성검증 및 포인트 계산
            UserPoint updated = beforeUse.validateAndCalculateUse(amount);

            // 사용 후 UserPoint 객체
            UserPoint afterUse = userPointRepository.insertOrUpdate(userId, updated.point());

            // 사용내역 저장
            pointHistoryRepository.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

            return afterUse;
        }
    }
}
