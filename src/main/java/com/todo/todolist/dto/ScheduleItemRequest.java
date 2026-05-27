package com.todo.todolist.dto;

import java.time.LocalDate;

public record ScheduleItemRequest(
        String title,
        String emoji,
        String memo,
        LocalDate startDate,
        LocalDate endDate,
        Integer priority,
        String priorityLabel,
        Long categoryId,
        RepeatRuleDto repeatRule,
        UpdateType updateType
) {}
