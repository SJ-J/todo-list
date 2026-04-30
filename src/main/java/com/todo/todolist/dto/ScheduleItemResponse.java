package com.todo.todolist.dto;

import com.todo.todolist.entity.ScheduleItem;

import java.time.LocalDate;

public record ScheduleItemResponse(
        Long id,
        String title,
        String memo,
        LocalDate startDate,
        LocalDate endDate,
        Integer priority,
        String priorityLabel,
        Integer sortOrder,
        Boolean completed,
        Integer completedOrder,
        Long categoryId
) {
    public static ScheduleItemResponse from(ScheduleItem item) {
        return new ScheduleItemResponse(
                item.getId(),
                item.getTitle(),
                item.getMemo(),
                item.getStartDate(),
                item.getEndDate(),
                item.getPriority(),
                item.getPriorityLabel(),
                item.getSortOrder(),
                item.getCompleted(),
                item.getCompletedOrder(),
                item.getCategory() != null ? item.getCategory().getId() : null
        );
    }
}
