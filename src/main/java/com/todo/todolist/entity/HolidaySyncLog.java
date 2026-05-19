package com.todo.todolist.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 월별 공공 API 동기화 완료 이력 - 빈 달 포함 중복 API 호출 방지
@Entity
@Table(
        name = "holiday_sync_log",
        // (year, month) 조합 unique - DB 레벨에서 동시 중복 동기화 차단
        uniqueConstraints = @UniqueConstraint(columnNames = {"year", "month"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HolidaySyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    // 동기화 수행 시각
    @Column(nullable = false)
    private LocalDateTime syncedAt;

    // 정적 팩토리 메서드 - 현재 시각으로 syncedAt 자동 설정
    public static HolidaySyncLog of(int year, int month) {
        HolidaySyncLog log = new HolidaySyncLog();
        log.year = year;
        log.month = month;
        log.syncedAt = LocalDateTime.now();
        return log;
    }
}
