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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyPageActivity extends AppCompatActivity {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    TextView tvNickname;
    TextView tvAvgLabel;
    TextView tvAvgScore;
    TextView tvSolveLabel;
    TextView tvSolveRank;
    TextView tvScoreLabel;
    TextView tvScoreRank;

    LinearLayout layoutHistory;

    ImageView btnHome;
    ImageView btnMy;

    private static final String PREFS_NAME = "MyPrefs";
    private static final String NICKNAME_KEY = "nickname";
    private static final String USER_ID_KEY = "userId";

    private String currentUserId;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        bindViews();
        setupBottomNav();

        loadNicknameFromPrefs();
        loadStats();
        loadHistory();
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

        View bottomNavView = findViewById(R.id.bottomNav);
        if (bottomNavView != null) {
            btnHome = bottomNavView.findViewById(R.id.btnHome);
            btnMy = bottomNavView.findViewById(R.id.btnMy);
        }
    }

    private void loadNicknameFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String nickname = prefs.getString(NICKNAME_KEY, null);
        currentUserId = prefs.getString(USER_ID_KEY, null);

        if (nickname != null) {
            tvNickname.setText(nickname + "님의");
        } else {
            tvNickname.setText("사용자님의");
        }

        if (currentUserId == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadStats() {
        tvAvgLabel.setText("평균\n점수는");
        tvSolveLabel.setText("풀이수 상위 ");
        tvScoreLabel.setText("평균점수 상위 ");

        if (currentUserId == null) {
            tvAvgScore.setText("0점");
            tvSolveRank.setText("0%");
            tvScoreRank.setText("0%");
            return;
        }

        new UserRepository().loadUserStats(currentUserId, new UserRepository.UserStatsCallback() {
            @Override
            public void onStatsLoaded(long totalScore, long attempt, int scoreRank, int attemptRank, int totalUsers) {
                if (attempt > 0) {
                    int avgScore = (int) (totalScore / attempt);
                    tvAvgScore.setText(String.format(Locale.getDefault(), "%d점", avgScore));

                    double solvePercent = (totalUsers > 0 && attemptRank > 0) ? (double) attemptRank / totalUsers * 100 : 0;
                    double scorePercent = (totalUsers > 0 && scoreRank > 0) ? (double) scoreRank / totalUsers * 100 : 0;

                    tvSolveRank.setText(String.format(Locale.getDefault(), "%d%%", (int) solvePercent));
                    tvScoreRank.setText(String.format(Locale.getDefault(), "%d%%", (int) scorePercent));

                } else {
                    tvAvgScore.setText("0점");
                    tvSolveRank.setText("0%");
                    tvScoreRank.setText("0%");
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MyPageActivity.this, "통계 로드 실패", Toast.LENGTH_SHORT).show();
                tvAvgScore.setText("0점");
                tvSolveRank.setText("0%");
                tvScoreRank.setText("0%");
            }
        });
    }

    private void loadHistory() {
        if (currentUserId == null) {
            layoutHistory.removeAllViews();
            return;
        }

        layoutHistory.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

        db.collection("evaluations")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        EvaluationResult result = document.toObject(EvaluationResult.class);

                        String conditionStr = result.getCondition();
                        Map<String, String> conditions = parseConditionString(conditionStr);

                        View item = inflater.inflate(R.layout.item_history, layoutHistory, false);

                        TextView date = item.findViewById(R.id.tvHistoryDate);
                        TextView situation = item.findViewById(R.id.tvHistorySituation);
                        TextView role = item.findViewById(R.id.tvHistoryRole);
                        TextView doc = item.findViewById(R.id.tvHistoryDoc);
                        TextView style = item.findViewById(R.id.tvHistoryStyle);

                        if (document.getTimestamp("timestamp") != null) {
                            Date timestampDate = document.getTimestamp("timestamp").toDate();
                            date.setText(sdf.format(timestampDate) + " KST");
                        } else {
                            date.setText("날짜 없음");
                        }

                        String situationText = conditions.getOrDefault("상황", "N/A");
                        String roleText = conditions.getOrDefault("직업", "N/A");
                        String docText = conditions.getOrDefault("작성물", "N/A");
                        String styleText = conditions.getOrDefault("스타일", "N/A");

                        String line2 = "상황: " + situationText + " / 직업: " + roleText;
                        String line3 = "작성물: " + docText + " / 스타일: " + styleText;

                        situation.setText(line2);
                        role.setText(line3);
                        doc.setText("");
                        style.setText("");


                        item.setOnClickListener(v -> showHistoryDialog(document.toObject(EvaluationResult.class)));

                        layoutHistory.addView(item);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "기록 로드 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private Map<String, String> parseConditionString(String condition) {
        Map<String, String> map = new HashMap<>();
        if (condition == null) return map;

        String[] lines = condition.split("\n");
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    map.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return map;
    }


    private void showHistoryDialog(EvaluationResult result) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.feedback_dialog, null);
        builder.setView(view);

        TextView tvTotalScore = view.findViewById(R.id.tvTotalScore);
        TextView tvFeedbackContent = view.findViewById(R.id.tvFeedbackContent);
        TextView tvExample = view.findViewById(R.id.tvExample);
        Button btnClose = view.findViewById(R.id.btnClose);

        TextView[] tvItems = {
                view.findViewById(R.id.tvItem1), view.findViewById(R.id.tvItem2),
                view.findViewById(R.id.tvItem3), view.findViewById(R.id.tvItem4),
                view.findViewById(R.id.tvItem5)
        };

        if (tvTotalScore != null) tvTotalScore.setText(String.valueOf(result.getTotalScore()));

        Map<String, Long> detailScores = result.getDetailScores();
        if (detailScores != null) {
            String[] categories = {"목적 적합성", "명확성", "맥락/배경", "형식/구조", "추론 유도"};
            String[] keys = {"fitness", "clarity", "context", "structure", "cot"};

            for (int i = 0; i < categories.length && i < tvItems.length; i++) {
                Long score = detailScores.getOrDefault(keys[i], 0L);
                if (tvItems[i] != null) {
                    tvItems[i].setText(String.format("%d. %s (%d점)", i + 1, categories[i], score.intValue()));
                }
            }
        }

        if (tvFeedbackContent != null) {
            tvFeedbackContent.setText(result.getSummary());
        }

        if (tvExample != null) {
            tvExample.setText(result.getExample());
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


    private void setupBottomNav() {

        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainTestActivity.class));
            finish();
        });

        btnMy.setOnClickListener(v -> {
        });
    }

    public static class EvaluationResult {
        public Long fitness = 0L;
        public Long clarity = 0L;
        public Long context = 0L;
        public Long structure = 0L;
        public Long cot = 0L;
        public Long totalScore = 0L;
        public String summary;
        public String example;
        public String condition;
        public String answer;

        public EvaluationResult() {}

        public int getTotalScore() { return totalScore != null ? totalScore.intValue() : 0; }
        public String getSummary() { return summary; }
        public String getExample() { return example; }
        public String getCondition() { return condition; }
        public String getAnswer() { return answer; }

        public Map<String, Long> getDetailScores() {
            Map<String, Long> map = new HashMap<>();
            map.put("fitness", this.fitness != null ? this.fitness : 0L);
            map.put("clarity", this.clarity != null ? this.clarity : 0L);
            map.put("context", this.context != null ? this.context : 0L);
            map.put("structure", this.structure != null ? this.structure : 0L);
            map.put("cot", this.cot != null ? this.cot : 0L);
            return map;
        }
    }
}
