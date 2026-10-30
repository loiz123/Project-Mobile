package com.phatvv9.wwarning.ui.admin;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.phatvv9.wwarning.R;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.ui.map.MapFragment;
import com.phatvv9.wwarning.util.ReportVisibility;

import java.util.ArrayList;
import java.util.List;

public class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.ViewHolder> {

    public interface Listener {
        void onReportClick(@NonNull Report report);

        void onReportLongClick(@NonNull Report report);
    }

    private final List<Report> reports = new ArrayList<>();
    private final Listener listener;

    public AdminReportAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setReports(@NonNull List<Report> newReports) {
        reports.clear();
        reports.addAll(newReports);
        notifyDataSetChanged();
    }

    @Nullable
    public Report getReportAt(int position) {
        if (position < 0 || position >= reports.size()) return null;
        return reports.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Report report = reports.get(position);
        Context context = holder.itemView.getContext();

        holder.tvCategory.setText(MapFragment.getCategoryDisplayName(report.getCategoryId()));
        holder.tvVotes.setText("Hữu ích: " + report.getUpCount() + " · Báo sai: " + report.getDownCount());

        Timestamp created = report.getCreatedAt();
        if (created != null) {
            CharSequence relative = DateUtils.getRelativeTimeSpanString(
                    created.toDate().getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.tvTime.setText(relative);
        } else {
            holder.tvTime.setText("Vừa xong");
        }

        long now = System.currentTimeMillis();
        ReportVisibility.State state = ReportVisibility.stateOf(report, now);
        String expiredSuffix = ReportVisibility.hasValidExpiry(report, now) ? "" : " · Hết hạn";
        if (state == ReportVisibility.State.HIDDEN) {
            holder.tvStatus.setText("Đã ẩn" + expiredSuffix);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.admin_status_hidden));
            holder.itemView.setAlpha(0.75f);
        } else if (state == ReportVisibility.State.DELETED) {
            holder.tvStatus.setText("Đã xóa" + expiredSuffix);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.admin_status_deleted));
            holder.itemView.setAlpha(0.75f);
        } else if (state == ReportVisibility.State.EXPIRED) {
            holder.tvStatus.setText("Đã hết hạn");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.admin_status_expired));
            holder.itemView.setAlpha(0.75f);
        } else {
            holder.tvStatus.setText("Còn hiệu lực");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.admin_status_visible));
            holder.itemView.setAlpha(1f);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onReportClick(report);
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                listener.onReportLongClick(report);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCategory;
        final TextView tvStatus;
        final TextView tvVotes;
        final TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvAdminCategory);
            tvStatus = itemView.findViewById(R.id.tvAdminStatus);
            tvVotes = itemView.findViewById(R.id.tvAdminVotes);
            tvTime = itemView.findViewById(R.id.tvAdminTime);
        }
    }
}
