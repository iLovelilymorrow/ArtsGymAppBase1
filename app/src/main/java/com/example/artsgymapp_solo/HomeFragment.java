package com.example.artsgymapp_solo;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.artsgymapp_solo.databinding.FragmentHomeBinding;
import com.bumptech.glide.Glide;
import java.io.File;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private DatabaseHelper databaseHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonClearPage.setOnClickListener(v -> {
            Log.d(TAG, "Clear button clicked");
            clearDisplayedMemberInfo();
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null)
            {
                mainActivity.startIdentification();
            }
        });

        if (getArguments() != null) {
            String identifiedMemberId = getArguments().getString("identifiedMemberId");
            if (identifiedMemberId != null && !identifiedMemberId.isEmpty()) {
                Log.d(TAG, "HomeFragment received identifiedMemberId: " + identifiedMemberId);
                loadAndDisplayMemberInfo(identifiedMemberId);
                // Clear the argument so it doesn't re-display on subsequent navigations
                getArguments().remove("identifiedMemberId");
            } else {
                // If identifiedMemberId is null or empty, clear the display
                Log.d(TAG, "HomeFragment received null/empty identifiedMemberId. Clearing display.");
                clearDisplayedMemberInfo();
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null)
                {
                    mainActivity.startIdentification();
                }
            }
        } else {
            // If no arguments are passed, clear the display (e.g., initial load or navigation without ID)
            Log.d(TAG, "HomeFragment received no arguments. Clearing display.");
            clearDisplayedMemberInfo();
        }
    }

    private void clearDisplayedMemberInfo() {
        binding.memberIdTextView.setText("");
        binding.fullNameTextView.setText("");
        binding.membershipTypeTextView.setText("");
        binding.membershipStartedTextView.setText("");
        binding.expiringTextView.setText("");
        binding.daysLeftTextView.setText("");
        binding.ageTextView.setText("");
        binding.phoneNumberTextView.setText("");
        binding.memberPictureImageView.setImageResource(0); // Clear image
    }

    private void loadAndDisplayMemberInfo(String memberId) {
        MemberDisplayInfo memberInfo = databaseHelper.getMemberDisplayInfo(memberId);

        if (memberInfo != null) {
            // The check for active membership is now primarily handled in MainActivity.
            // HomeFragment just displays what it's given.
            // However, it's good to re-check here for robustness or if HomeFragment can be navigated to directly.
            if (databaseHelper.isMembershipActive(memberId)) {
                binding.memberIdTextView.setText(memberInfo.getMemberID());
                binding.fullNameTextView.setText(memberInfo.getFullName());
                binding.membershipTypeTextView.setText(memberInfo.getMemberTypeName());

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
                if (memberInfo.getRegistrationDate() != null) {
                    binding.membershipStartedTextView.setText(memberInfo.getRegistrationDate().format(formatter));
                } else {
                    binding.membershipStartedTextView.setText("N/A");
                }

                if (memberInfo.getExpirationDate() != null) {
                    binding.expiringTextView.setText(memberInfo.getExpirationDate().format(formatter));
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), memberInfo.getExpirationDate());
                    binding.daysLeftTextView.setText(String.valueOf(daysLeft) + " days left");
                } else {
                    binding.expiringTextView.setText("N/A");
                    binding.daysLeftTextView.setText("N/A");
                }

                binding.ageTextView.setText(String.valueOf(memberInfo.getAge()));
                binding.phoneNumberTextView.setText(memberInfo.getPhoneNumber());

                if (memberInfo.getImageFilePath() != null && !memberInfo.getImageFilePath().isEmpty()) {
                    File imgFile = new File(memberInfo.getImageFilePath());
                    if (imgFile.exists()) {
                        Glide.with(this)
                                .load(imgFile)
                                .placeholder(R.mipmap.ic_launcher_round)
                                .error(R.mipmap.ic_launcher_round)
                                .into(binding.memberPictureImageView);
                    } else {
                        binding.memberPictureImageView.setImageResource(R.mipmap.ic_launcher_round);
                    }
                } else {
                    binding.memberPictureImageView.setImageResource(R.mipmap.ic_launcher_round);
                }
            } else {
                // Membership inactive, clear display and show message
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Membership for " + memberInfo.getFullName() + " is inactive.", Toast.LENGTH_LONG).show();
                }
                clearDisplayedMemberInfo();
            }
        } else {
            // Member not found, clear display and show message
            if (getContext() != null) {
                Toast.makeText(getContext(), "Member not found.", Toast.LENGTH_SHORT).show();
            }
            clearDisplayedMemberInfo();
        }
    }
}
