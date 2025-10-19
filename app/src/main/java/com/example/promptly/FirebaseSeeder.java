package com.example.promptly;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirebaseSeeder {

    private final FirebaseFirestore db;

    public FirebaseSeeder(FirebaseFirestore db) {
        this.db = db;
    }

    public void seed() {
        seedUsersAndEvaluations();
        seedQuestions();
    }

    // 유저 생성 + 샘플 평가
    private void seedUsersAndEvaluations() {
        // Alice 생성
        createUser("Alice", userId -> {
            // Alice 샘플 평가
            createEvaluation(userId, "q1", 17, 16, 15, 18, 18); // 총합 84
            createEvaluation(userId, "q2", 15, 14, 16, 17, 15); // 총합 77
            createEvaluation(userId, "q3", 18, 17, 18, 16, 17); // 총합 86
        });

        // Bob 생성
        createUser("Bob", userId -> {
            // Bob 샘플 평가
            createEvaluation(userId, "q1", 14, 15, 13, 14, 16); // 총합 72
            createEvaluation(userId, "q2", 16, 16, 15, 15, 15); // 총합 77
        });
    }

    // 콜백 인터페이스
    interface UserIdCallback {
        void onUserCreated(String userId);
    }

    // 유저 생성 메서드
    private void createUser(String nickname, UserIdCallback callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        user.put("solvedCount", 0);
        user.put("attempt", 0);
        user.put("attemptRank", 0);
        user.put("totalScore", 0);
        user.put("scoreRank", 0);

        db.collection("users").add(user).addOnSuccessListener(documentReference -> {
            String userId = documentReference.getId();
            callback.onUserCreated(userId);
        });
    }

    // 문제
    private void seedQuestions() {
        createQuestion("q1", "학교 과제 제출", "대학생", "보고서", "객관적/논리적", "보고서");
        createQuestion("q2", "주말 요리", "홈셰프", "레시피", "간단하게", "요리");
        createQuestion("q3", "여행 후", "여행 블로거", "여행 후기", "친절하고 자세하게", "후기");
    }

    private void createQuestion(String docId, String situation, String role, String document,
                                String style, String category) {
        Map<String, Object> q = new HashMap<>();
        q.put("situation", situation);
        q.put("role", role);
        q.put("document", document);
        q.put("style", style);
        q.put("category", category);

        db.collection("questions").document(docId).set(q);
    }

    // 평가
    private void createEvaluation(String userId, String questionId,
                                  int clarity, int specificity, int logic,
                                  int creativity, int context) {
        int total = clarity + specificity + logic + creativity + context;

        Map<String, Object> eval = new HashMap<>();
        eval.put("userId", userId);
        eval.put("questionId", questionId);
        eval.put("clarity", clarity);
        eval.put("specificity", specificity);
        eval.put("logic", logic);
        eval.put("creativity", creativity);
        eval.put("context", context);
        eval.put("totalScore", total);

        db.collection("evaluations").add(eval);

        // 평가 후 유저 점수 갱신
        incrementAttemptAndScore(userId, total);
    }

    // 점수, 순위 관리
    public void incrementAttemptAndScore(String userId, int newScore) {
        db.collection("users").document(userId).get().addOnSuccessListener(document -> {
            if (!document.exists()) return;

            long currentAttempt = document.getLong("attempt") != null ? document.getLong("attempt") : 0;
            currentAttempt++;
            long currentTotal = document.getLong("totalScore") != null ? document.getLong("totalScore") : 0;
            long updatedTotal = currentTotal + newScore;

            Map<String, Object> updates = new HashMap<>();
            updates.put("attempt", currentAttempt);
            updates.put("totalScore", updatedTotal);

            db.collection("users").document(userId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateAttemptRank();
                        updateScoreRank();
                    });
        });
    }

    private void updateAttemptRank() {
        db.collection("users")
                .orderBy("attempt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    int rank = 1;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        db.collection("users").document(doc.getId())
                                .update("attemptRank", rank);
                        rank++;
                    }
                });
    }

    private void updateScoreRank() {
        db.collection("users")
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener((QuerySnapshot querySnapshot) -> {
                    int rank = 1;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        db.collection("users").document(doc.getId())
                                .update("scoreRank", rank);
                        rank++;
                    }
                });
    }
}
