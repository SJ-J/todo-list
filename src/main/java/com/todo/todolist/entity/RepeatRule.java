package com.todo.todolist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repeat_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class RepeatRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPEAT_TYPE", nullable = false)
    private RepeatType repeatType;

    @Column(name = "REPEAT_INTERVAL", nullable = false)
    private int repeatInterval;

    // weekly 전용(JSON 배열을 문자열로 저장)
    @Column(name = "REPEAT_DAYS", columnDefinition = "json")
    private String repeatDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPEAT_END_TYPE", nullable = false)
    private RepeatEndType repeatEndType;

    @Column(name = "REPEAT_END_DATE")
    private LocalDate repeatEndDate;

    @Column(name = "REPEAT_COUNT")
    private Integer repeatCount;

    @Column(name = "REG_DATE", nullable = false, updatable = false)
    private LocalDateTime regDate;

    // 최초 저장 시점에 등록일 자동 세팅
    @PrePersist
    protected void onRegDate() {
        this.regDate = LocalDateTime.now();
    }
    public enum RepeatType {
        daily, weekly, monthly, yearly;
    }
    public enum RepeatEndType {
        none, date, count
    }
}
