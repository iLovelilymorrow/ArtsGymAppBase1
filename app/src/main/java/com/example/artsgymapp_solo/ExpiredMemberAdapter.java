package com.example.artsgymapp_solo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpiredMemberAdapter extends RecyclerView.Adapter<ExpiredMemberAdapter.ViewHolder> {

    private List<MemberDisplayInfo> expiredMemberList;
    private List<MemberDisplayInfo> expiredMemberListFull; 
    private final OnExpiredMemberClickListener listener;
    private final Context context;
    
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);


    public interface OnExpiredMemberClickListener {
        void onExpiredMemberClick(MemberDisplayInfo memberDisplayInfo);
    }

    public ExpiredMemberAdapter(Context context, List<MemberDisplayInfo> expiredMemberList, OnExpiredMemberClickListener listener) {
        this.context = context;
        this.expiredMemberList = new ArrayList<>(expiredMemberList);
        this.expiredMemberListFull = new ArrayList<>(expiredMemberList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_display, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MemberDisplayInfo member = expiredMemberList.get(position);
        holder.bind(member, listener, context);
    }

    @Override
    public int getItemCount() {
        return expiredMemberList.size();
    }

    public void updateList(List<MemberDisplayInfo> newList) {
        expiredMemberList.clear();
        expiredMemberList.addAll(newList);
        expiredMemberListFull.clear();
        expiredMemberListFull.addAll(newList);
        notifyDataSetChanged();
    }

    public void filter(String text) {
        expiredMemberList.clear();
        if (text.isEmpty()) {
            expiredMemberList.addAll(expiredMemberListFull);
        } else {
            text = text.toLowerCase(Locale.getDefault());
            for (MemberDisplayInfo item : expiredMemberListFull) {
                if ((item.getFullName() != null && item.getFullName().toLowerCase(Locale.getDefault()).contains(text)) ||
                        (item.getMemberID() != null && item.getMemberID().toLowerCase(Locale.getDefault()).contains(text)) ||
                        (item.getReceiptNumber() != null && item.getReceiptNumber().toLowerCase(Locale.getDefault()).contains(text))) {
                    expiredMemberList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewMemberPhoto;
        TextView textViewName;
        TextView textViewMemberId;
        TextView textViewLastMembershipType; 
        TextView textViewExpireDate;        
        TextView textViewLastReceipt;       
        TextView textViewPhoneNumber;

        ViewHolder(View itemView) {
            super(itemView);
            imageViewMemberPhoto = itemView.findViewById(R.id.imageViewMemberListItem);
            textViewName = itemView.findViewById(R.id.textViewNameListItem);
            textViewMemberId = itemView.findViewById(R.id.textViewMemberIdListItem);
            textViewLastMembershipType = itemView.findViewById(R.id.textViewLastMembershipListItem); 
            textViewExpireDate = itemView.findViewById(R.id.textViewExpireDateListItem);
            textViewLastReceipt = itemView.findViewById(R.id.textViewLastReceiptListItem);
            textViewPhoneNumber = itemView.findViewById(R.id.textViewPhoneNumberListItem);
        }

        void bind(final MemberDisplayInfo member, final OnExpiredMemberClickListener listener, Context context) {
            textViewName.setText(member.getFullName());
            textViewMemberId.setText("ID: " + (member.getMemberID() != null ? member.getMemberID() : "N/A"));
            textViewPhoneNumber.setText("Phone: " + (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty() ? member.getPhoneNumber() : "N/A"));

            textViewLastMembershipType.setText("Last Type: " + (member.getMemberTypeName() != null ? member.getMemberTypeName() : "N/A"));

            if (member.getExpirationDate() != null) {
                textViewExpireDate.setText("Expired On: " + member.getExpirationDate().format(DISPLAY_DATE_FORMATTER));
            } else {
                textViewExpireDate.setText("Expired On: N/A");
            }

            textViewLastReceipt.setText("Last Receipt: " + (member.getReceiptNumber() != null && !member.getReceiptNumber().isEmpty() ? member.getReceiptNumber() : "N/A"));

            if (member.getImageFilePath() != null && !member.getImageFilePath().isEmpty()) {
                Glide.with(context)
                        .load(new File(member.getImageFilePath()))
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(imageViewMemberPhoto);
            } else {
                Glide.with(context)
                        .load(R.drawable.ic_launcher_background)
                        .into(imageViewMemberPhoto);
            }

            itemView.setOnClickListener(v -> listener.onExpiredMemberClick(member));
        }
    }
}