package com.example.promptly;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PreTestResultActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MyPrefs";
    private static final String NICKNAME_KEY = "nickname";
    private static final String PRETEST_DONE_KEY = "pretest_completed";

    private TextView tvResultIntro, tvFinalScore, tvMessage;
    private ProgressBar[] progressBars;

    private final int[] barColors = {
            Color.parseColor("#add7a0"), // 명확성
            Color.parseColor("#8ecf8c"), // 구체성
            Color.parseColor("#6caf6b"), // 형식
            Color.parseColor("#518a4f"), // 역할
            Color.parseColor("#456f3e")  // 맥락
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pretest_result);

        tvResultIntro = findViewById(R.id.tv_result_intro);
        tvFinalScore = findViewById(R.id.tv_final_score);
        tvMessage = findViewById(R.id.tv_message);
        ImageButton btnNext = findViewById(R.id.btn_next);

        progressBars = new ProgressBar[]{
                findViewById(R.id.progress_clarity),
                findViewById(R.id.progress_specificity),
                findViewById(R.id.progress_logic),
                findViewById(R.id.progress_creativity),
                findViewById(R.id.progress_context)
        };

        for (int i = 0; i < progressBars.length; i++) {
            progressBars[i].setProgressTintList(
                    ColorStateList.valueOf(barColors[i])
            );
            progressBars[i].setProgressBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
            );
            progressBars[i].setProgress(0);
        }

        // 닉네임 불러오기
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String nickname = prefs.getString(NICKNAME_KEY, "사용자");

        tvResultIntro.setText(nickname + "님의 점수는");

        // 더미 점수
        int totalScore = 90;
        int[] dummyScores = {17, 16, 15, 18, 18};

        showAnimatedScore(totalScore);
        showBarChart(dummyScores);
        showMessage(totalScore);
/*
        // 다음 버튼 → 프리테스트 완료 처리 후 메인 이동
        btnNext.setOnClickListener(v -> {
            prefs.edit().putBoolean(PRETEST_DONE_KEY, true).apply();
            Intent intent = new Intent(this, MainTestActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
 */
    }

    // 총점 애니메이션
    private void showAnimatedScore(int finalScore) {
        tvFinalScore.setText("0점");
        tvFinalScore.setTextColor(getScoreColor(finalScore));

        ValueAnimator animator = ValueAnimator.ofInt(0, finalScore);
        animator.setDuration(1500);
        animator.addUpdateListener(animation ->
                tvFinalScore.setText(animation.getAnimatedValue() + "점"));
        animator.start();
    }

    // 점수별 색상
    private int getScoreColor(int score) {
        if (score >= 90) return Color.parseColor("#40b334");
        else if (score >= 75) return Color.parseColor("#479dc4");
        else if (score >= 60) return Color.parseColor("#c7992e");
        else return Color.parseColor("#bd4037");
    }

    // 막대그래프 애니메이션
    private void showBarChart(int[] scores) {
        for (int i = 0; i < progressBars.length; i++) {
            int index = i;
            ValueAnimator animator = ValueAnimator.ofInt(0, scores[i]);
            animator.setDuration(1200);
            animator.addUpdateListener(animation ->
                    progressBars[index].setProgress((int) animation.getAnimatedValue()));
            animator.start();
        }
    }

    // 격려 메시지
    private void showMessage(int score) {
        if (score >= 90) tvMessage.setText("완벽해요! 훌륭한 프롬프터군요!");
        else if (score >= 75) tvMessage.setText("아주 좋아요! 조금만 더 다듬어봐요!");
        else if (score >= 60) tvMessage.setText("기초는 충분해요! 더 잘할 수 있어요.");
        else tvMessage.setText("좋은 시작이에요! 차근차근 연습해요.");
    }
}
