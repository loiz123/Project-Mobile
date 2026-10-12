package uen.edu.vn.quoc.dinhquoc;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class WeatherReminderWorker extends Worker {

    public WeatherReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Tác vụ chạy ngầm định kỳ 15 phút (ví dụ: kiểm tra thời tiết)
        return Result.success();
    }

    /**
     * Phương thức tĩnh để lên lịch chạy đếm ngược 15 phút bằng WorkManager
     * @param context Ngữ cảnh ứng dụng (MainActivity)
     */
    public static void schedule15MinReminder(Context context) {
        PeriodicWorkRequest reminderRequest =
                new PeriodicWorkRequest.Builder(WeatherReminderWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(context).enqueue(reminderRequest);
    }
}