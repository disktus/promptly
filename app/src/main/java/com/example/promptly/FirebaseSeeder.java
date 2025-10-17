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
        Map<String, Object> user1 = new HashMap<>();
        user1.put("nickname", "user1");
        user1.put("solvedCount", 0); // 푼 문제 수
        user1.put("scoreRank", 0); // 점수 기준 순위
        user1.put("attempt", 0); // 문제 푼 횟수
        user1.put("attemptRank", 0); // 푼 횟수 기준

        Map<String, Object> user2 = new HashMap<>();
        user2.put("nickname", "user2");
        user2.put("solvedCount", 0);
        user2.put("scoreRank", 0);
        user2.put("attempt", 0);
        user2.put("attemptRank", 0);

        db.collection("users").document("user1").set(user1);
        db.collection("users").document("user2").set(user2);
    }

    // question
    private void seedQuestions() {
        Map<String, Object> q1 = new HashMap<>();
        q1.put("situation", "학교 과제 제출");
        q1.put("role", "대학생");
        q1.put("document", "보고서");
        q1.put("style", "객관적/논리적");
        q1.put("category", "보고서");

        Map<String, Object> q2 = new HashMap<>();
        q2.put("situation", "주말 요리");
        q2.put("role", "홈셰프");
        q2.put("document", "레시피");
        q2.put("style", "간단하게");
        q2.put("category", "요리");

        Map<String, Object> q3 = new HashMap<>();
        q3.put("situation", "여행 후");
        q3.put("role", "여행 블로거");
        q3.put("document", "여행 후기");
        q3.put("style", "친절하고 자세하게");
        q3.put("category", "후기");

        db.collection("questions").document("q1").set(q1);
        db.collection("questions").document("q2").set(q2);
        db.collection("questions").document("q3").set(q3);
    }

    // 유저가 문제 푼 횟수 증가 및 attemptRank 업데이트
    public void incrementAttempt(String userId) {
        db.collection("users").document(userId).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                long currentAttempt = document.getLong("attempt") != null ? document.getLong("attempt") : 0;
                currentAttempt++;
                db.collection("users").document(userId).update("attempt", currentAttempt);

                // attemptRank 업데이트 예시
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
        });
    }
}