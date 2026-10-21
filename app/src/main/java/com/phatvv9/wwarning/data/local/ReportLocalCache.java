package com.phatvv9.wwarning.data.local;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.phatvv9.wwarning.model.Report;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ReportLocalCache {

    private final ReportDao reportDao;
    private final Executor executor;

    public ReportLocalCache(Context context) {
        this.reportDao = AppDatabase.getInstance(context).reportDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void saveReportsToRoom(final List<Report> reports) {
        if (reports == null || reports.isEmpty()) return;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<ReportEntity> entities = ReportMapper.toEntityList(reports);
                reportDao.insertAll(entities);
            }
        });
    }

    public LiveData<List<Report>> getCachedReports() {
        final MediatorLiveData<List<Report>> result = new MediatorLiveData<>();
        final LiveData<List<ReportEntity>> liveEntities = reportDao.getAllReports();

        result.addSource(liveEntities, new Observer<List<ReportEntity>>() {
            @Override
            public void onChanged(List<ReportEntity> entities) {
                result.setValue(ReportMapper.toDomainList(entities));
            }
        });

        return result;
    }

    public void cleanExpiredReports(final long currentTimeMs) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                reportDao.deleteOlderThan(currentTimeMs);
            }
        });
    }
}
