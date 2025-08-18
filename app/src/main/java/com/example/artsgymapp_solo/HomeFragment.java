package com.example.artsgymapp_solo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.os.Looper;
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

public class HomeFragment extends Fragment
{
    private FragmentHomeBinding binding;
    private DatabaseHelper databaseHelper;

    private static final long MEMBER_INFO_DISPLAY_TIMEOUT_MS = 50 * 1000L;
    private Handler memberInfoDisplayHandler;
    private Runnable clearMemberInfoRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(getContext());

        memberInfoDisplayHandler = new Handler(Looper.getMainLooper());
        clearMemberInfoRunnable = () -> {
            if(isAdded() && binding != null)
            {
                clearDisplayedMemberInfo();
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        Object layoutTag = binding.getRoot().getTag();
        Log.d("FragmentLayoutTracker", "HomeFragment using layout: " + layoutTag);

        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.getRoot().setOnClickListener(v -> resetMemberInfoDisplayTimer());

        binding.getRoot().setOnTouchListener((v, event) -> {
            resetMemberInfoDisplayTimer();
            return false;
        });

        binding.buttonClearPage.setOnClickListener(v ->
        {
            clearDisplayedMemberInfo();
            stopMemberInfoDisplayTimer();
        });

        Bundle args = getArguments();
        if (args != null) {
            String identifiedMemberId = args.getString("identifiedMemberId");
            if (identifiedMemberId != null && !identifiedMemberId.isEmpty())
            {
                loadAndDisplayMemberInfo(identifiedMemberId);
                resetMemberInfoDisplayTimer();
            }
            else
            {
                clearDisplayedMemberInfo();
                stopMemberInfoDisplayTimer();
            }
        } else {
            clearDisplayedMemberInfo();
            stopMemberInfoDisplayTimer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (binding != null && binding.memberIdTextView.getText() != null && !binding.memberIdTextView.getText().toString().isEmpty()) {
            resetMemberInfoDisplayTimer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        stopMemberInfoDisplayTimer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopMemberInfoDisplayTimer();
        binding = null;
    }

    private void resetMemberInfoDisplayTimer() {
        if (memberInfoDisplayHandler != null && clearMemberInfoRunnable != null) {
            memberInfoDisplayHandler.removeCallbacks(clearMemberInfoRunnable);

            if (binding != null && binding.memberIdTextView.getText() != null &&
                    !binding.memberIdTextView.getText().toString().isEmpty()) {
                memberInfoDisplayHandler.postDelayed(clearMemberInfoRunnable, MEMBER_INFO_DISPLAY_TIMEOUT_MS);
            }
        }
    }

    private void stopMemberInfoDisplayTimer() {
        if (memberInfoDisplayHandler != null && clearMemberInfoRunnable != null) {
            memberInfoDisplayHandler.removeCallbacks(clearMemberInfoRunnable);
        }
    }

    private void clearDisplayedMemberInfo()
    {
        if(!binding.memberIdTextView.getText().toString().isEmpty())
        {
            binding.memberIdTextView.setText("");
            binding.fullNameTextView.setText("");
            binding.membershipTypeTextView.setText("");
            binding.membershipStartedTextView.setText("");
            binding.expiringTextView.setText("");
            binding.daysLeftTextView.setText("");
            binding.ageTextView.setText("");
            binding.memberPictureImageView.setImageResource(0);
            binding.expiringTextWarning.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Page Cleared!", Toast.LENGTH_SHORT).show();
        }
        else
        {

        }
    }

    private void loadAndDisplayMemberInfo(String memberId) {
        MemberDisplayInfo memberInfo = databaseHelper.getMemberDisplayInfo(memberId);
        binding.expiringTextWarning.setVisibility(View.GONE);

        if (memberInfo != null) {
            
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

                if (memberInfo.getExpirationDate() != null)
                {
                    binding.expiringTextView.setText(memberInfo.getExpirationDate().format(formatter));
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), memberInfo.getExpirationDate());
                    binding.daysLeftTextView.setText(String.valueOf(daysLeft));

                    if(daysLeft <= 3)
                    {
                        binding.expiringTextWarning.setVisibility(View.VISIBLE);
                        binding.expiringTextWarning.setText("Membership is Expiring in: " + daysLeft + " days");
                    }
                }
                else
                {
                    binding.daysLeftTextView.setVisibility(View.GONE);
                    binding.expiringTextView.setText("N/A");
                    binding.daysLeftTextView.setText("N/A");
                }

                binding.ageTextView.setText(String.valueOf(memberInfo.getAge()));

                if (memberInfo.getImageFilePath() != null && !memberInfo.getImageFilePath().isEmpty())
                {
                    File imgFile = new File(memberInfo.getImageFilePath());

                    if (imgFile.exists())
                    {
                        Glide.with(this)
                                .load(imgFile)
                                .placeholder(R.mipmap.ic_launcher_round)
                                .error(R.mipmap.ic_launcher_round)
                                .into(binding.memberPictureImageView);
                    }
                    else
                    {
                        binding.memberPictureImageView.setImageResource(R.mipmap.ic_launcher_round);
                    }
                }
                else
                {
                    binding.memberPictureImageView.setImageResource(R.mipmap.ic_launcher_round);
                }
            }
            else
            {
                if (getContext() != null)
                {
                    Toast.makeText(getContext(), "Membership for " + memberInfo.getFullName() + " is inactive.", Toast.LENGTH_LONG).show();
                }
                clearDisplayedMemberInfo();
            }
        } else {
            
            if (getContext() != null) {
                Toast.makeText(getContext(), "Member not found.", Toast.LENGTH_SHORT).show();
            }
            clearDisplayedMemberInfo();
        }
    }
}