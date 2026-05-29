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
import java.util.Objects;

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
        ScheduleItem item = buildScheduleItem(
                request,
                request.startDate(),
                request.endDate() != null ? request.endDate() : request.startDate(),
                nextOrder,
                category,
                null,
                false,
                null
        );

        return ScheduleItemResponse.from(scheduleItemRepository.save(item));
    }

    // 신규 일정 저장(반복 일정)
    @Transactional
    public List<ScheduleItemResponse> createRepeat(ScheduleItemRequest request) {
        RepeatRuleDto repeatRuleDto = request.repeatRule();
        Category category = resolveCategory(request.categoryId());

        // 1. Repeat Rule 저장
        RepeatRule repeatRule = saveRepeatRule(repeatRuleDto);

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

        List<ScheduleItem> items = buildRepeatItems(request, dates, durationDays, nextOrder, category, repeatRule, 0);
        return scheduleItemRepository.saveAll(items).stream()
                .map(ScheduleItemResponse::from)
                .toList();
    }

    // ID로 일정 조회 후 요청 데이터로 필드 업데이트
    @Transactional
    public ScheduleItemResponse update(Long id, ScheduleItemRequest request) {
        // 수정 대상 일정 조회
        ScheduleItem item = scheduleItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        UpdateType updateType = request.updateType() != null ? request.updateType() : UpdateType.THIS_ONLY;
        boolean ruleChanged = isRepeatRuleChanged(item.getRepeatRule(), request.repeatRule());

        // 단건 일정을 반복 일정으로 전환하는 경우
        if (item.getRepeatRule() == null && request.repeatRule() != null) {
            return ScheduleItemResponse.from(convertSingleToRepeat(item, request));
        }

        // 반복 일정이 아니거나 이 일정만 수정하는 경우
        if (item.getRepeatRule() == null || updateType == UpdateType.THIS_ONLY) {
            // 이 일정만 수정 시 주기 변경 시도하면 예외 발생
            if (ruleChanged) {
                throw new IllegalArgumentException("반복 주기는 해당 일정만 수정할 수 없습니다.");
            }
            // 상세 정보 업데이트 및 개별 수정 여부 마킹
            applyFullUpdate(item, request);
            if (item.getRepeatRule() != null) {
                item.setCustomized(true);
            }
        } else {
            // 이후 또는 전체 수정
            if (ruleChanged) {
                // 주기 변경 시 기존 삭제 후 재생성
                return ScheduleItemResponse.from(updateRepeatRule(item, request, updateType));
            } else {
                // 내용만 변경 시 개별 수정된 일정 제외하고 업데이트
                updateContentOnly(item, request, updateType);
            }
        }
        return ScheduleItemResponse.from(item);
    }

    // 반복 규칙 변경 여부 확인
    private boolean isRepeatRuleChanged(RepeatRule entity, RepeatRuleDto dto) {
        // 둘 다 없으면 변경 없음
        if (entity == null && dto == null) return false;
        // 한쪽만 있거나 필드 값이 다르면 변경됨으로 간주
        // 요청 규칙이 없으면 내용 수정으로 처리
        if (dto == null) return false;
        if (entity == null) return true;

        return entity.getRepeatType() != dto.repeatType() ||
                entity.getRepeatInterval() != dto.repeatInterval() ||
                !Objects.equals(entity.getRepeatDays(), dto.repeatDays()) ||
                entity.getRepeatEndType() != dto.repeatEndType() ||
                !Objects.equals(entity.getRepeatEndDate(), dto.repeatEndDate()) ||
                !Objects.equals(entity.getRepeatCount(), dto.repeatCount());
    }

    // 반복 규칙 엔티티 저장
    private RepeatRule saveRepeatRule(RepeatRuleDto repeatRuleDto) {
        return repeatRuleRepository.save(RepeatRule.builder()
                .repeatType(repeatRuleDto.repeatType())
                .repeatInterval(repeatRuleDto.repeatInterval())
                .repeatDays(repeatRuleDto.repeatDays())
                .repeatEndType(repeatRuleDto.repeatEndType())
                .repeatEndDate(repeatRuleDto.repeatEndDate())
                .repeatCount(repeatRuleDto.repeatCount())
                .build());
    }

    // 요청 정보로 일정 엔티티 생성
    private ScheduleItem buildScheduleItem(
            ScheduleItemRequest request,
            LocalDate startDate,
            LocalDate endDate,
            int sortOrder,
            Category category,
            RepeatRule repeatRule,
            boolean repeatOrigin,
            Integer repeatSeq
    ) {
        return ScheduleItem.builder()
                .title(request.title())
                .emoji(request.emoji())
                .memo(request.memo())
                .startDate(startDate)
                .endDate(endDate)
                .priority(request.priority())
                .priorityLabel(request.priorityLabel())
                .sortOrder(sortOrder)
                .completed(false)
                .completedOrder(null)
                .category(category)
                .repeatRule(repeatRule)
                .repeatOrigin(repeatOrigin)
                .repeatSeq(repeatSeq)
                .customized(false)
                .build();
    }

    // 반복 날짜 목록으로 일정 엔티티 생성
    private List<ScheduleItem> buildRepeatItems(
            ScheduleItemRequest request,
            List<LocalDate> dates,
            long durationDays,
            int startSortOrder,
            Category category,
            RepeatRule repeatRule,
            int startIndex
    ) {
        List<ScheduleItem> items = new ArrayList<>();
        for (int i = startIndex; i < dates.size(); i++) {
            LocalDate startDate = dates.get(i);
            items.add(buildScheduleItem(
                    request,
                    startDate,
                    startDate.plusDays(durationDays),
                    startSortOrder + i - startIndex,
                    category,
                    repeatRule,
                    i == 0,
                    i + 1
            ));
        }
        return items;
    }

    // 단건 일정을 반복 일정으로 변환
    private ScheduleItem convertSingleToRepeat(ScheduleItem item, ScheduleItemRequest request) {
        RepeatRuleDto rd = request.repeatRule();

        // 새 반복 규칙 저장
        RepeatRule newRule = saveRepeatRule(rd);

        // 반복 시작일과 기간 기준으로 생성할 날짜 계산
        LocalDate baseStartDate = request.startDate() != null ? request.startDate() : item.getStartDate();
        long durationDays = resolveDurationDays(item, request);
        List<LocalDate> dates = calculateRepeatDates(baseStartDate, rd);
        if (dates.isEmpty()) {
            throw new IllegalArgumentException("생성할 반복 일정이 없습니다.");
        }

        Integer maxOrder = scheduleItemRepository.findMaxSortOrder();
        int nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;
        Category category = resolveCategory(request.categoryId());

        // 기존 단건 일정을 첫 반복 일정으로 업데이트
        LocalDate firstStartDate = dates.get(0);
        item.setTitle(request.title());
        item.setEmoji(request.emoji());
        item.setMemo(request.memo());
        item.setStartDate(firstStartDate);
        item.setEndDate(firstStartDate.plusDays(durationDays));
        item.setPriority(request.priority());
        item.setPriorityLabel(request.priorityLabel());
        item.setCategory(category);
        item.setRepeatRule(newRule);
        item.setRepeatOrigin(true);
        item.setRepeatSeq(1);
        item.setCustomized(false);

        // 두 번째 반복 날짜부터 새 일정 생성
        List<ScheduleItem> newItems = buildRepeatItems(request, dates, durationDays, nextOrder, category, newRule, 1);
        scheduleItemRepository.saveAll(newItems);
        return item;
    }

    // 내용만 일괄 변경 (개별 수정 일정 보호)
    private void updateContentOnly(ScheduleItem item, ScheduleItemRequest request, UpdateType updateType) {
        List<ScheduleItem> targets;
        if (updateType == UpdateType.FROM_THIS) {
            // 현재 순번 이후 일정 조회
            targets = scheduleItemRepository.findByRepeatRuleAndRepeatSeqGreaterThanEqualOrderByRepeatSeqAsc(item.getRepeatRule(), item.getRepeatSeq());
        } else {
            // 전체 일정 조회
            targets = scheduleItemRepository.findByRepeatRuleOrderByRepeatSeqAsc(item.getRepeatRule());
        }

        // 개별 수정되지 않은 일정들만 골라서 내용 업데이트
        targets.stream()
                .filter(i -> !i.isCustomized())
                .forEach(i -> applyContentUpdate(i, request));
    }

    // 반복 주기 변경 처리 (삭제 후 재생성)
    private ScheduleItem updateRepeatRule(ScheduleItem item, ScheduleItemRequest request, UpdateType updateType) {
        RepeatRule oldRule = item.getRepeatRule();
        LocalDate baseStartDate;
        long durationDays;

        if (updateType == UpdateType.FROM_THIS) {
            // 재생성 일정 기간 기준 계산
            durationDays = resolveDurationDays(item, request);
            // 현재 일정 포함 이후 일정들 삭제
            List<ScheduleItem> targets = scheduleItemRepository.findByRepeatRuleAndRepeatSeqGreaterThanEqualOrderByRepeatSeqAsc(oldRule, item.getRepeatSeq());
            scheduleItemRepository.deleteAll(targets);

            // 재생성 기준일 설정: 요청 startDate 우선, 없으면 현재 일정의 기존 시작일 사용
            baseStartDate = request.startDate() != null ? request.startDate() : item.getStartDate();
        } else {
            // ALL: 해당 규칙의 모든 일정 삭제
            List<ScheduleItem> targets = scheduleItemRepository.findByRepeatRuleOrderByRepeatSeqAsc(oldRule);
            // 첫 번째 일정의 시작일을 전체 재생성의 기준일로 사용
            ScheduleItem firstItem = targets.get(0);
            baseStartDate = firstItem.getStartDate();
            // 전체 재생성 일정 기간 기준 계산
            durationDays = resolveDurationDays(firstItem, request);

            scheduleItemRepository.deleteAll(targets);
            repeatRuleRepository.delete(oldRule);
        }

        // 새로운 반복 규칙 저장
        RepeatRuleDto rd = request.repeatRule();
        RepeatRule newRule = saveRepeatRule(rd);

        // 새로운 규칙에 따라 날짜 목록 계산
        List<LocalDate> dates = calculateRepeatDates(baseStartDate, rd);

        // 새로운 일정들 생성 및 저장
        Integer maxOrder = scheduleItemRepository.findMaxSortOrder();
        int nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;

        Category category = resolveCategory(request.categoryId());
        List<ScheduleItem> newItems = buildRepeatItems(request, dates, durationDays, nextOrder, category, newRule, 0);
        return scheduleItemRepository.saveAll(newItems).get(0);
    }

    // 요청 기간이 있으면 계산하고 없으면 기존 일정 기간 유지
    private long resolveDurationDays(ScheduleItem item, ScheduleItemRequest request) {
        if (request.startDate() != null || request.endDate() != null) {
            LocalDate startDate = request.startDate() != null ? request.startDate() : item.getStartDate();
            LocalDate endDate = request.endDate() != null ? request.endDate() : startDate;
            return ChronoUnit.DAYS.between(startDate, endDate);
        }
        return ChronoUnit.DAYS.between(item.getStartDate(), item.getEndDate());
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