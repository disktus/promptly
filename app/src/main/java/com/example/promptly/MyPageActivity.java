package com.example.promptly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class MyPageActivity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ===== 상단 정보 =====
    TextView tvNickname;
    TextView tvAvgLabel;
    TextView tvAvgScore;
    TextView tvSolveLabel;
    TextView tvSolveRank;
    TextView tvScoreLabel;
    TextView tvScoreRank;

    // ===== 최근 기록 =====
    LinearLayout layoutHistory;

    // ===== 하단 네비 =====
    ImageView btnHome;
    ImageView btnMy;

    // SharedPreferences 키 (MainActivity와 동일)
    private static final String PREFS_NAME = "MyPrefs";
    private static final String NICKNAME_KEY = "nickname";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        bindViews();
        setupBottomNav();

        loadNicknameFromPrefs();   // 로그인된 닉네임 불러오기
        applyDummyStats();        // 점수, 퍼센트 더미
        loadHistoryMock();        // 최근기록 더미
    }

    private void bindViews() {

        tvNickname = findViewById(R.id.tvNickname);
        tvAvgLabel = findViewById(R.id.tvAvgLabel);
        tvAvgScore = findViewById(R.id.tvAvgScore);

        tvSolveLabel = findViewById(R.id.tvSolveLabel);
        tvSolveRank = findViewById(R.id.tvSolveRank);

        tvScoreLabel = findViewById(R.id.tvScoreLabel);
        tvScoreRank = findViewById(R.id.tvScoreRank);

        layoutHistory = findViewById(R.id.layoutHistory);

        btnHome = findViewById(R.id.btnHome);
        btnMy = findViewById(R.id.btnMy);
    }

    // 닉네임은 SharedPreferences 기준 (로그인 연동)
    private void loadNicknameFromPrefs() {

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String nickname = prefs.getString(NICKNAME_KEY, null);

        if (nickname != null) {
            tvNickname.setText(nickname + "님의");
        } else {
            tvNickname.setText("사용자님의");
        }
    }

    // 통계는 전부 더미 데이터
    private void applyDummyStats() {

        tvAvgLabel.setText("평균\n점수는");
        tvAvgScore.setText("87점");   // 더미 평균점수

        tvSolveLabel.setText("풀이수 상위 ");
        tvSolveRank.setText("15%");  // 더미 풀이수 퍼센트

        tvScoreLabel.setText("평균점수 상위 ");
        tvScoreRank.setText("9%");   // 더미 평균점수 퍼센트
    }

    // 최근 기록 더미 (30개)
    private void loadHistoryMock() {

        layoutHistory.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 1; i <= 30; i++) {
            View item = inflater.inflate(R.layout.item_history, layoutHistory, false);

            TextView date = item.findViewById(R.id.tvHistoryDate);
            TextView situation = item.findViewById(R.id.tvHistorySituation);
            TextView role = item.findViewById(R.id.tvHistoryRole);
            TextView doc = item.findViewById(R.id.tvHistoryDoc);
            TextView style = item.findViewById(R.id.tvHistoryStyle);

            date.setText("2025-12-" + String.format("%02d", i));
            situation.setText("상황: 학교 과제");
            role.setText(" / 직업: 대학생");
            doc.setText(" / 작성물: 보고서");
            style.setText(" / 스타일: 객관적");

            item.setOnClickListener(v -> showHistoryDialog());

            layoutHistory.addView(item);
        }
    }

    // 문제 / 답변 / 피드백 다이얼로그 (더미) - 수정됨
    private void showHistoryDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 변경된 레이아웃 inflate
        View view = getLayoutInflater().inflate(R.layout.feedback_dialog, null);
        builder.setView(view);

        // 1. 새로운 ID 찾기
        LinearLayout layoutLoading = view.findViewById(R.id.layoutLoading);
        ScrollView layoutResult = view.findViewById(R.id.layoutResult);
        
        TextView tvTotalScore = view.findViewById(R.id.tvTotalScore);
        TextView tvFeedbackContent = view.findViewById(R.id.tvFeedbackContent); // tvFeedback -> tvFeedbackContent
        TextView tvExample = view.findViewById(R.id.tvExample);
        
        // 상세 항목 뷰 (더미 데이터용)
        TextView tvItem1 = view.findViewById(R.id.tvItem1);
        TextView tvItem2 = view.findViewById(R.id.tvItem2);
        TextView tvItem3 = view.findViewById(R.id.tvItem3);
        TextView tvItem4 = view.findViewById(R.id.tvItem4);
        TextView tvItem5 = view.findViewById(R.id.tvItem5);

        Button btnClose = view.findViewById(R.id.btnClose);

        // 2. 로딩 화면 숨기고 결과 화면 바로 보이기 (더미니까)
        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
        if (layoutResult != null) layoutResult.setVisibility(View.VISIBLE);

        // 3. 더미 데이터 세팅
        if (tvTotalScore != null) tvTotalScore.setText("85");
        
        if (tvItem1 != null) tvItem1.setText("1. 목적 적합성 (18점): 목표를 잘 달성함");
        if (tvItem2 != null) tvItem2.setText("2. 명확성 (16점): 표현이 명확함");
        if (tvItem3 != null) tvItem3.setText("3. 맥락 (17점): 배경 설명이 충분함");
        if (tvItem4 != null) tvItem4.setText("4. 구조 (15점): 단계별 구성이 좋음");
        if (tvItem5 != null) tvItem5.setText("5. CoT (19점): 논리적 흐름이 있음");

        if (tvFeedbackContent != null) {
            tvFeedbackContent.setText(
                    "문제: 다음 조건을 만족하는 프롬프트 작성\n\n" +
                    "답변: (사용자의 예시 답변)\n\n" +
                    "총평: 전반적으로 잘 작성된 프롬프트입니다. 제약 조건을 좀 더 구체적으로 명시하면 완벽할 것입니다."
            );
        }

        if (tvExample != null) {
            tvExample.setText("이곳에 모범 답안 예시가 표시됩니다.");
        }

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    // 하단 네비게이션
    private void setupBottomNav() {

        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainTestActivity.class));
            finish();
        });

        btnMy.setOnClickListener(v -> {
            // 현재 페이지 유지
        });
    }
}