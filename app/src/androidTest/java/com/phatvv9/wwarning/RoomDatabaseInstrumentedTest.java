package com.phatvv9.wwarning;

import static org.junit.Assert.*;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.phatvv9.wwarning.data.local.AppDatabase;
import com.phatvv9.wwarning.data.local.ReportEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class RoomDatabaseInstrumentedTest {
    private AppDatabase db;
    @Before public void setup() { db=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().getTargetContext(),AppDatabase.class).allowMainThreadQueries().build(); }
    @After public void teardown() { if(db!=null)db.close(); }
    private ReportEntity report(String id,long expiry) { return new ReportEntity(id,"u","cat_ngap",null,10.77,106.7,"abc","active",2,1,1000,expiry); }
    @Test public void insertAndReadOfflineCache() { db.reportDao().insertAll(Arrays.asList(report("r1",5000))); assertEquals("r1",db.reportDao().getReportById("r1").getId()); assertEquals(1,db.reportDao().getAllReportsSync().size()); }
    @Test public void updateReportStatus() { db.reportDao().insertAll(Arrays.asList(report("r1",5000))); db.reportDao().updateReportStatus("r1","hidden"); assertEquals("hidden",db.reportDao().getReportById("r1").getStatus()); }
    @Test public void deleteOnlyExpiredReports() { db.reportDao().insertAll(Arrays.asList(report("expired",1000),report("active",9000))); db.reportDao().deleteOlderThan(5000); assertNull(db.reportDao().getReportById("expired")); assertNotNull(db.reportDao().getReportById("active")); }
    @Test public void replaceSameReportId() { db.reportDao().insertAll(Arrays.asList(report("r1",5000))); db.reportDao().insertAll(Arrays.asList(report("r1",8000))); assertEquals(1,db.reportDao().getAllReportsSync().size()); assertEquals(8000,db.reportDao().getReportById("r1").getExpiresAt()); }
}
