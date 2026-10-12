package uen.edu.vn.quoc.dinhquoc;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private VoteRepository voteRepository;
    private static final String TEST_REPORT_ID = "ExIQbCRK1QJOCMYKHk4J";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        voteRepository = new VoteRepository();

        Button btnUpvote = findViewById(R.id.btnUpvote);
        Button btnDownvote = findViewById(R.id.btnDownvote);
        Button btnScheduleReminder = findViewById(R.id.btnScheduleReminder);
        Button btnOpenAdmin = findViewById(R.id.btnOpenAdmin);

        if (btnUpvote != null) {
            btnUpvote.setOnClickListener(v -> {
                voteRepository.castVote(TEST_REPORT_ID, 1);
                Toast.makeText(this, "Đã gửi Upvote lên Firestore!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnDownvote != null) {
            btnDownvote.setOnClickListener(v -> {
                voteRepository.castVote(TEST_REPORT_ID, -1);
                Toast.makeText(this, "Đã gửi Downvote lên Firestore!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnScheduleReminder != null) {
            btnScheduleReminder.setOnClickListener(v -> {
                WeatherReminderWorker.schedule15MinReminder(this);
                Toast.makeText(this, "Đã lên lịch nhắc nhở sau 15 phút!", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnOpenAdmin != null) {
            btnOpenAdmin.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                startActivity(intent);
            });
        }
    }
}