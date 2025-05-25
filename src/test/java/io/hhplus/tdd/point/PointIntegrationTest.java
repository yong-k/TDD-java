package io.hhplus.tdd.point;


import io.hhplus.tdd.point.repository.UserPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
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

    @Autowired
    private UserPointRepository userPointRepository;

    @BeforeEach
    void prepare() {
        userPointRepository.insertOrUpdate(1L, 10000);
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
                .andDo(print());
    }

    @Test
    void 포인트충전_동시성문제() throws InterruptedException {
        long userId = 1L;

        // 10개의 스레드로 각각 10만원씩 충전 시도
        int threadCount = 10;
        long amount = 100_000;

        // ExecutorService: 스레드풀 만들어서 여러 작업 병렬 실행할 수 있게 해줌
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // CountDownLatch: 모든 스레드가 작업마칠 때까지 메인테스트스레드 기다리게 하는 동기화 도구
        CountDownLatch latch = new CountDownLatch(threadCount);

        // threadCount만큼 반복하면서, 충전 요청보냄
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/{id}/charge", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(String.valueOf(amount)))
                            .andDo(print());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();          // 모든 스레드가 끝날 때까지 대기
        executor.shutdown();    // 스레드풀 종료

        UserPoint finalPoint = pointService.selectById(userId);
        long expected = 10_000 + threadCount * amount;

        assertThat(finalPoint.point())
                .as("동시성 문제: 최종 포인트 일치하지 않음")
                .isEqualTo(expected);
    }

    @Test
    void 포인트사용_동시성문제() throws InterruptedException {
        long userId = 1L;
        userPointRepository.insertOrUpdate(userId, 1_000_000);

        // 10개의 스레드로 각각 10만원씩 사용 시도
        int threadCount = 10;
        long amount = 100_000;

        // ExecutorService: 스레드풀 만들어서 여러 작업 병렬 실행할 수 있게 해줌
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // CountDownLatch: 모든 스레드가 작업마칠 때까지 메인테스트스레드 기다리게 하는 동기화 도구
        CountDownLatch latch = new CountDownLatch(threadCount);

        // threadCount만큼 반복하면서, 사용 요청보냄
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/{id}/use", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(String.valueOf(amount)))
                            .andDo(print());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();          // 모든 스레드가 끝날 때까지 대기
        executor.shutdown();    // 스레드풀 종료

        UserPoint finalPoint = pointService.selectById(userId);
        long expected = 1_000_000 - threadCount * amount;

        assertThat(finalPoint.point())
                .as("동시성 문제: 최종 포인트 일치하지 않음")
                .isEqualTo(expected);
    }

    @Test
    void 포인트_충전_사용_순차처리테스트() throws InterruptedException {
        long userId = 1L;
        long initialAmount = 100_000;
        userPointRepository.insertOrUpdate(userId, initialAmount);

        int chargeCount = 10;
        int useCount = 10;
        long chargeAmount = 100_000;
        long useAmount = 100_000;

        int totalThreadCount = chargeCount + useCount;

        // ExecutorService: 스레드풀 만들어서 여러 작업 병렬 실행할 수 있게 해줌
        ExecutorService executor = Executors.newFixedThreadPool(totalThreadCount);

        // CountDownLatch: 모든 스레드가 작업마칠 때까지 메인테스트스레드 기다리게 하는 동기화 도구
        CountDownLatch latch = new CountDownLatch(totalThreadCount);

        // 충전 요청 10개
        for (int i = 0; i < chargeCount; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/{id}/charge", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(String.valueOf(chargeAmount)))
                            .andDo(print());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 사용 요청 10개
        for (int i = 0; i < useCount; i++) {
            executor.submit(() -> {
                try {
                    mockMvc.perform(patch("/point/{id}/use", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(String.valueOf(useAmount)))
                            .andDo(print());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();          // 모든 스레드가 끝날 때까지 대기
        executor.shutdown();    // 스레드풀 종료

        long actual = pointService.selectById(userId).point();

        // 최솟값: 사용 요청이 모두 먼저 처리된 경우
        long minExpected = initialAmount + (chargeCount * chargeAmount) - (useCount * useAmount);

        // 최댓값: 사용 요청이 모두 늦게 처리된 경우
        long maxExpected = initialAmount + (chargeCount * chargeAmount);

        assertThat(actual)
                .as("포인트 값이 음수거나 충전 총합보다 큰 경우 동시성 문제 가능성")
                .isGreaterThanOrEqualTo(0)
                .isLessThanOrEqualTo(maxExpected);
    }
}