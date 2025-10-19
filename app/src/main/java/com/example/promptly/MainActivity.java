package com.example.promptly;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "MyPrefs";
    private static final String NICKNAME_KEY = "nickname";

    private EditText nicknameEditText;
    private Button checkNicknameButton;
    private Button logoutButton;
    private TextView welcomeText;
    private Group loginView;
    private Group profileView;

    private SharedPreferences prefs;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userRepository = new UserRepository();

        nicknameEditText = findViewById(R.id.nickname_edit_text);
        checkNicknameButton = findViewById(R.id.check_nickname_button);
        logoutButton = findViewById(R.id.logout_button);
        welcomeText = findViewById(R.id.welcome_text);
        loginView = findViewById(R.id.login_view);
        profileView = findViewById(R.id.profile_view);

        checkLoginStatus();

        // 닉네임 입력 후 버튼 클릭 이벤트
        checkNicknameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nickname = nicknameEditText.getText().toString().trim();
                if (!nickname.isEmpty()) {
                    loginOrCreateUser(nickname);
                }
            }
        });

        // 로그아웃 버튼 클릭 이벤트
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }

    // SharedPreferences를 확인하여 로그인 상태 판단
    private void checkLoginStatus() {
        String nickname = prefs.getString(NICKNAME_KEY, null);
        if (nickname != null) {
            showProfileView(nickname);
        } else {
            showLoginView();
        }
    }


    // 닉네임 중복 체크
    private void loginOrCreateUser(String nickname) {
        userRepository.checkNicknameExists(nickname, new UserRepository.UserRepositoryCallback() {
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    // 닉네임이 이미 존재하는 경우
                    Toast.makeText(MainActivity.this, "이미 존재하는 닉네임입니다. 다른 닉네임으로 시도해주세요.", Toast.LENGTH_SHORT).show();
                } else {
                    // 새로운 닉네임인 경우
                    userRepository.createUser(nickname);
                    saveNicknameAndLogin(nickname);
                    Toast.makeText(MainActivity.this, "닉네임이 생성되었습니다!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "Error checking nickname: ", e);
                Toast.makeText(MainActivity.this, "Error checking nickname.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // SharedPreferences에 닉네임 저장 후 다음 화면으로 전환
    private void saveNicknameAndLogin(String nickname) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(NICKNAME_KEY, nickname);
        editor.apply();
        showProfileView(nickname);
    }

    private void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(NICKNAME_KEY);
        editor.apply();

        showLoginView();
        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void showProfileView(String nickname) {
        loginView.setVisibility(View.GONE);
        profileView.setVisibility(View.VISIBLE);
        welcomeText.setText("안녕하세요, " + nickname + "님!");
    }

    private void showLoginView() {
        profileView.setVisibility(View.GONE);
        loginView.setVisibility(View.VISIBLE);
    }
}
