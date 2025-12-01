package com.example.promptly;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.promptly.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PreTestActivity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    TextView[] tvTitles = new TextView[3];
    TextView[] tvConditions = new TextView[3];
    EditText[] etAnswers = new EditText[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_test);

        // question_layout의 3세트 뷰 찾기
        setupQuestionViews();

        //Firestore에서 문제 3개 가져오기
        loadQuestionsFromDB();

        // 제출 버튼 처리
        Button btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> {
            String answer1 = etAnswers[0].getText().toString();
            String answer2 = etAnswers[1].getText().toString();
            String answer3 = etAnswers[2].getText().toString();

            // TODO: 제출 처리 로직
        });
    }

    // question_layout 내부 뷰 연결
    private void setupQuestionViews() {
        int[] questionIds = { R.id.question1, R.id.question2, R.id.question3 };

        for (int i = 0; i < 3; i++) {
            android.view.View qView = findViewById(questionIds[i]);

            tvTitles[i] = qView.findViewById(R.id.tvQuestionTitle);
            tvConditions[i] = qView.findViewById(R.id.tvConditions);
            etAnswers[i] = qView.findViewById(R.id.etAnswer);
        }
    }

    // Firestore에서 문제 3개 랜덤 로드
    private void loadQuestionsFromDB() {

        db.collection("questions")
                .get()
                .addOnSuccessListener(result -> {

                    List<Map<String, Object>> list = new ArrayList<>();

                    for (var doc : result) {
                        list.add(doc.getData());
                    }

                    Collections.shuffle(list);

                    for (int i = 0; i < 3; i++) {
                        Map<String, Object> q = list.get(i);

                        tvTitles[i].setText(
                                "Q" + (i + 1) + ". 다음 조건을 만족하는 프롬프트를\n입력하세요."
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
}
