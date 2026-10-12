package uen.edu.vn.quoc.dinhquoc;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Đổi thành layout admin nếu có layout riêng
    }

    /**
     * Khóa tài khoản / Device ID vi phạm
     * @param targetUserId ID người dùng bị khóa
     * @param deviceId Mã thiết bị bị chặn
     * @param reason Lý do khóa
     */
    public void banUser(String targetUserId, String deviceId, String reason) {
        db.collection("users").document(targetUserId)
                .update("status", "Banned")
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> bannedData = new HashMap<>();
                    bannedData.put("device_id", deviceId);
                    bannedData.put("reason", reason);
                    bannedData.put("banned_at", Timestamp.now());

                    db.collection("banned_devices").add(bannedData)
                            .addOnSuccessListener(doc ->
                                    Toast.makeText(AdminActivity.this, "Đã khóa thiết bị vi phạm!", Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(AdminActivity.this, "Lỗi khi khóa tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}