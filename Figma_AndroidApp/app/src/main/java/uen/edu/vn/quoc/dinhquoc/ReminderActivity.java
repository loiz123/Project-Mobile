package uen.edu.vn.quoc.dinhquoc;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class ReminderActivity extends AppCompatActivity {

    public static final String REMINDER_TAG = "WEATHER_15MIN_REMINDER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Kích hoạt đếm ngược 15 phút sau khi người dùng gửi báo cáo
     */
    public static void schedule15MinReminder(Context context) {
        // Hủy các nhắc nhở cũ nếu có
        WorkManager.getInstance(context).cancelAllWorkByTag(REMINDER_TAG);

        // Lên lịch 15 phút
        OneTimeWorkRequest reminderRequest = new OneTimeWorkRequest.Builder(WeatherReminderWorker.class)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .addTag(REMINDER_TAG)
                .build();

        WorkManager.getInstance(context).enqueue(reminderRequest);
        Toast.makeText(context, "Đã hẹn giờ nhắc nhở cập nhật sau 15 phút", Toast.LENGTH_SHORT).show();
    }

    /**
     * Hủy nhắc nhở khi người dùng chọn "Bỏ qua" hoặc "Tôi đã về nhà"
     */
    public static void cancelReminder(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(REMINDER_TAG);
        Toast.makeText(context, "Đã hủy nhắc nhở", Toast.LENGTH_SHORT).show();
    }
}