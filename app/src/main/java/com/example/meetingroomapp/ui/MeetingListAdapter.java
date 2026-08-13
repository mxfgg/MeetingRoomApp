package com.example.meetingroomapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meetingroomapp.R;
import com.example.meetingroomapp.data.model.MeetingInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 未来会议列表适配器
 */
public class MeetingListAdapter extends RecyclerView.Adapter<MeetingListAdapter.VH> {
    private List<MeetingInfo> items = new ArrayList<>();

    public void setMeetings(List<MeetingInfo> all, MeetingInfo current) {
        items = new ArrayList<>();
        if (all != null) for (MeetingInfo m : all) if (m != current && m.isFutureMeeting()) items.add(m);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v) { return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_future_meeting, p, false)); }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) { h.bind(items.get(pos), pos); }
    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime, tvSummary, tvApplicant;
        VH(View v) { super(v); tvTime = v.findViewById(R.id.tv_future_time); tvSummary = v.findViewById(R.id.tv_future_summary); tvApplicant = v.findViewById(R.id.tv_future_applicant); }
        void bind(MeetingInfo m, int pos) {
            tvTime.setText(m.getDisplayTime()); tvSummary.setText(m.getSummary()); tvApplicant.setText(m.getOwnerName());
            boolean first = pos == 0;
            tvTime.setTextSize(first ? 22 : 16); tvSummary.setTextSize(first ? 22 : 16); tvApplicant.setTextSize(first ? 18 : 14);
            itemView.setBackgroundColor(itemView.getContext().getColor(pos % 2 == 0 ? R.color.bg_card_alt : R.color.bg_card_alt2));
        }
    }
}
