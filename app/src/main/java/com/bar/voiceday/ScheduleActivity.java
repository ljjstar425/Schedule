package com.bar.voiceday;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.format.TitleFormatter;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

import androidx.core.content.ContextCompat;

public class ScheduleActivity extends AppCompatActivity {

    private TextView yearMonthText;
    private ImageButton downArrow;
    private MaterialCalendarView calendarView;

    // 오늘의 일정 패널
    private CardView dayPanel;
    private TextView panelDay;
    private TextView panelWeekday;
    private ImageButton btnAddEvent;
    private View dayPanelRoot;

    // 일정 리스트 컨테이너
    private LinearLayout scheduleListLayout;

    private CalendarDay selectedDate = null;

    // Firestore
    private FirebaseFirestore db;

    // 일정 있는 날짜에 점 찍는 데코레이터들 (개수별)
    private ColoredDotDecorator blueDotDecorator;   // 1개
    private ColoredDotDecorator greenDotDecorator;  // 2개
    private ColoredDotDecorator orangeDotDecorator; // 3개
    private ColoredDotDecorator redDotDecorator;    // 4개 이상

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ★ 항상 라이트 모드
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // Firestore
        db = FirebaseFirestore.getInstance();

        yearMonthText = findViewById(R.id.year_month);
        downArrow     = findViewById(R.id.down_arrow);
        calendarView  = findViewById(R.id.calendarView);

        // 날짜칸(타일) 크기 키우기
        calendarView.setTileHeightDp(60);

        dayPanel       = findViewById(R.id.day_panel);
        dayPanelRoot   = findViewById(R.id.day_panel_root);
        panelDay       = findViewById(R.id.panel_day);
        panelWeekday   = findViewById(R.id.panel_weekday);
        btnAddEvent    = findViewById(R.id.btn_add_event);
        scheduleListLayout = findViewById(R.id.schedule_list_container);

        // 상단 "YYYY년 MM월"
        Calendar calNow = Calendar.getInstance();
        int year  = calNow.get(Calendar.YEAR);
        int month = calNow.get(Calendar.MONTH) + 1;  // 0~11 → 1~12
        yearMonthText.setText(year + "년 " + month + "월");

        // 캘린더 헤더: "MM월"
        calendarView.setTitleFormatter(new TitleFormatter() {
            @Override
            public CharSequence format(CalendarDay day) {
                int m = day.getMonth() + 1; // 0~11 → 1~12
                return m + "월";
            }
        });

        // ===== 달력 데코레이션 =====

