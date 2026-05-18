package com.todo.todolist.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.todo.todolist.config.HolidayApiProperties;
import com.todo.todolist.dto.HolidayResponse;
import com.todo.todolist.entity.Holiday;
import com.todo.todolist.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final RestClient REST_CLIENT = RestClient.builder().build();

    private final HolidayRepository holidayRepository;
    private final HolidayApiProperties holidayApiProperties;

    @Transactional
    public List<HolidayResponse> getHolidays(int year, int month) {
        LocalDate start = monthStart(year, month);
        LocalDate end = monthEnd(year, month);

        if (holidayRepository.countByLocDateBetween(start, end) == 0) {
            return syncFromOpenApi(year, month);
        }

        return holidayRepository.findByLocDateBetween(start, end).stream()
                .filter(h -> "Y".equalsIgnoreCase(h.getIsHoliday()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<HolidayResponse> syncFromOpenApi(int year, int month) {
        String serviceKey = holidayApiProperties.serviceKey();
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException(
                    "공휴일 API 인증키가 없습니다."
            );
        }

        String uri = UriComponentsBuilder
                .fromUriString(holidayApiProperties.url())
                .queryParam("solYear", year)
                .queryParam("solMonth", String.format("%02d", month))
                .queryParam("ServiceKey", serviceKey)
                .queryParam("numOfRows", 100)
                .queryParam("_type", "json")
                .build()
                .toUriString();

        String body = REST_CLIENT.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        List<Holiday> parsed = parseHolidayItems(body);
        LocalDate start = monthStart(year, month);
        LocalDate end = monthEnd(year, month);
        holidayRepository.deleteByLocDateBetween(start, end);
        holidayRepository.saveAll(parsed);

        return parsed.stream()
                .filter(h -> "Y".equalsIgnoreCase(h.getIsHoliday()))
                .map(this::toResponse)
                .toList();
    }

    private List<Holiday> parseHolidayItems(String json) {
        try {
            JsonNode root = JSON_MAPPER.readTree(json);
            String resultCode = root.path("response").path("header").path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                String msg = root.path("response").path("header").path("resultMsg").asText("공휴일 API 오류");
                throw new IllegalStateException(msg);
            }

            JsonNode itemNode = root.path("response").path("body").path("items").path("item");
            if (itemNode.isMissingNode() || itemNode.isNull()) {
                return List.of();
            }

            List<Holiday> holidays = new ArrayList<>();
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

    private HolidayResponse toResponse(Holiday holiday) {
        return new HolidayResponse(holiday.getLocDate().toString(), holiday.getDateName());
    }

    private static LocalDate monthStart(int year, int month) {
        return LocalDate.of(year, month, 1);
    }

    private static LocalDate monthEnd(int year, int month) {
        return YearMonth.of(year, month).atEndOfMonth();
    }

    /** 공공 API locdate(20260505) → LocalDate */
    private static LocalDate fromApiLocdate(int locdate) {
        return LocalDate.of(locdate / 10000, (locdate / 100) % 100, locdate % 100);
    }

    private static Holiday toHoliday(JsonNode item) {
        return Holiday.builder()
                .locDate(fromApiLocdate(item.path("locdate").asInt()))
                .dateName(item.path("dateName").asText())
                .isHoliday(item.path("isHoliday").asText("N"))
                .build();
    }
}
