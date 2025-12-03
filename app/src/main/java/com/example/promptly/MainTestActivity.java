package com.example.promptly;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainTestActivity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    TextView[] tvTitles = new TextView[5];
    TextView[] tvConditions = new TextView[5];
    EditText[] etAnswers = new EditText[5];
    Button[] btnSubmits = new Button[5];

    TextView tvCountdown;

    // ===== Timer =====
    private static final long REFRESH_INTERVAL = 12 * 60 * 60 * 1000; // 12시간
    Handler handler = new Handler();

    // ===== SharedPreferences =====
    SharedPreferences timePref;
    SharedPreferences submitPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintest);

        timePref = getSharedPreferences("main_test_time", MODE_PRIVATE);
        submitPref = getSharedPreferences("main_test_submit", MODE_PRIVATE);

        tvCountdown = findViewById(R.id.tvCountdown);

        setupViews();
        setupSubmitButtons();

        loadQuestionsFromDB();
        startCountdownTimer();
    }

    // 문제 5개 뷰 연결
    private void setupViews() {
        int[] ids = {R.id.q1, R.id.q2, R.id.q3, R.id.q4, R.id.q5};

        for (int i = 0; i < 5; i++) {
            View v = findViewById(ids[i]);

            tvTitles[i] = v.findViewById(R.id.tvQuestionTitle);
            tvConditions[i] = v.findViewById(R.id.tvConditions);
            etAnswers[i] = v.findViewById(R.id.etAnswer);
            btnSubmits[i] = v.findViewById(R.id.btnSubmitOne);
        }
    }

    // Firestore에서 5문제 랜덤 로딩 + 12시간 체크
    private void loadQuestionsFromDB() {

        long lastTime = timePref.getLong("last_refresh_time", 0);
        long now = System.currentTimeMillis();

        // 12시간 안 지났으면 새로 뽑지 않음
        if (now - lastTime < REFRESH_INTERVAL) {
            return;
        }

        db.collection("questions")
                .get()
                .addOnSuccessListener(result -> {

                    List<Map<String, Object>> list = new ArrayList<>();

                    for (var doc : result) {
                        list.add(doc.getData());
                    }

                    if (list.size() < 5) return;

                    Collections.shuffle(list);

                    for (int i = 0; i < 5; i++) {
                        Map<String, Object> q = list.get(i);

                        tvTitles[i].setText(
                                "Q" + (i + 1) + ". 다음 조건을 만족하는 프롬프트를 입력하세요."
                        );

                        String cond =
                                "상황: " + q.get("situation") + "\n" +
                                        "직업: " + q.get("role") + "\n" +
                                        "작성물: " + q.get("document") + "\n" +
                                        "스타일: " + q.get("style");

                        tvConditions[i].setText(cond);

                        enableQuestion(i);
                    }

                    // 시간 / 제출 상태 초기화
                    timePref.edit().putLong("last_refresh_time", now).apply();
                    submitPref.edit().clear().apply();
                });
    }

    // --------------------------------------------------
    // 제출 버튼 로직
    // --------------------------------------------------
    private void setupSubmitButtons() {

        for (int i = 0; i < 5; i++) {
            final int index = i;

            boolean submitted = submitPref.getBoolean("submitted_" + index, false);
            if (submitted) disableQuestion(index);

            btnSubmits[i].setOnClickListener(v -> {

                String answer = etAnswers[index].getText().toString();

                // ✅ TODO: 백엔드 GPT 채점 → DB 저장 (현재 비워둠)

                // ✅ 프론트엔드 제출 처리
                submitPref.edit().putBoolean("submitted_" + index, true).apply();

                disableQuestion(index);
                showFeedbackDialog();
            });
        }
    }

    // 문제 비활성화
    private void disableQuestion(int index) {
        etAnswers[index].setEnabled(false);

        btnSubmits[index].setEnabled(false);
        btnSubmits[index].setBackgroundTintList(
                ColorStateList.valueOf(Color.LTGRAY)
        );
    }

    private void enableQuestion(int index) {
        etAnswers[index].setEnabled(true);
        btnSubmits[index].setEnabled(true);
    }

    // GPT 피드백 다이얼로그 (프론트 전용)
    private void showFeedbackDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("피드백을 불러오는 중입니다...\n\n(백엔드 연동 예정)")
                .setPositiveButton("확인", null)
                .show();
    }

    // 12시간 카운트다운
    private void startCountdownTimer() {

        handler.post(new Runnable() {
            @Override
            public void run() {

                long lastTime = timePref.getLong("last_refresh_time", 0);
                long target = lastTime + REFRESH_INTERVAL;

                long now = System.currentTimeMillis();
                long diff = target - now;

                if (diff <= 0) {
                    tvCountdown.setText("새 문제를 불러오는 중...");
                    loadQuestionsFromDB();
                    handler.postDelayed(this, 1000);
                    return;
                }

                long h = diff / (1000 * 60 * 60);
                long m = (diff / (1000 * 60)) % 60;
                long s = (diff / 1000) % 60;

                tvCountdown.setText(
                        String.format("다음 문제까지 %02d:%02d:%02d", h, m, s)
                );

                handler.postDelayed(this, 1000);
            }
        });
    }
}
