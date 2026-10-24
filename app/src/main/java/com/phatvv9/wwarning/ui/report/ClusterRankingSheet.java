package com.phatvv9.wwarning.ui.report;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.phatvv9.wwarning.model.Report;
import com.phatvv9.wwarning.model.ReportCluster;
import com.phatvv9.wwarning.ui.map.MapFragment;

import java.util.List;

/**
 * Overlay bảng xếp hạng: hiện khi chạm vào marker cụm (từ 2 báo cáo trở lên trong 500m).
 * Điểm mỗi hiện tượng = số báo cáo + số vote đúng. Chạm một dòng để mở chi tiết và bình chọn.
 */
public class ClusterRankingSheet extends BottomSheetDialogFragment {

    // Giữ cụm đang xem (cùng cách làm với ReportDetailSheet)
    private static ReportCluster currentCluster;

    public static ClusterRankingSheet newInstance(@NonNull ReportCluster cluster) {
        currentCluster = cluster;
        return new ClusterRankingSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final Context ctx = requireContext();
        if (currentCluster == null || currentCluster.getReportCount() == 0) {
            dismissAllowingStateLoss();
            return new View(ctx);
        }
        float d = getResources().getDisplayMetrics().density;
        int textPrimary = resolveColor(ctx, android.R.attr.textColorPrimary);
        int textSecondary = resolveColor(ctx, android.R.attr.textColorSecondary);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (20 * d), (int) (16 * d), (int) (20 * d), (int) (20 * d));

        TextView title = new TextView(ctx);
        title.setText("🏆 Bảng xếp hạng khu vực 500m");
        title.setTextSize(19);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(textPrimary);
        root.addView(title);

        TextView subtitle = new TextView(ctx);
        subtitle.setText(currentCluster.getReportCount() + " báo cáo • Điểm = số báo cáo + số vote đúng");
        subtitle.setTextSize(13);
        subtitle.setTextColor(textSecondary);
        LinearLayout.LayoutParams lpSub = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpSub.topMargin = (int) (2 * d);
        lpSub.bottomMargin = (int) (12 * d);
        root.addView(subtitle, lpSub);

        LinearLayout list = new LinearLayout(ctx);
        list.setOrientation(LinearLayout.VERTICAL);
        List<ReportCluster.CategoryStat> stats = currentCluster.getCategoryStats();
        for (int i = 0; i < stats.size(); i++) {
            list.addView(buildRow(ctx, d, i + 1, stats.get(i)));
        }

        ScrollView scroll = new ScrollView(ctx);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(ctx);
        hint.setText("Chạm một dòng để xem chi tiết và bình chọn");
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        hint.setTextColor(textSecondary);
        LinearLayout.LayoutParams lpHint = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpHint.topMargin = (int) (8 * d);
        root.addView(hint, lpHint);
        return root;
    }

    private View buildRow(Context ctx, float d, int rank, final ReportCluster.CategoryStat stat) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (10 * d));

        boolean top = rank == 1;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(top ? 0xFFFFF8E1 : 0xFFF5F6F9);
        bg.setCornerRadius(12 * d);
        bg.setStroke((int) (1 * d), top ? 0xFFFFB300 : 0xFFE0E0E0);
        row.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * d);
        row.setLayoutParams(lp);

        LinearLayout top1 = new LinearLayout(ctx);
        top1.setOrientation(LinearLayout.HORIZONTAL);
        top1.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(ctx);
        name.setText(rankLabel(rank) + "  " + MapFragment.getCategoryIconAndName(stat.getCategoryId()));
        name.setTextSize(16);
        name.setTypeface(null, Typeface.BOLD);
        name.setTextColor(0xFF212121);
        top1.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView score = new TextView(ctx);
        score.setText(stat.getScore() + " điểm");
        score.setTextSize(15);
        score.setTypeface(null, Typeface.BOLD);
        score.setTextColor(top ? 0xFFE65100 : 0xFF616161);
        top1.addView(score);
        row.addView(top1);

        TextView detail = new TextView(ctx);
        detail.setText("📝 " + stat.getReportCount() + " báo cáo   ·   👍 " + stat.getTotalUpCount()
                + " vote đúng   ·   👎 " + stat.getTotalDownCount() + " báo sai");
        detail.setTextSize(13);
        detail.setTextColor(0xFF616161);
        LinearLayout.LayoutParams lpD = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpD.topMargin = (int) (4 * d);
        row.addView(detail, lpD);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Report target = stat.getTopReport();
                if (target == null) return;
                // Mở chi tiết (xem + vote) của báo cáo được xác nhận nhiều nhất trong hiện tượng này
                ReportDetailSheet.newInstance(target)
                        .show(getParentFragmentManager(), "ReportDetailSheet");
                dismiss();
            }
        });
        return row;
    }

    private static String rankLabel(int rank) {
        switch (rank) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            default: return rank + ".";
        }
    }

    private static int resolveColor(Context ctx, int attr) {
        TypedValue tv = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.resourceId != 0) return ContextCompat.getColor(ctx, tv.resourceId);
            return tv.data;
        }
        return 0xFF212121;
    }
}
