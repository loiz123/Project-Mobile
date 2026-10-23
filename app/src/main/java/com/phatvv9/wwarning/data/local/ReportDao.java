package com.phatvv9.wwarning.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ReportEntity> reports);

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    LiveData<List<ReportEntity>> getAllReports();

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    List<ReportEntity> getAllReportsSync();

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    ReportEntity getReportById(String id);

    @Query("UPDATE reports SET status = :status WHERE id = :id")
    void updateReportStatus(String id, String status);

    @Query("DELETE FROM reports WHERE expiresAt < :currentTime")
    void deleteOlderThan(long currentTime);
}
