package io.hhplus.tdd.database;

import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserPointTableTest {

    private UserPointTable userPointTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
    }

    @Test
    void selectById_포인트조회() {
        // given
        long userId = 1L;
        UserPoint expected = userPointTable.insertOrUpdate(userId, 10000);

        // when
        UserPoint actual = userPointTable.selectById(userId);

        // then
        assertThat(actual.id()).isEqualTo(expected.id());
        assertThat(actual.point()).isEqualTo(expected.point());
    }

    @Test
    void selectById_존재하지않을때_null반환() {
        assertThat(userPointTable.selectById(999L)).isNull();
    }

    @Test
    void insertOrUpdate_신규회원() {
        // given
        long userId = 1L;
        long amount = 10000;

        // when
        UserPoint actual = userPointTable.insertOrUpdate(userId, amount);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.id()).isEqualTo(userId);
        assertThat(actual.point()).isEqualTo(amount);
        assertThat(userPointTable.selectById(userId)).isEqualTo(actual);
    }

    @Test
    void insertOrUpdate_기존회원() {
        // given
        long userId = 1L;

        // when
        UserPoint insert = userPointTable.insertOrUpdate(userId, 10000);
        UserPoint update = userPointTable.insertOrUpdate(userId, 5000);

        // then
        assertThat(update).isNotNull();
        assertThat(update.id()).isEqualTo(userId);
        assertThat(update.point()).isEqualTo(15000);
        assertThat(userPointTable.selectById(userId)).isEqualTo(update);
    }
}