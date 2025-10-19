package com.example.test;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;

public class MainActivity extends AppCompatActivity {

    private Button btnStart;

    // 예시 데이터 (중복/금칙어)
    private final Set<String> duplicated = new HashSet<>(Arrays.asList("홍길동", "Alice", "User01"));
    private final Set<String> banned = new HashSet<>(Arrays.asList("바보", "금칙어", "badword"));

    // 한글/영어/숫자, 최대 12자
    private final Pattern allow = Pattern.compile("^[A-Za-z0-9가-힣]{1,12}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v ->
                Toast.makeText(this, "START!", Toast.LENGTH_SHORT).show()
        );

        // 앱 처음 열면 닉네임 입력
        showNicknameDialog();
    }

    private void showNicknameDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_nickname, null, false);
        EditText et = view.findViewById(R.id.etNickname);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();


        // ★ 팝업을 가운데, 흰색 모서리 제거
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dlg.getWindow().setGravity(Gravity.CENTER);
        }

        view.findViewById(R.id.btnOk).setOnClickListener(v -> {
            String name = et.getText() == null ? "" : et.getText().toString().trim();

            // 1) 형식
            if (!allow.matcher(name).matches()) {
                showMessage("한글/영어/숫자만 가능해요\n다시 입력해주세요", () -> et.requestFocus());
                return;
            }
            // 2) 금칙어
            if (containsBanned(name)) {
                showMessage("금칙어가 포함되어있습니다\n다시 입력해주세요", () -> et.requestFocus());
                return;
            }
            // 3) 중복
            if (duplicated.contains(name)) {
                showMessage("이미 존재하는 이름입니다\n다시 입력해주세요", () -> et.requestFocus());
                return;
            }

            // 통과
            dlg.dismiss();
            btnStart.setVisibility(View.VISIBLE);
        });

        dlg.show();
    }

    private boolean containsBanned(String name) {
        for (String w : banned) {
            if (name.contains(w)) return true;
        }
        return false;
    }

    private void showMessage(String msg, Runnable onOk) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_message, null, false);
        TextView tv = view.findViewById(R.id.tvMessage);
        tv.setText(msg);

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();


        // ★ 팝업을 가운데, 흰색 모서리 제거
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dlg.getWindow().setGravity(Gravity.CENTER);
        }

        view.findViewById(R.id.btnMsgOk).setOnClickListener(v -> {
            dlg.dismiss();
            if (onOk != null) onOk.run();
        });

        dlg.show();
    }
}
