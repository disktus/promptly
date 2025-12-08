package com.example.promptly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.promptly.GeminiModels.Content;
import com.example.promptly.GeminiModels.GeminiRequest;
import com.example.promptly.GeminiModels.GeminiResponse;
import com.example.promptly.GeminiModels.Part;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainTestActivity extends AppCompatActivity {

    private static final String TAG = "MainTestActivity";

    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final boolean TEST_MODE = false;

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    GeminiApiService geminiApiService;

    TextView[] tvTitles = new TextView[5];
    TextView[] tvConditions = new TextView[5];
    EditText[] etAnswers = new EditText[5];
    Button[] btnSubmits = new Button[5];
    TextView tvCountdown;
    ImageView btnHome, btnMy;

    private List<Question> loadedQuestions = new ArrayList<>();
    private static final long REFRESH_INTERVAL = 12 * 60 * 60 * 1000;
    Handler handler = new Handler();

    SharedPreferences timePref;
    SharedPreferences submitPref;
    SharedPreferences userPref;

    private AlertDialog currentLoadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintest);

        timePref = getSharedPreferences("main_test_time", MODE_PRIVATE);
        submitPref = getSharedPreferences("main_test_submit", MODE_PRIVATE);
        userPref = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        initGeminiApi();
        bindViews();
        setupSubmitButtons();
        loadQuestionsFromDB();
        startCountdownTimer();
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

    private void bindViews() {
        tvCountdown = findViewById(R.id.tvCountdown);
        btnHome = findViewById(R.id.btnHome);
        btnMy = findViewById(R.id.btnMy);

        btnHome.setOnClickListener(v -> {});
        btnMy.setOnClickListener(v -> {
            Intent intent = new Intent(MainTestActivity.this, MyPageActivity.class);
            startActivity(intent);
        });

        int[] ids = {R.id.q1, R.id.q2, R.id.q3, R.id.q4, R.id.q5};
        for (int i = 0; i < 5; i++) {
            View v = findViewById(ids[i]);
            tvTitles[i] = v.findViewById(R.id.tvQuestionTitle);
            tvConditions[i] = v.findViewById(R.id.tvConditions);
            etAnswers[i] = v.findViewById(R.id.etAnswer);
            btnSubmits[i] = v.findViewById(R.id.btnSubmitOne);
        }
    }

    private void loadQuestionsFromDB() {
        db.collection("questions").get().addOnSuccessListener(result -> {
            if (result.isEmpty() || result.size() < 5) {
                tvCountdown.setText("문제가 5개 미만입니다");
                return;
            }

            List<Question> allQuestions = new ArrayList<>();
            for (var doc : result) {
                allQuestions.add(new Question(doc.getId(), doc.getData()));
            }

            Collections.shuffle(allQuestions);
            loadedQuestions = allQuestions.subList(0, 5);

            for (int i = 0; i < 5; i++) {
                Question q = loadedQuestions.get(i);
                tvTitles[i].setText("Q" + (i + 1) + ". 다음 조건을 만족하는 프롬프트를 입력하세요.");
                String cond = "상황: " + q.data.get("situation") + "\n" +
                        "직업: " + q.data.get("role") + "\n" +
                        "작성물: " + q.data.get("document") + "\n" +
                        "스타일: " + q.data.get("style");
                tvConditions[i].setText(cond);
                enableQuestion(i);
            }

            timePref.edit().putLong("last_refresh_time", System.currentTimeMillis()).apply();
            submitPref.edit().clear().apply();

        }).addOnFailureListener(e -> {
            tvCountdown.setText("문제 로딩 실패");
            e.printStackTrace();
        });
    }

    private void setupSubmitButtons() {
        for (int i = 0; i < 5; i++) {
            final int index = i;
            boolean submitted = submitPref.getBoolean("submitted_" + index, false);
            if (submitted) disableQuestion(index);

            btnSubmits[i].setOnClickListener(v -> {
                if (loadedQuestions.size() <= index) return;

                String answer = etAnswers[index].getText().toString().trim();
                if (answer.isEmpty()) {
                    Toast.makeText(this, "답변을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                submitPref.edit().putBoolean("submitted_" + index, true).apply();
                disableQuestion(index);

                String questionId = loadedQuestions.get(index).id;
                String condition = tvConditions[index].getText().toString();
                callGeminiApi(questionId, condition, answer);
            });
        }
    }

    private void callGeminiApi(String questionId, String condition, String answer) {
        currentLoadingDialog = showLoadingDialog();

        if (TEST_MODE) {
            return;
        }

        String prompt = "다음은 사용자가 작성한 프롬프트 엔지니어링 답변에 대한 조건입니다:\n" +
                "--- 조건 ---\n" + condition + "\n\n" +
                "--- 사용자 답변 ---\n" + answer + "\n\n" +
                "--- 지시 사항 ---\n" +
                "이 답변을 다음 5가지 기준으로 평가하여 **반드시 JSON 형식으로만** 응답해 주세요. 다른 말은 하지 마세요.\n" +
                "JSON 구조:\n" +
                "{\n" +
                "  \"totalScore\": (0~100 사이 정수),\n" +
                "  \"details\": [\n" +
                "    { \"category\": \"목적 적합성 (Fitness)\", \"score\": (0~20 정수), \"comment\": \"짧은 평가\" },\n" +
                "    { \"category\": \"명확성 (Clarity)\", \"score\": (0~20 정수), \"comment\": \"짧은 평가\" },\n" +
                "    { \"category\": \"맥락/배경 (Context)\", \"score\": (0~20 정수), \"comment\": \"짧은 평가\" },\n" +
                "    { \"category\": \"형식/구조 (Structure)\", \"score\": (0~20 정수), \"comment\": \"짧은 평가\" },\n" +
                "    { \"category\": \"추론 유도 (CoT)\", \"score\": (0~20 정수), \"comment\": \"짧은 평가\" }\n" +
                "  ],\n" +
                "  \"summary\": \"총평 및 개선할 점 요약\",\n" +
                "  \"example\": \"조건을 완벽하게 충족하는 모범 답안 (한국어)\"\n" +
                "}";

        List<Part> parts = new ArrayList<>();
        parts.add(new Part(prompt));
        GeminiRequest request = new GeminiRequest(Collections.singletonList(new Content(parts)));

        String apiKey = GEMINI_API_KEY != null ? GEMINI_API_KEY.trim() : "";

        geminiApiService.generateContent(apiKey, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (currentLoadingDialog != null && currentLoadingDialog.isShowing()) {
                    currentLoadingDialog.dismiss();
                }

                if (response.isSuccessful() && response.body() != null && response.body().candidates != null && !response.body().candidates.isEmpty()) {
                    try {
                        String jsonText = response.body().candidates.get(0).content.parts.get(0).text;
                        if (jsonText.startsWith("```")) {
                            jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "");
                        }
                        Gson gson = new Gson();
                        FeedbackJson feedback = gson.fromJson(jsonText, FeedbackJson.class);

                        showResultDialog(feedback);
                        saveEvaluationResult(questionId, condition, answer, feedback);

                    } catch (Exception e) {
                        e.printStackTrace();
                        showErrorToast("결과 파싱 오류: " + e.getMessage());
                    }
                } else {
                    showErrorToast("API 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                if (currentLoadingDialog != null && currentLoadingDialog.isShowing()) {
                    currentLoadingDialog.dismiss();
                }
                showErrorToast("네트워크 오류: " + t.getMessage());
            }
        });
    }

    private void saveEvaluationResult(String questionId, String condition, String userAnswer, FeedbackJson result) {
        String userId = userPref.getString(MainActivity.USER_ID_KEY, "unknown_user");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("questionId", questionId);
        data.put("answer", userAnswer);
        data.put("totalScore", result.totalScore);
        data.put("summary", result.summary);
        data.put("example", result.example);
        data.put("timestamp", FieldValue.serverTimestamp());

        data.put("condition", condition);
        data.put("isPreTest", false);

        if (result.details != null) {
            for (FeedbackJson.ScoreItem item : result.details) {
                String key = "unknown";
                if (item.category.contains("목적")) key = "fitness";
                else if (item.category.contains("명확성")) key = "clarity";
                else if (item.category.contains("맥락")) key = "context";
                else if (item.category.contains("형식")) key = "structure";
                else if (item.category.contains("추론")) key = "cot";
                data.put(key, item.score);
            }
        }


        db.collection("evaluations")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Result saved with ID: " + docRef.getId());
                    String currentUserId = userPref.getString(MainActivity.USER_ID_KEY, null);
                    if (currentUserId != null) {
                        new UserRepository().incrementAttemptAndScore(currentUserId, result.totalScore, new UserRepository.RankUpdateCallback() {
                            @Override
                            public void onRanksUpdated() {
                                Log.d(TAG, "User stats and ranks updated.");
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error saving result", e));
    }

    private static class Question {
        String id;
        Map<String, Object> data;

        Question(String id, Map<String, Object> data) {
            this.id = id;
            this.data = data;
        }
    }

    public static class FeedbackJson {
        public int totalScore;
        public List<ScoreItem> details;
        public String summary, example;

        public static class ScoreItem {
            public String category, comment;
            public int score;
        }
    }

    private void disableQuestion(int index) {
        etAnswers[index].setEnabled(false);
        btnSubmits[index].setEnabled(false);
        btnSubmits[index].setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
    }

    private void enableQuestion(int index) {
        etAnswers[index].setEnabled(true);
        btnSubmits[index].setEnabled(true);
    }

    private AlertDialog showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.loading_dialog, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(false);

        dialog.show();
        return dialog;
    }

    private void showResultDialog(FeedbackJson data) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.feedback_dialog, null);
        builder.setView(view);

        TextView tvTotalScore = view.findViewById(R.id.tvTotalScore);
        TextView tvFeedbackContent = view.findViewById(R.id.tvFeedbackContent);
        TextView tvExample = view.findViewById(R.id.tvExample);
        Button btnClose = view.findViewById(R.id.btnClose);

        TextView[] tvItems = {
                view.findViewById(R.id.tvItem1),
                view.findViewById(R.id.tvItem2),
                view.findViewById(R.id.tvItem3),
                view.findViewById(R.id.tvItem4),
                view.findViewById(R.id.tvItem5)
        };

        if (tvTotalScore != null) tvTotalScore.setText(String.valueOf(data.totalScore));
        if (tvFeedbackContent != null) tvFeedbackContent.setText(data.summary);
        if (tvExample != null) tvExample.setText(data.example);

        if (data.details != null) {
            for (int i = 0; i < Math.min(data.details.size(), 5); i++) {
                FeedbackJson.ScoreItem item = data.details.get(i);
                String text = String.format("%d. %s (%d점): %s", i + 1, item.category, item.score, item.comment);
                if (tvItems[i] != null) tvItems[i].setText(text);
            }
        }

        View layoutLoading = view.findViewById(R.id.layoutLoading);
        View layoutResult = view.findViewById(R.id.layoutResult);

        if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
        if (layoutResult != null) layoutResult.setVisibility(View.VISIBLE);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showErrorToast(String errorMsg) {
        runOnUiThread(() -> {
            Toast.makeText(MainTestActivity.this, "오류 발생: " + errorMsg, Toast.LENGTH_LONG).show();
        });
    }

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
                tvCountdown.setText(String.format("다음 문제까지 %02d:%02d:%02d", h, m, s));
                handler.postDelayed(this, 1000);
            }
        });
    }
}