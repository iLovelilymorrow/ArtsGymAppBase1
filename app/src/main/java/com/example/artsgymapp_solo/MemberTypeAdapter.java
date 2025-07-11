package com.example.artsgymapp_solo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class MemberTypeAdapter extends RecyclerView.Adapter<MemberTypeAdapter.MemberTypeViewHolder> {

    private List<MemberType> memberTypeList;
    private Context context;
    private OnMemberTypeDeleteListener deleteListener;

    public interface OnMemberTypeDeleteListener {
        void onDeleteClicked(MemberType memberType);
    }

    public MemberTypeAdapter(Context context, List<MemberType> memberTypeList, OnMemberTypeDeleteListener listener) {
        this.context = context;
        this.memberTypeList = memberTypeList;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public MemberTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member_type, parent, false);
        return new MemberTypeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberTypeViewHolder holder, int position) {
        MemberType memberType = memberTypeList.get(position);
        holder.textViewName.setText(memberType.getName());
        holder.textViewDuration.setText(String.format(Locale.getDefault(), "Duration: %d days", memberType.getDurationDays()));
        holder.textViewIsTwoInOne.setText(String.format(Locale.getDefault(), "Two In One: %s", memberType.isTwoInOne() ? "Yes" : "No"));

        holder.buttonDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClicked(memberType);
            }
        });
    }

    @Override
    public int getItemCount() {
        return memberTypeList != null ? memberTypeList.size() : 0;
    }

    public void updateData(List<MemberType> newMemberTypeList) {
        this.memberTypeList.clear();
        if (newMemberTypeList != null) {
            this.memberTypeList.addAll(newMemberTypeList);
        }
        notifyDataSetChanged(); // Consider using DiffUtil for better performance with large lists
    }

    static class MemberTypeViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewDuration;
        TextView textViewIsTwoInOne;
        ImageButton buttonDelete;

        public MemberTypeViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewItemMemberTypeName);
            textViewDuration = itemView.findViewById(R.id.textViewItemDuration);
            textViewIsTwoInOne = itemView.findViewById(R.id.textViewItemIsTwoInOne);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteItemMemberType);
        }
    }
}