package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Test
    void 포인트조회() {
        // given
        long id = 1L;
        UserPoint expected = new UserPoint(id, 1000, System.currentTimeMillis());
        when(userPointTable.selectById(id)).thenReturn(expected);

        // when
        UserPoint actual = pointService.selectById(id);

        // then
        assertThat(actual.point()).isEqualTo(expected.point());
    }

    @Test
    void 포인트내역조회() {
        // given
        long userId = 1L;
        List<PointHistory> expected = new ArrayList<>();
        expected.add(new PointHistory(1L, userId, 10000, TransactionType.CHARGE, System.currentTimeMillis()));
        expected.add(new PointHistory(2L, userId, 5000, TransactionType.USE, System.currentTimeMillis()));
        expected.add(new PointHistory(3L, userId, 2000, TransactionType.CHARGE, System.currentTimeMillis()));
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expected);

        // when
        List<PointHistory> actual = pointService.selectHistoryById(userId);

        // then
        assertThat(actual).hasSize(expected.size());
        assertThat(actual.get(0).amount()).isEqualTo(expected.get(0).amount());
        assertThat(actual.get(1).type()).isEqualTo(expected.get(1).type());
    }

    @Test
    void 포인트충전() {
        // given (포인트충전 -> 충전내역에도 저장)
        long userId = 1L;
        long amount = 5000;
        long now = System.currentTimeMillis();
        UserPoint expected = new UserPoint(userId, 15000, now);

        // 충전하기 전 포인트
        UserPoint beforeCharge = new UserPoint(userId, 10000, now);
        when(userPointTable.selectById(userId)).thenReturn(beforeCharge);

        // 충전 후 포인트
        when(userPointTable.insertOrUpdate(userId, beforeCharge.point() + amount)).thenReturn(expected);

        // 충전내역 insert
        when(pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, now))
                .thenReturn(new PointHistory(1L, userId, amount, TransactionType.CHARGE, now));

        // when
        UserPoint actual = pointService.charge(userId, amount);

        // then (정책: 1회 최대 충전금액은 2,000,000)
        assertThat(actual.point()).isEqualTo(expected.point());
        assertThatThrownBy(() -> pointService.charge(userId, 2000001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1회 최대 충전 금액은 2,000,000원입니다.");
    }

    @Test
    void 포인트충전_최대잔고초과() {
        // given
        long userId = 1L;
        long amount = 10000;

        // 이미 최대 잔고 (2,000,000)
        UserPoint beforeCharge = new UserPoint(userId, 2000000, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(beforeCharge);

        // when
        // then (정책: 보유 포인트는 최대 2,000,000)
        assertThatThrownBy(() -> pointService.charge(userId, 10000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트는 최대 2,000,000원까지 보유할 수 있습니다.");
    }


    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
}
