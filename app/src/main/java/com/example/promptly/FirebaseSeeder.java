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
        seedUsers();
        seedQuestions();
    }

    // user
    private void seedUsers() {
        createUser("user1", "user1");
        createUser("user2", "user2");
    }

    private void createUser(String docId, String nickname) {
        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        user.put("solvedCount", 0);    // 푼 문제 수
        user.put("attempt", 0);        // 문제 푼 횟수
        user.put("attemptRank", 0);    // 푼 횟수 기준 순위
        user.put("totalScore", 0);     // 총점
        user.put("scoreRank", 0);      // 점수 기준 순위

        db.collection("users").document(docId).set(user);
    }

    // questions
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

    // 점수, 순위 관리

    /**
     * 유저가 문제 푼 후 호출
     * - attempt 증가
     * - totalScore 갱신
     * - attemptRank / scoreRank 갱신
     */
    public void incrementAttemptAndScore(String userId, int newScore) {
        // 1) attempt 증가
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
                        // 2) 순위 갱신
                        updateAttemptRank();
                        updateScoreRank();
                    });
        });
    }

    // 순위 계산
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
