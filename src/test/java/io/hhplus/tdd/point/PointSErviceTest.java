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


    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */


    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
}
