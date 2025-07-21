package com.example.artsgymapp_solo;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RenewMembershipFragment extends Fragment implements  FingerprintEnrollmentCallback {

    private static final String TAG = "RenewMembershipFragment";

    // Views for displaying member info
    private ImageView memberImageView;
    private TextView firstNameTextView, lastNameTextView, genderTextView, phoneNumberTextView, ageTextView, memberIdTextView, fingerprintStatusTextView;
    private Spinner memberTypeSpinner;
    private EditText receiptNumberEditText;
    private TextView startDateTextView, endDateTextView;
    private MaterialButton renewMemberButton, cancelButton, verifyExistingMemberButton;

    private RelativeLayout secondMemberContainer;
    private RelativeLayout secondMemberViews;
    private EditText firstNameEditText2, lastNameEditText2, phoneNumberEditText2, ageEditText2, existingMemberIdEditText;
    private ImageView memberImageView2ndMember, fingerprintImageView;
    private Spinner genderSpinner2, secondMemberSpinner;
    private Member selectedExistingPartner;

    private NavController navController;
    private DatabaseHelper dbHelper;
    private String currentMemberIdString; // The ID of the member being renewed
    private int passedSelectedMemberTypeId = -1;
    private Member currentMember; // Loaded member details
    private List<MemberType> memberTypesList;
    private ArrayAdapter<MemberType> memberTypeArrayAdapter;
    private MemberType selectedMemberType;
    private LocalDate selectedStartDate;
    private LocalDate selectedEndDate;

    public static final String ARG_SELECTED_MEMBER_TYPE_ID = "selectedMemberTypeID";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final DateTimeFormatter displayDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    ArrayAdapter<CharSequence> genderAdapter2;

    private String currentPhotoPathForNewPartner;
    private Uri capturedImageUriForNewPartner;
    private Uri selectedImageUriForNewPartner;

    private ActivityResultLauncher<Intent> takePictureLauncherForNewPartner;
    private ActivityResultLauncher<Intent> pickImageLauncherForNewPartner;
    private ActivityResultLauncher<String[]> requestPermissionsLauncherForNewPartner;

    private byte[] capturedFingerprintTemplateForNewPartner;

    public RenewMembershipFragment() {
        // Required empty public constructor
    }

    // In RenewMembershipFragment.java

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(getContext());
        memberTypesList = new ArrayList<>();

        if (getArguments() != null)
        {
            currentMemberIdString = getArguments().getString("memberID");
            passedSelectedMemberTypeId = getArguments().getInt(ARG_SELECTED_MEMBER_TYPE_ID, -1);
            Log.d(TAG, "onCreate: Received memberID: " + currentMemberIdString +
                    ", " + ARG_SELECTED_MEMBER_TYPE_ID + ": " + passedSelectedMemberTypeId);
        } else {
            Log.e(TAG, "onCreate: Arguments are NULL!");
        }

        // --- NEW: Initialize ActivityResultLaunchers for New Partner Image ---

        // For requesting permissions (Camera and Storage)
        requestPermissionsLauncherForNewPartner = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean cameraGranted = permissions.getOrDefault(android.Manifest.permission.CAMERA, false);
                    boolean storageGranted = permissions.getOrDefault(android.Manifest.permission.READ_EXTERNAL_STORAGE, false) ||
                            permissions.getOrDefault(android.Manifest.permission.READ_MEDIA_IMAGES, false); // For API 33+

                    if (cameraGranted && storageGranted) { // Or just cameraGranted if openCamera was the trigger
                        // This callback is generic. Specific action (openCamera/openGallery)
                        // will be retried in the calling function if permission was just granted.
                        // For simplicity, we can have the user click again, or directly call the method
                        // if we store which action was pending.
                        // For now, let's assume the user will click the "take photo" / "choose from gallery" again
                        // or we can call the method that triggered the permission request.
                        // Let's assume for now, the user clicks again for simplicity.
                        Toast.makeText(getContext(), "Permissions granted. Please try again.", Toast.LENGTH_SHORT).show();
                    } else if (cameraGranted) {
                        // This might happen if openCamera was called.
                        Toast.makeText(getContext(), "Camera permission granted. Please try again.", Toast.LENGTH_SHORT).show();
                    } else if (storageGranted) {
                        // This might happen if openGallery was called.
                        Toast.makeText(getContext(), "Storage permission granted. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getContext(), "Camera and/or Storage permissions are required to select an image.", Toast.LENGTH_LONG).show();
                    }
                });

        // For taking a picture
        takePictureLauncherForNewPartner = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Image captured and saved to capturedImageUriForNewPartner
                        selectedImageUriForNewPartner = null; // Clear gallery selection
                        // currentPhotoPathForNewPartner is already set by createImageFile()
                        Glide.with(this)
                                .load(capturedImageUriForNewPartner) // Use the URI obtained from FileProvider
                                .placeholder(R.drawable.addimage)
                                .error(R.mipmap.ic_launcher_round)
                                .into(memberImageView2ndMember);
                    } else {
                        Toast.makeText(getContext(), "Failed to capture image", Toast.LENGTH_SHORT).show();
                        capturedImageUriForNewPartner = null; // Reset if capture failed
                        currentPhotoPathForNewPartner = null;
                    }
                });

        // For picking an image from the gallery
        pickImageLauncherForNewPartner = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUriForNewPartner = result.getData().getData();
                        capturedImageUriForNewPartner = null; // Clear camera capture
                        currentPhotoPathForNewPartner = null;

                        Glide.with(this)
                                .load(selectedImageUriForNewPartner)
                                .placeholder(R.drawable.addimage)
                                .error(R.mipmap.ic_launcher_round)
                                .into(memberImageView2ndMember);
                    } else {
                        Toast.makeText(getContext(), "Image selection cancelled", Toast.LENGTH_SHORT).show();
                        selectedImageUriForNewPartner = null; // Reset if selection failed or was cancelled
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_renewmembership, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        Log.d(TAG, "onViewCreated: Initializing. currentMemberIdString: " + currentMemberIdString +
                ", passedSelectedMemberTypeId: " + passedSelectedMemberTypeId);

        bindViews(view);
        setupSpinners();
        setupListeners();

        if (currentMemberIdString != null) {
            loadMemberDetails(currentMemberIdString);
            loadMemberTypes();
        } else {
            Toast.makeText(getContext(), "Error: Member ID not found.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Member ID is null in onViewCreated.");
            navController.popBackStack(); // Go back if no ID
        }

        // Set click listeners for fingerprint image views
        fingerprintImageView.setOnClickListener(v -> {
            Log.d(TAG, "Fingerprint 1 clicked. Starting enrollment.");
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null)
            {
                mainActivity.startFingerprintEnrollment(2, this);
            }
        });

        // Set default start date to today
        selectedStartDate = LocalDate.now();
        startDateTextView.setText(selectedStartDate.format(displayDateFormatter));
        secondMemberContainer.setVisibility(View.GONE);
        secondMemberViews.setVisibility(View.GONE);
        existingMemberIdEditText.setVisibility(View.GONE);
        verifyExistingMemberButton.setVisibility(View.GONE);
    }

    // --- Implement FingerprintEnrollmentCallback methods ---
    @Override
    public void onEnrollmentProgress(int target, int progress, int total, String message) {

        if (target == 2) { // Ensure it's for the new partner
            Log.d(TAG, "Enrollment Progress (New Partner): " + message);
            // Update UI, e.g., a TextView near the fingerprint image
            if (fingerprintStatusTextView != null) {
                fingerprintStatusTextView.setText(message);
            }
        }
    }
    @Override
    public void onEnrollmentComplete(int target, Bitmap capturedImage, byte[] mergedTemplate) {
        if (target == 2) { // Ensure it's for the new partner
            Log.d(TAG, "Enrollment Complete (New Partner). Template size: " + mergedTemplate.length);
            capturedFingerprintTemplateForNewPartner = mergedTemplate; // Store the new template
            Toast.makeText(getContext(), "New partner fingerprint captured successfully!", Toast.LENGTH_LONG).show();
            if (fingerprintStatusTextView != null) {
                fingerprintStatusTextView.setText("Fingerprint Captured!");
            }
            // Optionally, display the captured image on fingerprintImageView if desired
            if (capturedImage != null) {
                fingerprintImageView.setImageBitmap(capturedImage);
            }
        }
    }
    @Override
    public void onEnrollmentFailed(int target, String errorMessage) {
        if (target == 2) { // Ensure it's for the new partner
            Log.e(TAG, "Enrollment Failed (New Partner): " + errorMessage);
            capturedFingerprintTemplateForNewPartner = null; // Clear any partial template
            Toast.makeText(getContext(), "New partner fingerprint capture failed: " + errorMessage, Toast.LENGTH_LONG).show();
            if (fingerprintStatusTextView != null) {
                fingerprintStatusTextView.setText("Capture Failed!");
            }
        }
    }

    public void setupSpinners() {
        // Gender Spinner for 2nd Member
        genderAdapter2 = ArrayAdapter.createFromResource(requireContext(),
                R.array.gender_array, android.R.layout.simple_spinner_item);
        genderAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner2.setAdapter(genderAdapter2);
        genderSpinner2.setSelection(0); // Set to hint "Select Gender"

        // Second Member Options Spinner (New/Existing)
        ArrayAdapter<CharSequence> secondMemberOptionsAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.member_array, android.R.layout.simple_spinner_item); // Ensure this array exists
        secondMemberOptionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        secondMemberSpinner.setAdapter(secondMemberOptionsAdapter);
        secondMemberSpinner.setSelection(0); // Set to hint "Select Option for Partner"
    }


    private void bindViews(View view) {
        // Member Info Display
        memberImageView = view.findViewById(R.id.memberImageView);
        firstNameTextView = view.findViewById(R.id.firstNameTextView);
        lastNameTextView = view.findViewById(R.id.lastNameTextView);
        genderTextView = view.findViewById(R.id.genderTextView);
        phoneNumberTextView = view.findViewById(R.id.phoneNumberTextView);
        ageTextView = view.findViewById(R.id.ageTextView);
        memberIdTextView = view.findViewById(R.id.memberIdTextView);

        // Renewal Inputs
        memberTypeSpinner = view.findViewById(R.id.memberTypeSpinner);
        receiptNumberEditText = view.findViewById(R.id.receiptNumberEditText);
        startDateTextView = view.findViewById(R.id.startDateTextView);
        endDateTextView = view.findViewById(R.id.endDateTextView);
        renewMemberButton = view.findViewById(R.id.renewMemberbutton);
        cancelButton = view.findViewById(R.id.cancelButton);

        //Second Member
        secondMemberContainer = view.findViewById(R.id.secondMemberContainer);
        secondMemberViews = view.findViewById(R.id.secondMemberViews);
        secondMemberSpinner = view.findViewById(R.id.secondMemberSpinner);
        verifyExistingMemberButton = view.findViewById(R.id.verifyExistingMemberButton);
        memberImageView2ndMember = view.findViewById(R.id.memberImageView2ndMember);
        genderSpinner2 = view.findViewById(R.id.genderSpinner2ndMember);
        firstNameEditText2 = view.findViewById(R.id.firstNameEditText2ndMember);
        lastNameEditText2 = view.findViewById(R.id.lastNameEditText2ndMember);
        phoneNumberEditText2 = view.findViewById(R.id.phoneNumberEditText2ndMember);
        ageEditText2 = view.findViewById(R.id.ageEditText2ndMember);
        existingMemberIdEditText = view.findViewById(R.id.existingMemberIdEditText);

        fingerprintStatusTextView = view.findViewById(R.id.fingerprintStatusTextView);
        fingerprintImageView = view.findViewById(R.id.fingerprintImageView);
    }

    private void setupListeners() {
        startDateTextView.setOnClickListener(v -> showDatePickerDialog(true));
        endDateTextView.setOnClickListener(v -> showDatePickerDialog(false));
        renewMemberButton.setOnClickListener(v -> attemptRenewal());
        cancelButton.setOnClickListener(v -> navController.popBackStack());
        verifyExistingMemberButton.setOnClickListener(v -> fetchExistingPartner());

        memberImageView2ndMember.setOnClickListener(v -> {
            if (secondMemberViews.getVisibility() == View.VISIBLE && firstNameEditText2.isEnabled()) {
                showImagePickDialogForNewPartner(); // Call the new dialog method
            } else if (secondMemberViews.getVisibility() == View.VISIBLE && !firstNameEditText2.isEnabled()) {
                Toast.makeText(getContext(), "Cannot change image for an existing verified partner.", Toast.LENGTH_SHORT).show();
            }
        });

        memberTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMemberType = (MemberType) parent.getItemAtPosition(position);
                if (selectedMemberType != null && selectedMemberType.getId() != -1) {
                    calculateAndDisplayEndDate();
                    if (selectedMemberType.isTwoInOne()) {
                        secondMemberContainer.setVisibility(View.VISIBLE);
                        secondMemberSpinner.setSelection(0); // Reset to "Select Option"
                        secondMemberViews.setVisibility(View.GONE);
                        existingMemberIdEditText.setVisibility(View.GONE);
                        verifyExistingMemberButton.setVisibility(View.GONE);
                        clearNewPartnerInputs();
                        selectedExistingPartner = null;
                    } else {
                        secondMemberContainer.setVisibility(View.GONE);
                        secondMemberViews.setVisibility(View.GONE);
                        existingMemberIdEditText.setVisibility(View.GONE);
                        verifyExistingMemberButton.setVisibility(View.GONE);
                    }
                } else {
                    selectedMemberType = null;
                    endDateTextView.setText("");
                    selectedEndDate = null;
                    secondMemberContainer.setVisibility(View.GONE);
                    secondMemberViews.setVisibility(View.GONE);
                    existingMemberIdEditText.setVisibility(View.GONE);
                    verifyExistingMemberButton.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMemberType = null;
                endDateTextView.setText("");
                selectedEndDate = null;
                secondMemberContainer.setVisibility(View.GONE);
            }
        });

        secondMemberSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                clearNewPartnerInputs();
                existingMemberIdEditText.setText("");
                existingMemberIdEditText.setError(null);
                selectedExistingPartner = null;
                setPartnerDetailsEditable(true); // Default to editable

                if (position == 1) { // "Add New Member as Partner"
                    secondMemberViews.setVisibility(View.VISIBLE);
                    existingMemberIdEditText.setVisibility(View.GONE);
                    verifyExistingMemberButton.setVisibility(View.GONE);
                } else if (position == 2) { // "Select Existing Member as Partner"
                    secondMemberViews.setVisibility(View.GONE); // Hide new input fields initially
                    existingMemberIdEditText.setVisibility(View.VISIBLE);
                    verifyExistingMemberButton.setVisibility(View.VISIBLE);
                } else { // "Select Option" or invalid
                    secondMemberViews.setVisibility(View.GONE);
                    existingMemberIdEditText.setVisibility(View.GONE);
                    verifyExistingMemberButton.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                secondMemberViews.setVisibility(View.GONE);
                existingMemberIdEditText.setVisibility(View.GONE);
                verifyExistingMemberButton.setVisibility(View.GONE);
            }
        });

        fingerprintImageView.setOnClickListener(v -> {
            Log.d(TAG, "Fingerprint image clicked for new partner. Starting enrollment.");
            MainActivity.startFingerprintEnrollment(2, this);
        });
    }

    private boolean validateNewPartnerInputs() {
        clearNewPartnerInputsErrors(); // Helper to clear previous errors

        String pFirstName = firstNameEditText2.getText().toString().trim();
        String pLastName = lastNameEditText2.getText().toString().trim();
        String pAgeStr = ageEditText2.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(pFirstName)) {
            firstNameEditText2.setError("First name required");
            isValid = false;
        }
        if (TextUtils.isEmpty(pLastName)) {
            lastNameEditText2.setError("Last name required");
            isValid = false;
        }
        if (TextUtils.isEmpty(pAgeStr)) {
            ageEditText2.setError("Age required");
            isValid = false;
        } else {
            try {
                int age = Integer.parseInt(pAgeStr);
                if (age <= 0 || age > 120) {
                    ageEditText2.setError("Invalid age");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                ageEditText2.setError("Invalid age format");
                isValid = false;
            }
        }
        if (genderSpinner2.getSelectedItemPosition() == 0) { // 0 is "Select Gender"
            ((TextView) genderSpinner2.getSelectedView()).setError("Select gender"); // Basic error
            Toast.makeText(getContext(), "Please select gender for the partner.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // --- Fingerprint Validation for Member 1 --- //PASTED THIS
        if (capturedFingerprintTemplateForNewPartner == null) {
            Toast.makeText(getContext(), "Fingerprint scan is required for the new partner.", Toast.LENGTH_LONG).show();
            if (fingerprintStatusTextView != null) fingerprintStatusTextView.setText("Fingerprint required!");
            Log.w(TAG, "Validation failed (New Partner): Fingerprint not captured.");
            return false; // Add this line to fail validation
        }

        return isValid;
    }

    private void attemptRenewal() {
        if (currentMember == null) {
            Toast.makeText(getContext(), "Member data not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedMemberType == null || selectedMemberType.getId() == -1) { // -1 is "Select Type"
            Toast.makeText(getContext(), "Please select a membership type.", Toast.LENGTH_SHORT).show();
            if (memberTypeSpinner.getSelectedView() != null) {
                ((TextView) memberTypeSpinner.getSelectedView()).setError("Required");
            }
            return;
        } else {
            if (memberTypeSpinner.getSelectedView() != null) {
                ((TextView) memberTypeSpinner.getSelectedView()).setError(null);
            }
        }

        if (selectedStartDate == null) {
            Toast.makeText(getContext(), "Please select a start date.", Toast.LENGTH_SHORT).show();
            startDateTextView.setError("Required");
            return;
        } else {
            startDateTextView.setError(null);
        }

        if (selectedEndDate == null) {
            Toast.makeText(getContext(), "End date not calculated. Select type or valid dates.", Toast.LENGTH_SHORT).show();
            endDateTextView.setError("Required");
            return;
        } else if (selectedEndDate.isBefore(selectedStartDate) || selectedEndDate.isEqual(selectedStartDate)) {
            Toast.makeText(getContext(), "End date must be after the start date.", Toast.LENGTH_SHORT).show();
            endDateTextView.setError("Invalid date");
            return;
        } else {
            endDateTextView.setError(null);
        }

        final String receiptNumber = receiptNumberEditText.getText().toString().trim();
        if (TextUtils.isEmpty(receiptNumber)) {
            receiptNumberEditText.setError("Receipt number is required.");
            Toast.makeText(getContext(), "Please enter a receipt number.", Toast.LENGTH_SHORT).show();
            return;
        } else {
            receiptNumberEditText.setError(null);
        }

        // --- Receipt Number Uniqueness Check ---
        executorService.execute(() -> {
            boolean receiptExists = dbHelper.isReceiptNumberExists(receiptNumber, -1); // -1 for new period
            mainThreadHandler.post(() -> {
                if (receiptExists) {
                    receiptNumberEditText.setError("Receipt number already exists.");
                    Toast.makeText(getContext(), "This receipt number is already in use.", Toast.LENGTH_SHORT).show();
                } else {
                    receiptNumberEditText.setError(null);
                    if (selectedMemberType.isTwoInOne()) {
                        int selectedPartnerOptionPos = secondMemberSpinner.getSelectedItemPosition();
                        // Assuming 0: "Select Option", 1: "New Member", 2: "Existing Member"

                        if (selectedPartnerOptionPos == 1) { // "New Member"
                            if (validateNewPartnerInputs()) {
                                String pFirstName = firstNameEditText2.getText().toString().trim();
                                String pLastName = lastNameEditText2.getText().toString().trim();
                                int pAge = Integer.parseInt(ageEditText2.getText().toString().trim()); // Already validated
                                String pPhone = phoneNumberEditText2.getText().toString().trim();
                                String pGender = genderSpinner2.getSelectedItem().toString();

                                // --- MODIFIED: Image Path Logic for New Partner ---
                                String imagePathToSaveForNewPartner = null;
                                if (currentPhotoPathForNewPartner != null && !currentPhotoPathForNewPartner.isEmpty()) {
                                    // Image was taken with camera, path is already app-specific and direct
                                    imagePathToSaveForNewPartner = currentPhotoPathForNewPartner;
                                    Log.d(TAG, "Using camera image path for new partner: " + imagePathToSaveForNewPartner);
                                } else if (selectedImageUriForNewPartner != null) {
                                    // Image was selected from gallery, save it to internal storage
                                    Log.d(TAG, "Gallery image selected for new partner. Attempting to save to internal storage. URI: " + selectedImageUriForNewPartner);
                                    // Create a name for the image file based on the new partner's name
                                    String partnerNameForFile = pFirstName + "_" + pLastName;
                                    imagePathToSaveForNewPartner = saveImageFromUriToInternalStorage(selectedImageUriForNewPartner, partnerNameForFile);
                                    if (imagePathToSaveForNewPartner == null) {
                                        Toast.makeText(getContext(), "Could not save partner image. Continuing without.", Toast.LENGTH_LONG).show();
                                    } else {
                                        Log.d(TAG, "Gallery image for new partner saved to internal path: " + imagePathToSaveForNewPartner);
                                    }
                                } else {
                                    Log.d(TAG, "No image selected or captured for new partner.");
                                }
                                saveRenewalTwoInOneNewPartner(receiptNumber, pFirstName, pLastName, pPhone, pGender, pAge, imagePathToSaveForNewPartner);
                            } else {
                                Toast.makeText(getContext(), "Please correct errors for the new partner.", Toast.LENGTH_LONG).show();
                            }
                        } else if (selectedPartnerOptionPos == 2) { // "Existing Member"
                            if (selectedExistingPartner == null) {
                                Toast.makeText(getContext(), "Please verify an existing partner ID.", Toast.LENGTH_LONG).show();
                                existingMemberIdEditText.setError("Verify ID first");
                                return;
                            }
                            // Double check, though UI should prevent this state if fetch failed
                            if (TextUtils.isEmpty(existingMemberIdEditText.getText().toString().trim()) || selectedExistingPartner == null){
                                Toast.makeText(getContext(), "No existing partner selected or verified.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (selectedExistingPartner.getMemberID().equals(currentMemberIdString)) {
                                Toast.makeText(getContext(), "Partner cannot be the same as the primary member.", Toast.LENGTH_LONG).show();
                                existingMemberIdEditText.setError("Cannot be primary"); // Re-set error
                                return;
                            }
                            saveRenewalTwoInOneExistingPartner(receiptNumber, selectedExistingPartner.getMemberID());
                        } else { // "Select Option" for partner
                            Toast.makeText(getContext(), "Please select a partner option for 2-in-1 membership.", Toast.LENGTH_LONG).show();
                            if(secondMemberSpinner.getSelectedView() != null) {
                                ((TextView) secondMemberSpinner.getSelectedView()).setError("Required");
                            }
                        }
                    } else { // Single Membership
                        saveSingleMembershipRenewal(receiptNumber);
                    }
                }
            });
        });
    }

    private void saveRenewalTwoInOneNewPartner(String receiptNumber, String pFirstName, String pLastName,
                                               String pPhone, String pGender, int pAge, @Nullable String pImagePath) {
        final String primaryMemberId = currentMember.getMemberID();
        final int memberTypeId = selectedMemberType.getId();

        executorService.execute(() ->
        {
            boolean success = dbHelper.renewTwoInOneWithNewPartner(primaryMemberId,
                    pFirstName, pLastName, pPhone, pGender, pAge, pImagePath, capturedFingerprintTemplateForNewPartner,
                    memberTypeId, selectedStartDate, selectedEndDate, receiptNumber);
            mainThreadHandler.post(() -> {
                if (success) {
                    Toast.makeText(getContext(), "2-in-1 Membership renewed with new partner!", Toast.LENGTH_SHORT).show();
                    if (navController != null) {
                        navController.popBackStack(R.id.memberListFragment, false);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to renew 2-in-1 membership (new partner).", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    //IMAGE STUFF
    private void showImagePickDialogForNewPartner() {
        // Check if the new partner section is actually for a "new" partner
        // and not displaying an existing, non-editable partner.
        if (!firstNameEditText2.isEnabled()) { // A simple check; adjust if your logic for "editable" is different
            Toast.makeText(getContext(), "Partner details are not editable.", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Set Image for Partner");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                Log.d(TAG, "Attempting to open camera for new partner.");
                checkCameraPermissionAndOpenCameraForNewPartner();
            } else if (options[item].equals("Choose from Gallery")) {
                Log.d(TAG, "Attempting to open gallery for new partner.");
                checkStoragePermissionAndOpenGalleryForNewPartner();
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpenCameraForNewPartner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // You might also want to check WRITE_EXTERNAL_STORAGE for older APIs if saving to public gallery
                // but for app-specific directory (via FileProvider), it's often not strictly needed after target API 29+
                openCameraForNewPartner();
            } else {
                // Request permission
                requestPermissionsLauncherForNewPartner.launch(new String[]{Manifest.permission.CAMERA});
            }
        } else {
            openCameraForNewPartner(); // No runtime permission needed before Marshmallow
        }
    }

    private void openCameraForNewPartner() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFileForNewPartner();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "IOException creating image file for new partner", ex);
                Toast.makeText(getContext(), "Error preparing for camera.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                // Store the path for later use if needed (e.g., for saving directly without Glide processing)
                // currentPhotoPathForNewPartner is already set in createImageFileForNewPartner

                capturedImageUriForNewPartner = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", // Make sure this matches your provider authority
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUriForNewPartner);
                Log.d(TAG, "Launching camera for new partner. Output URI: " + capturedImageUriForNewPartner);
                takePictureLauncherForNewPartner.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(getContext(), "No camera app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFileForNewPartner() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_PARTNER_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES); // App-specific storage

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPathForNewPartner = image.getAbsolutePath();
        Log.d(TAG, "New partner image file created: " + currentPhotoPathForNewPartner);
        return image;
    }

    private void checkStoragePermissionAndOpenGalleryForNewPartner() {
        String permissionToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            permissionToRequest = Manifest.permission.READ_MEDIA_IMAGES;
        } else { // Below API 33
            permissionToRequest = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permissionToRequest) == PackageManager.PERMISSION_GRANTED) {
            openGalleryForNewPartner();
        } else {
            requestPermissionsLauncherForNewPartner.launch(new String[]{permissionToRequest});
        }
    }

    private void openGalleryForNewPartner() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncherForNewPartner.launch(pickIntent);
        Log.d(TAG, "Launching gallery for new partner.");
    }

    private String saveImageFromUriToInternalStorage(Uri sourceUri, String partnerName) {
        if (sourceUri == null) return null;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "PARTNER_" + partnerName.replaceAll("\\s+", "_") + "_" + timeStamp + ".jpg";
        File storageDir = requireContext().getFilesDir(); // Internal storage - private to the app

        File destinationFile = new File(storageDir, imageFileName);

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

            if (inputStream == null) {
                Log.e(TAG, "Failed to get InputStream from URI for partner: " + sourceUri);
                return null;
            }

            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            Log.d(TAG, "Partner image saved to internal storage: " + destinationFile.getAbsolutePath());
            return destinationFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save partner image from URI to internal storage", e);
            Toast.makeText(getContext(), "Error saving partner image.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    //IMAGE STUFF

    private void fetchExistingPartner() {
        String partnerIdToVerify = existingMemberIdEditText.getText().toString().trim();
        if (TextUtils.isEmpty(partnerIdToVerify)) {
            Toast.makeText(getContext(), "Enter Partner Member ID", Toast.LENGTH_SHORT).show();
            existingMemberIdEditText.setError("Required");
            return;
        }
        if (partnerIdToVerify.equals(currentMemberIdString)) {
            Toast.makeText(getContext(), "Partner cannot be the same as the primary member.", Toast.LENGTH_LONG).show();
            existingMemberIdEditText.setError("Cannot be primary member");
            return;
        }
        existingMemberIdEditText.setError(null); // Clear previous error

        executorService.execute(() -> {
            Member fetchedPartner = dbHelper.getMemberById(partnerIdToVerify);
            mainThreadHandler.post(() -> {
                if (fetchedPartner != null) {
                    selectedExistingPartner = fetchedPartner;
                    Toast.makeText(getContext(), "Partner " + fetchedPartner.getFirstName() + " " + fetchedPartner.getLastName() + " found.", Toast.LENGTH_SHORT).show();
                    populateNewPartnerFieldsWithExisting(fetchedPartner); // Populate the common fields
                    secondMemberViews.setVisibility(View.VISIBLE);      // Show the fields
                    setPartnerDetailsEditable(false);                   // Make them non-editable
                } else {
                    selectedExistingPartner = null;
                    existingMemberIdEditText.setError("Not Found");
                    Toast.makeText(getContext(), "Existing Member ID not found.", Toast.LENGTH_SHORT).show();
                    secondMemberViews.setVisibility(View.GONE); // Hide if previously shown or for a new attempt
                    clearNewPartnerInputs(); // Clear if we hide, ensure editable state for next time
                }
            });
        });
    }

    private void populateNewPartnerFieldsWithExisting(Member partner) {
        firstNameEditText2.setText(partner.getFirstName());
        lastNameEditText2.setText(partner.getLastName());
        ageEditText2.setText(String.valueOf(partner.getAge()));
        phoneNumberEditText2.setText(partner.getPhoneNumber());

        if (partner.getGender() != null && genderAdapter2 != null) {
            int genderPosition = genderAdapter2.getPosition(partner.getGender());
            if (genderPosition >= 0) {
                genderSpinner2.setSelection(genderPosition);
            } else {
                genderSpinner2.setSelection(0); // Default if gender not found in adapter
                Log.w(TAG, "Gender for existing partner not found in adapter: " + partner.getGender());
            }
        } else {
            genderSpinner2.setSelection(0);
        }

        selectedImageUriForNewPartner = null; // Existing members don't get a new image selection here
        if (partner.getImageFilePath() != null && !partner.getImageFilePath().isEmpty()) {
            Glide.with(this)
                    .load(new File(partner.getImageFilePath()))
                    .placeholder(R.drawable.addimage)
                    .error(R.mipmap.ic_launcher_round)
                    .into(memberImageView2ndMember);
        } else {
            Glide.with(this)
                    .load(R.drawable.addimage)
                    .into(memberImageView2ndMember);
        }
    }

    private void saveRenewalTwoInOneExistingPartner(String receiptNumber, String existingPartnerId) {
        final String primaryMemberId = currentMember.getMemberID();
        final int memberTypeId = selectedMemberType.getId();

        executorService.execute(() -> {
            boolean success = dbHelper.renewTwoInOneWithExistingPartner(primaryMemberId, existingPartnerId,
                    memberTypeId, selectedStartDate, selectedEndDate, receiptNumber);
            mainThreadHandler.post(() -> {
                if (success) {
                    Toast.makeText(getContext(), "2-in-1 Membership renewed with existing partner!", Toast.LENGTH_SHORT).show();
                    if (navController != null) {
                        navController.popBackStack(R.id.memberListFragment, false);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to renew 2-in-1 membership (existing partner).", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void saveSingleMembershipRenewal(String receiptNumber) {
        final String memberIdToRenew = currentMember.getMemberID();
        final int memberTypeIdToRenew = selectedMemberType.getId();

        executorService.execute(() -> {
            boolean success = dbHelper.renewSingleMembership(
                    memberIdToRenew, memberTypeIdToRenew, selectedStartDate, selectedEndDate, receiptNumber);
            mainThreadHandler.post(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Membership renewed successfully!", Toast.LENGTH_SHORT).show();
                    if (navController != null) {
                        navController.popBackStack(R.id.memberListFragment, false);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to renew membership.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loadMemberDetails(String memberIdStr) {
        executorService.execute(() -> {
            currentMember = dbHelper.getMemberById(memberIdStr); // put the data in this varialbe
            mainThreadHandler.post(() -> {
                if (currentMember != null) {
                    populateMemberDetailsUI();
                } else {
                    Toast.makeText(getContext(), "Failed to load member details.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load member details for ID: " + memberIdStr);
                    navController.popBackStack();
                }
            });
        });
    }

    private void populateMemberDetailsUI() {
        if (currentMember == null) return;

        firstNameTextView.setText(currentMember.getFirstName());
        lastNameTextView.setText(currentMember.getLastName());
        genderTextView.setText(currentMember.getGender());
        phoneNumberTextView.setText(currentMember.getPhoneNumber());
        ageTextView.setText(String.valueOf(currentMember.getAge())); // Convert int to String
        memberIdTextView.setText(currentMember.getMemberID());

        if (currentMember.getImageFilePath() != null && !currentMember.getImageFilePath().isEmpty()) {
            Glide.with(this)
                    .load(new File(currentMember.getImageFilePath()))
                    .placeholder(R.drawable.addimage) // Replace with your placeholder
                    .error(R.mipmap.ic_launcher_round) // Replace
                    .into(memberImageView);
        } else {
            Glide.with(this)
                    .load(R.mipmap.ic_launcher_round) // Replace
                    .into(memberImageView);
        }
    }

    private void loadMemberTypes() {
        executorService.execute(() -> {
            List<MemberType> typesFromDb = dbHelper.getAllMemberTypes();
            mainThreadHandler.post(() -> {
                memberTypesList.clear();
                MemberType hintType = new MemberType(-1, "Select Membership Type", 0, false);
                memberTypesList.add(hintType); // Add a hint item at the beginning
                memberTypesList.addAll(typesFromDb);

                if (getContext() == null) {
                    Log.e(TAG, "Context is null in loadMemberTypes, cannot create ArrayAdapter.");
                    return;
                }

                memberTypeArrayAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, memberTypesList);
                memberTypeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                memberTypeSpinner.setAdapter(memberTypeArrayAdapter);

                // --- NEW: Pre-select the spinner based on passedSelectedMemberTypeId ---
                if (passedSelectedMemberTypeId != -1) {
                    boolean foundType = false;
                    for (int i = 0; i < memberTypesList.size(); i++) {
                        if (memberTypesList.get(i).getId() == passedSelectedMemberTypeId) {
                            memberTypeSpinner.setSelection(i);
                            selectedMemberType = memberTypesList.get(i); // Also update the selectedMemberType field
                            calculateAndDisplayEndDate(); // Update end date based on pre-selected type
                            Log.d(TAG, "Pre-selected membership type: " + selectedMemberType.getName());

                            // Handle visibility of 2-in-1 specific views
                            if (selectedMemberType.isTwoInOne()) {
                                secondMemberContainer.setVisibility(View.VISIBLE);
                                // Reset partner selection parts as it's a fresh load with pre-selected type
                                secondMemberSpinner.setSelection(0);
                                secondMemberViews.setVisibility(View.GONE);
                                existingMemberIdEditText.setVisibility(View.GONE);
                                verifyExistingMemberButton.setVisibility(View.GONE);
                                clearNewPartnerInputs();
                                selectedExistingPartner = null;
                            } else {
                                secondMemberContainer.setVisibility(View.GONE);
                                secondMemberViews.setVisibility(View.GONE);
                            }
                            foundType = true;
                            break;
                        }
                    }
                    if (!foundType) {
                        Log.w(TAG, "Passed selectedMemberTypeID " + passedSelectedMemberTypeId + " not found in the list. Defaulting to hint.");
                        memberTypeSpinner.setSelection(0); // Default to hint if not found
                    }
                } else {
                    memberTypeSpinner.setSelection(0); // Select the hint by default if no ID was passed
                }
                // --- END NEW ---
            });
        });
    }

    private void showDatePickerDialog(final boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        if (isStartDate && selectedStartDate != null) {
            calendar.set(selectedStartDate.getYear(), selectedStartDate.getMonthValue() - 1, selectedStartDate.getDayOfMonth());
        } else if (!isStartDate && selectedEndDate != null) {
            calendar.set(selectedEndDate.getYear(), selectedEndDate.getMonthValue() - 1, selectedEndDate.getDayOfMonth());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    if (isStartDate) {
                        selectedStartDate = selectedDate;
                        startDateTextView.setText(selectedStartDate.format(displayDateFormatter));
                        calculateAndDisplayEndDate(); // Recalculate end date if start date changes
                    } else {
                        selectedEndDate = selectedDate;
                        endDateTextView.setText(selectedEndDate.format(displayDateFormatter));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void calculateAndDisplayEndDate() {
        if (selectedStartDate != null && selectedMemberType != null && selectedMemberType.getId() != -1) {
            if (selectedMemberType.getDurationDays() > 0) {
                selectedEndDate = selectedStartDate.plusDays(selectedMemberType.getDurationDays());
                endDateTextView.setText(selectedEndDate.format(displayDateFormatter));
            } else {
                endDateTextView.setHint("Select Valid Type"); // Or clear it
                selectedEndDate = null;
            }
        } else if (selectedStartDate != null && selectedMemberType == null) {
            // If a member type was selected then de-selected, clear the end date
            endDateTextView.setText("");
            endDateTextView.setHint("Select End Date"); // Reset hint
            selectedEndDate = null;
        }
    }

    private void setPartnerDetailsEditable(boolean editable) {
        firstNameEditText2.setEnabled(editable);
        lastNameEditText2.setEnabled(editable);
        ageEditText2.setEnabled(editable);
        phoneNumberEditText2.setEnabled(editable);
        genderSpinner2.setEnabled(editable);
        memberImageView2ndMember.setClickable(editable);
    }

    private void clearNewPartnerInputs()
    {
        firstNameEditText2.setText("");
        lastNameEditText2.setText("");
        ageEditText2.setText("");
        phoneNumberEditText2.setText("");
        genderSpinner2.setSelection(0);
        Glide.with(this).clear(memberImageView2ndMember); // Clear image
        selectedImageUriForNewPartner = null; // if you have URI tracking
        setPartnerDetailsEditable(true); // Make them editable again for a new entry
    }

    private void clearNewPartnerInputsErrors() {
        firstNameEditText2.setError(null);
        lastNameEditText2.setError(null);
        ageEditText2.setError(null);
        phoneNumberEditText2.setError(null);
        if (genderSpinner2.getSelectedView() != null) {
            ((TextView) genderSpinner2.getSelectedView()).setError(null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity.stopFingerprintEnrollment();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify views to help GC, though not strictly necessary with view binding in some cases
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
        // dbHelper is managed by the fragment lifecycle, usually no need to close here
        // unless you're creating/closing it per method call which is inefficient.
    }
}