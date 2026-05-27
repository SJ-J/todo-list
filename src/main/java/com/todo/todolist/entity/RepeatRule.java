package com.todo.todolist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repeat_rules")
@Getter
@Setter
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
    // 반복 종료 유형(none, count, date)
    private RepeatEndType repeatEndType;

    @Column(name = "REPEAT_COUNT")
    // 일정 반복 횟수(RepeatEndType -> count)
    private Integer repeatCount;

    @Column(name = "REPEAT_END_DATE")
    // 종료 날짜(RepeatEndType -> date)
    private LocalDate repeatEndDate;

    @Column(name = "REG_DATE", nullable = false, updatable = false)
    private LocalDateTime regDate;

    // 최초 저장 시점에 등록일 자동 세팅
    @PrePersist
    protected void onRegDate() {
        this.regDate = LocalDateTime.now();
    }
    public enum RepeatType {
        daily,      // n일마다
        weekly,     // n주마다
        monthly,    // n개월마다
        yearly;     // n년마다
    }
    public enum RepeatEndType {
        none,       // 계속 반복
        count,      // 일정 반복 횟수
        date        // 종료 날짜
    }
}