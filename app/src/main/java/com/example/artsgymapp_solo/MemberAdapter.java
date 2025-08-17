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
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<MemberDisplayInfo> memberDisplayInfoList;
    private Context context;
    private OnItemClickListener listener;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    public interface OnItemClickListener {
        void onItemClick(MemberDisplayInfo memberDisplayInfo);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    
    public MemberAdapter(List<MemberDisplayInfo> memberDisplayInfoList, Context context) {
        this.memberDisplayInfoList = memberDisplayInfoList;
        this.context = context;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false); 
        return new MemberViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        
        MemberDisplayInfo currentMemberInfo = memberDisplayInfoList.get(position);

        String firstName = currentMemberInfo.getFirstName() != null ? currentMemberInfo.getFirstName() : "";
        String lastName = currentMemberInfo.getLastName() != null ? currentMemberInfo.getLastName() : "";
        String formattedFullName = String.format("%s %s", firstName, lastName).trim();

        if (formattedFullName.isEmpty() || formattedFullName.equals("Member Name:")) { 
            holder.memberNameTextView.setText(this.context.getString(R.string.memberList_NameEmpty));
        } else {
            holder.memberNameTextView.setText(formattedFullName);
        }

        
        String memberTypeName = currentMemberInfo.getMemberTypeName();
        holder.memberTypeTextView.setText(String.format("Membership Type: %s", memberTypeName != null ? memberTypeName : "N/A"));

        String memberGender = currentMemberInfo.getGender();
        holder.memberGenderTextView.setText(String.format("Gender: %s", memberGender != null ? memberGender : "N/A"));

        holder.memberAgeTextView.setText(String.format("Age: %d", currentMemberInfo.getAge()));

        String memberPhoneNumber = currentMemberInfo.getPhoneNumber();
        holder.memberPhoneNumberTextView.setText(String.format("Phone Number: %s", !memberPhoneNumber.equals("") ? memberPhoneNumber : "N/A"));

        String memberId = currentMemberInfo.getMemberID(); 
        holder.memberIdTextView.setText(String.format("Member ID: %s", memberId != null ? memberId : "N/A"));

        String receiptNumber = currentMemberInfo.getReceiptNumber();
        holder.receiptNumberTextView.setText(String.format("Receipt No.: %s", receiptNumber != null ? receiptNumber : "N/A"));

        
        if (currentMemberInfo.getRegistrationDate() != null) {
            holder.startDateTextView.setText(String.format("Start Date: %s", currentMemberInfo.getRegistrationDate().format(dateFormatter)));
        } else {
            holder.startDateTextView.setText("Start Date: N/A");
        }

        if (currentMemberInfo.getExpirationDate() != null) {
            holder.endDateTextView.setText(String.format("End Date: %s", currentMemberInfo.getExpirationDate().format(dateFormatter)));
        } else {
            holder.endDateTextView.setText("End Date: N/A");
        }

        
        if (currentMemberInfo.getImageFilePath() != null && !currentMemberInfo.getImageFilePath().isEmpty()) {
            File imgFile = new File(currentMemberInfo.getImageFilePath());
            if (imgFile.exists()) {
                Glide.with(this.context)
                        .load(imgFile)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(holder.memberImageView);
            } else {
                holder.memberImageView.setImageResource(R.mipmap.ic_launcher_round); 
            }
        } else {
            holder.memberImageView.setImageResource(R.mipmap.ic_launcher_round); 
        }
    }

    @Override
    public int getItemCount() {
        
        return memberDisplayInfoList == null ? 0 : memberDisplayInfoList.size();
    }

    
    
    public void setMemberDisplayInfoList(List<MemberDisplayInfo> newMemberDisplayInfoList) {
        this.memberDisplayInfoList = newMemberDisplayInfoList;
        notifyDataSetChanged(); 
    }

    
    class MemberViewHolder extends RecyclerView.ViewHolder
    {
        ImageView memberImageView;
        TextView memberNameTextView;
        TextView memberTypeTextView;
        TextView memberGenderTextView;
        TextView memberAgeTextView;
        TextView memberPhoneNumberTextView;
        TextView memberIdTextView;
        TextView receiptNumberTextView;
        TextView startDateTextView;
        TextView endDateTextView;

        public MemberViewHolder(@NonNull View itemView)
        {
            super(itemView);
            memberImageView = itemView.findViewById(R.id.memberImageView);
            memberNameTextView = itemView.findViewById(R.id.memberNameTextView);
            memberTypeTextView = itemView.findViewById(R.id.memberTypeTextView);
            memberGenderTextView = itemView.findViewById(R.id.memberGenderTextView);
            memberAgeTextView = itemView.findViewById(R.id.memberAgeTextView);
            memberPhoneNumberTextView = itemView.findViewById(R.id.memberPhoneNumberTextView);
            memberIdTextView = itemView.findViewById(R.id.memberIdTextView);
            receiptNumberTextView = itemView.findViewById(R.id.receiptNumberEditText);
            startDateTextView = itemView.findViewById(R.id.startDateTextView);
            endDateTextView = itemView.findViewById(R.id.endDateTextView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition(); 
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        
                        listener.onItemClick(memberDisplayInfoList.get(position));
                    }
                }
            });
        }
    }
}