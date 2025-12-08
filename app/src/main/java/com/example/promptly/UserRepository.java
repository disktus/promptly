package com.example.promptly;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private final FirebaseFirestore db;
    private final CollectionReference usersRef;
    private final CollectionReference evaluationsRef;

    // 닉네임 존재 여부 확인 결과를 위한 콜백 인터페이스
    public interface UserRepositoryCallback {
        void onResult(boolean exists);
        void onError(Exception e);
    }

    // 유저 ID 반환을 위한 콜백 인터페이스
    public interface UserIdCallback {
        void onUserIdFound(String userId);
    }

    // 사전 테스트 완료 상태 확인 결과를 위한 콜백 인터페이스
    public interface PreTestStatusCallback {
        void onStatusChecked(boolean isCompleted);
    }

    // 순위 갱신 완료 콜백 인터페이스 추가
    public interface RankUpdateCallback {
        void onRanksUpdated();
    }

    // 사용자 통계 데이터를 반환하기 위한 콜백 인터페이스
    public interface UserStatsCallback {
        void onStatsLoaded(long totalScore, long attempt, int scoreRank, int attemptRank, int totalUsers);
        void onError(Exception e);
    }

    // FirebaseFirestore 인스턴스를 초기화하고 컬렉션 참조 설정
    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection("users");
        this.evaluationsRef = db.collection("evaluations");
    }

    // 닉네임이 이미 존재하는지 확인
    public void checkNicknameExists(String nickname, final UserRepositoryCallback callback) {
        usersRef.whereEqualTo("nickname", nickname).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    callback.onResult(!task.getResult().isEmpty());
                } else {
                    callback.onError(task.getException());
                }
            }
        });
    }

    // 새로운 유저를 생성하고 생성된 유저 ID를 콜백으로 반환
    public void createUser(String nickname, final UserIdCallback callback) {
        Map<String, Object> user = createInitialUserMap(nickname);
        usersRef.add(user).addOnSuccessListener(documentReference -> {
            callback.onUserIdFound(documentReference.getId());
        }).addOnFailureListener(e -> {
            callback.onUserIdFound(null);
        });
    }

    // 닉네임을 기반으로 기존 유저의 ID를 조회하여 반환
    public void getUserIdByNickname(String nickname, final UserIdCallback callback) {
        usersRef.whereEqualTo("nickname", nickname)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot result = task.getResult();
                            if (result != null && !result.isEmpty()) {
                                DocumentSnapshot document = result.getDocuments().get(0);
                                callback.onUserIdFound(document.getId());
                            } else {
                                callback.onUserIdFound(null);
                            }
                        } else {
                            callback.onUserIdFound(null);
                        }
                    }
                });
    }

    // 특정 유저 ID를 가진 사용자가 사전 테스트를 완료했는지 여부를 확인
    public void isPreTestCompleted(String userId, final PreTestStatusCallback callback) {
        evaluationsRef
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onStatusChecked(!task.getResult().isEmpty());
                    } else {
                        callback.onStatusChecked(false);
                    }
                });
    }

    // 사용자 통계 데이터를 로드하여 콜백으로 반환
    public void loadUserStats(String userId, final UserStatsCallback callback) {
        usersRef.document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                DocumentSnapshot document = task.getResult();
                long totalScore = document.getLong("totalScore") != null ? document.getLong("totalScore") : 0;
                long attempt = document.getLong("attempt") != null ? document.getLong("attempt") : 0;
                int scoreRank = document.getLong("scoreRank") != null ? document.getLong("scoreRank").intValue() : 0;
                int attemptRank = document.getLong("attemptRank") != null ? document.getLong("attemptRank").intValue() : 0;

                // 총 유저 수 계산 (랭크 퍼센트 계산용)
                usersRef.get().addOnSuccessListener(querySnapshot -> {
                    int totalUsers = querySnapshot.size();
                    callback.onStatsLoaded(totalScore, attempt, scoreRank, attemptRank, totalUsers);
                }).addOnFailureListener(e -> callback.onError(e));
            } else {
                callback.onError(task.getException());
            }
        });
    }

    // 유저의 시도 횟수와 총점을 증가시키고 순위 갱신 후 콜백 호출
    public void incrementAttemptAndScore(String userId, int newScore, final RankUpdateCallback callback) {
        usersRef.document(userId).get().addOnSuccessListener(document -> {
            if (!document.exists()) {
                callback.onRanksUpdated();
                return;
            }

            long currentAttempt = document.getLong("attempt") != null ? document.getLong("attempt") : 0;
            currentAttempt++;
            long currentTotal = document.getLong("totalScore") != null ? document.getLong("totalScore") : 0;
            long updatedTotal = currentTotal + newScore;

            Map<String, Object> updates = new HashMap<>();
            updates.put("attempt", currentAttempt);
            updates.put("totalScore", updatedTotal);

            usersRef.document(userId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateAttemptRank();
                        updateScoreRank(callback);
                    })
                    .addOnFailureListener(e -> callback.onRanksUpdated());
        });
    }

    // 시도 횟수를 기준으로 유저 순위를 업데이트
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

    // 총점을 기준으로 유저 순위를 업데이트하고 완료 시 콜백 호출
    private void updateScoreRank(final RankUpdateCallback callback) {
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
                    callback.onRanksUpdated(); // 순위 갱신 완료 후 콜백 호출
                })
                .addOnFailureListener(e -> callback.onRanksUpdated());
    }

    // 유저 생성 시 필요한 기본 데이터를 포함한 맵을 반환
    private Map<String, Object> createInitialUserMap(String nickname) {
        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        user.put("solvedCount", 0L);
        user.put("attempt", 0L);
        user.put("attemptRank", 0);
        user.put("totalScore", 0L);
        user.put("scoreRank", 0);
        return user;
    }
}