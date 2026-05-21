package com.todo.todolist.controller;

import com.todo.todolist.dto.ScheduleItemRequest;
import com.todo.todolist.dto.ScheduleItemResponse;
import com.todo.todolist.dto.UpdateType;
import com.todo.todolist.service.ScheduleItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ScheduleItemController {

    private final ScheduleItemService scheduleItemService;

    // 일정 목록 조회(date 파라미터 없으면 전체, 있으면 해당 날짜 필터링)
    @GetMapping
    public List<ScheduleItemResponse> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // date 유무에 따라 전체 조회 또는 날짜 필터 조회 분기
        if (date != null) {
            return scheduleItemService.findByDate(date);
        }
        return scheduleItemService.findAll();
    }

    // 일정 생성(repeatRule 유무에 따라 단건/반복 분기)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ScheduleItemRequest request) {
        // repeatRule 존재 시 반복 일정 생성, 없으면 단건 생성
        if (request.repeatRule() != null) {
            return ResponseEntity.ok(scheduleItemService.createRepeat(request));
        }
        return ResponseEntity.ok(scheduleItemService.create(request));
    }

    // ID로 일정 수정
    @PutMapping("/{id}")
    public ScheduleItemResponse update(@PathVariable Long id, @RequestBody ScheduleItemRequest request) {
        return scheduleItemService.update(id, request);
    }

    // 일정 완료 상태 토글
    @PatchMapping("/{id}/complete")
    public ScheduleItemResponse toggleComplete(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        // 요청 body에서 completed 값 추출하여 서비스 위임
        return scheduleItemService.toggleComplete(id, body.get("completed"));
    }

    // ID로 일정 삭제(updateType으로 삭제 범위 결정)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(defaultValue = "THIS_ONLY") UpdateType updateType) {
        scheduleItemService.delete(id, updateType);
        return ResponseEntity.noContent().build();
    }
}