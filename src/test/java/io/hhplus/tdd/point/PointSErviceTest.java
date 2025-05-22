package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointTable userPointTable;

    @Test
    void 포인트조회() {
        // given
        long id = 1L;
        UserPoint userPoint = new UserPoint(1L, 1000, System.currentTimeMillis());
        when(userPointTable.selectById(id)).thenReturn(userPoint);

        // when
        UserPoint result = pointService.selectById(id);

        // then
        assertThat(result.point()).isEqualTo(1000);
    }


    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */


    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */


    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
}
