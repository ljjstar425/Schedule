package com.bar.voiceday;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 아주 단순한 메모리 저장소 (앱 끄면 날아감)
public class ScheduleStore {

    // key: "2025년 11월 11일" 같은 날짜 문자열
    private static final Map<String, List<Schedule>> data = new HashMap<>();

    private ScheduleStore() {}

    // 일정 추가
    public static void addSchedule(String dateKey, Schedule schedule) {
        List<Schedule> list = data.get(dateKey);
        if (list == null) {
            list = new ArrayList<>();
            data.put(dateKey, list);
        }
        list.add(schedule);
    }

    // 해당 날짜의 일정 목록 가져오기
    public static List<Schedule> getSchedules(String dateKey) {
        List<Schedule> list = data.get(dateKey);
        if (list == null) return new ArrayList<>();
        return new ArrayList<>(list);
    }
}