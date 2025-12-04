package com.example.promptly;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    // Extra 키 정의
    public static final String EXTRA_NICKNAME = "extra_nickname";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 액티비티 생성 및 초기 설정
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 뷰 참조 설정
        TextView welcomeTextView = findViewById(R.id.tvWelcomeMessage);
        Button startButton = findViewById(R.id.btnStartTest);

        // 이전 액티비티에서 전달받은 닉네임 정보를 화면에 표시
        String nickname = getIntent().getStringExtra(EXTRA_NICKNAME);
        if (nickname != null) {
            String message = "처음이시군요, " + nickname + "님!\n프롬프트 테스트를 시작해보세요.";
            welcomeTextView.setText(message);
        } else {
            // 닉네임이 없을 경우를 대비한 기본 메시지
            welcomeTextView.setText("환영합니다!\n프롬프트 테스트를 시작해보세요.");
        }

        // 테스트 시작 버튼 클릭 이벤트 설정
        startButton.setOnClickListener(v -> {
            startPreTestActivity();
        });
    }

    // PreTestActivity (사전 테스트 화면)로 이동
    private void startPreTestActivity() {
        Intent intent = new Intent(WelcomeActivity.this, PreTestActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}