package com.example.artsgymapp_solo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton; // If using MaterialButton

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MemberEditFragment extends Fragment
{
    private static final String TAG = "MemberEditFragment";
    private ImageView memberImageView;
    private TextView memberIdTextView;
    private MaterialButton buttonDeleteMember;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private Spinner membershipTypeSpinner;
    private Spinner genderSpinner;
    private EditText ageEditText;
    private EditText phoneNumberEditText;
    private EditText receiptNumberEditText;
    private MaterialButton buttonConfirmEdit;
    private MaterialButton buttonCancelEdit;

    private DatabaseHelper databaseHelper;
    private String currentMemberIdToEdit;
    private int currentPeriodIdToEdit = -1;
    private Member currentMemberDetails; // To hold the loaded member data
    private MembershipPeriod currentMembershipPeriodDetails;
    private String originalReceiptNumber; // << NEW: To store the initial receipt number of the current period

    private String originalImageFilePath; // To store the initial image path
    private String newSelectedPhotoPath; // For camera captures
    private Uri capturedImageUri;      // For camera output
    private Uri newSelectedGalleryImageUri; // For gallery selections

    // ActivityResultLaunchers (copied from AddMember)
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    private NavController navController;

    private Drawable defaultGenderSpinnerBackground;
    private Drawable defaultMembershipTypeSpinnerBackground;

    private TextView registrationDateTextView;
    private TextView expirationDateTextView;
    private List<MemberType> memberTypeList;
    private ArrayAdapter<String> memberTypeSpinnerAdapter;
    private List<String> memberTypeNames;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private int originalFkMemberTypeIdForPeriod = -1;
    private boolean isInitialMemberTypeSpinnerSetupDone = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(requireContext());

        if (getArguments() != null) {
            currentMemberIdToEdit = getArguments().getString("memberID");
            currentPeriodIdToEdit = getArguments().getInt("periodID", -1); // << NEW: Retrieve periodId
            Log.d(TAG, "onCreate: Received memberID: " + currentMemberIdToEdit + ", periodID: " + currentPeriodIdToEdit);
        } else {
            Log.e(TAG, "onCreate: Arguments are null.");
        }

        setupActivityLaunchers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_memberedit, container, false);
        initializeViews(view);

        // Store default backgrounds AFTER views are found
        if (genderSpinner != null) {
            defaultGenderSpinnerBackground = genderSpinner.getBackground();
        }
        if (membershipTypeSpinner != null) {
            defaultMembershipTypeSpinnerBackground = membershipTypeSpinner.getBackground();
        }

        setupSpinners(); // You'll need to create this or adapt from AddMember
        setupSpinnerErrorClearing(); // Call new method
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        if (currentMemberIdToEdit != null && currentPeriodIdToEdit != -1) {
            isInitialMemberTypeSpinnerSetupDone = false; // Ensure it's false before loading
            loadMemberData(currentMemberIdToEdit, currentPeriodIdToEdit);
            // setupMembershipTypeSpinnerNavigationListener(); // <-- MOVE THIS
        } else {
            Toast.makeText(getContext(), "Error: Member or Period ID not found.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "onViewCreated: currentMemberIdToEdit is " + currentMemberIdToEdit +
                    " or currentPeriodIdToEdit is " + currentPeriodIdToEdit + " (invalid). Cannot load data.");
            if (navController != null) navController.popBackStack();
        }

        setClickListeners();
    }

    private void initializeViews(View view)
    {
        memberImageView = view.findViewById(R.id.memberImageView);
        memberIdTextView = view.findViewById(R.id.memberIdTextView);
        buttonDeleteMember = view.findViewById(R.id.buttonDeleteMember);
        firstNameEditText = view.findViewById(R.id.firstNameEditText);
        lastNameEditText = view.findViewById(R.id.lastNameEditText);
        membershipTypeSpinner = view.findViewById(R.id.membershipTypeSpinner);
        genderSpinner = view.findViewById(R.id.genderSpinner2ndMember);
        ageEditText = view.findViewById(R.id.ageEditText);
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        receiptNumberEditText = view.findViewById(R.id.receiptNumberEditText);
        buttonConfirmEdit = view.findViewById(R.id.buttonConfirmEdit);
        buttonCancelEdit = view.findViewById(R.id.buttonCancelEdit);
        buttonDeleteMember = view.findViewById(R.id.buttonDeleteMember);
        registrationDateTextView = view.findViewById(R.id.registrationDateTextView);
        expirationDateTextView = view.findViewById(R.id.expirationDateTextView);
    }

    private void setupSpinners()
    {
        // Gender Spinner
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.gender_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        // ++ MODIFIED: Member Type Spinner (Database Driven) ++
        memberTypeList = databaseHelper.getAllMemberTypes(); // Fetch from DB
        memberTypeNames = new ArrayList<>();
        memberTypeNames.add("Select Membership Type"); // Prompt

        if (memberTypeList != null && !memberTypeList.isEmpty()) {
            for (MemberType type : memberTypeList) {
                memberTypeNames.add(type.getName());
            }
        } else {
            Log.w(TAG, "No member types found in database. Spinner will be empty or show only prompt.");
            // Optionally, disable the spinner or show a specific message
        }

        memberTypeSpinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, memberTypeNames);
        memberTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        membershipTypeSpinner.setAdapter(memberTypeSpinnerAdapter);
    }

    private void setupSpinnerErrorClearing() {
        if (genderSpinner != null) {
            genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) { // If a valid item (not the prompt) is selected
                        genderSpinner.setBackground(defaultGenderSpinnerBackground);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (membershipTypeSpinner != null) {
            membershipTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) { // If a valid item (not the prompt) is selected
                        membershipTypeSpinner.setBackground(defaultMembershipTypeSpinnerBackground);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void setupActivityLaunchers() {
        // --- Permission Launcher (Same as AddMember) ---
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    // ... (copy exact logic from AddMember's requestPermissionsLauncher callback)
                    // ... (handle camera and storage permissions separately if possible)
                    boolean cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                    String readPermissionNeeded = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ?
                            Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
                    boolean readStorageGranted = permissions.getOrDefault(readPermissionNeeded, false);

                    // Check which action triggered the permission request,
                    // you might need a flag if you want to distinguish
                    if (permissions.containsKey(Manifest.permission.CAMERA)) {
                        if (cameraGranted) openCamera();
                        else Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                    if (permissions.containsKey(readPermissionNeeded)) {
                        if (readStorageGranted) openGallery();
                        else Toast.makeText(getContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        // --- Take Picture Launcher (Same as AddMember) ---
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        newSelectedGalleryImageUri = null; // Clear gallery selection
                        if (capturedImageUri != null) {
                            Glide.with(this).load(capturedImageUri).into(memberImageView);
                            Log.d(TAG, "Image captured, URI: " + capturedImageUri.toString() + (newSelectedPhotoPath != null ? " | Path: " + newSelectedPhotoPath : ""));
                            // newSelectedPhotoPath is already set by createImageFile
                        } else {
                            Log.e(TAG, "CapturedImageUri is null after taking picture.");
                        }
                    } else {
                        Log.d(TAG, "Image capture cancelled or failed. Code: " + result.getResultCode());
                        if (newSelectedPhotoPath != null) { // If a file was created
                            File photoFile = new File(newSelectedPhotoPath);
                            if (photoFile.exists() && photoFile.length() == 0) { // Check if empty file was created
                                if(photoFile.delete()){
                                    Log.d(TAG, "Empty photo file deleted: " + newSelectedPhotoPath);
                                }
                            }
                        }
                        newSelectedPhotoPath = null;
                        capturedImageUri = null;
                    }
                });

        // --- Pick Image From Gallery Launcher (Same as AddMember) ---
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        newSelectedGalleryImageUri = uri;
                        capturedImageUri = null; // Clear camera selection
                        newSelectedPhotoPath = null; // Gallery URIs are not direct file paths until copied
                        Glide.with(this).load(newSelectedGalleryImageUri).into(memberImageView);
                        Log.d(TAG, "Image selected from gallery: " + newSelectedGalleryImageUri.toString());
                    } else {
                        Log.d(TAG, "No image selected from gallery.");
                    }
                });
    }

    private void loadMemberData(String memberId, int periodId) {
        Log.d(TAG, "Loading data for member ID: " + memberId + " and period ID: " + periodId);

        if (membershipTypeSpinner != null) {
            membershipTypeSpinner.setOnItemSelectedListener(null);
            Log.d(TAG, "loadMemberData: Temporarily detached OnItemSelectedListener.");
        }

        currentMemberDetails = databaseHelper.getMemberById(memberId);
        currentMembershipPeriodDetails = databaseHelper.getMembershipPeriodById(periodId); // << NEW DB HELPER METHOD NEEDED

        if (currentMemberDetails != null && currentMembershipPeriodDetails != null)
        {
            firstNameEditText.setText(currentMemberDetails.getFirstName());
            lastNameEditText.setText(currentMemberDetails.getLastName());
            ageEditText.setText(String.valueOf(currentMemberDetails.getAge()));
            phoneNumberEditText.setText(currentMemberDetails.getPhoneNumber());
            memberIdTextView.setText(String.format("Member ID: %s", currentMemberDetails.getMemberID()));
            selectSpinnerItemByValue(genderSpinner, currentMemberDetails.getGender());
            originalImageFilePath = currentMemberDetails.getImageFilePath();
            if (originalImageFilePath != null && !originalImageFilePath.isEmpty()) {
                Glide.with(this)
                        .load(new File(originalImageFilePath))
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.mipmap.ic_launcher_round)
                        .into(memberImageView);
            } else {
                memberImageView.setImageResource(R.mipmap.ic_launcher_round);
            }

            originalFkMemberTypeIdForPeriod = currentMembershipPeriodDetails.getFkMemberTypeId();
            Log.d(TAG, "Original fkMemberTypeId for this period: " + originalFkMemberTypeIdForPeriod);

            // Populate period-specific info from currentMembershipPeriodDetails
            String currentPeriodMemberTypeName = null;
            MemberType typeDetailsForPeriod = databaseHelper.getMemberTypeById(currentMembershipPeriodDetails.getFkMemberTypeId());
            if (typeDetailsForPeriod != null) {
                currentPeriodMemberTypeName = typeDetailsForPeriod.getName();
            } else {
                Log.w(TAG, "Could not find member type name for fkMemberTypeId: " + currentMembershipPeriodDetails.getFkMemberTypeId());
            }

            if (currentPeriodMemberTypeName != null && memberTypeSpinnerAdapter != null) {
                boolean found = false;
                for (int i = 0; i < memberTypeSpinnerAdapter.getCount(); i++) {
                    if (currentPeriodMemberTypeName.equals(memberTypeSpinnerAdapter.getItem(i))) {
                        membershipTypeSpinner.setSelection(i, false);
                        Log.d(TAG, "loadMemberData: Programmatically set membershipTypeSpinner to position: " + i + " for type: " + currentPeriodMemberTypeName);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Log.w(TAG, "Member's current type '" + currentPeriodMemberTypeName + "' not found in spinner. Defaulting.");
                    if (membershipTypeSpinner.getCount() > 0) membershipTypeSpinner.setSelection(0, false);
                }
            } else if (memberTypeSpinnerAdapter != null && membershipTypeSpinner.getCount() > 0) {
                Log.w(TAG, "Current member type name is null or adapter is null. Defaulting spinner.");
                membershipTypeSpinner.setSelection(0, false);
            }

            // Dates from the specific MembershipPeriod
            if (currentMembershipPeriodDetails.getRegistrationDate() != null) {
                registrationDateTextView.setText(String.format("Registered: %s",
                        currentMembershipPeriodDetails.getRegistrationDate().format(dateFormatter)));
            } else {
                registrationDateTextView.setText("Registered: N/A");
            }

            if (currentMembershipPeriodDetails.getExpirationDate() != null) {
                expirationDateTextView.setText(String.format("Expires: %s",
                        currentMembershipPeriodDetails.getExpirationDate().format(dateFormatter)));
            } else {
                expirationDateTextView.setText("Expires: N/A");
            }

            // Receipt Number from the specific MembershipPeriod
            originalReceiptNumber = currentMembershipPeriodDetails.getReceiptNumber();
            if (originalReceiptNumber != null) {
                receiptNumberEditText.setText(originalReceiptNumber);
            } else {
                receiptNumberEditText.setText("");
            }

            // NOW, after programmatic selection, set the flag and attach the listener
            // Attach listener AFTER programmatic selection is done
            // The flag isInitialMemberTypeSpinnerSetupDone is still useful to ensure this runs once
            // after data is fully loaded for the first time or after recreation.
            boolean needsListenerSetup = !isInitialMemberTypeSpinnerSetupDone || (membershipTypeSpinner != null && membershipTypeSpinner.getOnItemSelectedListener() == null);

            if (needsListenerSetup) {
                setupMembershipTypeSpinnerNavigationListener(); // This will install the new listener
                Log.d(TAG, "loadMemberData: Called setupMembershipTypeSpinnerNavigationListener.");
            }

            if (!isInitialMemberTypeSpinnerSetupDone) {
                isInitialMemberTypeSpinnerSetupDone = true; // Mark that main setup is done
                Log.d(TAG, "loadMemberData: isInitialMemberTypeSpinnerSetupDone = true");
            }

        } else {
            if (currentMemberDetails == null) {
                Log.e(TAG, "Member with ID " + memberId + " not found in database.");
            }
            if (currentMembershipPeriodDetails == null) {
                Log.e(TAG, "MembershipPeriod with ID " + periodId + " not found in database.");
            }
            Toast.makeText(getContext(), "Failed to load member or period details.", Toast.LENGTH_LONG).show();
            if (navController != null) navController.popBackStack();
        }
    }

    private void setupMembershipTypeSpinnerNavigationListener() {
        if (membershipTypeSpinner == null) return;

        membershipTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                Log.d(TAG, "NAV_LISTENER: onItemSelected ENTRY - event_position: " + position +
                        ", isInitialSetupDone: " + isInitialMemberTypeSpinnerSetupDone);

                int currentSpinnerActualSelectedPosition = -1;
                if (membershipTypeSpinner != null) { // Defensive check
                    currentSpinnerActualSelectedPosition = membershipTypeSpinner.getSelectedItemPosition();
                    Log.d(TAG, "NAV_LISTENER: membershipTypeSpinner.getSelectedItemPosition() reports: " + currentSpinnerActualSelectedPosition);
                }

                // This is the CRITICAL check. If the event's position doesn't match what the spinner
                // currently thinks is selected, the event might be stale.
                if (currentSpinnerActualSelectedPosition != -1 && currentSpinnerActualSelectedPosition != position) {
                    Log.w(TAG, "NAV_LISTENER: MISMATCH! Event_position is " + position +
                            " but spinner's actual selected position is " + currentSpinnerActualSelectedPosition +
                            ". Trusting spinner's actual position for logic.");
                    position = currentSpinnerActualSelectedPosition; // USE THE SPINNER'S ACTUAL POSITION
                    Log.d(TAG, "NAV_LISTENER: Using corrected position: " + position);
                }

                // If isInitialMemberTypeSpinnerSetupDone is false, it means loadMemberData hasn't finished
                // its full setup, so we should wait.
                if (!isInitialMemberTypeSpinnerSetupDone) {
                    Log.w(TAG, "MembershipTypeSpinner NAV LISTENER: Initial setup not yet complete. Skipping navigation.");
                    return;
                }

                if (getContext() != null && defaultMembershipTypeSpinnerBackground != null && position > 0) {
                    membershipTypeSpinner.setBackground(defaultMembershipTypeSpinnerBackground);
                }

                if (position == 0 || memberTypeList == null || memberTypeList.isEmpty()) {
                    Log.d(TAG, "MembershipTypeSpinner NAV LISTENER: Prompt selected or list empty. No navigation.");
                    return;
                }

                MemberType selectedMemberType = null;
                if (position > 0 && (position - 1) < memberTypeList.size()) { // Corrected bounds check for memberTypeList
                    selectedMemberType = memberTypeList.get(position - 1);
                } else {
                    Log.e(TAG, "MembershipTypeSpinner NAV LISTENER: Corrected position " + position + " still out of bounds for memberTypeList or invalid.");
                    return;
                }

                if (selectedMemberType != null && currentMemberDetails != null && currentMembershipPeriodDetails != null) {
                    Log.d(TAG, "MembershipTypeSpinner NAV LISTENER: User selected type: " + selectedMemberType.getName() +
                            ", ID: " + selectedMemberType.getId());
                    Log.d(TAG, "MembershipTypeSpinner NAV LISTENER: Original fkMemberTypeID for period: " + originalFkMemberTypeIdForPeriod);

                    if (selectedMemberType.getId() != originalFkMemberTypeIdForPeriod) {
                        Log.i(TAG, "MembershipType NAV LISTENER: Type changed by user from ID " +
                                originalFkMemberTypeIdForPeriod + " to " + selectedMemberType.getId() +
                                ". Navigating to RenewMembershipFragment.");

                        String originalMemberTypeName = null;
                        MemberType originalTypeDetails = databaseHelper.getMemberTypeById(originalFkMemberTypeIdForPeriod);
                        if (originalTypeDetails != null) originalMemberTypeName = originalTypeDetails.getName();

                        if (originalMemberTypeName != null && memberTypeSpinnerAdapter != null) {
                            for (int i = 0; i < memberTypeSpinnerAdapter.getCount(); i++) {
                                if (originalMemberTypeName.equals(memberTypeSpinnerAdapter.getItem(i))) {
                                    final AdapterView.OnItemSelectedListener currentListener = membershipTypeSpinner.getOnItemSelectedListener();
                                    membershipTypeSpinner.setOnItemSelectedListener(null); // Temporarily remove
                                    Log.d(TAG, "Navigating away: Setting isProgrammaticSpinnerChange = true before spinner reset");
                                    membershipTypeSpinner.setSelection(i, false);
                                    Log.d(TAG, "Navigating away: Temporarily reset spinner to original type: " + originalMemberTypeName);
                                    // The listener will be re-attached by loadMemberData or if we re-add it here carefully
                                    // For now, allow loadMemberData to re-attach on back navigation.
                                    // Or, re-attach it immediately for other scenarios, but be careful with timing.
                                    // membershipTypeSpinner.setOnItemSelectedListener(currentListener); // Re-attach if needed immediately
                                    break;
                                }
                            }
                        }

                        Bundle bundle = new Bundle();
                        bundle.putString("memberID", currentMemberDetails.getMemberID());
                        // Use the CONSTANT for the key
                        bundle.putInt(RenewMembershipFragment.ARG_SELECTED_MEMBER_TYPE_ID, selectedMemberType.getId());

                        Log.d(TAG, "Navigating with Bundle: memberID=" + currentMemberDetails.getMemberID() +
                                ", " + RenewMembershipFragment.ARG_SELECTED_MEMBER_TYPE_ID + "=" + selectedMemberType.getId());
                        if (navController != null) {
                            navController.navigate(R.id.action_memberEditFragment_to_renewMembershipFragment, bundle);
                        }
                    } else {
                        Log.d(TAG, "MembershipTypeSpinner NAV LISTENER: Selected member type is the same as original. No navigation.");
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        Log.d(TAG, "setupMembershipTypeSpinnerNavigationListener: Listener INSTALLED / RE-INSTALLED.");
    }

    public static void selectSpinnerItemByValue(Spinner spinner, String value) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        if (adapter == null) return;
        for (int position = 0; position < adapter.getCount(); position++) {
            if (adapter.getItem(position).equals(value)) {
                spinner.setSelection(position);
                return;
            }
        }
        // If value not found, select first item or do nothing
        Log.w(TAG, "Value '" + value + "' not found in spinner. Defaulting to first item or current selection.");
    }

    private void setClickListeners() {
        memberImageView.setOnClickListener(v -> showImagePickDialog());

        buttonConfirmEdit.setOnClickListener(v -> attemptUpdateMember());

        buttonCancelEdit.setOnClickListener(v -> {
            if (navController != null) {
                navController.popBackStack();
            }
        });

        buttonDeleteMember.setOnClickListener(v -> confirmDeleteMember());
    }

    private void confirmDeleteMember() {
        // Check against currentMemberDetails for the name, as basic member info is needed.
        // The actual deletion of the member in the DB should handle their periods.
        if (currentMemberDetails == null || currentMemberIdToEdit == null) {
            Toast.makeText(getContext(), "Cannot delete: Member data not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Member")
                .setMessage("Are you sure you want to delete " + currentMemberDetails.getFirstName() + " " +
                        currentMemberDetails.getLastName() + "? This action and all associated membership records cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (databaseHelper != null) {
                        boolean deleted = databaseHelper.deleteMember(currentMemberIdToEdit); // This should delete the member AND their periods
                        if (deleted) {
                            Toast.makeText(getContext(), "Member and their records deleted successfully.", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Member " + currentMemberIdToEdit + " and associated periods/image deleted.");
                            if (originalImageFilePath != null && !originalImageFilePath.isEmpty()) {
                                File imageFile = new File(originalImageFilePath);
                                if (imageFile.exists() && imageFile.isFile()) {
                                    if (imageFile.delete()) {
                                        Log.d(TAG, "Associated image file deleted: " + originalImageFilePath);
                                    } else {
                                        Log.w(TAG, "Failed to delete associated image file: " + originalImageFilePath);
                                    }
                                }
                            }
                            if (navController != null) {
                                navController.popBackStack();
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to delete member from database.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to delete member " + currentMemberIdToEdit + " from database.");
                        }
                    } else {
                        Toast.makeText(getContext(), "Database error. Cannot delete member.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "DatabaseHelper was null when trying to delete member " + currentMemberIdToEdit);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void attemptUpdateMember() {
        // Check if both core member data and the specific period data are loaded
        if (currentMemberDetails == null || currentMembershipPeriodDetails == null) {
            Toast.makeText(getContext(), "Error: Original member or period data not loaded. Cannot update.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "attemptUpdateMember called but currentMemberDetails is " + currentMemberDetails +
                    " or currentMembershipPeriodDetails is " + currentMembershipPeriodDetails);
            return;
        }

        // --- 1. Retrieve Input ---
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        String newReceiptNumber = receiptNumberEditText.getText().toString().trim(); // << NEW

        // --- 2. Validation ---
        boolean isValid = true;
        // Clear previous errors
        firstNameEditText.setError(null);
        lastNameEditText.setError(null);
        ageEditText.setError(null);
        receiptNumberEditText.setError(null); // << NEW
        // Reset spinner backgrounds (ensure getContext() is not null)
        if (getContext() != null) {
            if (defaultGenderSpinnerBackground != null) genderSpinner.setBackground(defaultGenderSpinnerBackground);
            if (defaultMembershipTypeSpinnerBackground != null) membershipTypeSpinner.setBackground(defaultMembershipTypeSpinnerBackground);
        }

        if (TextUtils.isEmpty(firstName)) {
            firstNameEditText.setError("First name cannot be empty.");
            isValid = false;
        }

        if (TextUtils.isEmpty(lastName)) {
            lastNameEditText.setError("Last name cannot be empty.");
            isValid = false;
        }

        int age = 0;
        if (TextUtils.isEmpty(ageStr)) {
            ageEditText.setError("Age cannot be empty.");
            isValid = false;
        } else {
            try {
                age = Integer.parseInt(ageStr);
                if (age <= 0 || age > 120) {
                    ageEditText.setError("Please enter a valid age (1-120).");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                ageEditText.setError("Please enter a valid number for age.");
                isValid = false;
            }
        }

        if (TextUtils.isEmpty(newReceiptNumber)) {
            receiptNumberEditText.setError("Receipt number cannot be empty.");
            isValid = false;
        }

        String gender = "";
        if (genderSpinner.getSelectedItemPosition() == 0) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please select a gender.", Toast.LENGTH_SHORT).show();
                genderSpinner.setBackgroundResource(R.drawable.spinner_error_background);
            }
            isValid = false;
        } else {
            gender = genderSpinner.getSelectedItem().toString();
        }

        int newlySelectedMemberTypeId = -1;
        MemberType newSelectedTypeDetails = null; // Full details of the newly selected type

        if (membershipTypeSpinner.getSelectedItemPosition() == 0 || memberTypeList == null || memberTypeList.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please select a membership type.", Toast.LENGTH_SHORT).show();
                membershipTypeSpinner.setBackgroundResource(R.drawable.spinner_error_background);
            }
            isValid = false;
        } else {
            int selectedSpinnerPosition = membershipTypeSpinner.getSelectedItemPosition();
            if (selectedSpinnerPosition > 0 && (selectedSpinnerPosition - 1) < memberTypeList.size()) {
                newSelectedTypeDetails = memberTypeList.get(selectedSpinnerPosition - 1);
                newlySelectedMemberTypeId = newSelectedTypeDetails.getId();
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Invalid membership type selection.", Toast.LENGTH_SHORT).show();
                    membershipTypeSpinner.setBackgroundResource(R.drawable.spinner_error_background);
                }
                isValid = false;
            }
        }

        if (!isValid) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please correct the errors in the form.", Toast.LENGTH_LONG).show();
            }
            Log.w(TAG, "Initial field validations failed.");
            return;
        }

        // If all initial validations passed, proceed to check receipt if it's not empty
        Log.d(TAG, "Initial field validations passed. Proceeding to check receipt number if provided.");

        // ***** START: RECEIPT NUMBER EXISTENCE CHECK (if newReceiptNumber is not empty) *****
        final String finalNewReceiptNumber = newReceiptNumber;
        // Capture all other necessary variables that would be used in updateMemberData
        final String finalFirstName = firstName;
        final String finalLastName = lastName;
        final int finalAge = age; // Assuming 'age' is correctly parsed from ageStr
        final String finalPhoneNumber = phoneNumber;
        final String finalGender = gender; // Assuming 'gender' is correctly retrieved
        // Ensure newSelectedTypeDetails and newlySelectedMemberTypeId are effectively final or re-fetch
        final MemberType finalNewSelectedTypeDetails = newSelectedTypeDetails; // Capture from a wider scope
        final int finalNewlySelectedMemberTypeId = newlySelectedMemberTypeId; // Capture from a wider scope

        // THE 'if (!TextUtils.isEmpty(finalNewReceiptNumber))' AND ITS 'else' ARE REMOVED
        // The executorService call is now direct:
        executorService.execute(() -> {
            // IMPORTANT: Exclude the current period ID being edited
            int periodIdToExclude = (currentMembershipPeriodDetails != null) ? currentMembershipPeriodDetails.getPeriodId() : -1;
            boolean receiptExists = databaseHelper.isReceiptNumberExists(finalNewReceiptNumber, periodIdToExclude);

            mainThreadHandler.post(() -> {
                if (receiptExists) {
                    receiptNumberEditText.setError("Receipt number already exists for another record.");
                    receiptNumberEditText.requestFocus();
                    Toast.makeText(getContext(), "This receipt number is already in use by another record.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Validation failed: Receipt number '" + finalNewReceiptNumber + "' already exists (excluding current period).");
                } else {
                    // Receipt is unique, proceed with the rest of the update
                    Log.d(TAG, "Receipt number '" + finalNewReceiptNumber + "' is unique. Proceeding to finalize update."); // Updated log
                    updateMemberData(finalFirstName, finalLastName, finalAge, finalPhoneNumber, finalGender,
                            finalNewReceiptNumber, finalNewSelectedTypeDetails, finalNewlySelectedMemberTypeId);
                }
            });
        });
        // ***** END: RECEIPT NUMBER EXISTENCE CHECK *****
    }

    // New method in MemberEditFragment.java to handle the actual updating part
    private void updateMemberData(String firstName, String lastName, int age, String phoneNumber, String gender,
                                  String newReceiptNumber, MemberType newSelectedTypeDetailsFromParam, int newlySelectedMemberTypeIdFromParam) {

        // --- 3. Image Path Handling --- (Your existing code)
        String finalImageFilePath = originalImageFilePath; // Start with existing
        // Your existing image handling logic (copied from your provided code):
        if (newSelectedPhotoPath != null && capturedImageUri != null) {
            finalImageFilePath = newSelectedPhotoPath;
            Log.d(TAG, "Update: Using NEW camera image path: " + finalImageFilePath);
            if (originalImageFilePath != null && !originalImageFilePath.equals(finalImageFilePath) && !originalImageFilePath.isEmpty()) {
                File oldImageFile = new File(originalImageFilePath);
                if (oldImageFile.exists() && oldImageFile.delete()) {
                    Log.d(TAG, "Old image file deleted: " + originalImageFilePath);
                } else if (oldImageFile.exists()) {
                    Log.w(TAG, "Failed to delete old image file: " + originalImageFilePath);
                }
            }
        } else if (newSelectedGalleryImageUri != null) {
            if (getContext() != null) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String imageFileName = "JPEG_EDIT_" + timeStamp + ".jpg";
                String copiedGalleryPath = saveImageFromUriToInternalStorage(newSelectedGalleryImageUri, imageFileName);
                if (copiedGalleryPath != null) {
                    finalImageFilePath = copiedGalleryPath;
                    Log.d(TAG, "Update: Using NEW (copied) gallery image path: " + finalImageFilePath);
                    if (originalImageFilePath != null && !originalImageFilePath.equals(finalImageFilePath) && !originalImageFilePath.isEmpty()) {
                        File oldImageFile = new File(originalImageFilePath);
                        if (oldImageFile.exists() && oldImageFile.delete()) {
                            Log.d(TAG, "Old image file deleted: " + originalImageFilePath);
                        } else if (oldImageFile.exists()) {
                            Log.w(TAG, "Failed to delete old image file: " + originalImageFilePath);
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to copy new gallery image for update. Keeping original image.");
                    Toast.makeText(getContext(), "Error saving new gallery image. Image not changed.", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "Context was null when trying to save gallery image for update.");
            }
        }

        // --- 4. Date and Membership Type Name Handling for the Period --- (Your existing code)
        LocalDate periodRegistrationDateToSave = currentMembershipPeriodDetails.getRegistrationDate();
        LocalDate periodExpirationDateToSave = currentMembershipPeriodDetails.getExpirationDate();
        String periodMemberTypeName = "";
        int actualNewlySelectedMemberTypeId = newlySelectedMemberTypeIdFromParam; // Use the param
        MemberType actualNewSelectedTypeDetails = newSelectedTypeDetailsFromParam; // Use the param

        // Check if the membership type for the period has actually changed
        if (actualNewSelectedTypeDetails != null && actualNewlySelectedMemberTypeId != currentMembershipPeriodDetails.getFkMemberTypeId()) {
            // Type has changed
            periodMemberTypeName = actualNewSelectedTypeDetails.getName();
            if (periodRegistrationDateToSave != null) {
                periodExpirationDateToSave = periodRegistrationDateToSave.plusDays(actualNewSelectedTypeDetails.getDurationDays());
                Log.d(TAG, "Membership type for period changed. New expiration date calculated: " + periodExpirationDateToSave);
            } else {
                Log.w(TAG, "Period's original registration date is null. Cannot accurately calculate new expiration date.");
                periodExpirationDateToSave = LocalDate.now().plusDays(actualNewSelectedTypeDetails.getDurationDays());
            }
        } else if (actualNewSelectedTypeDetails != null) {
            // Type did NOT change, but we still need its name
            periodMemberTypeName = actualNewSelectedTypeDetails.getName();
        } else {
            // Fallback (your existing logic)
            Log.e(TAG, "Error: newSelectedTypeDetails is null after validation passed. Using original type name for period.");
            MemberType originalType = databaseHelper.getMemberTypeById(currentMembershipPeriodDetails.getFkMemberTypeId());
            if (originalType != null) {
                periodMemberTypeName = originalType.getName();
                actualNewlySelectedMemberTypeId = originalType.getId();
            } else {
                Log.e(TAG, "Could not retrieve original member type details for period.");
                periodMemberTypeName = "Unknown";
                actualNewlySelectedMemberTypeId = currentMembershipPeriodDetails.getFkMemberTypeId();
            }
        }

        // --- 5. Create/Update Member and MembershipPeriod Objects --- (Your existing code)
        currentMemberDetails.setFirstName(firstName);
        currentMemberDetails.setLastName(lastName);
        currentMemberDetails.setPhoneNumber(phoneNumber);
        currentMemberDetails.setGender(gender);
        currentMemberDetails.setAge(age);
        currentMemberDetails.setImageFilePath(finalImageFilePath);

        currentMembershipPeriodDetails.setFkMemberTypeId(actualNewlySelectedMemberTypeId);
        currentMembershipPeriodDetails.setMemberTypeName(periodMemberTypeName);
        currentMembershipPeriodDetails.setExpirationDate(periodExpirationDateToSave);
        currentMembershipPeriodDetails.setReceiptNumber(newReceiptNumber);

        // --- 6. Update in Database --- (Your existing code)
        boolean success = false;
        if (databaseHelper != null) {
            success = databaseHelper.updateMemberAndPeriod(currentMemberDetails, currentMembershipPeriodDetails);
        } else {
            // ... (your existing DB null error handling) ...
            return;
        }

        // --- 7. User Feedback and Navigation --- (Your existing code)
        if (success) {
            // ... (your existing success feedback and navigation) ...
        } else {
            // ... (your existing failure feedback) ...
        }
    }

    private void showImagePickDialog() {
        if (getContext() == null) return;

        CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Member Image");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                checkCameraPermissionAndOpenCamera();
            } else if (options[item].equals("Choose from Gallery")) {
                checkStoragePermissionAndOpenGallery();
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    // Check for camera permission and then open the camera
    private void checkCameraPermissionAndOpenCamera() {
        if (getContext() == null) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            // Request camera permission
            // If you want to explain why you need permission, do it before requesting.
            requestPermissionsLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
    }

    private void openCamera() {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot open camera.");
            Toast.makeText(getActivity(), "Error: Cannot access camera.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile(); // This sets newSelectedPhotoPath
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "IOException in createImageFile()", ex);
                Toast.makeText(getContext(), "Error creating image file.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                // IMPORTANT: 'capturedImageUri' will store the content URI provided by FileProvider
                // 'newSelectedPhotoPath' will store the absolute file system path to the image.
                capturedImageUri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".provider", // Make sure this matches your FileProvider authority
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
                Log.d(TAG, "Launching camera. Output URI: " + capturedImageUri + " | Expected file path: " + newSelectedPhotoPath);
                takePictureLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(getContext(), "No camera app found.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No activity found to handle ACTION_IMAGE_CAPTURE intent.");
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_EDIT_" + timeStamp + "_"; // "EDIT" to differentiate if needed
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir == null) {
            Log.e(TAG, "External storage directory (Pictures) is null.");
            throw new IOException("Cannot access external storage for pictures.");
        }
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e(TAG, "Failed to create Pictures directory: " + storageDir.getAbsolutePath());
            throw new IOException("Failed to create directory for pictures.");
        }

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        newSelectedPhotoPath = image.getAbsolutePath(); // Store the absolute path
        Log.d(TAG, "Image file created: " + newSelectedPhotoPath);
        return image;
    }

    // Check for storage permission and then open the gallery
    private void checkStoragePermissionAndOpenGallery() {
        if (getContext() == null) return;

        String permissionToRequest;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionToRequest = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permissionToRequest = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permissionToRequest) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            // Request storage permission
            requestPermissionsLauncher.launch(new String[]{permissionToRequest});
        }
    }

    // Method to open the gallery
    private void openGallery() {
        // newSelectedPhotoPath and capturedImageUri should be nulled here or before calling,
        // as gallery selection gives a content URI, not a direct file path initially.
        // The ActivityResultLauncher for picking image already handles nulling these.
        pickImageLauncher.launch("image/*"); // "image/*" type for gallery
    }

    private String saveImageFromUriToInternalStorage(Uri contentUri, String desiredFileName) {
        if (getContext() == null || contentUri == null) {
            Log.e(TAG, "Cannot save image from URI: context or URI is null.");
            return null;
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        File outputFile = null;

        try {
            inputStream = requireContext().getContentResolver().openInputStream(contentUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + contentUri);
                return null;
            }

            // Path: /data/user/0/com.example.artsgymapp_solo/files/images/your_image.jpg
            // Using a subdirectory "images" within getFilesDir()
            File internalImageDir = new File(requireContext().getFilesDir(), "images");
            if (!internalImageDir.exists()) {
                if (!internalImageDir.mkdirs()) {
                    Log.e(TAG, "Failed to create internal 'images' directory.");
                    return null;
                }
            }

            outputFile = new File(internalImageDir, desiredFileName);
            outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            Log.d(TAG, "Image successfully copied from gallery to internal storage: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath(); // Return the absolute path of the new file

        } catch (IOException e) {
            Log.e(TAG, "Error saving image from URI to internal storage", e);
            if (outputFile != null && outputFile.exists()) { // Clean up if partially created file
                outputFile.delete();
            }
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}