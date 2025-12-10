package com.bar.voiceday;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class AddScheduleActivity extends AppCompatActivity {

    private ImageButton leftArrow2;
    private EditText editTitle;
    private TextView startDateText;
    private TextView startTimeText;
    private TextView endDateText;
    private TextView endTimeText;
    private Switch switchAllday;
    private TextView repeatText;
    private TextView placeText;
    private TextView memoText;
    private Button submitScheduleButton;
    private TextView deleteScheduleButton;


    // Firestore 인스턴스
    private FirebaseFirestore db;

    // 반복/장소/메모 임시 저장용
    private String repeatRule = "안 함";
    private String placeValue = "";
    private String memoValue  = "";

    private ImageView arrow1;
    private ImageView arrow2;
    private ImageView arrow3;

    // ★ 수정 모드 관련
    private boolean isEditMode = false;
    private String editingScheduleId = null;
    private String originalDateKey   = null;   // 필요하면 ScheduleStore 등에서 쓸 수 있음

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);

        // Firestore 초기화
        db = FirebaseFirestore.getInstance();

        // 뷰 바인딩
        leftArrow2          = findViewById(R.id.left_arrow2);
        editTitle           = findViewById(R.id.edit_title);
        startDateText       = findViewById(R.id.start_date);
        startTimeText       = findViewById(R.id.start_time);
        endDateText         = findViewById(R.id.end_date);
        endTimeText         = findViewById(R.id.end_time);
        switchAllday        = findViewById(R.id.switch_allday);
        repeatText          = findViewById(R.id.text_repeat);
        placeText           = findViewById(R.id.text_place);
        memoText            = findViewById(R.id.text_memo);
        submitScheduleButton = findViewById(R.id.btn_submit_schedule);
        deleteScheduleButton = findViewById(R.id.btn_delete_schedule);


        arrow1 = findViewById(R.id.right_arrow1);
        arrow2 = findViewById(R.id.right_arrow2);
        arrow3 = findViewById(R.id.right_arrow3);

        // ===== 1) Intent에서 값 받아오기 =====
        Intent intentFromRecord = getIntent();

        // ★ 모드 / id / 원래 dateKey
        String mode = intentFromRecord.getStringExtra("mode");
        isEditMode = "edit".equals(mode);
        editingScheduleId = intentFromRecord.getStringExtra("schedule_id");
        originalDateKey   = intentFromRecord.getStringExtra("original_date_key");

        // 수정 모드가 아니면 삭제 버튼 숨기기
        if (isEditMode) {
            deleteScheduleButton.setVisibility(View.VISIBLE);
        } else {
            deleteScheduleButton.setVisibility(View.GONE);
        }

        // 기본 날짜 (선택된 날짜)
        String selectedDate = intentFromRecord.getStringExtra("selected_date");
        if (selectedDate != null && !selectedDate.isEmpty()) {
            startDateText.setText(selectedDate);
            endDateText.setText(selectedDate);
        }

        // STT 또는 수정용 데이터(우리가 재사용)
        String sttTitle      = intentFromRecord.getStringExtra("stt_title");
        String sttStartDate  = intentFromRecord.getStringExtra("stt_start_date");
        String sttEndDate    = intentFromRecord.getStringExtra("stt_end_date");
        String sttStartTime  = intentFromRecord.getStringExtra("stt_start_time");
        String sttEndTime    = intentFromRecord.getStringExtra("stt_end_time");
        boolean sttAllDay    = intentFromRecord.getBooleanExtra("stt_all_day", false);
        String sttMemo       = intentFromRecord.getStringExtra("stt_memo");
        String sttRepeat     = intentFromRecord.getStringExtra("stt_repeat");
        String sttPlace      = intentFromRecord.getStringExtra("stt_place");

        // ===== 2) 값이 있으면 UI에 채워주기 =====
        if (sttTitle != null && !sttTitle.isEmpty()) {
            editTitle.setText(sttTitle);
        }
        if (sttStartDate != null && !sttStartDate.isEmpty()) {
            startDateText.setText(sttStartDate);
        }
        if (sttEndDate != null && !sttEndDate.isEmpty()) {
            endDateText.setText(sttEndDate);
        }
        if (sttStartTime != null && !sttStartTime.isEmpty()) {
            startTimeText.setText(sttStartTime);
        }
        if (sttEndTime != null && !sttEndTime.isEmpty()) {
            endTimeText.setText(sttEndTime);
        }
        if (sttMemo != null && !sttMemo.isEmpty()) {
            memoValue = sttMemo;
            memoText.setText("메모 있음");
        }
        if (sttRepeat != null && !sttRepeat.isEmpty()) {
            repeatRule = sttRepeat;
            repeatText.setText(sttRepeat);
        }
        if (sttPlace != null && !sttPlace.isEmpty()) {
            placeValue = sttPlace;
            placeText.setText(sttPlace);
        }
        switchAllday.setChecked(sttAllDay);

        // ===== 3) 날짜/시간 선택 =====

        startDateText.setOnClickListener(v -> showDatePicker(startDateText));
        endDateText.setOnClickListener(v -> showDatePicker(endDateText));

        startTimeText.setOnClickListener(v -> {
            if (!switchAllday.isChecked() && startTimeText.isEnabled()) {
                showTimePicker(startTimeText);
            }
        });
        endTimeText.setOnClickListener(v -> {
            if (!switchAllday.isChecked() && endTimeText.isEnabled()) {
                showTimePicker(endTimeText);
            }
        });

        // ===== 4) 하루종일 스위치 + 초기 상태 정리 =====
        switchAllday.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateAllDayUI(isChecked);
        });
        // 현재 값 기준으로 한 번 정리
        updateAllDayUI(switchAllday.isChecked());

        // ===== 5) 반복 / 장소 / 메모 입력 =====

        // 메모
        arrow3.setOnClickListener(v -> showInputDialog(
                "메모 입력",
                memoValue,
                value -> {
                    memoValue = value;
                    memoText.setText(value.isEmpty() ? "남기기" : "메모 있음");
                }
        ));

        // 장소
        arrow2.setOnClickListener(v -> showInputDialog(
                "장소 입력",
                placeValue,
                value -> {
                    placeValue = value;
                    placeText.setText(value.isEmpty() ? "추가하기" : value);
                }
        ));

        // 반복
        arrow1.setOnClickListener(v -> {
            final String[] items = new String[]{"안 함", "매일", "매주", "매월"};
            int currentIndex = 0;
            for (int i = 0; i < items.length; i++) {
                if (items[i].equals(repeatRule)) {
                    currentIndex = i;
                    break;
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle("반복 설정")
                    .setSingleChoiceItems(items, currentIndex, (dialog, which) -> {
                        repeatRule = items[which];
                    })
                    .setPositiveButton("확인", (dialog, which) -> {
                        repeatText.setText(repeatRule);
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        // 뒤로가기
        leftArrow2.setOnClickListener(v -> finish());

        // ===== 6) 저장 버튼 =====
        submitScheduleButton.setOnClickListener(v -> {
            String title     = editTitle.getText().toString().trim();
            String startDate = startDateText.getText().toString().trim();
            String startTime = startTimeText.getText().toString().trim();
            String endDate   = endDateText.getText().toString().trim();
            String endTime   = endTimeText.getText().toString().trim();
            boolean allDay   = switchAllday.isChecked();

            // 검증
            if (title.isEmpty() || startDate.isEmpty()) {
                Toast.makeText(this, "제목과 시작 날짜는 필수입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 하루종일이면 시간은 공백 처리
            if (allDay) {
                startTime = "";
                endTime   = "";
            }

            // 이 일정의 기준 날짜 키 (여기서는 startDate 문자열 그대로 사용)
            String dateKey = startDate;

            // ★ 새로 추가 vs 수정 모드에 따라 문서 참조 다르게
            DocumentReference ref;
            String id;

            if (isEditMode && editingScheduleId != null && !editingScheduleId.isEmpty()) {
                // 기존 문서 업데이트
                ref = db.collection("schedules").document(editingScheduleId);
                id  = editingScheduleId;
            } else {
                // 새 일정 추가
                ref = db.collection("schedules").document();
                id  = ref.getId();
            }

            // Schedule 객체 생성 (필드 전부 넘김)
            Schedule schedule = new Schedule(
                    id,
                    dateKey,
                    title,
                    startDate,
                    endDate,
                    startTime,
                    endTime,
                    allDay,
                    repeatRule,
                    placeValue,
                    memoValue
            );

            // Firestore 저장
            ref.set(schedule)
                    .addOnSuccessListener(unused -> {

                        // ScheduleStore는 지금 구조에선 안 써도 되지만,
                        // 만약 쓰고 있다면 여기는 옵션.
                        // if (!isEditMode) {
                        //     ScheduleStore.addSchedule(dateKey, schedule);
                        // }

                        Toast.makeText(AddScheduleActivity.this,
                                isEditMode ? "일정이 수정되었습니다." : "일정이 저장되었습니다.",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(AddScheduleActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddScheduleActivity.this,
                                "저장 실패: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });

        // ===== 7) 삭제 버튼 (수정 모드에서만) =====
        deleteScheduleButton.setOnClickListener(v -> {
            if (!isEditMode || editingScheduleId == null || editingScheduleId.isEmpty()) {
                Toast.makeText(this, "삭제할 일정 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("일정 삭제")
                    .setMessage("이 일정을 정말 삭제할까요?\n(반복으로 표시되던 일정들도 모두 사라집니다.)")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        db.collection("schedules")
                                .document(editingScheduleId)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this,
                                            "일정이 삭제되었습니다.",
                                            Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(AddScheduleActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this,
                                            "삭제 실패: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });
    }

    // ================== 하루종일 UI ==================

    private void updateAllDayUI(boolean isChecked) {
        if (isChecked) {
            startTimeText.setText("하루종일");
            endTimeText.setText("");
            startTimeText.setEnabled(false);
            endTimeText.setEnabled(false);
            startTimeText.setAlpha(0.5f);
            endTimeText.setAlpha(0.5f);
        } else {
            if (startTimeText.getText().toString().equals("하루종일")
                    || startTimeText.getText().toString().isEmpty()) {
                startTimeText.setText("오전 09:00");
            }
            if (endTimeText.getText().toString().isEmpty()) {
                endTimeText.setText("오후 06:00");
            }
            startTimeText.setEnabled(true);
            endTimeText.setEnabled(true);
            startTimeText.setAlpha(1.0f);
            endTimeText.setAlpha(1.0f);
        }
    }

    // ================== 날짜 다이얼로그 ==================

    private void showDatePicker(TextView targetView) {
        Calendar c = Calendar.getInstance();

        int[] ymd = parseDate(targetView.getText().toString());
        if (ymd != null) {
            c.set(ymd[0], ymd[1] - 1, ymd[2]);
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    String text = y + "년 " + (m + 1) + "월 " + d + "일";
                    targetView.setText(text);
                },
                year, month, day
        );
        dialog.show();
    }

    private int[] parseDate(String text) {
        try {
            int idxYear = text.indexOf("년");
            int idxMonth = text.indexOf("월");
            int idxDay = text.indexOf("일");
            if (idxYear == -1 || idxMonth == -1 || idxDay == -1) return null;

            int year = Integer.parseInt(text.substring(0, idxYear).trim());
            int month = Integer.parseInt(text.substring(idxYear + 1, idxMonth).trim());
            int day = Integer.parseInt(text.substring(idxMonth + 1, idxDay).trim());

            return new int[]{year, month, day};
        } catch (Exception e) {
            return null;
        }
    }

    // ================== 시간 다이얼로그 ==================

    private void showTimePicker(TextView targetView) {
        Calendar c = Calendar.getInstance();
        int[] hm = parseTime(targetView.getText().toString());
        int hour = hm[0];
        int minute = hm[1];

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, min) -> {
                    String text = formatTime(hourOfDay, min);
                    targetView.setText(text);
                },
                hour,
                minute,
                false
        );
        dialog.show();
    }

    private int[] parseTime(String text) {
        try {
            if (text == null || text.isEmpty()
                    || text.equals("하루종일")) {
                return new int[]{9, 0};
            }
            text = text.trim();
            String[] parts = text.split(" ");
            if (parts.length != 2) return new int[]{9, 0};

            String ampm = parts[0];
            String hhmm = parts[1];
            String[] hm = hhmm.split(":");
            int h = Integer.parseInt(hm[0]);
            int m = Integer.parseInt(hm[1]);

            if ("오후".equals(ampm) && h < 12) {
                h += 12;
            }
            if ("오전".equals(ampm) && h == 12) {
                h = 0;
            }
            return new int[]{h, m};
        } catch (Exception e) {
            return new int[]{9, 0};
        }
    }

    private String formatTime(int hourOfDay, int minute) {
        String ampm = (hourOfDay >= 12) ? "오후" : "오전";
        int h = hourOfDay % 12;
        if (h == 0) h = 12;
        String mm = (minute < 10) ? ("0" + minute) : String.valueOf(minute);
        String hh = (h < 10) ? ("0" + h) : String.valueOf(h);
        return ampm + " " + hh + ":" + mm;
    }

    // ================== 공통 입력 다이얼로그 ==================

    private interface OnTextConfirmed {
        void onConfirmed(String value);
    }

    private void showInputDialog(String title, String initial, OnTextConfirmed listener) {
        final EditText input = new EditText(this);
        input.setText(initial);
        input.setSelection(input.getText().length());
        input.setSingleLine(false);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("확인", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    listener.onConfirmed(text);
                })
                .setNegativeButton("취소", null)
                .show();
    }
}