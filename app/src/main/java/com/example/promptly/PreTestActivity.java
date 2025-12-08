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

import com.example.promptly.GeminiModels.Content;
import com.example.promptly.GeminiModels.GeminiRequest;
import com.example.promptly.GeminiModels.GeminiResponse;
import com.example.promptly.GeminiModels.Part;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PreTestActivity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    TextView[] tvTitles = new TextView[3];
    TextView[] tvConditions = new TextView[3];
    EditText[] etAnswers = new EditText[3];

    private List<String> loadedQuestionIds = new ArrayList<>();
    private String currentUserId;
    private LoadingDialog loadingDialog;

    private GeminiApiService geminiApiService;
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_test);

        loadingDialog = new LoadingDialog(this);
        initGeminiApi();

        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = prefs.getString(MainActivity.USER_ID_KEY, null);
        if (currentUserId == null) {
            Toast.makeText(this, "유저 정보를 찾을 수 없습니다. 재로그인하세요.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupQuestionViews();
        loadQuestionsFromDB();

        Button btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> {
            submitPreTest();
        });
    }

    private void initGeminiApi() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        geminiApiService = retrofit.create(GeminiApiService.class);
    }

    private void setupQuestionViews() {
        int[] questionIds = { R.id.question1, R.id.question2, R.id.question3 };

        for (int i = 0; i < 3; i++) {
            android.view.View qView = findViewById(questionIds[i]);

            tvTitles[i] = qView.findViewById(R.id.tvQuestionTitle);
            tvConditions[i] = qView.findViewById(R.id.tvConditions);
            etAnswers[i] = qView.findViewById(R.id.etAnswer);
        }
    }

    private void loadQuestionsFromDB() {
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

    private void submitPreTest() {
        for (EditText etAnswer : etAnswers) {
            if (etAnswer.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "모든 문제에 답변을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        loadingDialog.show();

        calculateAndSaveResults();
    }

    private void calculateAndSaveResults() {
        StringBuilder combinedPrompt = new StringBuilder();

        for (int i = 0; i < 3; i++) {
            combinedPrompt.append("--- 문제 ").append(i + 1).append(" ---\n");
            combinedPrompt.append("조건:\n").append(tvConditions[i].getText().toString()).append("\n");
            combinedPrompt.append("답변:\n").append(etAnswers[i].getText().toString()).append("\n\n");
        }

        String finalPrompt = generateGeminiPrompt(combinedPrompt.toString());

        List<Part> parts = new ArrayList<>();
        parts.add(new Part(finalPrompt));
        GeminiRequest request = new GeminiRequest(Collections.singletonList(new Content(parts)));

        String apiKey = GEMINI_API_KEY != null ? GEMINI_API_KEY.trim() : "";

        geminiApiService.generateContent(apiKey, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                loadingDialog.dismiss();
                if (response.isSuccessful() && response.body() != null && response.body().candidates != null && !response.body().candidates.isEmpty()) {
                    try {
                        String jsonText = response.body().candidates.get(0).content.parts.get(0).text;
                        if (jsonText.startsWith("```")) {
                            jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "");
                        }
                        Gson gson = new Gson();
                        PreTestFeedback feedback = gson.fromJson(jsonText, PreTestFeedback.class);

                        markTestCompleted();

                        // itemScores_100 (5개 항목 각 100점 만점)을 최종 화면으로 전달
                        int[] itemScores_100 = convertItemScores(feedback.details);
                        // 총점 100점을 그대로 전달
                        navigateToResultScreen(feedback.totalScore, itemScores_100);

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(PreTestActivity.this, "AI 응답 파싱 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        markTestCompletedAndGoMain();
                    }
                } else {
                    Toast.makeText(PreTestActivity.this, "AI 채점 오류: " + response.code(), Toast.LENGTH_LONG).show();
                    markTestCompletedAndGoMain();
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                loadingDialog.dismiss();
                Toast.makeText(PreTestActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
                markTestCompletedAndGoMain();
            }
        });
    }

    private String generateGeminiPrompt(String combinedConditionsAndAnswers) {
        return "다음은 사용자가 3개의 문제에 대해 작성한 프롬프트 엔지니어링 답변입니다:\n" +
                combinedConditionsAndAnswers +
                "--- 지시 사항 ---\n" +
                "이 3개 답변의 품질을 종합적으로 평가하여 **반드시 JSON 형식으로만** 응답해 주세요. 다른 말은 하지 마세요.\n" +
                "JSON 구조:\n" +
                "{\n" +
                "  \"totalScore\": (3개 문제 종합 0~100 사이 정수),\n" +
                "  \"details\": [\n" +
                "    { \"category\": \"목적 적합성 (Fitness)\", \"score\": (0~20 정수), \"comment\": \"짧은 평가\" },\n" +
                "    { \"category\": \"명확성 (Clarity)\", \"score\": (0~20 정수), \"comment\": \"짧은 평가\" },\n" +
                "    { \"category\": \"맥락/배경 (Context)\", \"score\": (0~20 정수), \"comment\": \"짧은 평가\" },\n" +
                "    { \"category\": \"형식/구조 (Structure)\", \"score\": (0~20 정수), \"comment\": \"짧은 평가\" },\n" +
                "    { \"category\": \"추론 유도 (CoT)\", \"score\": (0~20 정수), \"comment\": \"짧은 평가\" }\n" +
                "  ]\n" +
                "}";
    }

    private void saveEvaluation(PreTestFeedback feedback) {
        Map<String, Object> eval = new HashMap<>();
        eval.put("userId", currentUserId);
        eval.put("questionIds", loadedQuestionIds);
        eval.put("totalScore", feedback.totalScore);
        eval.put("timestamp", FieldValue.serverTimestamp());

        eval.put("isPreTest", true); // PreTest 기록임을 명시하는 태그

        for (PreTestFeedback.ScoreItem item : feedback.details) {
            String key = item.category.split(" ")[0].toLowerCase();
            eval.put(key, item.score);
        }

        StringBuilder conditions = new StringBuilder();
        for (TextView tv : tvConditions) {
            conditions.append(tv.getText().toString()).append("\n");
        }
        eval.put("condition", conditions.toString());

        db.collection("evaluations").add(eval);
    }

    private int[] convertItemScores(List<PreTestFeedback.ScoreItem> details) {
        int[] scores = new int[5];

        // 5개 항목 순서대로 0~100점 기준으로 변환: AI는 0~20점을 반환하므로 5배를 곱함
        if (details != null && details.size() >= 5) {
            scores[0] = details.get(0).score * 5; // 목적 적합성 (Fitness)
            scores[1] = details.get(1).score * 5; // 명확성 (Clarity)
            scores[2] = details.get(2).score * 5; // 맥락/배경 (Context)
            scores[3] = details.get(3).score * 5; // 형식/구조 (Structure)
            scores[4] = details.get(4).score * 5; // 추론 유도 (CoT)
        }
        return scores;
    }

    private void markTestCompleted() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(MainActivity.PRETEST_COMPLETED_KEY, true);
        editor.apply();
    }

    private void markTestCompletedAndGoMain() {
        markTestCompleted();
        Intent mainIntent = new Intent(PreTestActivity.this, MainTestActivity.class);
        startActivity(mainIntent);
        finish();
    }

    public static class PreTestFeedback {
        public int totalScore;
        public List<ScoreItem> details;

        public static class ScoreItem {
            public String category, comment;
            public int score;
        }
    }

    private void navigateToResultScreen(int totalScore, int[] itemScores) {
        Intent intent = new Intent(PreTestActivity.this, PreTestResultActivity.class);
        intent.putExtra("TOTAL_SCORE", totalScore);
        intent.putExtra("ITEM_SCORES", itemScores);
        startActivity(intent);
        finish();
    }
}