package com.todo.todolist.service;

import com.todo.todolist.dto.ScheduleItemRequest;
import com.todo.todolist.dto.ScheduleItemResponse;
import com.todo.todolist.entity.Category;
import com.todo.todolist.entity.ScheduleItem;
import com.todo.todolist.repository.CategoryRepository;
import com.todo.todolist.repository.ScheduleItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleItemService {

    private final ScheduleItemRepository scheduleItemRepository;
    private final CategoryRepository categoryRepository;

    public List<ScheduleItemResponse> findAll() {
        return scheduleItemRepository.findAll().stream()
                .map(ScheduleItemResponse::from)
                .toList();
    }

    public List<ScheduleItemResponse> findByDate(LocalDate date) {
        return scheduleItemRepository.findByDate(date).stream()
                .map(ScheduleItemResponse::from)
                .toList();
    }

    @Transactional
    public ScheduleItemResponse create(ScheduleItemRequest request) {
        Integer maxOrder = scheduleItemRepository.findMaxSortOrder();
        int nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;

        Category category = resolveCategory(request.categoryId());

        ScheduleItem item = ScheduleItem.builder()
                .title(request.title())
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

    @Transactional
    public ScheduleItemResponse update(Long id, ScheduleItemRequest request) {
        ScheduleItem item = scheduleItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        item.setTitle(request.title());
        item.setMemo(request.memo());
        item.setStartDate(request.startDate());
        item.setEndDate(request.endDate() != null ? request.endDate() : request.startDate());
        item.setPriority(request.priority());
        item.setPriorityLabel(request.priorityLabel());
        item.setCategory(resolveCategory(request.categoryId()));

        return ScheduleItemResponse.from(item);
    }

    @Transactional
    public ScheduleItemResponse toggleComplete(Long id, boolean completed) {
        ScheduleItem item = scheduleItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        item.setCompleted(completed);
        if (completed) {
            Integer maxCompletedOrder = scheduleItemRepository.findMaxCompletedOrder();
            item.setCompletedOrder((maxCompletedOrder == null ? 0 : maxCompletedOrder) + 1);
        } else {
            item.setCompletedOrder(null);
        }

        return ScheduleItemResponse.from(item);
    }

    @Transactional
    public void delete(Long id) {
        scheduleItemRepository.deleteById(id);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));
    }
}
