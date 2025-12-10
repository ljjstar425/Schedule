package com.bar.voiceday;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddSiteActivity extends AppCompatActivity {

    private EditText titleInput;
    private EditText linkInput;
    private ImageView backButton;
    private MaterialButton confirmButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_site);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        titleInput = findViewById(R.id.titleInput);
        linkInput = findViewById(R.id.linkInput);
        backButton = findViewById(R.id.backButton);
        confirmButton = findViewById(R.id.confirmButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        confirmButton.setOnClickListener(v -> saveSite());
    }

    private void saveSite() {
        String title = titleInput.getText().toString().trim();
        String link = linkInput.getText().toString().trim();

        // 입력 검증
        if (title.isEmpty()) {
            Toast.makeText(this, "제목을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (link.isEmpty()) {
            Toast.makeText(this, "링크를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // URL 형식 검증 (http:// 또는 https:// 추가)
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            link = "https://" + link;
        }

        // Firestore에 저장
        Map<String, Object> site = new HashMap<>();
        site.put("title", title);
        site.put("url", link);
        site.put("imageUrl", "");
        site.put("visitCount", 0);
        site.put("createdAt", System.currentTimeMillis());

        String finalLink = link;
        db.collection("frequentSites")
                .add(site)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "사이트가 추가되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "추가 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}