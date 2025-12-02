package com.example.promptly;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

    int[] questionIds = {
            R.id.mainQuestion1,
            R.id.mainQuestion2,
            R.id.mainQuestion3,
            R.id.mainQuestion4,
            R.id.mainQuestion5
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupQuestionViews();
        loadQuestionsFromDB();
    }

    // --------------------------------
    // question_layout 5개 연결
    // --------------------------------
    private void setupQuestionViews() {

        for (int i = 0; i < 5; i++) {
            View qView = findViewById(questionIds[i]);

            tvTitles[i] = qView.findViewById(R.id.tvQuestionTitle);
            tvConditions[i] = qView.findViewById(R.id.tvConditions);
            etAnswers[i] = qView.findViewById(R.id.etAnswer);

            // 제출 버튼 동적 추가
            Button submitBtn = new Button(this);
            submitBtn.setText("제출");
            ((android.widget.LinearLayout) qView).addView(submitBtn);
            btnSubmits[i] = submitBtn;

            int finalI = i;
            submitBtn.setOnClickListener(v -> {
                String userAnswer = etAnswers[finalI].getText().toString();
                handleSubmit(userAnswer, finalI);
            });
        }
    }

    // --------------------------------
    // 제출 처리 (프론트엔드 전용)
    // --------------------------------
    private void handleSubmit(String userAnswer, int index) {

        if (userAnswer.trim().isEmpty()) {
            showFeedbackDialog("프롬프트를 입력해주세요!");
            return;
        }

        // 1. 로딩 다이얼로그 표시
        Dialog loadingDialog = showLoadingDialog();

        // 2. TODO: 여기서 서버로 채점용 프롬프트 + 정답 전송 예정
        /*
           TODO BACKEND:
           - 문제 조건 + 사용자 프롬프트 전송
           - ChatGPT 채점 요청
           - DB 저장 요청
         */

        // 3. 지금은 2초 뒤 더미 피드백 표시
        new Handler().postDelayed(() -> {
            loadingDialog.dismiss();

            // 더미 피드백
            String fakeFeedback =
                    "\uD83C\uDF31 전체 구조는 잘 작성되었습니다!\n" +
                            "✍️ 역할 지정이 조금 더 구체적이면 좋아요.\n" +
                            "✨ 출력 조건을 명확하게 하면 더 좋은 결과를 얻을 수 있어요!";

            showFeedbackDialog(fakeFeedback);

        }, 2000);
    }

    // --------------------------------
    // Firestore 랜덤 문제 5개 로드
    // --------------------------------
    private void loadQuestionsFromDB() {

        db.collection("questions")
                .get()
                .addOnSuccessListener(result -> {

                    List<Map<String, Object>> list = new ArrayList<>();

                    for (var doc : result) {
                        list.add(doc.getData());
                    }

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
                    }
                });
    }

    // --------------------------------
    // 피드백 다이얼로그
    // --------------------------------
    private void showFeedbackDialog(String feedback) {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.feedback_dialog);

        TextView tvFeedback = dialog.findViewById(R.id.tvFeedback);
        Button btnClose = dialog.findViewById(R.id.btnClose);

        tvFeedback.setText(feedback);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // --------------------------------
    // 로딩 다이얼로그
    // --------------------------------
    private Dialog showLoadingDialog() {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.loading_dialog);
        dialog.setCancelable(false);
        dialog.show();

        return dialog;
    }
}