        // 1) 모든 날짜 밑에 회색 줄
        DayViewDecorator lineDecorator = new DayViewDecorator() {
            private final android.graphics.drawable.Drawable bg =
                    ContextCompat.getDrawable(ScheduleActivity.this, R.drawable.day_bottom_line);

            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return true;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setBackgroundDrawable(bg);
            }
        };

        // 2) 일요일 빨간 글씨
        DayViewDecorator sundayDecorator = new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                if (day == null) return false;
                Calendar c = Calendar.getInstance();
                c.setTime(day.getDate());
                return c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new ForegroundColorSpan(Color.RED));
            }
        };

        // 3) 오늘 날짜 회색 박스
        final CalendarDay today = CalendarDay.today();
        DayViewDecorator todayDecorator = new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day != null && day.equals(today);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setBackgroundDrawable(
                        ContextCompat.getDrawable(ScheduleActivity.this, R.drawable.today_background)
                );
            }
        };

        // 4) 개수별 점 데코레이터 (초기에는 빈 Set)
        blueDotDecorator   = new ColoredDotDecorator(new HashSet<>(), 0xFF3182F7); // 파랑
        greenDotDecorator  = new ColoredDotDecorator(new HashSet<>(), 0xFF2ECC71); // 연두
        orangeDotDecorator = new ColoredDotDecorator(new HashSet<>(), 0xFFF39C12); // 주황
        redDotDecorator    = new ColoredDotDecorator(new HashSet<>(), 0xFFE74C3C); // 빨강

        calendarView.addDecorators(
                lineDecorator,
                sundayDecorator,
                todayDecorator,
                blueDotDecorator,
                greenDotDecorator,
                orangeDotDecorator,
                redDotDecorator
        );

        // ===== 월 선택 팝업 =====
        downArrow.setOnClickListener(view -> {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.popup_month_list, null);
            ListView listView = popupView.findViewById(R.id.month_list_view);

            CalendarDay current = calendarView.getCurrentDate();
            int baseYear  = current.getYear();
            int baseMonth = current.getMonth() + 1; // 1~12

            List<String> items = new ArrayList<>();
            List<CalendarDay> monthList = new ArrayList<>();

            for (int offset = -12; offset <= 12; offset++) {
                int y = baseYear;
                int m = baseMonth + offset;

                while (m < 1) {
                    m += 12;
                    y -= 1;
                }
                while (m > 12) {
                    m -= 12;
                    y += 1;
                }

                Calendar c = Calendar.getInstance();
                c.set(y, m - 1, 1);
                CalendarDay cd = CalendarDay.from(c);

                monthList.add(cd);
                items.add(y + "년 " + m + "월");
            }

            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(ScheduleActivity.this,
                            android.R.layout.simple_list_item_1, items);
            listView.setAdapter(adapter);

            final PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    yearMonthText.getWidth() + downArrow.getWidth(),
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    true
            );
            popupWindow.setOutsideTouchable(true);
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            popupWindow.showAsDropDown(yearMonthText);

            listView.setOnItemClickListener((adapterView, v, pos, id) -> {
                CalendarDay selected = monthList.get(pos);
                calendarView.setCurrentDate(selected);
                calendarView.setSelectedDate(selected);

                int selYear  = selected.getYear();
                int selMonth = selected.getMonth() + 1;
                yearMonthText.setText(selYear + "년 " + selMonth + "월");

                popupWindow.dismiss();
            });
        });

        // ===== 날짜 클릭 시: 패널 열고 일정 보여주기 =====
        calendarView.setOnDateChangedListener(
                new OnDateSelectedListener() {
                    @Override
                    public void onDateSelected(@NonNull MaterialCalendarView widget,
                                               @Nullable CalendarDay date,
                                               boolean selected) {
                        if (date == null) return;

                        selectedDate = date;

                        panelDay.setText(String.valueOf(date.getDay()));
                        panelWeekday.setText(getKoreanWeekday(date));

                        dayPanelRoot.setVisibility(View.VISIBLE);

                        showSchedulesForDate(date);
                    }
                });

        // 패널 바깥 클릭 시 닫기
        dayPanelRoot.setOnClickListener(v -> dayPanelRoot.setVisibility(View.GONE));

        // 카드 영역 클릭 시는 닫히지 않게
        dayPanel.setOnClickListener(v -> { /* no-op */ });

        // + 버튼: 선택된 날짜 → RecordActivity로 전달
        btnAddEvent.setOnClickListener(v -> {
            CalendarDay selected = calendarView.getSelectedDate();
            if (selected == null) return;

            String dateStr = makeDateKey(selected); // "2025년 12월 9일" 형식
            Intent intent = new Intent(ScheduleActivity.this, RecordActivity.class);
            intent.putExtra("selected_date", dateStr);
            startActivity(intent);
        });

        // 앱 시작 시 한 번: 점 로딩
        loadEventDotsFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 일정이 추가/수정됐을 수 있으니 점 다시 로딩
        loadEventDotsFromFirestore();

        // 선택된 날짜가 남아 있으면 그 날짜 일정 다시 보여주기
        if (selectedDate != null) {
            showSchedulesForDate(selectedDate);
        }
    }

    // ======= 요일 이름 =======

    private String getKoreanWeekday(CalendarDay date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date.getDate()); // CalendarDay → Date

        switch (c.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:    return "월요일";
            case Calendar.TUESDAY:   return "화요일";
            case Calendar.WEDNESDAY: return "수요일";
            case Calendar.THURSDAY:  return "목요일";
            case Calendar.FRIDAY:    return "금요일";
            case Calendar.SATURDAY:  return "토요일";
            case Calendar.SUNDAY:    return "일요일";
        }
        return "";
    }

    // "2025년 12월 9일" 형식으로
    private String makeDateKey(CalendarDay date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date.getDate());
        int year  = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day   = c.get(Calendar.DAY_OF_MONTH);
        return year + "년 " + month + "월 " + day + "일";
    }

    // "2025년 12월 9일" → "12월 9일"
    private String toMonthDayFromFullDate(String fullDate) {
        try {
            int idxYear  = fullDate.indexOf("년");
            int idxMonth = fullDate.indexOf("월");
            int idxDay   = fullDate.indexOf("일");

            int month = Integer.parseInt(fullDate.substring(idxYear + 1, idxMonth).trim());
            int day   = Integer.parseInt(fullDate.substring(idxMonth + 1, idxDay).trim());

            return month + "월 " + day + "일";
        } catch (Exception e) {
            return fullDate;
        }
    }

    // Calendar → "12월 9일"
    private String formatMonthDay(Calendar c) {
        int m = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DAY_OF_MONTH);
        return m + "월 " + d + "일";
    }


    // 문자열 날짜 → Calendar
    private Calendar calendarFromDateString(String text) {
        try {
            int idxYear  = text.indexOf("년");
            int idxMonth = text.indexOf("월");
            int idxDay   = text.indexOf("일");
            if (idxYear == -1 || idxMonth == -1 || idxDay == -1) return null;

            int year  = Integer.parseInt(text.substring(0, idxYear).trim());
            int month = Integer.parseInt(text.substring(idxYear + 1, idxMonth).trim());
            int day   = Integer.parseInt(text.substring(idxMonth + 1, idxDay).trim());

            Calendar c = Calendar.getInstance();
            c.set(year, month - 1, day, 0, 0, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    // 두 날짜 사이 일수 (end - start)
    private long daysBetween(Calendar start, Calendar end) {
        long msPerDay = 24L * 60L * 60L * 1000L;
        long diffMs = end.getTimeInMillis() - start.getTimeInMillis();
        return diffMs / msPerDay;
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
                && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
    }

    // ======= 이 날짜에 이 일정이 보이는지 (반복 포함) =======

    private boolean isScheduleOnDate(Schedule s, Calendar target) {
        if (s == null || s.startDate == null || s.startDate.isEmpty()) return false;

        Calendar start = calendarFromDateString(s.startDate);
        Calendar end   = (s.endDate == null || s.endDate.isEmpty())
                ? calendarFromDateString(s.startDate)
                : calendarFromDateString(s.endDate);

        if (start == null || end == null) return false;

        // start > end 인 데이터가 있으면 swap
        if (start.after(end)) {
            Calendar tmp = start;
            start = end;
            end = tmp;
        }

        // target 시간 0시로 맞추기
        Calendar t = (Calendar) target.clone();
        t.set(Calendar.HOUR_OF_DAY, 0);
        t.set(Calendar.MINUTE, 0);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);

        String repeat = (s.repeat == null) ? "안 함" : s.repeat;

        // 반복 없음 → 그냥 구간 안에만
        if ("안 함".equals(repeat)) {
            return !t.before(start) && !t.after(end);
        }

        // target이 시작 이전이면 어떤 반복도 나타나지 않음
        if (t.before(start)) return false;

        // === 매일 === (하루짜리만 허용하도록 AddSchedule에서 막고 있음)
        if ("매일".equals(repeat)) {
            // start 이후 모든 날에 표시
            return !t.before(start);
        }

        // === 매주 === : 원래 패턴(start~end)을 주 단위로 반복
        if ("매주".equals(repeat)) {
            long rangeDays = daysBetween(start, end) + 1; // 최소 1
            long diff = daysBetween(start, t);
            if (diff < 0) return false;

            int offset = (int) (diff % 7);
            return offset < rangeDays;
        }

        // === 매월 === : 시작~종료 일(day) 범위를 매달 반복
        if ("매월".equals(repeat)) {
            int startDay = start.get(Calendar.DAY_OF_MONTH);
            int endDay   = end.get(Calendar.DAY_OF_MONTH);

            int startYM  = start.get(Calendar.YEAR) * 12 + start.get(Calendar.MONTH);
            int targetYM = t.get(Calendar.YEAR) * 12 + t.get(Calendar.MONTH);
            if (targetYM < startYM) return false;

            Calendar tmp = (Calendar) t.clone();
            int lastDayOfMonth = tmp.getActualMaximum(Calendar.DAY_OF_MONTH);

            int realStart = Math.min(startDay, lastDayOfMonth);
            int realEnd   = Math.min(endDay, lastDayOfMonth);

            int d = t.get(Calendar.DAY_OF_MONTH);
            return d >= realStart && d <= realEnd;
        }

        // 혹시 다른 값이 들어있으면 일단 반복 없는 것처럼 처리
        return !t.before(start) && !t.after(end);
    }

    // 일정 월일만 뽑음
    private String toMonthDay(String fullDate) {
        // fullDate: "2025년 12월 9일"
        try {
            int idxYear = fullDate.indexOf("년");
            int idxMonth = fullDate.indexOf("월");
            int idxDay = fullDate.indexOf("일");

            int month = Integer.parseInt(fullDate.substring(idxYear + 1, idxMonth).trim());
            int day   = Integer.parseInt(fullDate.substring(idxMonth + 1, idxDay).trim());

            return month + "월 " + day + "일";
        } catch (Exception e) {
            return fullDate; // 혹시 파싱 실패하면 그냥 전체 리턴
        }
    }


    // ======= 특정 날짜의 일정 목록 보여주기 (반복 포함) =======

    private void showSchedulesForDate(CalendarDay date) {
        if (date == null) return;

        // 이 날이 카드뷰에 표시되는 "타겟 날짜"
        Calendar target = Calendar.getInstance();
        target.setTime(date.getDate());
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        // 기존 리스트 비우기
        scheduleListLayout.removeAllViews();

        db.collection("schedules")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Schedule s = doc.toObject(Schedule.class);
                        if (s == null) continue;

                        // 이 날짜에 실제로 이 일정이 보이는지 (반복 포함해서) 체크
                        if (!isScheduleOnDate(s, target)) continue;

                        View itemView = getLayoutInflater().inflate(
                                R.layout.item_schedule, scheduleListLayout, false);

                        TextView titleView = itemView.findViewById(R.id.text_schedule_title);
                        TextView timeView  = itemView.findViewById(R.id.text_schedule_time);

                        titleView.setText(s.title);

                        // 하루 종일
                        if (s.allDay) {
                            timeView.setText("하루종일");
                        } else {
                            // 시간/날짜 표시 부분

                            Calendar startCal = calendarFromDateString(s.startDate);
                            Calendar endCal   = (s.endDate == null || s.endDate.isEmpty())
                                    ? calendarFromDateString(s.startDate)
                                    : calendarFromDateString(s.endDate);

                            // 데이터가 이상하면 그냥 비워두기
                            if (startCal == null || endCal == null
                                    || s.startTime == null || s.endTime == null
                                    || s.startTime.isEmpty() || s.endTime.isEmpty()) {
                                timeView.setText("");
                            } else {
                                String repeat = (s.repeat == null) ? "안 함" : s.repeat;
                                String labelStart;
                                String labelEnd;

                                // 1) 반복 없음: 원본 구간 그대로
                                if ("안 함".equals(repeat)) {
                                    labelStart = formatMonthDay(startCal);
                                    labelEnd   = formatMonthDay(endCal);
                                }
                                // 2) 매일: AddSchedule에서 하루짜리만 허용했으니까
                                //    이 타겟 날짜 하루 구간만 표시
                                else if ("매일".equals(repeat)) {
                                    labelStart = formatMonthDay(target);
                                    labelEnd   = formatMonthDay(target);
                                }
                                // 3) 매주: 원본(start~end) 패턴을 주 단위로 이동시킨 뒤,
                                //    이번 주 인스턴스의 start/end를 구해서 라벨로 사용
                                else if ("매주".equals(repeat)) {
                                    long diff = daysBetween(startCal, target);      // 시작일부터 몇 일 지났는지
                                    long range = Math.max(0, daysBetween(startCal, endCal)); // 0이면 하루짜리, 1이면 2일짜리 ...
                                    long weekIndex = diff / 7;                      // 몇 번째 주 인스턴스인지

                                    Calendar instStart = (Calendar) startCal.clone();
                                    instStart.add(Calendar.DAY_OF_MONTH, (int) (weekIndex * 7));

                                    Calendar instEnd = (Calendar) instStart.clone();
                                    instEnd.add(Calendar.DAY_OF_MONTH, (int) range);

                                    labelStart = formatMonthDay(instStart);
                                    labelEnd   = formatMonthDay(instEnd);
                                }
                                // 4) 매월: 해당 월에서 사용할 start/end day를 계산해서 표시
                                else if ("매월".equals(repeat)) {
                                    int startDay = startCal.get(Calendar.DAY_OF_MONTH);
                                    int endDay   = endCal.get(Calendar.DAY_OF_MONTH);

                                    Calendar instStart = (Calendar) target.clone();
                                    Calendar instEnd   = (Calendar) target.clone();

                                    int lastDay = target.getActualMaximum(Calendar.DAY_OF_MONTH);
                                    int realStart = Math.min(startDay, lastDay);
                                    int realEnd   = Math.min(endDay, lastDay);

                                    instStart.set(Calendar.DAY_OF_MONTH, realStart);
                                    instEnd.set(Calendar.DAY_OF_MONTH, realEnd);

                                    labelStart = formatMonthDay(instStart);
                                    labelEnd   = formatMonthDay(instEnd);
                                }
                                // 5) 혹시 모르는 이상한 값 들어오면 그냥 원본 구간
                                else {
                                    labelStart = formatMonthDay(startCal);
                                    labelEnd   = formatMonthDay(endCal);
                                }

                                // 시작/끝 날짜가 같으면 "12월 16일 09:00 ~ 18:00"
                                if (labelStart.equals(labelEnd)) {
                                    timeView.setText(labelStart + " " + s.startTime
                                            + " ~ " + s.endTime);
                                } else {
                                    // 다르면 "12월 16일 09:00 ~ 12월 17일 18:00"
                                    timeView.setText(labelStart + " " + s.startTime
                                            + " ~ " + labelEnd + " " + s.endTime);
                                }
                            }
                        }
                        //  여기서부터 "이 일정 아이템을 눌렀을 때" 수정 화면으로 이동
                        itemView.setOnClickListener(v -> {
                            Intent editIntent = new Intent(ScheduleActivity.this, AddScheduleActivity.class);

                            // 수정 모드임을 표시
                            editIntent.putExtra("mode", "edit");
                            // 어떤 문서를 수정할지 ID 전달
                            editIntent.putExtra("schedule_id", s.id);
                            // 필요하면 원래 dateKey도 전달 (지금은 옵션)
                            editIntent.putExtra("original_date_key", s.dateKey);

                            // AddScheduleActivity에서 그대로 쓰도록 하는 필드들
                            editIntent.putExtra("selected_date", s.startDate);
                            editIntent.putExtra("stt_title", s.title);
                            editIntent.putExtra("stt_start_date", s.startDate);
                            editIntent.putExtra("stt_end_date", s.endDate);
                            editIntent.putExtra("stt_start_time", s.startTime);
                            editIntent.putExtra("stt_end_time", s.endTime);
                            editIntent.putExtra("stt_all_day", s.allDay);
                            editIntent.putExtra("stt_memo", s.memo);
                            editIntent.putExtra("stt_repeat", s.repeat);
                            editIntent.putExtra("stt_place", s.place);

                            startActivity(editIntent);
                        });

                        scheduleListLayout.addView(itemView);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "일정 불러오기 실패: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }


    // ======= Firestore에서 일정 전체 가져와서 점 찍기 (반복 포함 + 개수별 색) =======

    private void loadEventDotsFromFirestore() {
        db.collection("schedules")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    // 날짜별 일정 개수 맵
                    Map<CalendarDay, Integer> eventCountMap = new HashMap<>();

                    // 점을 찍을 기간: 오늘 기준 -1년 ~ +1년
                    Calendar from = Calendar.getInstance();
                    from.add(Calendar.DAY_OF_YEAR, -365);
                    from.set(Calendar.HOUR_OF_DAY, 0);
                    from.set(Calendar.MINUTE, 0);
                    from.set(Calendar.SECOND, 0);
                    from.set(Calendar.MILLISECOND, 0);

                    Calendar to = Calendar.getInstance();
                    to.add(Calendar.DAY_OF_YEAR, 365);
                    to.set(Calendar.HOUR_OF_DAY, 0);
                    to.set(Calendar.MINUTE, 0);
                    to.set(Calendar.SECOND, 0);
                    to.set(Calendar.MILLISECOND, 0);

                    List<Schedule> schedules = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Schedule s = doc.toObject(Schedule.class);
                        if (s != null) schedules.add(s);
                    }

                    Calendar cur = (Calendar) from.clone();
                    while (!cur.after(to)) {
                        CalendarDay cd = CalendarDay.from(cur);
                        int count = 0;

                        for (Schedule s : schedules) {
                            if (isScheduleOnDate(s, cur)) {
                                count++;
                            }
                        }

                        if (count > 0) {
                            eventCountMap.put(cd, count);
                        }

                        cur.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    // 개수에 따라 날짜를 색별 Set으로 분리
                    Set<CalendarDay> blueDays   = new HashSet<>(); // 1개
                    Set<CalendarDay> greenDays  = new HashSet<>(); // 2개
                    Set<CalendarDay> orangeDays = new HashSet<>(); // 3개
                    Set<CalendarDay> redDays    = new HashSet<>(); // 4개 이상

                    for (Map.Entry<CalendarDay, Integer> entry : eventCountMap.entrySet()) {
                        CalendarDay day = entry.getKey();
                        int count = entry.getValue();

                        if (count <= 1) {
                            blueDays.add(day);
                        } else if (count == 2) {
                            greenDays.add(day);
                        } else if (count == 3) {
                            orangeDays.add(day);
                        } else {
                            redDays.add(day);
                        }
                    }

                    // 데코레이터에 Set 반영
                    blueDotDecorator.setDates(blueDays);
                    greenDotDecorator.setDates(greenDays);
                    orangeDotDecorator.setDates(orangeDays);
                    redDotDecorator.setDates(redDays);

                    calendarView.invalidateDecorators();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "이벤트 점 로딩 실패: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ======= 색깔별 점 데코레이터 =======

    private static class ColoredDotDecorator implements DayViewDecorator {

        private final Set<CalendarDay> dates;
        private final int color;

        ColoredDotDecorator(Set<CalendarDay> dates, int color) {
            this.dates = dates;
            this.color = color;
        }

        void setDates(Set<CalendarDay> newDates) {
            dates.clear();
            dates.addAll(newDates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(6f, color));
        }
    }
}