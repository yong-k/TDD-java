package io.hhplus.tdd.database;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PointHistoryTableTest {

    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setup() {
        pointHistoryTable = new PointHistoryTable();
    }

    @Test
    void 포인트내역_저장() {
        // given
        long userId = 1L;
        long amount = 10000;

        // when
        PointHistory actual = pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.id()).isEqualTo(1L);
        assertThat(actual.userId()).isEqualTo(userId);
        assertThat(actual.type()).isEqualTo(TransactionType.CHARGE);
    }

    @Test
    void 사용자별_내역조회() {
        // given
        long userId1 = 1L;
        long userId2 = 2L;
        long now = System.currentTimeMillis();
        pointHistoryTable.insert(userId1, 3000, TransactionType.CHARGE, now);
        pointHistoryTable.insert(userId2, 2000, TransactionType.CHARGE, now);
        pointHistoryTable.insert(userId1, 1000, TransactionType.USE, now);

        // when
        List<PointHistory> actual_user1 = pointHistoryTable.selectAllByUserId(userId1);
        List<PointHistory> actual_user2 = pointHistoryTable.selectAllByUserId(userId2);

        // then
        assertThat(actual_user1).hasSize(2);
        assertThat(actual_user1).allMatch(history -> history.userId() == userId1);
        assertThat(actual_user2).hasSize(1);
        assertThat(actual_user2).allMatch(history -> history.userId() == userId2);
    }

    @Test
    void 내역조회_이력없으면_빈리스트반환() {
        // given
        long userId = 9999L;

        // when
        List<PointHistory> actual = pointHistoryTable.selectAllByUserId(userId);

        // then
        assertThat(actual).isEmpty();
    }
}