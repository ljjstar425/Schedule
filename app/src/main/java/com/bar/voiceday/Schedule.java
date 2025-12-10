package com.bar.voiceday;

public class Schedule {
    public String id;          // Firestore 문서 ID
    public String dateKey;     // 기준 날짜: "2025년 12월 8일"
    public String title;

    public String startDate;   // 일정 시작 날짜
    public String endDate;     // 일정 끝 날짜
    public String startTime;   // "오전 09:00"
    public String endTime;     // "오후 06:00"
    public boolean allDay;     // 하루 종일 여부

    public String repeat;      // 반복: "안 함", "매일", "매주", "매월"
    public String place;       // 장소: 사용자가 입력한 문자열
    public String memo;        // 메모: 사용자가 입력한 메모 내용

    // Firebase에서 객체로 만들 때 필요한 기본 생성자
    public Schedule() {}

    public Schedule(String id,
                    String dateKey,
                    String title,
                    String startDate,
                    String endDate,
                    String startTime,
                    String endTime,
                    boolean allDay,
                    String repeat,
                    String place,
                    String memo) {

        this.id = id;
        this.dateKey = dateKey;
        this.title = title;

        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.allDay = allDay;

        this.repeat = repeat;
        this.place = place;
        this.memo = memo;
    }
}