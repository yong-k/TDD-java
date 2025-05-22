package io.hhplus.tdd.point;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @Test
    void 포인트조회() throws Exception {
        // given
        long userId = 1L;
        UserPoint expected = new UserPoint(userId, 10000, System.currentTimeMillis());
        when(pointService.selectById(userId)).thenReturn(expected);

        // when
        // then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expected.id()))
                .andExpect(jsonPath("$.point").value(expected.point()));
    }

    @Test
    void 포인트내역조회() throws Exception {
        // given
        long userId = 1L;
        List<PointHistory> histories = List.of(
                new PointHistory(1L, userId, 10000, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 5000, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointService.selectHistoryById(userId)).thenReturn(histories);

        // when
        // then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(histories.size()))
                .andExpect(jsonPath("$[0].amount").value(10000))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].amount").value(5000))
                .andExpect(jsonPath("$[1].type").value("USE"));
    }
}