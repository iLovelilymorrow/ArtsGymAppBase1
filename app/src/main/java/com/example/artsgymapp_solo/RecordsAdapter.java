package com.example.artsgymapp_solo;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.ViewHolder>
{
    private List<MemberDisplayInfo> recordsList;
    private List<MemberDisplayInfo> recordsListFull;
    private final Context context;

    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);

    public RecordsAdapter(Context context, List<MemberDisplayInfo> recordsList) {
        this.context = context;
        this.recordsList = new ArrayList<>(recordsList);
        this.recordsListFull = new ArrayList<>(recordsList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MemberDisplayInfo member = recordsList.get(position);
        holder.bind(member, context);
    }

    @Override
    public int getItemCount() {
        return recordsList.size();
    }

    public void updateList(List<MemberDisplayInfo> newList) {
        recordsList.clear();
        recordsList.addAll(newList);
        recordsListFull.clear();
        recordsListFull.addAll(newList);
        notifyDataSetChanged();
    }

    // In RecordsAdapter.java

    public void filter(String text) {
        recordsList.clear();
        if (text.isEmpty()) {
            recordsList.addAll(recordsListFull);
        } else {
            String searchText = text.toLowerCase(Locale.getDefault());
            String searchStatusKeyword = null;

            // Check if the search text is a status keyword
            if (searchText.equals("expired")) {
                searchStatusKeyword = "expired";
            } else if (searchText.equals("expiring") || searchText.equals("expiring soon")) {
                // Allow "expiring" or "expiring soon"
                searchStatusKeyword = "expiring_soon";
            } else if (searchText.equals("active")) {
                searchStatusKeyword = "active";
            }

            for (MemberDisplayInfo item : recordsListFull) {
                boolean matches = false;
                if (searchStatusKeyword != null) {
                    // If a status keyword was typed, filter by status
                    MemberDisplayInfo.MembershipStatus currentStatus = item.getCurrentMembershipStatus();
                    switch (searchStatusKeyword) {
                        case "expired":
                            if (currentStatus == MemberDisplayInfo.MembershipStatus.EXPIRED) {
                                matches = true;
                            }
                            break;
                        case "expiring_soon":
                            if (currentStatus == MemberDisplayInfo.MembershipStatus.EXPIRING_SOON) {
                                matches = true;
                            }
                            break;
                        case "active":
                            if (currentStatus == MemberDisplayInfo.MembershipStatus.ACTIVE) {
                                matches = true;
                            }
                            break;
                    }
                } else {
                    // Original text-based search for name, ID, receipt
                    if ((item.getFullName() != null && item.getFullName().toLowerCase(Locale.getDefault()).contains(searchText)) ||
                            (item.getMemberID() != null && item.getMemberID().toLowerCase(Locale.getDefault()).contains(searchText)) ||
                            (item.getReceiptNumber() != null && item.getReceiptNumber().toLowerCase(Locale.getDefault()).contains(searchText))) {
                        matches = true;
                    }
                }

                if (matches) {
                    recordsList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }


    public List<MemberDisplayInfo> getCurrentFilteredList() {
        return new ArrayList<>(recordsList);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewMemberPhoto;
        TextView textViewName;
        TextView textViewMemberId;
        TextView textViewLastMembershipType;
        TextView textViewStartDate;
        TextView textViewExpireDate;
        TextView textViewLastReceipt;
        TextView textViewPhoneNumber;
        TextView statusTextView;

        ViewHolder(View itemView) {
            super(itemView);
            imageViewMemberPhoto = itemView.findViewById(R.id.imageViewMemberListItem);
            textViewName = itemView.findViewById(R.id.textViewNameListItem);
            textViewMemberId = itemView.findViewById(R.id.textViewMemberIdListItem);
            textViewLastMembershipType = itemView.findViewById(R.id.textViewLastMembershipListItem);
            textViewStartDate = itemView.findViewById(R.id.textViewStartDateListItem); // Initialize
            textViewExpireDate = itemView.findViewById(R.id.textViewExpireDateListItem);
            textViewLastReceipt = itemView.findViewById(R.id.textViewLastReceiptListItem);
            textViewPhoneNumber = itemView.findViewById(R.id.textViewPhoneNumberListItem);
            statusTextView = itemView.findViewById(R.id.statusTextView);
        }

        void bind(final MemberDisplayInfo member, Context context)
        {
            textViewName.setText(member.getFullName());
            textViewMemberId.setText("ID: " + (member.getMemberID() != null ? member.getMemberID() : "N/A"));
            textViewPhoneNumber.setText("Phone: " + (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty() ? member.getPhoneNumber() : "N/A"));

            textViewLastMembershipType.setText("Last Type: " + (member.getMemberTypeName() != null ? member.getMemberTypeName() : "N/A"));

            if (member.getRegistrationDate() != null) {
                textViewStartDate.setText("Start Date: " + member.getRegistrationDate().format(DISPLAY_DATE_FORMATTER));
            } else {
                textViewStartDate.setText("Start Date: N/A");
            }

            if (member.getExpirationDate() != null) {
                textViewExpireDate.setText("Expire Date: " + member.getExpirationDate().format(DISPLAY_DATE_FORMATTER));
            } else {
                textViewExpireDate.setText("Expire Date: N/A");
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

            MemberDisplayInfo.MembershipStatus status = member.getCurrentMembershipStatus();
            switch (status)
            {
                case EXPIRING_SOON:
                    statusTextView.setText(context.getString(R.string.Expiring));
                    statusTextView.setBackgroundColor(ContextCompat.getColor(context, R.color.orange));
                    break;
                case EXPIRED:
                    statusTextView.setText(context.getString(R.string.Expired));
                    statusTextView.setBackgroundColor(ContextCompat.getColor(context, R.color.red));
                    break;
                case ACTIVE:
                default:
                    statusTextView.setText(context.getString(R.string.Active));
                    statusTextView.setBackgroundColor(ContextCompat.getColor(context, R.color.green));
                    break;
            }
            statusTextView.setVisibility(View.VISIBLE);
        }
    }
}
