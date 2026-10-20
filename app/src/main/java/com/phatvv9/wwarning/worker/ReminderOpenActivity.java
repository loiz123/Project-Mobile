package com.phatvv9.wwarning.worker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.firestore.FirebaseFirestore;
import com.phatvv9.wwarning.MainActivity;
import com.phatvv9.wwarning.util.FirestoreNames;

/** Trung gian khi bấm thông báo nhắc: ghi notifications.isInteracted = true rồi mở màn chính. */
public class ReminderOpenActivity extends Activity {
    public static final String EXTRA_NOTIFICATION_DOC_ID = "notificationDocId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String docId = getIntent().getStringExtra(EXTRA_NOTIFICATION_DOC_ID);
        if (docId != null && !docId.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection(FirestoreNames.NOTIFICATIONS)
                    .document(docId)
                    .update("isInteracted", true);
        }

        Intent main = new Intent(this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(main);
        finish();
    }
}