package io.hhplus.tdd.point;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    private final PointService pointService;

    @GetMapping("{id}")
    public ResponseEntity<UserPoint> point(@PathVariable long id) {
        UserPoint userPoint = pointService.selectById(id);
        return ResponseEntity.ok(userPoint);
    }

    @GetMapping("{id}/histories")
    public ResponseEntity<List<PointHistory>> history(@PathVariable long id) {
        List<PointHistory> histories = pointService.selectHistoryById(id);
        return ResponseEntity.ok(histories);
    }

    @PatchMapping("{id}/charge")
    public ResponseEntity<UserPoint> charge(@PathVariable long id, @RequestBody long amount) {
        UserPoint charge = pointService.charge(id, amount);
        return ResponseEntity.ok(charge);
    }

    @PatchMapping("{id}/use")
    public ResponseEntity<UserPoint> use(@PathVariable long id, @RequestBody long amount) {
        UserPoint use = pointService.use(id, amount);
        return ResponseEntity.ok(use);
    }
}
