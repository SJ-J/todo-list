package com.todo.todolist.dto;

import com.todo.todolist.entity.RepeatRule;
import com.todo.todolist.entity.ScheduleItem;

import java.time.LocalDate;

public record ScheduleItemResponse(
        Long id,
        String title,
        String emoji,
        String memo,
        LocalDate startDate,
        LocalDate endDate,
        Integer priority,
        String priorityLabel,
        Integer sortOrder,
        Boolean completed,
        Integer completedOrder,
        Long categoryId,
        boolean repeatOrigin,
        Integer repeatSeq,
        Long repeatRuleId,
        RepeatRule.RepeatType repeatType,
        Integer repeatInterval,
        String repeatDays,
        RepeatRule.RepeatEndType repeatEndType,
        LocalDate repeatEndDate,
        Integer repeatCount
) {
    // ScheduleItem 엔티티를 응답 DTO로 변환
    public static ScheduleItemResponse from(ScheduleItem item) {
        // category, repeatRule은 null 가능하므로 null-safe 처리
        RepeatRule rule = item.getRepeatRule();
        return new ScheduleItemResponse(
                item.getId(),
                item.getTitle(),
                item.getEmoji(),
                item.getMemo(),
                item.getStartDate(),
                item.getEndDate(),
                item.getPriority(),
                item.getPriorityLabel(),
                item.getSortOrder(),
                item.getCompleted(),
                item.getCompletedOrder(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.isRepeatOrigin(),
                item.getRepeatSeq(),
                rule != null ? rule.getId() : null,
                rule != null ? rule.getRepeatType() : null,
                rule != null ? rule.getRepeatInterval() : null,
                rule != null ? rule.getRepeatDays() : null,
                rule != null ? rule.getRepeatEndType() : null,
                rule != null ? rule.getRepeatEndDate() : null,
                rule != null ? rule.getRepeatCount() : null
        );
    }
}
