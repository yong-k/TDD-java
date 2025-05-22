package io.hhplus.tdd.point;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PointService pointService;

    @BeforeEach
    void prepare() {
        pointService.charge(1L, 10000);
    }

    @Test
    void 포인트충전_정상() throws Exception {
        // given
        long userId = 1L;
        long amount = 5000;
        UserPoint before = pointService.selectById(userId);

        // when
        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(before.point() + amount))
                .andDo(print());

    }

    @Test
    void 포인트충전_실패_1회최대충전금액초과() throws Exception {
        // given
        long userId = 1L;
        long amount = 2000001;

        // when
        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("1회 최대 충전 금액은 2,000,000원입니다."))
                .andDo(print());
    }

    @Test
    void 포인트충전_실패_최대보유포인트초과() throws Exception {
        // given
        long userId = 1L;
        long amount = 1_990_001;

        // when
        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("포인트는 최대 2,000,000원까지 보유할 수 있습니다."))
                .andDo(print());
    }

    @Test
    void 포인트사용_정상() throws Exception {
        // given
        long userId = 1L;
        long amount = 3000;
        UserPoint before = pointService.selectById(userId);

        // when
        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(before.point() - amount))
                .andDo(print());
    }

    @Test
    void 포인트사용_실패_포인트부족() throws Exception {
        // given
        long userId = 1L;
        UserPoint before = pointService.selectById(userId);
        long amount = before.point() + 10000;

        // when
        // then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("포인트가 부족합니다."))
                .andDo(print());
    }
}
