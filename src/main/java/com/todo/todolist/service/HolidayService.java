package com.todo.todolist.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.todo.todolist.config.HolidayApiProperties;
import com.todo.todolist.dto.HolidayResponse;
import com.todo.todolist.entity.Holiday;
import com.todo.todolist.entity.HolidaySyncLog;
import com.todo.todolist.repository.HolidayRepository;
import com.todo.todolist.repository.HolidaySyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

// 공공데이터 공휴일 API 조회 및 DB 동기화
@Service
@RequiredArgsConstructor
public class HolidayService {

    // 스레드 안전, 재사용 가능하므로 static 공유
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private final HolidayRepository holidayRepository;
    private final HolidaySyncLogRepository syncLogRepository;
    private final HolidayApiProperties holidayApiProperties;
    private final RestClient holidayRestClient;

    // 동기화 이력 여부로 API 중복 호출 방지, isHoliday=Y인 항목만 반환
    @Transactional
    public List<HolidayResponse> getHolidays(int year, int month) {
        // 동기화 이력 없는 달만 API 호출
        if (!syncLogRepository.existsByYearAndMonth(year, month)) {
            syncFromOpenApi(year, month);
        }

        LocalDate start = monthStart(year, month);
        LocalDate end = monthEnd(year, month);
        return holidayRepository.findByLocDateBetween(start, end).stream()
                // 공휴일(Y)만 필터링, 대체공휴일·기념일 등 제외
                .filter(h -> "Y".equalsIgnoreCase(h.getIsHoliday()))
                .map(this::toResponse)
                .toList();
    }

    // 공공 API 호출 → DB 저장 → 동기화 완료 기록
    private void syncFromOpenApi(int year, int month) {
        String serviceKey = holidayApiProperties.serviceKey();
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("공휴일 API 인증키가 없습니다.");
        }

        // encode() 로 인코딩 1회 적용, toUri()로 URI 타입 전달하여 이중 인코딩 방지
        URI uri = UriComponentsBuilder
                .fromUriString(holidayApiProperties.url())
                .queryParam("solYear", year)
                .queryParam("solMonth", String.format("%02d", month))
                .queryParam("ServiceKey", serviceKey)
                .queryParam("numOfRows", 100)
                .queryParam("_type", "json")
                .encode()
                .build()
                .toUri();

        String body = holidayRestClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        List<Holiday> parsed = parseHolidayItems(body);
        LocalDate start = monthStart(year, month);
        LocalDate end = monthEnd(year, month);
        // 재동기화 시 기존 데이터를 제거 후 새 데이터 저장
        holidayRepository.deleteByLocDateBetween(start, end);
        holidayRepository.saveAll(parsed);
        // 동기화 완료 기록 — 다음 요청부터 API 재호출 생략
        syncLogRepository.save(HolidaySyncLog.of(year, month));
    }

    // API 응답 JSON → Holiday 엔티티 리스트 변환
    private List<Holiday> parseHolidayItems(String json) {
        try {
            JsonNode root = JSON_MAPPER.readTree(json);
            String resultCode = root.path("response").path("header").path("resultCode").asText();
            // "00" 이외는 API 오류
            if (!"00".equals(resultCode)) {
                String msg = root.path("response").path("header").path("resultMsg").asText("공휴일 API 오류");
                throw new IllegalStateException(msg);
            }

            JsonNode itemNode = root.path("response").path("body").path("items").path("item");
            // 해당 월 공휴일 없음 (빈 응답)
            if (itemNode.isMissingNode() || itemNode.isNull()) {
                return List.of();
            }

            List<Holiday> holidays = new ArrayList<>();
            // 단일 항목은 object, 복수 항목은 array로 내려옴
            if (itemNode.isArray()) {
                for (JsonNode item : itemNode) {
                    holidays.add(toHoliday(item));
                }
            } else {
                holidays.add(toHoliday(itemNode));
            }
            return holidays;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("공휴일 API 응답 파싱 실패", e);
        }
    }

    // sync_log 삭제 — 다음 getHolidays() 호출 시 공공 API 재호출 및 holiday 데이터 갱신
    @Transactional
    public void resetSync(int year, int month) {
        syncLogRepository.deleteByYearAndMonth(year, month);
    }

    // Holiday 엔티티 → 응답 DTO 변환
    private HolidayResponse toResponse(Holiday holiday) {
        return new HolidayResponse(holiday.getLocDate().toString(), holiday.getDateName());
    }

    // 해당 월 첫째 날
    private static LocalDate monthStart(int year, int month) {
        return LocalDate.of(year, month, 1);
    }

    // 해당 월 마지막 날 (28~31일 자동 계산)
    private static LocalDate monthEnd(int year, int month) {
        return YearMonth.of(year, month).atEndOfMonth();
    }

    // 공공 API locdate(YYYYMMDD 정수) → LocalDate 변환
    private static LocalDate fromApiLocdate(int locdate) {
        return LocalDate.of(locdate / 10000, (locdate / 100) % 100, locdate % 100);
    }

    // API 응답 단일 item 노드 → Holiday 엔티티 변환
    private static Holiday toHoliday(JsonNode item) {
        return Holiday.builder()
                .locDate(fromApiLocdate(item.path("locdate").asInt()))
                .dateName(item.path("dateName").asText())
                // API 미제공 시 기본값 N
                .isHoliday(item.path("isHoliday").asText("N"))
                .build();
    }
}
