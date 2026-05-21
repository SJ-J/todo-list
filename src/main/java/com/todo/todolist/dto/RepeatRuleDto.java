package com.todo.todolist.dto;

import com.todo.todolist.entity.RepeatRule;

import java.time.LocalDate;

public record RepeatRuleDto(
        RepeatRule.RepeatType repeatType,
        int repeatInterval,
        String repeatDays,
        RepeatRule.RepeatEndType repeatEndType,
        LocalDate repeatEndDate,
        Integer repeatCount
) {}