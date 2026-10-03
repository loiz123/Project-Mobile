package uen.edu.vn.quoc.dinhquoc;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ReminderActivity extends AppCompatActivity {

    private RelativeLayout layoutLockScreen, layoutInApp;
    private Button btnLockscreenTab, btnInAppTab;
    private Button btnOption1, btnOption2, btnOption3, btnUpdateInApp;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        layoutLockScreen = findViewById(R.id.layoutLockScreen);
        layoutInApp = findViewById(R.id.layoutInApp);
        btnLockscreenTab = findViewById(R.id.btnLockscreenTab);
        btnInAppTab = findViewById(R.id.btnInAppTab);

        btnOption1 = findViewById(R.id.btnOption1);
        btnOption2 = findViewById(R.id.btnOption2);
        btnOption3 = findViewById(R.id.btnOption3);
        btnUpdateInApp = findViewById(R.id.btnUpdateInApp);

        btnLockscreenTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(true);
            }
        });

        btnInAppTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(false);
                startInAppCountDown();
            }
        });

        View.OnClickListener quickResponseListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button clickedButton = (Button) v;
                Toast.makeText(ReminderActivity.this,
                        "Cảm ơn! Đã ghi nhận: " + clickedButton.getText() + " (+10 Điểm uy tín)",
                        Toast.LENGTH_SHORT).show();
            }
        };

        btnOption1.setOnClickListener(quickResponseListener);
        btnOption2.setOnClickListener(quickResponseListener);
        btnOption3.setOnClickListener(quickResponseListener);

        btnUpdateInApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ReminderActivity.this,
                        "Đã cập nhật trạng thái thời tiết thành công!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchTab(boolean isLockscreen) {
        if (isLockscreen) {
            layoutLockScreen.setVisibility(View.VISIBLE);
            layoutInApp.setVisibility(View.GONE);
            btnLockscreenTab.setBackgroundColor(Color.parseColor("#2563EB"));
            btnLockscreenTab.setTextColor(Color.WHITE);
            btnInAppTab.setBackgroundColor(Color.TRANSPARENT);
            btnInAppTab.setTextColor(Color.parseColor("#94A3B8"));
        } else {
            layoutLockScreen.setVisibility(View.GONE);
            layoutInApp.setVisibility(View.VISIBLE);
            btnInAppTab.setBackgroundColor(Color.parseColor("#2563EB"));
            btnInAppTab.setTextColor(Color.WHITE);
            btnLockscreenTab.setBackgroundColor(Color.TRANSPARENT);
            btnLockscreenTab.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    private void startInAppCountDown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                btnUpdateInApp.setText("⚡ Cập nhật (" + secondsLeft + "s)");
            }

            @Override
            public void onFinish() {
                btnUpdateInApp.setText("⚡ Hết thời gian cập nhật");
            }
        }.start();
    }
}