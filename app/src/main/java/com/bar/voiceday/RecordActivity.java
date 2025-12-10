package com.bar.voiceday;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class RecordActivity extends AppCompatActivity {

    private static final int REQ_CODE_SPEECH = 1000;

    private ImageButton leftArrow;
    private ImageButton btnRecord;
    private ImageButton cancelButton;
    private ImageButton checkButton;

    private TextView recordDate;
    private EditText recordTextbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);   // xml 이름이 activity_record.xml 이어야 함

        // 뷰 연결
        leftArrow      = findViewById(R.id.left_arrow);
        btnRecord      = findViewById(R.id.btn_record);
        cancelButton   = findViewById(R.id.cancel_button);
        checkButton    = findViewById(R.id.check_button);
        recordDate     = findViewById(R.id.record_date);
        recordTextbox  = findViewById(R.id.record_textbox);

        // MainActivity에서 선택된 날짜 받아서 상단에 표시
        Intent intent = getIntent();
        String selectedDate = intent.getStringExtra("selected_date");
        if (selectedDate != null && !selectedDate.isEmpty()) {
            recordDate.setText(selectedDate);
        }

        // ← 뒤로가기 버튼
        leftArrow.setOnClickListener(v -> finish());

        // 🎙 마이크 버튼 → STT 시작
        btnRecord.setOnClickListener(v -> startSpeechToText());

        // X 취소 버튼 > 내용 싹 지우기
        cancelButton.setOnClickListener(v -> {
            recordTextbox.setText("");
        });


        // ✔ 완료 버튼
        checkButton.setOnClickListener(v -> {
            String text = recordTextbox.getText().toString().trim();

            if (text.isEmpty()) {
                // 🔹 녹음/입력 안 해도 일정추가 화면으로 넘어가고 싶을 때:
                //    제목/메모는 비워두고, 날짜만 들고 AddScheduleActivity로 이동
                String date = recordDate.getText().toString();

                goToAddSchedule(
                        "",      // title 비워둠
                        date,    // startDate
                        date,    // endDate
                        "",      // startTime
                        "",      // endTime
                        true,    // 하루종일 일정으로 가정
                        "",       // memo 없음
                        "",         // repeat
                        ""          // place
                );
            } else {
                // 🔹 텍스트가 있으면: 이걸 STT 결과라고 보고 파싱 단계로 넘김
                onSpeechParsed(text);
            }
        });
    }

    // ================== STT 시작 ==================

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "말씀하세요");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "음성 인식을 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String recognizedText = result.get(0);
                // STT 결과를 EditText에 채워줌 (사용자가 수정 가능)
                recordTextbox.setText(recognizedText);
            }
        }
    }



    // ================== STT 결과 → 일정 데이터로 가정 ==================

    /**
     * 여기서는 "녹음 내용에서 정보 추출이 이미 끝났다"고 가정하고
     * 간단하게 날짜/제목/메모를 만들어 AddScheduleActivity로 넘긴다.
     */
    private void onSpeechParsed(String recognizedText) {
        // 상단에 표시된 날짜를 기준으로 start/end date 사용
        String date = recordDate.getText().toString();

        // 아주 단순한 가정:
        // - 제목: 녹음 내용 전체
        // - 메모: 녹음 내용 전체
        // - 날짜: 선택한 하루
        // - 하루종일 일정
        String parsedTitle     = recognizedText;
        String parsedStartDate = date;
        String parsedEndDate   = date;
        String parsedStartTime = "";       // 시간 추출은 다른 파트에서 한다고 가정
        String parsedEndTime   = "";
        boolean parsedAllDay   = true;
        String parsedMemo      = recognizedText;
        String parsedRepeat    = recognizedText;
        String parsedPlace     = recognizedText;

        goToAddSchedule(parsedTitle,
                parsedStartDate, parsedEndDate,
                parsedStartTime, parsedEndTime,
                parsedAllDay, parsedMemo,
                parsedRepeat, parsedPlace);
    }

    /**
     * AddScheduleActivity로 추출된 일정 정보를 넘긴다.
     */
    private void goToAddSchedule(String title,
                                 String startDate,
                                 String endDate,
                                 String startTime,
                                 String endTime,
                                 boolean allDay,
                                 String memo,
                                 String repeat,
                                 String place) {

        Intent intent = new Intent(RecordActivity.this, AddScheduleActivity.class);

        // MainActivity에서 선택한 날짜와 동일하게 사용
        intent.putExtra("selected_date", startDate);

        // STT에서 "추출된 값"이라고 가정하는 부분
        intent.putExtra("stt_title",      title);
        intent.putExtra("stt_start_date", startDate);
        intent.putExtra("stt_end_date",   endDate);
        intent.putExtra("stt_start_time", startTime);
        intent.putExtra("stt_end_time",   endTime);
        intent.putExtra("stt_all_day",    allDay);
        intent.putExtra("stt_memo",       memo);
        intent.putExtra("stt_repeat",       repeat);
        intent.putExtra("stt_place",       place);


        startActivity(intent);
    }
}