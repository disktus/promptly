package com.example.promptly;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue; // FieldValue 임포트 추가

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PreTestActivity extends AppCompatActivity {

    // Firebase Firestore 인스턴스
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // 뷰 배열 선언
    TextView[] tvTitles = new TextView[3];
    TextView[] tvConditions = new TextView[3];
    EditText[] etAnswers = new EditText[3];

    // 로드된 문제 ID를 저장할 리스트
    private List<String> loadedQuestionIds = new ArrayList<>();
    // 유저 ID
    private String currentUserId;
    // UserRepository 인스턴스 (점수 갱신에 필요)
    private UserRepository userRepository;

    // 로딩 다이얼로그
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 액티비티 생성 및 초기 설정
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_test);

        // 초기화
        userRepository = new UserRepository();
        loadingDialog = new LoadingDialog(this);

        // 유저 ID 가져오기 (MainActivity에서 저장했던 것)
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getString(MainActivity.USER_ID_KEY, null);
        if (currentUserId == null) {
            Toast.makeText(this, "유저 정보를 찾을 수 없습니다. 재로그인하세요.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // question_layout의 3세트 뷰 찾기
        setupQuestionViews();

        // Firestore에서 문제 3개 가져오기
        loadQuestionsFromDB();

        // 제출 버튼 처리
        Button btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> {
            submitPreTest();
        });
    }

    // question_layout 내부 뷰 연결
    private void setupQuestionViews() {
        // 뷰 ID 배열
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
        // DB에서 질문 데이터를 가져와 화면에 표시
        db.collection("questions")
                .get()
                .addOnSuccessListener(result -> {

                    List<Map<String, Object>> questionListWithId = new ArrayList<>();

                    for (DocumentSnapshot doc : result) {
                        Map<String, Object> questionData = doc.getData();
                        if (questionData != null) {
                            questionData.put("id", doc.getId());
                            questionListWithId.add(questionData);
                        }
                    }

                    Collections.shuffle(questionListWithId);

                    loadedQuestionIds.clear();

                    int count = Math.min(3, questionListWithId.size());
                    for (int i = 0; i < count; i++) {
                        Map<String, Object> q = questionListWithId.get(i);

                        String qId = (String) q.get("id");
                        loadedQuestionIds.add(qId);

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

    // 제출 버튼 클릭 시 로직 처리 및 화면 전환
    private void submitPreTest() {
        // 모든 답변이 채워졌는지 확인하는 로직
        for (EditText etAnswer : etAnswers) {
            if (etAnswer.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "모든 문제에 답변을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        loadingDialog.show();

        // 점수 계산 및 DB 저장
        calculateAndSaveResults();
    }

    // 채점 결과를 계산하고 Firestore에 저장한 후 결과 화면으로 이동
    private void calculateAndSaveResults() {
        // 임시 채점 로직 (실제 API 호출 대신 사용)
        Random random = new Random();

        // 🔥 totalScore를 배열로 선언하여 콜백 내부에서 참조 가능하게 함 (effectively final 해결)
        final int[] totalScore = new int[]{0};
        final int[] itemScores = new int[5]; // 항목별 점수 저장용 배열

        for (int i = 0; i < 3; i++) {
            String questionId = loadedQuestionIds.get(i);
            String answer = etAnswers[i].getText().toString();

            // 임의의 점수 (10~20점) 5개 항목 설정
            int clarity = 10 + random.nextInt(11);
            int specificity = 10 + random.nextInt(11);
            int logic = 10 + random.nextInt(11);
            int creativity = 10 + random.nextInt(11);
            int context = 10 + random.nextInt(11);

            int score = clarity + specificity + logic + creativity + context;

            // 🔥 totalScore 배열에 누적
            totalScore[0] += score;

            // 항목별 점수 누적 (총 3문제의 평균을 보여주지 않고 총점을 보여줄 것이므로, 여기서는 항목별 총합을 계산하지 않음)
            // PreTestResultActivity는 문제 1개의 평가를 기준으로 그래프를 보여주므로, 여기서는 마지막 문제의 점수를 사용하거나
            // 모든 문제의 평균 점수를 계산해야 함. 여기서는 계산 편의상 마지막 문제의 점수를 저장
            if (i == 2) {
                itemScores[0] = clarity;
                itemScores[1] = specificity;
                itemScores[2] = logic;
                itemScores[3] = creativity;
                itemScores[4] = context;
            }


            // 평가 기록 저장
            saveEvaluation(questionId, answer, clarity, specificity, logic, creativity, context, score);
        }

        // 유저 총점 갱신 및 순위 재계산
        userRepository.incrementAttemptAndScore(currentUserId, totalScore[0], new UserRepository.RankUpdateCallback() {
            @Override
            public void onRanksUpdated() {
                // 모든 작업 완료 후 결과 화면으로 이동
                loadingDialog.dismiss();
                markTestCompleted();
                // 🔥 배열에서 값 참조
                navigateToResultScreen(totalScore[0], itemScores);
            }
        });
    }

    // Firestore에 평가 데이터를 저장
    private void saveEvaluation(String questionId, String answer, int clarity, int specificity, int logic, int creativity, int context, int total) {
        // 평가 데이터를 Firestore에 저장
        Map<String, Object> eval = new HashMap<>();
        eval.put("userId", currentUserId);
        eval.put("questionId", questionId);
        eval.put("answer", answer);
        eval.put("clarity", clarity);
        eval.put("specificity", specificity);
        eval.put("logic", logic);
        eval.put("creativity", creativity);
        eval.put("context", context);
        eval.put("totalScore", total);
        eval.put("timestamp", FieldValue.serverTimestamp());

        db.collection("evaluations").add(eval);
    }

    // SharedPreferences에 테스트 완료 상태를 true로 저장
    private void markTestCompleted() {
        // SharedPreferences에 테스트 완료 상태를 true로 업데이트
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(MainActivity.PRETEST_COMPLETED_KEY, true);
        editor.apply();
    }

    // PreTestResultActivity (채점 결과 화면)로 이동
    private void navigateToResultScreen(int totalScore, int[] itemScores) {
        // 총점과 항목별 점수를 담아 결과 화면으로 이동
        Intent intent = new Intent(PreTestActivity.this, PreTestResultActivity.class);
        intent.putExtra("TOTAL_SCORE", totalScore);
        intent.putExtra("ITEM_SCORES", itemScores); // 항목별 점수 배열도 함께 전달
        startActivity(intent);
        finish();
    }
}