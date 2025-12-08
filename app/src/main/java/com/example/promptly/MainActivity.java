package com.example.promptly;

import android.content.Context;
import android.content.Intent;
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

    // 상수 정의
    private static final String TAG = "MainActivity";
    public static final String PREFS_NAME = "MyPrefs";
    public static final String NICKNAME_KEY = "nickname";
    public static final String USER_ID_KEY = "userId";
    public static final String PRETEST_COMPLETED_KEY = "preTestCompleted";

    // 뷰 변수 선언
    private EditText nicknameEditText;
    private Button checkNicknameButton;
    private Button logoutButton;
    private TextView welcomeText;
    private Group loginView;
    private Group profileView;

    // 객체 변수 선언
    private SharedPreferences prefs;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 액티비티 생성 및 초기 설정
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 초기 객체 및 뷰 참조 설정
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userRepository = new UserRepository();

        // XML ID 매칭
        nicknameEditText = findViewById(R.id.nickname_edit_text);
        checkNicknameButton = findViewById(R.id.check_nickname_button);
        logoutButton = findViewById(R.id.logout_button);
        welcomeText = findViewById(R.id.welcome_text);
        loginView = findViewById(R.id.login_view);
        profileView = findViewById(R.id.profile_view);

        // 로그인 상태 확인 및 적절한 화면으로 전환
        checkLoginStatus();

        // 닉네임 입력 후 버튼 클릭 이벤트 설정
        checkNicknameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nickname = nicknameEditText.getText().toString().trim();
                if (!nickname.isEmpty()) {
                    loginOrCreateUser(nickname);
                }
            }
        });

        // 로그아웃 버튼 클릭 이벤트를 설정
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 로그아웃 기능만 수행
                logout();
            }
        });
    }

    // SharedPreferences를 확인하여 로그인 상태를 판단하고 테스트 완료 여부를 확인
    private void checkLoginStatus() {
        String nickname = prefs.getString(NICKNAME_KEY, null);
        String userId = prefs.getString(USER_ID_KEY, null);

        if (nickname != null && userId != null) {
            // 💡 로그인된 상태라면, 테스트 완료 여부 확인 후 적절한 화면으로 이동
            checkTestStatusAndNavigate(userId);
        } else {
            showLoginView();
        }
    }


    // 닉네임 존재 여부를 체크하고, 신규 유저는 가입 후 Welcome 화면으로, 기존 유저는 테스트 상태 확인 후 분기
    private void loginOrCreateUser(String nickname) {
        userRepository.checkNicknameExists(nickname, new UserRepository.UserRepositoryCallback() {
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    // 닉네임이 이미 존재하는 경우 (로그인)
                    userRepository.getUserIdByNickname(nickname, new UserRepository.UserIdCallback() {
                        @Override
                        public void onUserIdFound(String userId) {
                            if (userId != null) {
                                saveNicknameAndLogin(nickname, userId);
                                Toast.makeText(MainActivity.this, nickname + "님, 환영합니다!", Toast.LENGTH_SHORT).show();
                                // 테스트 상태 확인 후 분기
                                checkTestStatusAndNavigate(userId);
                            } else {
                                Toast.makeText(MainActivity.this, "사용자 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else {
                    // 새로운 닉네임인 경우 (회원가입)
                    userRepository.createUser(nickname, new UserRepository.UserIdCallback() {
                        @Override
                        public void onUserIdFound(String userId) {
                            if (userId != null) {
                                saveNicknameAndLogin(nickname, userId);
                                Toast.makeText(MainActivity.this, "환영합니다! " + nickname + "님!", Toast.LENGTH_SHORT).show();
                                // 신규 유저는 환영 화면 (WelcomeActivity)으로 이동
                                startWelcomeActivity(nickname);
                            } else {
                                Log.e(TAG, "Error creating new user.");
                                Toast.makeText(MainActivity.this, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "Error checking nickname: ", e);
                Toast.makeText(MainActivity.this, "Error checking nickname.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // SharedPreferences에 닉네임과 유저 ID를 저장
    private void saveNicknameAndLogin(String nickname, String userId) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(NICKNAME_KEY, nickname);
        editor.putString(USER_ID_KEY, userId);
        editor.apply();
    }

    // 유저의 테스트 완료 상태를 확인하고 적절한 화면으로 이동
    private void checkTestStatusAndNavigate(String userId) {
        // 로컬 SharedPreferences에서 완료 상태를 확인
        boolean isCompletedLocally = prefs.getBoolean(PRETEST_COMPLETED_KEY, false);

        if (isCompletedLocally) {
            // 로컬에 완료 기록이 있으면 MainTestActivity로 이동
            startMainTestActivity();
            return;
        }

        // 로컬에 완료 기록이 없으면 Firestore에서 최종 확인
        userRepository.isPreTestCompleted(userId, new UserRepository.PreTestStatusCallback() {
            @Override
            public void onStatusChecked(boolean isCompleted) {
                if (isCompleted) {
                    // Firestore에 완료 기록이 있으면 로컬 저장 후 MainTestActivity로 이동
                    prefs.edit().putBoolean(PRETEST_COMPLETED_KEY, true).apply();
                    startMainTestActivity();
                } else {
                    // Firestore에 완료 기록이 없으면 PreTestActivity로 이동 (필수 테스트)
                    startPreTestActivity();
                }
            }
        });
    }

    // SharedPreferences 정보를 삭제하고 로그인 화면으로 전환
    private void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        // 모든 사용자 정보 삭제
        editor.remove(NICKNAME_KEY);
        editor.remove(USER_ID_KEY);
        editor.remove(PRETEST_COMPLETED_KEY);
        editor.clear(); // 안전하게 모든 prefs를 지움
        editor.apply();

        showLoginView();
        Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
    }

    // 로그인 뷰를 보이게 설정 (화면 재구성)
    private void showLoginView() {
        profileView.setVisibility(View.GONE);
        loginView.setVisibility(View.VISIBLE);
        nicknameEditText.setText("");
        nicknameEditText.requestFocus();
    }

    // WelcomeActivity (환영 화면)로 이동
    private void startWelcomeActivity(String nickname) {
        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
        intent.putExtra(WelcomeActivity.EXTRA_NICKNAME, nickname);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // PreTestActivity (사전 테스트 화면)로 이동
    private void startPreTestActivity() {
        Intent intent = new Intent(MainActivity.this, PreTestActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // MainTestActivity (테스트 완료 후 메인 화면)로 이동
    private void startMainTestActivity() {
        Intent intent = new Intent(MainActivity.this, MainTestActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}