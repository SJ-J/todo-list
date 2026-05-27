package com.todo.todolist.service;

import com.todo.todolist.dto.RepeatRuleDto;
import com.todo.todolist.dto.ScheduleItemRequest;
import com.todo.todolist.dto.ScheduleItemResponse;
import com.todo.todolist.dto.UpdateType;
import com.todo.todolist.entity.Category;
import com.todo.todolist.entity.RepeatRule;
import com.todo.todolist.entity.ScheduleItem;
import com.todo.todolist.repository.CategoryRepository;
import com.todo.todolist.repository.RepeatRuleRepository;
import com.todo.todolist.repository.ScheduleItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleItemService {

    private final ScheduleItemRepository scheduleItemRepository;
    private final CategoryRepository categoryRepository;
    private final RepeatRuleRepository repeatRuleRepository;
    private final ObjectMapper objectMapper;

    // 전체 일정 목록 조회
    public List<ScheduleItemResponse> findAll() {
        return scheduleItemRepository.findAllWithRepeatRule().stream()
                .map(ScheduleItemResponse::from)
                .toList();
    }

    // 특정 날짜의 일정 목록 조회
    public List<ScheduleItemResponse> findByDate(LocalDate date) {
        // 날짜 조건으로 조회한 결과를 DTO로 변환하여 반환
        return scheduleItemRepository.findByDate(date).stream()
                .map(ScheduleItemResponse::from)
                .toList();
    }

    // 신규 일정 저장(단건)
    @Transactional
    public ScheduleItemResponse create(ScheduleItemRequest request) {
        // 현재 최대 정렬 순서에 +1을 부여하여 다음 순서 결정
        Integer maxOrder = scheduleItemRepository.findMaxSortOrder();
        int nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;

        Category category = resolveCategory(request.categoryId());

        // endDate 미입력 시 startDate로 대체하여 엔티티 생성
        ScheduleItem item = ScheduleItem.builder()
                .title(request.title())
                .emoji(request.emoji())
                .memo(request.memo())
                .startDate(request.startDate())
                .endDate(request.endDate() != null ? request.endDate() : request.startDate())
                .priority(request.priority())
                .priorityLabel(request.priorityLabel())
                .sortOrder(nextOrder)
                .completed(false)
                .completedOrder(null)
                .category(category)
                .build();

        return ScheduleItemResponse.from(scheduleItemRepository.save(item));
    }

    // 신규 일정 저장(반복 일정)
    @Transactional
    public List<ScheduleItemResponse> createRepeat(ScheduleItemRequest request) {
        RepeatRuleDto repeatRuleDto = request.repeatRule();
        Category category = resolveCategory(request.categoryId());

        // 1. Repeat Rule 저장
        RepeatRule repeatRule = repeatRuleRepository.save(RepeatRule.builder()
                .repeatType(repeatRuleDto.repeatType())
                .repeatInterval(repeatRuleDto.repeatInterval())
                .repeatDays(repeatRuleDto.repeatDays())
                .repeatEndType(repeatRuleDto.repeatEndType())
                .repeatEndDate(repeatRuleDto.repeatEndDate())
                .repeatCount(repeatRuleDto.repeatCount())
                .build());

        // 2. 반복 날짜 목록 계산
        List<LocalDate> dates = calculateRepeatDates(request.startDate(), repeatRuleDto);

        // 3. ScheduleItem 일괄 생성
        /* 모든 반복 일정을 일괄 생성하여 DB에 저장하는 방식 채택
        *  동적 생성 방식은 특정 날짜 수정 시 예외로 처리해야 해서 로직이 복잡
        */
        Integer maxOrder = scheduleItemRepository.findMaxSortOrder();
        int nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;
        long durationDays = ChronoUnit.DAYS.between(
                request.startDate(),
                request.endDate() != null ? request.endDate() : request.startDate()
        );

        List<ScheduleItem> items = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            LocalDate repeatStartDate = dates.get(i);
            items.add(ScheduleItem.builder()
                    .title(request.title())
                    .emoji(request.emoji())
                    .memo(request.memo())
                    .startDate(repeatStartDate)
                    .endDate(repeatStartDate.plusDays(durationDays))
                    .priority(request.priority())
                    .priorityLabel(request.priorityLabel())
                    .sortOrder(nextOrder + i)
                    .completed(false)
                    .category(category)
                    .repeatRule(repeatRule)
                    .repeatOrigin(i == 0)
                    .repeatSeq(i + 1)
                    .build());
        }
        return scheduleItemRepository.saveAll(items).stream()
                .map(ScheduleItemResponse::from)
                .toList();
    }

    // ID로 일정 조회 후 요청 데이터로 필드 업데이트
    @Transactional
    public ScheduleItemResponse update(Long id, ScheduleItemRequest request) {
        // 존재하지 않는 ID 요청 시 예외 발생
        ScheduleItem item = scheduleItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        UpdateType updateType = request.updateType() != null ? request.updateType() : UpdateType.THIS_ONLY;

        if (item.getRepeatRule() == null || updateType == UpdateType.THIS_ONLY) {
            // 미반복 또는 이 일정만(날짜 포함) 수정
            applyFullUpdate(item, request);
        } else if (updateType == UpdateType.FROM_THIS) {
            // 이후 일정 날짜 유지, 내용만 수정
            scheduleItemRepository.findByRepeatRuleAndRepeatSeqGreaterThanEqual(item.getRepeatRule(), item.getRepeatSeq())
                    .forEach(i -> applyContentUpdate(i, request));
        } else {
            // ALL: 내용 수정
            scheduleItemRepository.findByRepeatRule(item.getRepeatRule())
                    .forEach(i -> applyContentUpdate(i, request));
        }

        // endDate 미입력 시 startDate로 대체하여 각 필드 갱신
//        item.setTitle(request.title());
//        item.setMemo(request.memo());
//        item.setStartDate(request.startDate());
//        item.setEndDate(request.endDate() != null ? request.endDate() : request.startDate());
//        item.setPriority(request.priority());
//        item.setPriorityLabel(request.priorityLabel());
//        item.setCategory(resolveCategory(request.categoryId()));

        return ScheduleItemResponse.from(item);
    }

    // 일정 완료 상태 전환 및 완료 순서 갱신
    @Transactional
    public ScheduleItemResponse toggleComplete(Long id, boolean completed) {
        ScheduleItem item = scheduleItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        item.setCompleted(completed);
        if (completed) {
            // 완료 처리 시 현재 최대 완료 순서에 +1 부여
            Integer maxCompletedOrder = scheduleItemRepository.findMaxCompletedOrder();
            item.setCompletedOrder((maxCompletedOrder == null ? 0 : maxCompletedOrder) + 1);
        } else {
            // 완료 취소 시 완료 순서 초기화
            item.setCompletedOrder(null);
        }

        return ScheduleItemResponse.from(item);
    }

    // ID로 일정 삭제
    @Transactional
    public void delete(Long id, UpdateType updateType) {
        // 해당 ID의 일정 삭제
        ScheduleItem item = scheduleItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        if (item.getRepeatRule() == null || updateType == UpdateType.THIS_ONLY) {
            // 비반복 또는 이 일정만 삭제
            scheduleItemRepository.delete(item);
        } else if (updateType == UpdateType.FROM_THIS) {
            // 현재 seq 이후 반복 일정 일괄 삭제
            scheduleItemRepository.deleteAll(scheduleItemRepository.findByRepeatRuleAndRepeatSeqGreaterThanEqual(item.getRepeatRule(), item.getRepeatSeq()));
        } else {
            // 반복 규칙에 속한 모든 일정 및 규칙 삭제
            RepeatRule repeatRule = item.getRepeatRule();
            scheduleItemRepository.deleteAll(scheduleItemRepository.findByRepeatRule(repeatRule));
            repeatRuleRepository.delete(repeatRule);
        }
    }

    // THIS_ONLY 또는 비반복 일정 수정(날짜 포함)
    private void applyFullUpdate(ScheduleItem item, ScheduleItemRequest request) {
        item.setTitle(request.title());
        item.setEmoji(request.emoji());
        item.setMemo(request.memo());
        item.setStartDate(request.startDate());
        item.setEndDate(request.endDate() != null ? request.endDate() : request.startDate());
        item.setPriority(request.priority());
        item.setPriorityLabel(request.priorityLabel());
        item.setCategory(resolveCategory(request.categoryId()));
    }

    // FROM_THIS, ALL 수정(날짜 유지, 내용만 변경)
    private void applyContentUpdate(ScheduleItem item, ScheduleItemRequest request) {
        item.setTitle(request.title());
        item.setEmoji(request.emoji());
        item.setMemo(request.memo());
        item.setPriority(request.priority());
        item.setPriorityLabel(request.priorityLabel());
        item.setCategory(resolveCategory(request.categoryId()));
    }

    // 반복 규칙에 따라 생성할 날짜 목록 계산
    private List<LocalDate> calculateRepeatDates(LocalDate startDate, RepeatRuleDto repeatRuleDto) {
        // none: 365회 반복 적용
        int maxCount = switch (repeatRuleDto.repeatEndType()) {
            case count -> repeatRuleDto.repeatCount();
            case date, none -> 365;
        };

        List<LocalDate> dates = new ArrayList<>();

        switch (repeatRuleDto.repeatType()) {
            // 일간
            case daily -> {
                LocalDate current = startDate;
                for (int i = 0; i < maxCount; i++) {
                    if (isPastEndDate(repeatRuleDto, current)) break;
                    dates.add(current);
                    current = current.plusDays(repeatRuleDto.repeatInterval());
                }
            }
            // 주간
            case weekly -> {
                List<DayOfWeek> days = parseRepeatDays(repeatRuleDto.repeatDays());
                LocalDate weekStart = startDate.with(DayOfWeek.MONDAY);
                int count = 0;
                outer:
                while (count < maxCount) {
                    for (DayOfWeek day : days) {
                        LocalDate date = weekStart.with(day);
                        if (date.isBefore(startDate)) continue;
                        if (isPastEndDate(repeatRuleDto, date)) break outer;
                        dates.add(date);
                        if (++count >= maxCount) break outer;
                    }
                    weekStart = weekStart.plusWeeks(repeatRuleDto.repeatInterval());
                }
            }
            // 월간
            case monthly -> {
                LocalDate current = startDate;
                for (int i = 0; i < maxCount; i++) {
                    if (isPastEndDate(repeatRuleDto, current)) break;
                    dates.add(current);
                    current = current.plusMonths(repeatRuleDto.repeatInterval());
                }
            }
            // 연간
            case yearly -> {
                LocalDate current = startDate;
                for (int i = 0; i < maxCount; i++) {
                    if (isPastEndDate(repeatRuleDto, current)) break;
                    dates.add(current);
                    current = current.plusYears(repeatRuleDto.repeatInterval());
                }
            }
        }
        return dates;
    }

    // 반복 종료일 초과 여부 확인(종료 타입이 date인 경우에만 적용)
    private boolean isPastEndDate(RepeatRuleDto repeatRuleDto, LocalDate date) {
        return repeatRuleDto.repeatEndType() == RepeatRule.RepeatEndType.date && date.isAfter(repeatRuleDto.repeatEndDate());
    }

    // JSON 배열 문자열을 DayOfWeek 목록으로 변환
    private List<DayOfWeek> parseRepeatDays(String repeatDaysJson) {
        try {
            // 파싱 실패 시 빈 리스트 반환하여 해당 주간 반복 건너뜀
            int[] values = objectMapper.readValue(repeatDaysJson, int[].class);
            return Arrays.stream(values)
                    .mapToObj(DayOfWeek::of)
                    .sorted()
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    // categoryId로 카테고리 엔티티 조회
    private Category resolveCategory(Long categoryId) {
        // categoryId가 null이면 카테고리 없음으로 처리
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
    }
}