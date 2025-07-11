package com.example.artsgymapp_solo;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import android.graphics.drawable.Drawable;
import android.widget.AdapterView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddMemberFragment extends Fragment
{
    private static final String TAG = "AddMemberFragment";

    private int activeImageTarget = 1;

    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText ageEditText;
    private Spinner genderSpinner;
    private EditText phoneNumberEditText;
    private Spinner memberTypeSpinner;
    private ImageView memberPreviewImageView;
    private MaterialButton buttonAddMember;

    private LinearLayout secondMemberContainer;
    private EditText firstNameEditText2;
    private EditText lastNameEditText2;
    private EditText ageEditText2;
    private Spinner genderSpinner2;
    private EditText phoneNumberEditText2;
    private ImageView memberPreviewImageView2;

    private DatabaseHelper databaseHelper;

    private String currentPhotoPath1;
    private Uri capturedImageUri1;
    private Uri selectedImageUri1;

    private String currentPhotoPath2;
    private Uri capturedImageUri2;
    private Uri selectedImageUri2;

    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    // To store default spinner backgrounds
    private Drawable defaultGenderSpinnerBackground;
    private Drawable defaultMemberTypeSpinnerBackground;

    private EditText receiptNumberEditText;
    private TextView startDateTextView;
    private LocalDate selectedRegistrationDate;
    private TextView expirationDateTextView;
    private LocalDate selectedExpirationDate;
    private DateTimeFormatter uiDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private List<MemberType> memberTypeList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(getContext());

        // Initialize Permission Launcher
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                    boolean readStorageGranted = false;
                    String readPermissionNeeded;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        readPermissionNeeded = Manifest.permission.READ_MEDIA_IMAGES;
                        readStorageGranted = permissions.getOrDefault(readPermissionNeeded, false);
                    } else {
                        readPermissionNeeded = Manifest.permission.READ_EXTERNAL_STORAGE;
                        readStorageGranted = permissions.getOrDefault(readPermissionNeeded, false);
                    }

                    if (permissions.containsKey(Manifest.permission.CAMERA)) { // If camera permission was part of the request
                        if (cameraGranted) {
                            Log.d(TAG, "Camera permission granted. Opening camera.");
                            openCamera(); // Proceed to open camera
                        } else {
                            Toast.makeText(getContext(), "Camera permission is required to take photos.", Toast.LENGTH_LONG).show();
                        }
                    }

                    if (permissions.containsKey(readPermissionNeeded)) { // If storage permission was part of the request
                        if (readStorageGranted) {
                            Log.d(TAG, "Storage permission granted. Opening gallery.");
                            openGallery(); // Proceed to open gallery
                        } else {
                            Toast.makeText(getContext(), "Storage permission is required to choose from gallery.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        // Initialize Take Picture Launcher (Keep your existing initialization)
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (activeImageTarget == 1) {
                            selectedImageUri1 = null; // Clear any gallery selection for Member 1
                            if (capturedImageUri1 != null && memberPreviewImageView != null) {
                                Glide.with(AddMemberFragment.this).load(capturedImageUri1).into(memberPreviewImageView);
                                Log.d(TAG, "Image captured for Member 1: " + capturedImageUri1.toString() + (currentPhotoPath1 != null ? " | Path: " + currentPhotoPath1 : ""));
                            } else {
                                Log.e(TAG, "capturedImageUri1 or memberPreviewImageView is null after taking picture for Member 1.");
                                if (capturedImageUri1 == null) Log.e(TAG, "CapturedImageUri1 is null");
                                // Optionally show a toast
                            }
                        } else { // activeImageTarget == 2
                            selectedImageUri2 = null; // Clear any gallery selection for Member 2
                            if (capturedImageUri2 != null && memberPreviewImageView2 != null) {
                                Glide.with(AddMemberFragment.this).load(capturedImageUri2).into(memberPreviewImageView2);
                                Log.d(TAG, "Image captured for Member 2: " + capturedImageUri2.toString() + (currentPhotoPath2 != null ? " | Path: " + currentPhotoPath2 : ""));
                            } else {
                                Log.e(TAG, "capturedImageUri2 or memberPreviewImageView2 is null after taking picture for Member 2.");
                                if (capturedImageUri2 == null) Log.e(TAG, "CapturedImageUri2 is null");
                                // Optionally show a toast
                            }
                        }
                    } else { // Result is not OK (cancelled or failed)
                        Log.d(TAG, "Image capture cancelled/failed. Target: " + activeImageTarget + ", Result code: " + result.getResultCode());
                        if (activeImageTarget == 1) {
                            if (currentPhotoPath1 != null) {
                                File photoFile = new File(currentPhotoPath1);
                                if (photoFile.exists() && photoFile.length() == 0) {
                                    if(photoFile.delete()){ Log.d(TAG, "Empty photo file deleted for Member 1: " + currentPhotoPath1); }
                                    else { Log.d(TAG, "Failed to delete empty photo file for Member 1: " + currentPhotoPath1); }
                                }
                            }
                            currentPhotoPath1 = null;
                            capturedImageUri1 = null;
                        } else { // activeImageTarget == 2
                            if (currentPhotoPath2 != null) {
                                File photoFile = new File(currentPhotoPath2);
                                if (photoFile.exists() && photoFile.length() == 0) {
                                    if(photoFile.delete()){ Log.d(TAG, "Empty photo file deleted for Member 2: " + currentPhotoPath2); }
                                    else { Log.d(TAG, "Failed to delete empty photo file for Member 2: " + currentPhotoPath2); }
                                }
                            }
                            currentPhotoPath2 = null;
                            capturedImageUri2 = null;
                        }
                    }
                });

        // Initialize Pick Image From Gallery Launcher (Keep your existing initialization)
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        if (activeImageTarget == 1) {
                            selectedImageUri1 = uri;
                            capturedImageUri1 = null; // Clear any camera selection for Member 1
                            currentPhotoPath1 = null; // Gallery URIs are not direct file paths
                            if (memberPreviewImageView != null) {
                                Glide.with(AddMemberFragment.this).load(selectedImageUri1).into(memberPreviewImageView);
                            }
                            Log.d(TAG, "Image selected from gallery for Member 1: " + selectedImageUri1.toString());
                        } else { // activeImageTarget == 2
                            selectedImageUri2 = uri;
                            capturedImageUri2 = null; // Clear any camera selection for Member 2
                            currentPhotoPath2 = null;
                            if (memberPreviewImageView2 != null) {
                                Glide.with(AddMemberFragment.this).load(selectedImageUri2).into(memberPreviewImageView2);
                            }
                            Log.d(TAG, "Image selected from gallery for Member 2: " + selectedImageUri2.toString());
                        }
                    } else {
                        Log.d(TAG, "No image selected from gallery for target: " + activeImageTarget);
                    }
                });
    }

    // --- onCreateView (MODIFIED to call showImagePickDialog) ---
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_addmember, container, false);

        firstNameEditText = view.findViewById(R.id.firstNameEditText);
        lastNameEditText = view.findViewById(R.id.lastNameEditText);
        ageEditText = view.findViewById(R.id.ageEditText);
        genderSpinner = view.findViewById(R.id.genderSpinner2ndMember);
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        memberPreviewImageView = view.findViewById(R.id.memberPictureImageView);

        memberTypeSpinner = view.findViewById(R.id.memberTypeSpinner1);
        receiptNumberEditText = view.findViewById(R.id.receiptNumberEditText);
        startDateTextView = view.findViewById(R.id.startDateTextView);
        expirationDateTextView = view.findViewById(R.id.expirationDateTextView);

        secondMemberContainer = view.findViewById(R.id.secondMemberContainer);

        firstNameEditText2 = view.findViewById(R.id.firstNameEditText2);
        lastNameEditText2 = view.findViewById(R.id.lastNameEditText2);
        ageEditText2 = view.findViewById(R.id.ageEditText2);
        genderSpinner2 = view.findViewById(R.id.genderSpinner2);
        phoneNumberEditText2 = view.findViewById(R.id.phoneNumberEditText2);
        memberPreviewImageView2 = view.findViewById(R.id.memberPreviewImageView2);

        buttonAddMember = view.findViewById(R.id.buttonAddMember);

        if (genderSpinner != null)
        {
            defaultGenderSpinnerBackground = genderSpinner.getBackground();
        }
        if (memberTypeSpinner != null) {
            defaultMemberTypeSpinnerBackground = memberTypeSpinner.getBackground();
        }

        setupSpinners();
        setupSpinnerErrorClearing();
        setupDatePickers();

        //adding image button
        memberPreviewImageView.setOnClickListener(v -> {
            Log.d(TAG, "Add image container clicked");
            this.activeImageTarget = 1;
            showImagePickDialog();
        });

        memberPreviewImageView2.setOnClickListener(v -> {
            Log.d(TAG, "Add image container clicked");
            this.activeImageTarget = 2;
            showImagePickDialog();
        });

        if (buttonAddMember != null) {
            buttonAddMember.setOnClickListener(v ->
            {
                Log.d(TAG, "Add member button clicked");
                attemptSaveMember();
            });
        }
        return view;
    }

    private void setupDatePickers() {
        if (startDateTextView != null) {
            startDateTextView.setOnClickListener(v -> {
                final java.util.Calendar c = java.util.Calendar.getInstance();
                int year, month, day;

                // Use selectedRegistrationDate if already picked, otherwise current date
                if (selectedRegistrationDate != null) {
                    year = selectedRegistrationDate.getYear();
                    month = selectedRegistrationDate.getMonthValue() - 1;
                    day = selectedRegistrationDate.getDayOfMonth();
                } else {
                    year = c.get(java.util.Calendar.YEAR);
                    month = c.get(java.util.Calendar.MONTH);
                    day = c.get(java.util.Calendar.DAY_OF_MONTH);
                }

                android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                        requireContext(),
                        (view, yearSelected, monthOfYear, dayOfMonth) -> {
                            selectedRegistrationDate = LocalDate.of(yearSelected, monthOfYear + 1, dayOfMonth);
                            startDateTextView.setText(selectedRegistrationDate.format(uiDateFormatter));
                            Log.d(TAG, "Registration Date selected: " + selectedRegistrationDate);
                            // When start date changes, auto-calculate a new default expiration date
                            autoCalculateAndSetDefaultExpirationDate();
                        }, year, month, day);
                datePickerDialog.show();
            });
        }

        // ++ NEW: Setup click listener for expirationDateTextView ++
        if (expirationDateTextView != null) {
            expirationDateTextView.setOnClickListener(v -> {
                final java.util.Calendar c = java.util.Calendar.getInstance();
                int year, month, day;

                // Use selectedExpirationDate if already picked, otherwise suggest a date based on start date or current
                if (selectedExpirationDate != null) {
                    year = selectedExpirationDate.getYear();
                    month = selectedExpirationDate.getMonthValue() - 1;
                    day = selectedExpirationDate.getDayOfMonth();
                } else if (selectedRegistrationDate != null) {
                    // Default to one month after start date if expiration not yet set
                    LocalDate suggestedExp = selectedRegistrationDate.plusMonths(1);
                    year = suggestedExp.getYear();
                    month = suggestedExp.getMonthValue() - 1;
                    day = suggestedExp.getDayOfMonth();
                } else {
                    year = c.get(java.util.Calendar.YEAR);
                    month = c.get(java.util.Calendar.MONTH);
                    day = c.get(java.util.Calendar.DAY_OF_MONTH);
                }

                android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                        requireContext(),
                        (view, yearSelected, monthOfYear, dayOfMonth) -> {
                            selectedExpirationDate = LocalDate.of(yearSelected, monthOfYear + 1, dayOfMonth);
                            expirationDateTextView.setText(selectedExpirationDate.format(uiDateFormatter));
                            Log.d(TAG, "Expiration Date MANUALLY selected: " + selectedExpirationDate);
                        }, year, month, day);

                // Optional: Set a minimum date for the expiration picker (e.g., cannot be before start date) //THIS IS ZAMN
                if (selectedRegistrationDate != null) {
                    c.set(selectedRegistrationDate.getYear(), selectedRegistrationDate.getMonthValue() -1, selectedRegistrationDate.getDayOfMonth());
                    datePickerDialog.getDatePicker().setMinDate(c.getTimeInMillis());
                }
                datePickerDialog.show();
            });
        }
    }

    private void autoCalculateAndSetDefaultExpirationDate() {
        if (expirationDateTextView == null || selectedRegistrationDate == null) {
            if (expirationDateTextView != null) {
                expirationDateTextView.setText("Select End Date"); // Or your new hint
            }
            selectedExpirationDate = null; // Clear any previously set expiration date
            return;
        }

        int selectedSpinnerPosition = memberTypeSpinner.getSelectedItemPosition();
        if (selectedSpinnerPosition == 0 || memberTypeList == null || memberTypeList.isEmpty()) {
            expirationDateTextView.setText("Select type for auto-calc"); // Hint
            selectedExpirationDate = null;
            return;
        }

        int actualListPosition = selectedSpinnerPosition - 1;
        LocalDate calculatedExpDate = null;

        if (actualListPosition >= 0 && actualListPosition < memberTypeList.size()) {
            MemberType selectedType = memberTypeList.get(actualListPosition);
            int durationDays = selectedType.getDurationDays();

            if (durationDays > 0) {
                calculatedExpDate = selectedRegistrationDate.plusDays(durationDays); // Simplest default for now
            }
        }

        if (calculatedExpDate != null) {
            selectedExpirationDate = calculatedExpDate; // Store the calculated date
            expirationDateTextView.setText(selectedExpirationDate.format(uiDateFormatter));
            Log.d(TAG, "Default Expiration Date auto-calculated: " + selectedExpirationDate);
        } else {
            expirationDateTextView.setText("Select End Date"); // Default if no calculation
            selectedExpirationDate = null;
            Log.d(TAG, "Could not auto-calculate default expiration date.");
        }
    }

    private void setupSpinners() {
        // IMPORTANT: Ensure your R.array.gender_array and R.array.member_types_array
        // have a prompt like "Select Gender" or "Select Membership Type" as the FIRST item (index 0).
        if (getContext() == null) return;

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.gender_array, android.R.layout.simple_spinner_item); // Use your array with the prompt

        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);
        genderSpinner2.setAdapter(genderAdapter);

        // Populate member type spinner
        memberTypeList = databaseHelper.getAllMemberTypes();
        List<MemberType> displayMemberTypes = new ArrayList<>();

        displayMemberTypes.add(new MemberType(0, "Select Membership Type*", 0, false));
        displayMemberTypes.addAll(memberTypeList);

        ArrayAdapter<MemberType> memberTypeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, displayMemberTypes);
        memberTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        memberTypeSpinner.setAdapter(memberTypeAdapter);
    }

    private void setupSpinnerErrorClearing() {
        // Listener for genderSpinner (to clear error background if a valid selection is made)
        if (genderSpinner != null) {
            genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Position 0 is usually the prompt like "Select Gender"
                    if (position > 0) {
                        // If a valid gender is selected, reset its background to default
                        genderSpinner.setBackground(defaultGenderSpinnerBackground);
                    }
                    // No action needed if the prompt is re-selected, error is set during validation
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Can be left empty or you might want to ensure a default state
                }
            });
        }

        // Listener for memberTypeSpinner
        if (memberTypeSpinner != null) {
            memberTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) { // Valid type selected
                        memberTypeSpinner.setBackground(defaultMemberTypeSpinnerBackground);
                        selectedRegistrationDate = LocalDate.now();
                        if (startDateTextView != null) {
                            startDateTextView.setText(selectedRegistrationDate.format(uiDateFormatter));
                            Log.d(TAG, "Member Type selected. Start date auto-set to: " + selectedRegistrationDate);
                        }
                        autoCalculateAndSetDefaultExpirationDate();

                        // --- NEW: Handle isTwoInOne ---
                        MemberType selectedType = null;
                        if (parent.getItemAtPosition(position) instanceof MemberType) {
                            selectedType = (MemberType) parent.getItemAtPosition(position);
                        }

                        if (selectedType != null && secondMemberContainer != null) {
                            if (selectedType.isTwoInOne()) { // Make sure MemberType has isTwoInOne()
                                secondMemberContainer.setVisibility(View.VISIBLE);
                                Log.d(TAG, "2-in-1 Membership selected. Showing second member fields.");
                            } else {
                                secondMemberContainer.setVisibility(View.GONE);
                                Log.d(TAG, "Standard Membership selected. Hiding second member fields.");
                                // Optionally clear fields for the second member if they were previously shown
                                // clearSecondMemberForm(); // You might want to create this method
                            }
                        }
                        // --- END NEW ---

                    } else { // Prompt selected
                        memberTypeSpinner.setBackground(defaultMemberTypeSpinnerBackground);
                        // ... (rest of your existing logic for clearing dates) ...
                        if (secondMemberContainer != null) {
                            secondMemberContainer.setVisibility(View.GONE); // Hide if prompt is selected
                            // clearSecondMemberForm();
                        }
                        Log.d(TAG, "Member Type prompt selected. Dates cleared, second member fields hidden.");
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // If nothing is selected (e.g., spinner is cleared programmatically)
                    if (startDateTextView != null) {
                        startDateTextView.setText("");
                    }
                    if (expirationDateTextView != null) {
                        expirationDateTextView.setText("");
                    }
                    selectedRegistrationDate = null;
                    selectedExpirationDate = null;
                }
            });
        }
    }

    // --- METHOD 1: Dialog to choose image source ---
    private void showImagePickDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> { // Using lambda for DialogInterface.OnClickListener
            if (options[which].equals("Take Photo")) {
                checkCameraPermissionAndOpenCamera();
            } else if (options[which].equals("Choose from Gallery")) {
                checkStoragePermissionAndOpenGallery();
            } else if (options[which].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpenCamera() {
        String[] permissionsToRequest = {Manifest.permission.CAMERA};

        // Check if all declared permissions in permissionsToRequest are granted
        boolean allPermissionsGranted = true;
        for (String perm : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            Log.d(TAG, "Camera permission already granted. Opening camera.");
            openCamera();
        } else {
            // Request permissions. The result is handled in the 'requestPermissionsLauncher' callback.
            Log.d(TAG, "Requesting camera permissions.");
            requestPermissionsLauncher.launch(permissionsToRequest);
        }
    }

    // --- METHOD 3: Create File for Camera Image and Launch Camera Intent ---
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                // createImageFile() will now use activeImageTarget to set
                // either currentPhotoPath1 or currentPhotoPath2
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "IOException creating image file for camera", ex);
                Toast.makeText(getContext(), "Could not create image file.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI;
                if (getContext() == null) { // Basic null check for context
                    Log.e(TAG, "Context is null in openCamera before FileProvider.getUriForFile");
                    return;
                }

                // Get a content URI for the file using FileProvider
                // The authority should be consistent.
                String authority = requireContext().getPackageName() + ".provider";

                if (activeImageTarget == 1) {
                    capturedImageUri1 = FileProvider.getUriForFile(requireContext(), authority, photoFile);
                    photoURI = capturedImageUri1;
                    Log.d(TAG, "Launching camera for Member 1. Output URI: " + (photoURI != null ? photoURI.toString() : "null"));
                } else { // activeImageTarget == 2
                    capturedImageUri2 = FileProvider.getUriForFile(requireContext(), authority, photoFile);
                    photoURI = capturedImageUri2;
                    Log.d(TAG, "Launching camera for Member 2. Output URI: " + (photoURI != null ? photoURI.toString() : "null"));
                }

                if (photoURI != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    takePictureLauncher.launch(takePictureIntent);
                } else {
                    Log.e(TAG, "photoURI is null after FileProvider.getUriForFile for activeTarget: " + activeImageTarget);
                    Toast.makeText(getContext(), "Failed to prepare image for camera.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.e(TAG, "No camera app available to handle intent.");
            Toast.makeText(getContext(), "No camera app found.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- METHOD 4: Create an Image File with a unique name ---
    private File createImageFile() throws IOException {
        // Create an image file name using a timestamp to ensure uniqueness
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory for images: " + storageDir.getAbsolutePath());
                throw new IOException("Failed to create directory: " + storageDir.getAbsolutePath());
            }
        }

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path based on activeImageTarget
        if (activeImageTarget == 1) {
            currentPhotoPath1 = image.getAbsolutePath();
            Log.d(TAG, "Image file created for Member 1: " + currentPhotoPath1);
        } else { // activeImageTarget == 2
            currentPhotoPath2 = image.getAbsolutePath();
            Log.d(TAG, "Image file created for Member 2: " + currentPhotoPath2);
        }
        return image;
    }

    // --- METHOD 5: Check Storage Permissions and then Open Gallery ---
    private void checkStoragePermissionAndOpenGallery() {
        String permissionNeeded;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 (API 33) and above, use granular media permissions
            permissionNeeded = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            // For older versions, use READ_EXTERNAL_STORAGE
            permissionNeeded = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permissionNeeded) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Storage permission already granted. Opening gallery.");
            openGallery();
        } else {
            // Request the permission. The result is handled in the 'requestPermissionsLauncher' callback.
            Log.d(TAG, "Requesting storage permission: " + permissionNeeded);
            requestPermissionsLauncher.launch(new String[]{permissionNeeded});
        }
    }

    // --- METHOD 6: Launch Gallery Intent ---
    private void openGallery()
    {
        Log.d(TAG, "Launching gallery.");
        pickImageLauncher.launch("image/*"); // MIME type for images
    }

    // --- METHOD 7: Attempt to Save Member Data ---
    // In AddMemberFragment.java

    private void attemptSaveMember() {
        Log.d(TAG, "Attempting to save member...");

        // --- Clear previous errors for Member 1 ---
        if (firstNameEditText != null) firstNameEditText.setError(null);
        if (lastNameEditText != null) lastNameEditText.setError(null);
        if (ageEditText != null) ageEditText.setError(null);
        if (receiptNumberEditText != null) receiptNumberEditText.setError(null);
        if (genderSpinner != null && defaultGenderSpinnerBackground != null) genderSpinner.setBackground(defaultGenderSpinnerBackground);
        if (memberTypeSpinner != null && defaultMemberTypeSpinnerBackground != null) memberTypeSpinner.setBackground(defaultMemberTypeSpinnerBackground);
        // For Member 2, errors will be cleared specifically if it's a 2-in-1 flow.

        // --- Validate Member 1 ---
        String firstName1 = firstNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(firstName1)) {
            firstNameEditText.setError("First name is required");
            firstNameEditText.requestFocus();
            Log.w(TAG, "Validation failed (M1): First name empty.");
            return;
        }

        String lastName1 = lastNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(lastName1)) {
            lastNameEditText.setError("Last name is required");
            lastNameEditText.requestFocus();
            Log.w(TAG, "Validation failed (M1): Last name empty.");
            return;
        }

        String ageStr1 = ageEditText.getText().toString().trim();
        int age1 = 0;
        if (TextUtils.isEmpty(ageStr1)) {
            ageEditText.setError("Age is required");
            ageEditText.requestFocus();
            Log.w(TAG, "Validation failed (M1): Age empty.");
            return;
        } else {
            try {
                age1 = Integer.parseInt(ageStr1);
                if (age1 <= 0 || age1 > 120) {
                    ageEditText.setError("Enter a valid age (1-120)");
                    ageEditText.requestFocus();
                    Log.w(TAG, "Validation failed (M1): Age out of range.");
                    return;
                }
            } catch (NumberFormatException e) {
                ageEditText.setError("Enter a valid age (number)");
                ageEditText.requestFocus();
                Log.w(TAG, "Validation failed (M1): Age not a number.");
                return;
            }
        }

        String phoneNumber1 = phoneNumberEditText.getText().toString().trim(); // Optional for M1

        String receiptNumberStr = receiptNumberEditText.getText().toString().trim();
        if (TextUtils.isEmpty(receiptNumberStr)) {
            receiptNumberEditText.setError("Receipt number is required");
            receiptNumberEditText.requestFocus();
            Log.w(TAG, "Validation failed: Receipt number empty.");
            return;
        }

        String gender1;
        if (genderSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Please select a gender.", Toast.LENGTH_SHORT).show(); // General message
            genderSpinner.setBackgroundResource(R.drawable.spinner_error_background);
            // genderSpinner.requestFocus(); // Focus might not work as well as highlight
            Log.w(TAG, "Validation failed (M1): Gender not selected.");
            return;
        } else {
            gender1 = genderSpinner.getSelectedItem().toString();
        }

        int selectedMemberTypeIdFromSpinner = -1;
        MemberType selectedTypeObject = null; // To get the MemberType object itself
        if (memberTypeSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Please select a membership type.", Toast.LENGTH_SHORT).show();
            memberTypeSpinner.setBackgroundResource(R.drawable.spinner_error_background);
            Log.w(TAG, "Validation failed: Membership type not selected.");
            return;
        } else {
            int actualListPosition = memberTypeSpinner.getSelectedItemPosition() - 1;
            if (memberTypeList != null && actualListPosition >= 0 && actualListPosition < memberTypeList.size()) {
                selectedTypeObject = memberTypeList.get(actualListPosition);
                selectedMemberTypeIdFromSpinner = selectedTypeObject.getId();
            } else {
                Toast.makeText(getContext(), "Invalid membership type selected.", Toast.LENGTH_SHORT).show();
                memberTypeSpinner.setBackgroundResource(R.drawable.spinner_error_background);
                Log.w(TAG, "Validation failed: Invalid membership type index.");
                return;
            }
        }

        // --- Date Validations (common for both members in a 2-in-1) ---
        if (selectedRegistrationDate == null) {
            Toast.makeText(getContext(), "Please select or confirm a registration date.", Toast.LENGTH_LONG).show();
            // Optionally, highlight startDateTextView
            Log.w(TAG, "Validation failed: Registration date is null.");
            return;
        }
        if (selectedExpirationDate == null) {
            Toast.makeText(getContext(), "Please select or confirm an expiration date.", Toast.LENGTH_LONG).show();
            // Optionally, highlight expirationDateTextView
            Log.w(TAG, "Validation failed: Expiration date is null.");
            return;
        }
        if (selectedExpirationDate.isBefore(selectedRegistrationDate)) {
            Toast.makeText(getContext(), "Expiration date cannot be before the registration date.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "Validation failed: Expiration date before registration date.");
            return;
        }

        // --- Image Path Processing for Member 1 ---
        String imagePathToSave1 = null;
        if (capturedImageUri1 != null && currentPhotoPath1 != null && !currentPhotoPath1.isEmpty()) {
            imagePathToSave1 = currentPhotoPath1;
        } else if (selectedImageUri1 != null) {
            if (getContext() != null) {
                String timeStamp1 = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_M1", java.util.Locale.getDefault()).format(new java.util.Date());
                String imageFileName1 = "MEMBER_IMG_" + timeStamp1 + ".jpg";
                imagePathToSave1 = saveImageFromUriToInternalStorage(selectedImageUri1, imageFileName1);
                if (imagePathToSave1 == null /* && image is mandatory for M1 */) {
                    Toast.makeText(getContext(), "Failed to save image for Member 1.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Image processing failed for Member 1 (Gallery).");
                    return;
                }
            } else {
                Log.e(TAG, "Context is null, cannot save gallery image for Member 1.");
                Toast.makeText(getActivity(), "Error saving image for Member 1.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        // else: No image selected or captured for M1. If mandatory, add a check here.


        // --- Determine if 2-in-1 and Validate Member 2 ---
        // boolean isTwoInOneMembership = isIsTwoInOneMembership(); // Using the method you had
        // Or, more directly if selectedTypeObject is reliable:
        boolean isTwoInOneMembership = selectedTypeObject != null && selectedTypeObject.isTwoInOne();


        String firstName2 = null;
        String lastName2 = null;
        int age2 = 0;
        String gender2 = null;
        String phoneNumber2 = null;
        String imagePathToSave2 = null;

        if (isTwoInOneMembership) {
            Log.d(TAG, "2-in-1 membership: Validating Member 2 details...");

            // Clear previous errors for Member 2 fields
            if (firstNameEditText2 != null) firstNameEditText2.setError(null);
            if (lastNameEditText2 != null) lastNameEditText2.setError(null);
            if (ageEditText2 != null) ageEditText2.setError(null);
            if (genderSpinner2 != null && defaultGenderSpinnerBackground != null) genderSpinner2.setBackground(defaultGenderSpinnerBackground);


            firstName2 = firstNameEditText2.getText().toString().trim();
            if (TextUtils.isEmpty(firstName2)) {
                firstNameEditText2.setError("First name is required"); // Same error message as M1
                firstNameEditText2.requestFocus();
                Log.w(TAG, "Validation failed (M2): First name empty.");
                return;
            }

            lastName2 = lastNameEditText2.getText().toString().trim();
            if (TextUtils.isEmpty(lastName2)) {
                lastNameEditText2.setError("Last name is required"); // Same error message
                lastNameEditText2.requestFocus();
                Log.w(TAG, "Validation failed (M2): Last name empty.");
                return;
            }

            String ageStr2 = ageEditText2.getText().toString().trim();
            if (TextUtils.isEmpty(ageStr2)) {
                ageEditText2.setError("Age is required"); // Same error message
                ageEditText2.requestFocus();
                Log.w(TAG, "Validation failed (M2): Age empty.");
                return;
            } else {
                try {
                    age2 = Integer.parseInt(ageStr2);
                    if (age2 <= 0 || age2 > 120) {
                        ageEditText2.setError("Enter a valid age (1-120)"); // Same error message
                        ageEditText2.requestFocus();
                        Log.w(TAG, "Validation failed (M2): Age out of range.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    ageEditText2.setError("Enter a valid age (number)"); // Same error message
                    ageEditText2.requestFocus();
                    Log.w(TAG, "Validation failed (M2): Age not a number.");
                    return;
                }
            }

            if (genderSpinner2.getSelectedItemPosition() == 0) {
                Toast.makeText(getContext(), "Please select a gender.", Toast.LENGTH_SHORT).show(); // Same Toast
                genderSpinner2.setBackgroundResource(R.drawable.spinner_error_background);
                Log.w(TAG, "Validation failed (M2): Gender not selected.");
                return;
            } else {
                gender2 = genderSpinner2.getSelectedItem().toString();
            }

            phoneNumber2 = phoneNumberEditText2.getText().toString().trim(); // Optional for M2

            // --- Image Path Processing for Member 2 ---
            if (capturedImageUri2 != null && currentPhotoPath2 != null && !currentPhotoPath2.isEmpty()) {
                imagePathToSave2 = currentPhotoPath2;
            } else if (selectedImageUri2 != null) {
                if (getContext() != null) {
                    String timeStamp2 = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_M2", java.util.Locale.getDefault()).format(new java.util.Date());
                    String imageFileName2 = "MEMBER_IMG_" + timeStamp2 + ".jpg";
                    imagePathToSave2 = saveImageFromUriToInternalStorage(selectedImageUri2, imageFileName2);
                    if (imagePathToSave2 == null /* && image is mandatory for M2 */) {
                        Toast.makeText(getContext(), "Failed to save image for Member 2.", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Image processing failed for Member 2 (Gallery).");
                        return;
                    }
                } else {
                    Log.e(TAG, "Context is null, cannot save gallery image for Member 2.");
                    Toast.makeText(getActivity(), "Error saving image for Member 2.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            // else: No image selected or captured for M2. If mandatory, add a check here.
            Log.d(TAG, "Member 2 preliminary validation and image processing passed (if applicable).");
        }

        Log.d(TAG, "All form validations passed. Proceeding to receipt check and save.");

        // Final versions for lambda and passing to saveMemberData
        final String finalFirstName1 = firstName1;
        final String finalLastName1 = lastName1;
        final String finalPhoneNumber1 = phoneNumber1;
        final String finalGender1 = gender1;
        final int finalAge1 = age1;
        final String finalImagePath1 = imagePathToSave1;

        final int finalSelectedMemberTypeId = selectedMemberTypeIdFromSpinner; // Use the ID from the selected type
        final LocalDate finalRegDate = selectedRegistrationDate;
        final LocalDate finalExpDate = selectedExpirationDate;
        final String finalReceiptNumber = receiptNumberStr; // Use the validated receipt number
        final boolean finalIsTwoInOneMembership = isTwoInOneMembership;

        // Final versions for Member 2 (will be null/0 if not 2-in-1 and not validated)
        final String finalFirstName2 = firstName2;
        final String finalLastName2 = lastName2;
        final String finalPhoneNumber2 = phoneNumber2;
        final String finalGender2 = gender2;
        final int finalAge2 = age2;
        final String finalImagePath2 = imagePathToSave2;

        executorService.execute(() -> {
            boolean localProceedWithSave = true;
            String localReceiptErrorMessage = null;

            // Your existing receipt check logic
            if (!finalIsTwoInOneMembership) {
                boolean receiptExists = databaseHelper.isReceiptNumberExists(finalReceiptNumber, -1);
                if (receiptExists) {
                    localProceedWithSave = false;
                    localReceiptErrorMessage = "This receipt number is already in use.";
                }
            }

            final boolean finalProceedWithSave = localProceedWithSave;
            final String finalReceiptErrMessageForPost = localReceiptErrorMessage;

            mainThreadHandler.post(() -> {
                if (!finalProceedWithSave && finalReceiptErrMessageForPost != null) {
                    receiptNumberEditText.setError(finalReceiptErrMessageForPost);
                    receiptNumberEditText.requestFocus();
                    Toast.makeText(getContext(), finalReceiptErrMessageForPost, Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Validation failed: Receipt number '" + finalReceiptNumber + "' error: " + finalReceiptErrMessageForPost);
                } else {
                    Log.d(TAG, "Receipt number validation passed (or bypassed for 2-in-1). Proceeding to call saveMemberData.");
                    saveMemberData(
                            finalFirstName1, finalLastName1, finalPhoneNumber1, finalGender1, finalAge1, finalImagePath1,
                            finalSelectedMemberTypeId, finalRegDate, finalExpDate, finalReceiptNumber,
                            finalIsTwoInOneMembership,
                            // Pass Member 2's data
                            finalFirstName2, finalLastName2, finalPhoneNumber2, finalGender2, finalAge2, finalImagePath2
                    );
                }
            });
        });
    }

    // In AddMemberFragment.java

    private void saveMemberData(String firstName1, String lastName1, String phoneNumber1, String gender1, int age1,
                                String imagePathToSave1, // Pass the processed image path
                                int selectedMemberTypeId, LocalDate registrationDate, LocalDate expirationDate,
                                String receiptNumber, boolean isTwoInOneMembership,
                                // Parameters for Member 2
                                String firstName2, String lastName2, String phoneNumber2, String gender2, int age2,
                                String imagePathToSave2 // Pass the processed image path for member 2
    ) {

        SQLiteDatabase db = null;
        boolean overallSuccess = false;
        String member1DbId = null;
        String member2DbId = null;

        try {
            db = databaseHelper.getWritableDatabase(); // Get a writable database instance ONCE
            db.beginTransaction(); // Start the single transaction for one or two members

            // --- Process Member 1 ---
            // Generate ID for Member 1 using the transaction's db instance
            String member1GeneratedId = databaseHelper.generateNewMemberId(db);
            if (member1GeneratedId == null) {
                Log.e(TAG, "Failed to generate ID for Member 1. Aborting transaction.");
                // overallSuccess remains false
                // Transaction will be rolled back in finally
            } else {
                Log.d(TAG, "Attempting to save Member 1 (ID: " + member1GeneratedId + ") within transaction.");
                member1DbId = databaseHelper.addMemberInExternalTransaction(db, member1GeneratedId,
                        firstName1, lastName1, phoneNumber1, gender1, age1, imagePathToSave1,
                        selectedMemberTypeId, registrationDate, expirationDate, receiptNumber);

                if (member1DbId != null) {
                    Log.d(TAG, "Member 1 processed successfully within transaction. DB ID: " + member1DbId);
                    if (isTwoInOneMembership) {
                        // --- Process Member 2 (only if Member 1 was successful) ---
                        String member2GeneratedId = databaseHelper.generateNewMemberId(db); // Generate new ID for member 2
                        if (member2GeneratedId == null) {
                            Log.e(TAG, "Failed to generate ID for Member 2. Aborting transaction.");
                        } else {
                            Log.d(TAG, "Attempting to save Member 2 (ID: " + member2GeneratedId + ") for 2-in-1 within transaction.");
                            member2DbId = databaseHelper.addMemberInExternalTransaction(db, member2GeneratedId,
                                    firstName2, lastName2, phoneNumber2, gender2, age2, imagePathToSave2,
                                    selectedMemberTypeId, registrationDate, expirationDate, receiptNumber);

                            if (member2DbId != null) {
                                Log.d(TAG, "Member 2 processed successfully within transaction. DB ID: " + member2DbId);
                                db.setTransactionSuccessful(); // Both members successful
                                overallSuccess = true;
                            } else {
                                Log.e(TAG, "Failed to process Member 2. Transaction will be rolled back.");
                                // overallSuccess remains false
                            }
                        }
                    } else {
                        // Not a 2-in-1, and Member 1 was successful
                        db.setTransactionSuccessful();
                        overallSuccess = true;
                    }
                } else {
                    Log.e(TAG, "Failed to process Member 1. Transaction will be rolled back.");
                    // overallSuccess remains false
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during saveMemberData transaction: " + e.getMessage(), e);
            overallSuccess = false; // Ensure it's false on any exception
        } finally {
            if (db != null && db.inTransaction()) { // Check if still in transaction before ending
                db.endTransaction(); // Commits if setSuccessful, otherwise rolls back
            }
            // SQLiteOpenHelper manages the closing of the database instance obtained via getWritableDatabase.
            // So, typically, you do not call db.close() here.
        }

        // --- UI Update Logic ---
        if (overallSuccess) {
            if (isTwoInOneMembership) {
                Toast.makeText(getContext(), "Both members added successfully!", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Both members added. Member 1 ID: " + member1DbId + ", Member 2 ID: " + member2DbId);
            } else {
                Toast.makeText(getContext(), "Member added successfully!", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Single member added successfully. Member ID: " + member1DbId);
            }
            clearForm();
        } else {
            // Transaction failed or was rolled back
            if (isTwoInOneMembership) {
                Toast.makeText(getContext(), "Failed to add one or both members. Please check details and try again.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to add 2-in-1 members. M1 attempt: " + firstName1 + (member1DbId == null ? " (failed pre-insert or ID gen)" : "") +
                        ", M2 attempt: " + firstName2 + (member2DbId == null && member1DbId != null ? " (failed pre-insert or ID gen)" : ""));
            } else {
                Toast.makeText(getContext(), "Failed to add member. Please try again.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to add single member: " + firstName1 + (member1DbId == null ? " (failed pre-insert or ID gen)" : ""));
            }
            // Do NOT clear form
        }
    }

    private void clearForm() {
        if (firstNameEditText != null) firstNameEditText.setText("");
        if (lastNameEditText != null) lastNameEditText.setText("");
        if (ageEditText != null) ageEditText.setText("");
        if (phoneNumberEditText != null) phoneNumberEditText.setText("");
        if (genderSpinner != null) {
            genderSpinner.setSelection(0);
            genderSpinner.setBackground(defaultGenderSpinnerBackground);
        }
        if (memberPreviewImageView != null) {
            memberPreviewImageView.setImageDrawable(null);
        }

        if (firstNameEditText2 != null) firstNameEditText2.setText("");
        if (lastNameEditText2 != null) lastNameEditText2.setText("");
        if (ageEditText2 != null) ageEditText2.setText("");
        if (phoneNumberEditText2 != null) phoneNumberEditText2.setText("");
        if (genderSpinner2 != null) {
            genderSpinner2.setSelection(0);
            genderSpinner2.setBackground(defaultGenderSpinnerBackground);
        }
        if (memberPreviewImageView2 != null) {
            memberPreviewImageView2.setImageDrawable(null);
        }

        if (startDateTextView != null) startDateTextView.setText("");
        if (expirationDateTextView != null) expirationDateTextView.setText("");
        if (receiptNumberEditText != null) receiptNumberEditText.setText("");
        if (memberTypeSpinner != null) {
            memberTypeSpinner.setSelection(0);
            memberTypeSpinner.setBackground(defaultMemberTypeSpinnerBackground);
        }
        selectedRegistrationDate = null;
        selectedExpirationDate = null;

        currentPhotoPath1 = null;
        capturedImageUri1 = null;
        selectedImageUri1 = null;

        currentPhotoPath2 = null;
        capturedImageUri2 = null;
        selectedImageUri2 = null;

        Log.d(TAG, "Form cleared including dates, receipt, and image state.");
    }

    // --- And ensure the saveImageFromUriToInternalStorage method is complete ---
    // (The one that got cut off in the previous response)
    private String saveImageFromUriToInternalStorage(Uri contentUri, String fileName) {
        if (getContext() == null || contentUri == null) {
            Log.e(TAG, "Context or Content URI is null in saveImageFromUriToInternalStorage.");
            return null;
        }

        File internalStorageDir = new File(getContext().getFilesDir(), "MemberImages");
        if (!internalStorageDir.exists()) {
            if (!internalStorageDir.mkdirs()) {
                Log.e(TAG, "Failed to create internal directory for member images: " + internalStorageDir.getAbsolutePath());
                return null;
            }
        }

        File destinationFile = new File(internalStorageDir, fileName);

        try (java.io.InputStream inputStream = getContext().getContentResolver().openInputStream(contentUri);
             java.io.OutputStream outputStream = new java.io.FileOutputStream(destinationFile)) {

            if (inputStream == null) {
                Log.e(TAG, "InputStream is null for URI: " + contentUri.toString());
                return null;
            }

            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            Log.d(TAG, "Image saved to internal storage: " + destinationFile.getAbsolutePath());
            return destinationFile.getAbsolutePath();

        } catch (java.io.FileNotFoundException e) {
            Log.e(TAG, "File not found for URI: " + contentUri.toString(), e);
        } catch (IOException e) {
            Log.e(TAG, "IOException while saving image from URI: " + contentUri.toString(), e);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while accessing URI: " + contentUri.toString(), e);
        }
        return null; // Return null if any error occurs
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}