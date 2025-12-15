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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PreTestResultActivity extends AppCompatActivity {

    // SharedPreferences 상수 정의 (MainActivity와 통일)
    private static final String PREFS_NAME = MainActivity.PREFS_NAME;
    private static final String NICKNAME_KEY = MainActivity.NICKNAME_KEY;
    private static final String PRETEST_COMPLETED_KEY = MainActivity.PRETEST_COMPLETED_KEY;

    // Intent Extra 키 정의
    private static final String EXTRA_TOTAL_SCORE = "TOTAL_SCORE";
    private static final String EXTRA_SCORES = "ITEM_SCORES";

    // 뷰 변수 선언
    private TextView tvResultIntro, tvFinalScore, tvMessage;
    private ProgressBar[] progressBars;

    // 점수 막대 색상 정의
    private int[] barColors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 액티비티 생성 및 초기 설정
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pretest_result);

        // 뷰 참조 설정
        tvResultIntro = findViewById(R.id.tv_result_intro);
        tvFinalScore = findViewById(R.id.tv_final_score);
        tvMessage = findViewById(R.id.tv_message);
        ImageButton btnNext = findViewById(R.id.btn_next);

        barColors = new int[] {
                getColor(R.color.scoreClarity),
                getColor(R.color.scoreSpecificity),
                getColor(R.color.scoreFormat),
                getColor(R.color.scoreRole),
                getColor(R.color.scoreContext)
        };

        // ProgressBar 배열 초기화
        progressBars = new ProgressBar[]{
                findViewById(R.id.progress_clarity),
                findViewById(R.id.progress_specificity),
                findViewById(R.id.progress_logic),
                findViewById(R.id.progress_creativity),
                findViewById(R.id.progress_context)
        };

        // ProgressBar 색상 초기 설정
        for (int i = 0; i < progressBars.length; i++) {
            progressBars[i].setProgressTintList(
                    ColorStateList.valueOf(barColors[i])
            );
            progressBars[i].setProgressBackgroundTintList(
                    ColorStateList.valueOf(getColor(R.color.progressBackground))
            );
            progressBars[i].setProgress(0);
        }

        // 닉네임 불러오기
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String nickname = prefs.getString(NICKNAME_KEY, "사용자");

        // 인텐트에서 점수 데이터 가져오기
        Intent intent = getIntent();
        // totalScore_100은 PreTestActivity에서 5개 항목(0~20점)의 합(0~100점)을 받음
        int totalScore_100 = intent.getIntExtra(EXTRA_TOTAL_SCORE, 0);
        int[] itemScores_100 = intent.getIntArrayExtra(EXTRA_SCORES);

        // 총점 표시 점수 처리: 0~100점 만점을 그대로 사용
        int finalDisplayScore = totalScore_100;

        // 점수 데이터 유효성 검사 및 기본값 설정
        if (itemScores_100 == null || itemScores_100.length != 5) {
            Toast.makeText(this, "점수 데이터 로드에 실패했습니다. 기본값으로 표시합니다.", Toast.LENGTH_LONG).show();
            itemScores_100 = new int[]{50, 50, 50, 50, 50};
            finalDisplayScore = 50;
        }

        // 화면 요소에 결과 데이터 반영 및 애니메이션 시작
        tvResultIntro.setText(nickname + "님의 최종 점수");
        showAnimatedScore(finalDisplayScore);
        showBarChart(itemScores_100);
        showMessage(finalDisplayScore);

        // 다음 버튼 클릭 이벤트 설정 (프리테스트 완료 처리 후 메인 이동)
        btnNext.setOnClickListener(v -> {
            prefs.edit().putBoolean(PRETEST_COMPLETED_KEY, true).apply();
            Intent mainIntent = new Intent(this, MainTestActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
            finish();
        });
    }

    // 총점 애니메이션을 표시
    private void showAnimatedScore(int finalScore) {
        // 총점 애니메이션 설정 및 시작
        tvFinalScore.setText("0점");
        tvFinalScore.setTextColor(getScoreColor(finalScore));

        ValueAnimator animator = ValueAnimator.ofInt(0, finalScore);
        animator.setDuration(1500);
        animator.addUpdateListener(animation ->
                tvFinalScore.setText(animation.getAnimatedValue() + "점"));
        animator.start();
    }

    // 점수에 따른 텍스트 색상을 결정
    private int getScoreColor(int score) {
        if (score >= 90) return getColor(R.color.scoreExcellent);
        else if (score >= 75) return getColor(R.color.scoreGood);
        else if (score >= 60) return getColor(R.color.scoreAverage);
        else return getColor(R.color.scoreLow);
    }

    // 항목별 막대그래프 애니메이션을 표시
    private void showBarChart(int[] scores_100) {
        // 항목별 점수를 막대그래프에 애니메이션으로 반영
        for (int i = 0; i < progressBars.length; i++) {
            int index = i;
            // scores_100(100점 만점)을 ProgressBar의 max값(20점 만점 가정)에 맞게 5로 나눔
            int score_20 = scores_100[i] / 5;

            ValueAnimator animator = ValueAnimator.ofInt(0, score_20);
            animator.setDuration(1200);
            animator.addUpdateListener(animation ->
                    progressBars[index].setProgress((int) animation.getAnimatedValue()));
            animator.start();
        }
    }

    // 총점에 따른 격려 메시지를 표시
    private void showMessage(int score) {
        // 점수 구간별 메시지 설정 (100점 만점 기준)
        if (score >= 90) tvMessage.setText("완벽해요! 훌륭한 프롬프터군요!");
        else if (score >= 75) tvMessage.setText("아주 좋아요! 조금만 더 다듬어봐요!");
        else if (score >= 60) tvMessage.setText("기초는 충분해요! 더 잘할 수 있어요.");
        else tvMessage.setText("좋은 시작이에요! 차근차근 연습해요.");
    }
}