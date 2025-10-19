package com.example.promptly;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private final CollectionReference usersRef;

    public interface UserRepositoryCallback {
        void onResult(boolean exists);
        void onError(Exception e);
    }

    public UserRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection("users");
    }

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

    public void createUser(String nickname) {
        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        usersRef.add(user);
    }
}
