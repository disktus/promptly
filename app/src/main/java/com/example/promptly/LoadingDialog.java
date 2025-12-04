package com.example.promptly;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;

import androidx.annotation.NonNull;

// 채점 중임을 표시하는 커스텀 로딩 다이얼로그
public class LoadingDialog extends Dialog {

    // 생성자에서 컨텍스트를 받아 다이얼로그 초기화
    public LoadingDialog(@NonNull Context context) {
        super(context);
        // 다이얼로그 제목 표시줄 제거
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 다이얼로그에 네가 만든 XML 레이아웃 설정
        setContentView(R.layout.loading_dialog);

        // 배경을 투명하게 설정 (다이얼로그 외부를 클릭해도 닫히지 않도록)
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setCanceledOnTouchOutside(false);
        }
    }
}