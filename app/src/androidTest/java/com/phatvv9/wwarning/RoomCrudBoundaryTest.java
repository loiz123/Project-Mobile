package com.phatvv9.wwarning;

import static org.junit.Assert.*;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.phatvv9.wwarning.data.local.AppDatabase;
import com.phatvv9.wwarning.data.local.CategoryEntity;
import com.phatvv9.wwarning.data.local.ReportEntity;
import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RoomCrudBoundaryTest {
    private AppDatabase db;
    @Before public void open() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation()
                .getTargetContext(), AppDatabase.class).allowMainThreadQueries().build();
    }
    @After public void close() { if (db != null) db.close(); }
    private ReportEntity report(String id, long created, long expiry) {
        return new ReportEntity(id, "user", "cat_mua", null, 10, 106,
                "hash", "Active", 0, 0, created, expiry);
    }
    @Test public void reportCrud_preservesExpiryBoundaryAndSortOrder() {
        db.reportDao().insertAll(Arrays.asList(report("old", 1, 999),
                report("boundary", 2, 1000), report("new", 3, 1001)));
        assertEquals("new", db.reportDao().getAllReportsSync().get(0).getId());
        db.reportDao().updateReportStatus("boundary", "Hidden");
        assertEquals("Hidden", db.reportDao().getReportById("boundary").getStatus());
        db.reportDao().deleteOlderThan(1000);
        assertNull(db.reportDao().getReportById("old"));
        assertNotNull(db.reportDao().getReportById("boundary"));
        assertEquals(2, db.reportDao().getAllReportsSync().size());
        db.reportDao().deleteOlderThan(1002);
        assertTrue(db.reportDao().getAllReportsSync().isEmpty());
    }
    @Test public void missingReportUpdateAndEmptyInsert_areNoOps() {
        db.reportDao().insertAll(Collections.emptyList());
        db.reportDao().updateReportStatus("missing", "Hidden");
        assertNull(db.reportDao().getReportById("missing"));
        assertTrue(db.reportDao().getAllReportsSync().isEmpty());
    }
    @Test public void categories_createReadReplaceDelete() {
        db.categoryDao().insertAll(Arrays.asList(
                new CategoryEntity("rain", "Rain", null, false, true),
                new CategoryEntity("storm", "Storm", "icon", true, true)));
        assertEquals(2, db.categoryDao().getAllCategoriesSync().size());
        db.categoryDao().insertAll(Collections.singletonList(
                new CategoryEntity("rain", "Updated", "new-icon", true, false)));
        assertEquals(2, db.categoryDao().getAllCategoriesSync().size());
        CategoryEntity updated = null;
        for (CategoryEntity item : db.categoryDao().getAllCategoriesSync()) {
            if ("rain".equals(item.getId())) updated = item;
        }
        assertNotNull(updated);
        assertEquals("Updated", updated.getName());
        assertEquals("new-icon", updated.getIconUrl());
        assertTrue(updated.isRisk());
        assertFalse(updated.isActive());
        db.categoryDao().deleteAll();
        assertTrue(db.categoryDao().getAllCategoriesSync().isEmpty());
    }
}
