package com.todo.todolist.controller;

import com.todo.todolist.dto.ScheduleItemRequest;
import com.todo.todolist.dto.ScheduleItemResponse;
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

    @GetMapping
    public List<ScheduleItemResponse> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date != null) {
            return scheduleItemService.findByDate(date);
        }
        return scheduleItemService.findAll();
    }

    @PostMapping
    public ScheduleItemResponse create(@RequestBody ScheduleItemRequest request) {
        return scheduleItemService.create(request);
    }

    @PutMapping("/{id}")
    public ScheduleItemResponse update(@PathVariable Long id, @RequestBody ScheduleItemRequest request) {
        return scheduleItemService.update(id, request);
    }

    @PatchMapping("/{id}/complete")
    public ScheduleItemResponse toggleComplete(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return scheduleItemService.toggleComplete(id, body.get("completed"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
