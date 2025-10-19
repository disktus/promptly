package com.example.testresult;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {

    private TextView tvResultIntro, tvFinalScore;
    private FirebaseFirestore db;

    private String[] criteriaNames = {"명확성", "구체성", "논리성", "창의성", "맥락이해"};
    private int[] barColors;
    private ProgressBar[] progressBars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResultIntro = findViewById(R.id.tv_result_intro);
        tvFinalScore = findViewById(R.id.tv_final_score);

        // ProgressBar 초기화
        progressBars = new ProgressBar[]{
                findViewById(R.id.progress_clarity),
                findViewById(R.id.progress_specificity),
                findViewById(R.id.progress_logic),
                findViewById(R.id.progress_creativity),
                findViewById(R.id.progress_context)
        };

        barColors = new int[]{
                Color.parseColor("#607D8B"), // 명확성
                Color.parseColor("#455A64"), // 구체성
                Color.parseColor("#90A4AE"), // 논리성
                Color.parseColor("#78909C"), // 창의성
                Color.parseColor("#B0BEC5")  // 맥락이해
        };

        for (int i = 0; i < progressBars.length; i++) {
            progressBars[i].setProgressTintList(ColorStateList.valueOf(barColors[i]));
            progressBars[i].setProgress(0); // 초기값 0
        }

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();

        // 예시: 테스트 시작 시 유저 ID를 intent로 전달
        String userId = getIntent().getStringExtra("userId");
        if (userId == null) userId = "user1"; // 임시 기본값

        loadUserAndEvaluationData(userId);
    }

    // 유저 정보 + 평가 데이터 불러오기
    private void loadUserAndEvaluationData(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String nickname = userDoc.getString("nickname");

                        // 평가 데이터 불러오기
                        db.collection("evaluations")
                                .whereEqualTo("userId", userId)
                                .get()
                                .addOnSuccessListener(querySnapshot ->
                                        processEvaluationData(nickname, querySnapshot));
                    } else {
                        tvResultIntro.setText("유저 정보를 찾을 수 없습니다.");
                    }
                })
                .addOnFailureListener(e -> tvResultIntro.setText("DB 연결 실패: " + e.getMessage()));
    }

    // 평가 데이터 평균 계산 및 UI 반영
    private void processEvaluationData(String nickname, QuerySnapshot snapshot) {
            if (snapshot.isEmpty()) { tvResultIntro.setText(nickname + "님의 평가 기록이 없습니다."); return; }
            /**
             if (snapshot.isEmpty()) {
                tvResultIntro.setText(nickname + "님의 점수는 (예시)");

                // 🔹 예시용 더미 점수
                int dummyTotal = 84;
                int[] dummyScores = {17, 16, 15, 18, 18};

                showAnimatedScore(dummyTotal);
                showBarChart(dummyScores);
                return;
            }
             */

        int count = 0;
        int claritySum = 0, specificitySum = 0, logicSum = 0, creativitySum = 0, contextSum = 0, totalSum = 0;

        for (QueryDocumentSnapshot doc : snapshot) {
            claritySum += doc.getLong("clarity");
            specificitySum += doc.getLong("specificity");
            logicSum += doc.getLong("logic");
            creativitySum += doc.getLong("creativity");
            contextSum += doc.getLong("context");
            totalSum += doc.getLong("totalScore");
            count++;
        }

        int avgClarity = claritySum / count;
        int avgSpecificity = specificitySum / count;
        int avgLogic = logicSum / count;
        int avgCreativity = creativitySum / count;
        int avgContext = contextSum / count;
        int avgTotal = totalSum / count;

        int[] criteriaScores = {avgClarity, avgSpecificity, avgLogic, avgCreativity, avgContext};

        // 결과 표시
        tvResultIntro.setText(nickname + "님의 점수는...");
        showAnimatedScore(avgTotal);
        showBarChart(criteriaScores);
    }

    // 총점 애니메이션
    private void showAnimatedScore(int finalScore) {
        // tvResultIntro는 이미 화면에 닉네임으로 세팅되어 있음
        tvFinalScore.setText("0점");
        tvFinalScore.setTextColor(getScoreColor(finalScore));

        ValueAnimator animator = ValueAnimator.ofInt(0, finalScore);
        animator.setDuration(2000);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            tvFinalScore.setText(value + "점");
        });
        animator.start();
    }

    // 점수 색상
    private int getScoreColor(int score) {
        if (score >= 90) return Color.parseColor("#66d96b");
        else if (score >= 75) return Color.parseColor("#52b2de");
        else if (score >= 60) return Color.parseColor("#f2c43a");
        else return Color.parseColor("#ed574c");
    }

    // 평가 기준별 막대그래프 애니메이션
    private void showBarChart(int[] criteriaScores) {
        for (int i = 0; i < progressBars.length; i++) {
            final int index = i;
            ValueAnimator animator = ValueAnimator.ofInt(0, criteriaScores[i]);
            animator.setDuration(1500);
            animator.addUpdateListener(animation ->
                    progressBars[index].setProgress((int) animation.getAnimatedValue()));
            animator.start();
        }
    }


}
