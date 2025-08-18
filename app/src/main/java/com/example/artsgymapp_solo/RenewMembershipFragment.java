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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.time.ZoneId;
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

    private ImageView memberImageView;
    private TextView firstNameTextView, lastNameTextView, genderTextView, phoneNumberTextView, ageTextView, memberIdTextView, fingerprintStatusTextView;
    private Spinner memberTypeSpinner;
    private EditText receiptNumberEditText;
    private TextView startDateTextView, endDateTextView;
    private MaterialButton renewMemberButton, cancelButton, verifyExistingMemberButton;

    private LinearLayout secondMemberContainer;
    private LinearLayout secondMemberViews;

    private LinearLayout fingerprintContainer;
    private EditText firstNameEditText2, lastNameEditText2, phoneNumberEditText2, ageEditText2, existingMemberIdEditText;
    private ImageView memberImageView2ndMember, fingerprintImageView;
    private Spinner genderSpinner2, secondMemberSpinner;
    private Member selectedExistingPartner;

    private NavController navController;
    private DatabaseHelper dbHelper;
    private String currentMemberIdString; 
    private int passedSelectedMemberTypeId = -1;
    private Member currentMember; 
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

    private boolean isEndDateManuallySet = false;

    public RenewMembershipFragment()
    {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(getContext());
        memberTypesList = new ArrayList<>();

        if (getArguments() != null)
        {
            currentMemberIdString = getArguments().getString("memberID");
            passedSelectedMemberTypeId = getArguments().getInt(ARG_SELECTED_MEMBER_TYPE_ID, -1);
        }
        
        requestPermissionsLauncherForNewPartner = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean cameraGranted = permissions.getOrDefault(android.Manifest.permission.CAMERA, false);
                    boolean storageGranted = permissions.getOrDefault(android.Manifest.permission.READ_EXTERNAL_STORAGE, false) ||
                            permissions.getOrDefault(android.Manifest.permission.READ_MEDIA_IMAGES, false); 

                    if (cameraGranted && storageGranted) {
                        Toast.makeText(getContext(), "Permissions granted. Please try again.", Toast.LENGTH_SHORT).show();
                    } else if (cameraGranted) {
                        
                        Toast.makeText(getContext(), "Camera permission granted. Please try again.", Toast.LENGTH_SHORT).show();
                    } else if (storageGranted) {
                        
                        Toast.makeText(getContext(), "Storage permission granted. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getContext(), "Camera and/or Storage permissions are required to select an image.", Toast.LENGTH_LONG).show();
                    }
                });

        
        takePictureLauncherForNewPartner = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        
                        selectedImageUriForNewPartner = null; 
                        
                        Glide.with(this)
                                .load(capturedImageUriForNewPartner) 
                                .placeholder(R.drawable.addimage)
                                .error(R.mipmap.ic_launcher_round)
                                .into(memberImageView2ndMember);
                    } else {
                        Toast.makeText(getContext(), "Failed to capture image", Toast.LENGTH_SHORT).show();
                        capturedImageUriForNewPartner = null; 
                        currentPhotoPathForNewPartner = null;
                    }
                });

        
        pickImageLauncherForNewPartner = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        selectedImageUriForNewPartner = result.getData().getData();
                        capturedImageUriForNewPartner = null; 
                        currentPhotoPathForNewPartner = null;

                        Glide.with(this)
                                .load(selectedImageUriForNewPartner)
                                .placeholder(R.drawable.addimage)
                                .error(R.mipmap.ic_launcher_round)
                                .into(memberImageView2ndMember);
                    } else {
                        Toast.makeText(getContext(), "Image selection cancelled", Toast.LENGTH_SHORT).show();
                        selectedImageUriForNewPartner = null; 
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

        bindViews(view);
        setupSpinners();
        setupListeners();

        if (currentMemberIdString != null) {
            loadMemberDetails(currentMemberIdString);
            loadMemberTypes();
        } else {
            Toast.makeText(getContext(), "Error: Member ID not found.", Toast.LENGTH_LONG).show();
            
            navController.popBackStack(); 
        }

        
        fingerprintImageView.setOnClickListener(v -> {
            
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null)
            {
                mainActivity.startFingerprintEnrollment(2, this, null);
            }
        });

        
        selectedStartDate = LocalDate.now();
        startDateTextView.setText(selectedStartDate.format(displayDateFormatter));
        secondMemberContainer.setVisibility(View.GONE);
        secondMemberViews.setVisibility(View.GONE);
        existingMemberIdEditText.setVisibility(View.GONE);
        verifyExistingMemberButton.setVisibility(View.GONE);
    }

    @Override
    public void onEnrollmentProgress(int target, int progress, int total, String message) {

        if (target == 2) {
            if (fingerprintStatusTextView != null) {
                fingerprintStatusTextView.setText(message);
            }
        }
    }

    @Override
    public void onEnrollmentComplete(int target, Bitmap capturedImage, byte[] mergedTemplate) {
        if (target == 2) { 
            
            capturedFingerprintTemplateForNewPartner = mergedTemplate; 
            Toast.makeText(getContext(), "New partner fingerprint captured successfully!", Toast.LENGTH_LONG).show();
            if (fingerprintStatusTextView != null) {
                fingerprintStatusTextView.setText("Fingerprint enrolled for New Member!");
            }
            
            if (capturedImage != null) {
                fingerprintImageView.setImageBitmap(capturedImage);
            }
        }
    }

    @Override
    public void onEnrollmentFailed(int target, String errorMessage) {
        if (target == 2) { 
            
            capturedFingerprintTemplateForNewPartner = null; 
            Toast.makeText(getContext(), "New member fingerprint capture failed: " + errorMessage, Toast.LENGTH_LONG).show();
            if (fingerprintStatusTextView != null) {
                fingerprintStatusTextView.setText("Enrollment Failed: " + errorMessage);
            }
        }
    }

    public void setupSpinners() {
        
        genderAdapter2 = ArrayAdapter.createFromResource(requireContext(),
                R.array.gender_array, R.layout.spinner_item_collapsed);
        genderAdapter2.setDropDownViewResource(R.layout.spinner_item);
        genderSpinner2.setAdapter(genderAdapter2);
        genderSpinner2.setSelection(0); 

        
        ArrayAdapter<CharSequence> secondMemberOptionsAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.member_array, R.layout.spinner_item_collapsed);
        secondMemberOptionsAdapter.setDropDownViewResource(R.layout.spinner_item);
        secondMemberSpinner.setAdapter(secondMemberOptionsAdapter);
        secondMemberSpinner.setSelection(0); 
    }


    private void bindViews(View view) {
        
        memberImageView = view.findViewById(R.id.memberImageView);
        firstNameTextView = view.findViewById(R.id.firstNameTextView);
        lastNameTextView = view.findViewById(R.id.lastNameTextView);
        genderTextView = view.findViewById(R.id.genderTextView);
        phoneNumberTextView = view.findViewById(R.id.phoneNumberTextView);
        ageTextView = view.findViewById(R.id.ageTextView);
        memberIdTextView = view.findViewById(R.id.memberIdTextView);

        
        memberTypeSpinner = view.findViewById(R.id.memberTypeSpinner);
        receiptNumberEditText = view.findViewById(R.id.receiptNumberEditText);
        startDateTextView = view.findViewById(R.id.startDateTextView);
        endDateTextView = view.findViewById(R.id.endDateTextView);
        renewMemberButton = view.findViewById(R.id.renewMemberbutton);
        cancelButton = view.findViewById(R.id.cancelButton);

        
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

        fingerprintContainer = view.findViewById(R.id.fingerprintContainer);
        fingerprintStatusTextView = view.findViewById(R.id.fingerprintStatusTextView);
        fingerprintImageView = view.findViewById(R.id.fingerprintImageView);
    }

    private void setupListeners() {
        startDateTextView.setOnClickListener(v -> showDatePickerDialog(true));

        endDateTextView.setOnClickListener(v ->
        {
            showDatePickerDialog(false);
            isEndDateManuallySet = true;
        });

        renewMemberButton.setOnClickListener(v -> attemptRenewal());
        cancelButton.setOnClickListener(v -> navController.popBackStack());
        verifyExistingMemberButton.setOnClickListener(v -> fetchExistingPartner());

        memberImageView2ndMember.setOnClickListener(v -> {
            if (secondMemberViews.getVisibility() == View.VISIBLE && firstNameEditText2.isEnabled()) {
                showImagePickDialogForNewPartner(); 
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
                        secondMemberSpinner.setSelection(0); 
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
                setPartnerDetailsEditable(true); 

                if (position == 1) { 
                    secondMemberViews.setVisibility(View.VISIBLE);
                    memberImageView2ndMember.setImageResource(R.drawable.addimage);
                    fingerprintContainer.setVisibility(View.VISIBLE);
                    existingMemberIdEditText.setVisibility(View.GONE);
                    verifyExistingMemberButton.setVisibility(View.GONE);
                } else if (position == 2) { 
                    secondMemberViews.setVisibility(View.GONE);
                    fingerprintContainer.setVisibility(View.GONE);
                    existingMemberIdEditText.setVisibility(View.VISIBLE);
                    verifyExistingMemberButton.setVisibility(View.VISIBLE);
                } else {
                    secondMemberViews.setVisibility(View.GONE);
                    fingerprintContainer.setVisibility(View.GONE);
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
            
            MainActivity.startFingerprintEnrollment(2, this, null);
        });
    }

    private boolean validateNewPartnerInputs() {
        clearNewPartnerInputsErrors(); 

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
        if (genderSpinner2.getSelectedItemPosition() == 0) { 
            ((TextView) genderSpinner2.getSelectedView()).setError("Select gender"); 
            Toast.makeText(getContext(), "Please select gender for the partner.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        
        if (capturedFingerprintTemplateForNewPartner == null) {
            Toast.makeText(getContext(), "Fingerprint scan is required for the new partner.", Toast.LENGTH_LONG).show();
            if (fingerprintStatusTextView != null) fingerprintStatusTextView.setText("Fingerprint required!");
            
            return false; 
        }

        return isValid;
    }

    private void attemptRenewal() {
        if (currentMember == null) {
            Toast.makeText(getContext(), "Member data not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedMemberType == null || selectedMemberType.getId() == -1) { 
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

        
        executorService.execute(() -> {
            boolean receiptExists = dbHelper.isReceiptNumberExists(receiptNumber, -1); 
            mainThreadHandler.post(() -> {
                if (receiptExists) {
                    receiptNumberEditText.setError("Receipt number already exists.");
                    Toast.makeText(getContext(), "This receipt number is already in use.", Toast.LENGTH_SHORT).show();
                } else {
                    receiptNumberEditText.setError(null);
                    if (selectedMemberType.isTwoInOne()) {
                        int selectedPartnerOptionPos = secondMemberSpinner.getSelectedItemPosition();

                        if (selectedPartnerOptionPos == 1) { 
                            if (validateNewPartnerInputs()) {
                                String pFirstName = firstNameEditText2.getText().toString().trim();
                                String pLastName = lastNameEditText2.getText().toString().trim();
                                int pAge = Integer.parseInt(ageEditText2.getText().toString().trim()); 
                                String pPhone = phoneNumberEditText2.getText().toString().trim();
                                String pGender = genderSpinner2.getSelectedItem().toString();

                                
                                String imagePathToSaveForNewPartner = null;
                                if (currentPhotoPathForNewPartner != null && !currentPhotoPathForNewPartner.isEmpty()) {
                                    
                                    imagePathToSaveForNewPartner = currentPhotoPathForNewPartner;
                                    
                                } else if (selectedImageUriForNewPartner != null) {

                                    String partnerNameForFile = pFirstName + "_" + pLastName;
                                    imagePathToSaveForNewPartner = saveImageFromUriToInternalStorage(selectedImageUriForNewPartner, partnerNameForFile);
                                    if (imagePathToSaveForNewPartner == null) {
                                        Toast.makeText(getContext(), "Could not save partner image. Continuing without.", Toast.LENGTH_LONG).show();
                                    }
                                }
                                saveRenewalTwoInOneNewPartner(receiptNumber, pFirstName, pLastName, pPhone, pGender, pAge, imagePathToSaveForNewPartner);
                            } else {
                                Toast.makeText(getContext(), "Please correct errors for the new partner.", Toast.LENGTH_LONG).show();
                            }
                        } else if (selectedPartnerOptionPos == 2) { 
                            if (selectedExistingPartner == null) {
                                Toast.makeText(getContext(), "Please verify an existing partner ID.", Toast.LENGTH_LONG).show();
                                existingMemberIdEditText.setError("Verify ID first");
                                return;
                            }
                            
                            if (TextUtils.isEmpty(existingMemberIdEditText.getText().toString().trim()) || selectedExistingPartner == null){
                                Toast.makeText(getContext(), "No existing partner selected or verified.", Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (selectedExistingPartner.getMemberID().equals(currentMemberIdString)) {
                                Toast.makeText(getContext(), "Partner cannot be the same as the primary member.", Toast.LENGTH_LONG).show();
                                existingMemberIdEditText.setError("Cannot be primary"); 
                                return;
                            }
                            saveRenewalTwoInOneExistingPartner(receiptNumber, selectedExistingPartner.getMemberID());
                        } else { 
                            Toast.makeText(getContext(), "Please select a partner option for 2-in-1 membership.", Toast.LENGTH_LONG).show();
                            if(secondMemberSpinner.getSelectedView() != null) {
                                ((TextView) secondMemberSpinner.getSelectedView()).setError("Required");
                            }
                        }
                    } else { 
                        saveSingleMembershipRenewal(receiptNumber);
                    }
                }
            });
        });
    }
    
    private void showImagePickDialogForNewPartner() {
        
        
        if (!firstNameEditText2.isEnabled()) { 
            Toast.makeText(getContext(), "Partner details are not editable.", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Set Image for Partner");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                
                checkCameraPermissionAndOpenCameraForNewPartner();
            } else if (options[item].equals("Choose from Gallery")) {
                
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
                
                
                openCameraForNewPartner();
            } else {
                
                requestPermissionsLauncherForNewPartner.launch(new String[]{Manifest.permission.CAMERA});
            }
        } else {
            openCameraForNewPartner(); 
        }
    }

    private void openCameraForNewPartner() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            
            File photoFile = null;
            try {
                photoFile = createImageFileForNewPartner();
            } catch (IOException ex) {
                
                
                Toast.makeText(getContext(), "Error preparing for camera.", Toast.LENGTH_SHORT).show();
                return;
            }

            capturedImageUriForNewPartner = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUriForNewPartner);

            takePictureLauncherForNewPartner.launch(takePictureIntent);
        } else {
            Toast.makeText(getContext(), "No camera app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFileForNewPartner() throws IOException {
        
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_PARTNER_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES); 

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        
        currentPhotoPathForNewPartner = image.getAbsolutePath();
        
        return image;
    }

    private void checkStoragePermissionAndOpenGalleryForNewPartner() {
        String permissionToRequest;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { 
            permissionToRequest = Manifest.permission.READ_MEDIA_IMAGES;
        } else { 
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
        
    }

    private String saveImageFromUriToInternalStorage(Uri sourceUri, String partnerName) {
        if (sourceUri == null) return null;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "PARTNER_" + partnerName.replaceAll("\\s+", "_") + "_" + timeStamp + ".jpg";
        File storageDir = requireContext().getFilesDir(); 

        File destinationFile = new File(storageDir, imageFileName);

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

            if (inputStream == null) {
                
                return null;
            }

            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            
            return destinationFile.getAbsolutePath();
        } catch (IOException e) {
            
            Toast.makeText(getContext(), "Error saving partner image.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    

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
        existingMemberIdEditText.setError(null); 

        executorService.execute(() -> {
            Member fetchedPartner = dbHelper.getMemberById(partnerIdToVerify);
            mainThreadHandler.post(() -> {
                if (fetchedPartner != null) {
                    selectedExistingPartner = fetchedPartner;
                    Toast.makeText(getContext(), "Partner " + fetchedPartner.getFirstName() + " " + fetchedPartner.getLastName() + " found.", Toast.LENGTH_SHORT).show();
                    populateNewPartnerFieldsWithExisting(fetchedPartner); 
                    secondMemberViews.setVisibility(View.VISIBLE);      
                    setPartnerDetailsEditable(false);                   
                } else {
                    selectedExistingPartner = null;
                    existingMemberIdEditText.setError("Not Found");
                    Toast.makeText(getContext(), "Existing Member ID not found.", Toast.LENGTH_SHORT).show();
                    secondMemberViews.setVisibility(View.GONE); 
                    clearNewPartnerInputs(); 
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
            genderSpinner2.setSelection(Math.max(genderPosition, 0));
        } else {
            genderSpinner2.setSelection(0);
        }

        selectedImageUriForNewPartner = null; 
        if (partner.getImageFilePath() != null && !partner.getImageFilePath().isEmpty()) {
            Glide.with(this)
                    .load(new File(partner.getImageFilePath()))
                    .placeholder(R.drawable.addimage)
                    .error(R.mipmap.ic_launcher_round)
                    .into(memberImageView2ndMember);
        } else {
            Glide.with(this)
                    .load(R.mipmap.ic_launcher_round)
                    .into(memberImageView2ndMember);
        }
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
                        navController.navigate(R.id.action_RenewMembershipFragment_to_memberListFragment);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to renew 2-in-1 membership (new partner).", Toast.LENGTH_LONG).show();
                }
            });
        });
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
                        navController.navigate(R.id.action_RenewMembershipFragment_to_memberListFragment);
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
                    if (navController != null)
                    {
                        navController.navigate(R.id.action_RenewMembershipFragment_to_memberListFragment);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to renew membership.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loadMemberDetails(String memberIdStr) {
        executorService.execute(() -> {
            currentMember = dbHelper.getMemberById(memberIdStr);

            mainThreadHandler.post(() -> {
                if (currentMember != null) {
                    populateMemberDetailsUI();

                    selectedStartDate = LocalDate.now();
                    startDateTextView.setText(selectedStartDate.format(displayDateFormatter));

                    calculateAndDisplayEndDate();
                    isEndDateManuallySet = false;

                    secondMemberContainer.setVisibility(View.GONE);
                    secondMemberViews.setVisibility(View.GONE);
                    existingMemberIdEditText.setVisibility(View.GONE);
                    verifyExistingMemberButton.setVisibility(View.GONE);
                } else {
                    Toast.makeText(getContext(), "Failed to load member details.", Toast.LENGTH_SHORT).show();
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
        ageTextView.setText(String.valueOf(currentMember.getAge())); 
        memberIdTextView.setText(currentMember.getMemberID());

        if (currentMember.getImageFilePath() != null && !currentMember.getImageFilePath().isEmpty()) {
            Glide.with(this)
                    .load(new File(currentMember.getImageFilePath()))
                    .placeholder(R.drawable.addimage) 
                    .error(R.mipmap.ic_launcher_round) 
                    .into(memberImageView);
        } else {
            Glide.with(this)
                    .load(R.mipmap.ic_launcher_round) 
                    .into(memberImageView);
        }
    }

    private void loadMemberTypes()
    {
        executorService.execute(() -> {
            List<MemberType> typesFromDb = dbHelper.getAllMemberTypes();
            mainThreadHandler.post(() -> {
                memberTypesList.clear();
                MemberType hintType = new MemberType(-1, "Select Membership Type", 0, false);
                memberTypesList.add(hintType); 
                memberTypesList.addAll(typesFromDb);

                if (getContext() == null) {
                    
                    return;
                }

                memberTypeArrayAdapter = new ArrayAdapter<>(requireContext(),
                        R.layout.spinner_item_collapsed, memberTypesList);
                memberTypeArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
                memberTypeSpinner.setAdapter(memberTypeArrayAdapter);

                
                if (passedSelectedMemberTypeId != -1) {
                    boolean foundType = false;
                    for (int i = 0; i < memberTypesList.size(); i++) {
                        if (memberTypesList.get(i).getId() == passedSelectedMemberTypeId) {
                            memberTypeSpinner.setSelection(i);
                            selectedMemberType = memberTypesList.get(i); 
                            calculateAndDisplayEndDate(); 

                            if (selectedMemberType.isTwoInOne()) {
                                secondMemberContainer.setVisibility(View.VISIBLE);
                                
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
                        
                        memberTypeSpinner.setSelection(0); 
                    }
                } else {
                    memberTypeSpinner.setSelection(0); 
                }
                
            });
        });
    }

    private void showDatePickerDialog(final boolean isStartDate) {
        // Clear any previous errors
        startDateTextView.setError(null);
        endDateTextView.setError(null);

        // Initialize calendar with today's date
        final Calendar calendar = Calendar.getInstance();
        int year, month, day;

        // Set initial year/month/day based on existing selection
        if (isStartDate && selectedStartDate != null) {
            year = selectedStartDate.getYear();
            month = selectedStartDate.getMonthValue() - 1;
            day = selectedStartDate.getDayOfMonth();
        } else if (!isStartDate && selectedEndDate != null) {
            year = selectedEndDate.getYear();
            month = selectedEndDate.getMonthValue() - 1;
            day = selectedEndDate.getDayOfMonth();
        } else if (!isStartDate && selectedStartDate != null) {
            year = selectedStartDate.getYear();
            month = selectedStartDate.getMonthValue() - 1;
            day = selectedStartDate.getDayOfMonth();
        } else {
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, yearSelected, monthOfYear, dayOfMonth) -> {
                    LocalDate pickedDate = LocalDate.of(yearSelected, monthOfYear + 1, dayOfMonth);
                    if (isStartDate) {
                        selectedStartDate = pickedDate;
                        startDateTextView.setText(selectedStartDate.format(displayDateFormatter));
                        isEndDateManuallySet = false;
                        calculateAndDisplayEndDate();
                    } else {
                        selectedEndDate = pickedDate;
                        endDateTextView.setText(selectedEndDate.format(displayDateFormatter));
                        isEndDateManuallySet = true;
                    }
                },
                year, month, day
        );

        if (isStartDate) {
            // Set min date (last 5 days of previous month + all current month days)
            Calendar today = Calendar.getInstance();
            Calendar minDateCalendar = getCalendar(today); // Uses your getCalendar() helper
            datePickerDialog.getDatePicker().setMinDate(minDateCalendar.getTimeInMillis());

            // Set max date (end of current month)
            Calendar maxDateCalendar = Calendar.getInstance();
            maxDateCalendar.set(Calendar.DAY_OF_MONTH, maxDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            maxDateCalendar.set(Calendar.HOUR_OF_DAY, 23);
            maxDateCalendar.set(Calendar.MINUTE, 59);
            maxDateCalendar.set(Calendar.SECOND, 59);
            maxDateCalendar.set(Calendar.MILLISECOND, 999);
            datePickerDialog.getDatePicker().setMaxDate(maxDateCalendar.getTimeInMillis());
        } else {
            // End date picker logic
            if (selectedStartDate != null) {
                Calendar minEndCal = Calendar.getInstance();
                minEndCal.set(
                        selectedStartDate.getYear(),
                        selectedStartDate.getMonthValue() - 1,
                        selectedStartDate.getDayOfMonth()
                );
                datePickerDialog.getDatePicker().setMinDate(minEndCal.getTimeInMillis());
            } else {
                // Same fallback as AddMemberFragment
                Calendar earliestAllowedStartDate = Calendar.getInstance();
                earliestAllowedStartDate.add(Calendar.MONTH, -1);
                earliestAllowedStartDate.set(Calendar.DAY_OF_MONTH,
                        earliestAllowedStartDate.getActualMaximum(Calendar.DAY_OF_MONTH));
                earliestAllowedStartDate.add(Calendar.DAY_OF_MONTH, -4);
                earliestAllowedStartDate.set(Calendar.HOUR_OF_DAY, 0);
                earliestAllowedStartDate.set(Calendar.MILLISECOND, 0);
                datePickerDialog.getDatePicker().setMinDate(earliestAllowedStartDate.getTimeInMillis());
            }
        }

        datePickerDialog.show();
    }

    @NonNull
    private static Calendar getCalendar(Calendar today) {
        Calendar minDateCalendar = Calendar.getInstance();
        minDateCalendar.setTime(today.getTime());
        minDateCalendar.add(Calendar.MONTH, -1);
        minDateCalendar.set(Calendar.DAY_OF_MONTH, minDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        minDateCalendar.add(Calendar.DAY_OF_MONTH, -4);
        minDateCalendar.set(Calendar.HOUR_OF_DAY, 0);
        minDateCalendar.set(Calendar.MINUTE, 0);
        minDateCalendar.set(Calendar.SECOND, 0);
        minDateCalendar.set(Calendar.MILLISECOND, 0);
        return minDateCalendar;
    }

    private void calculateAndDisplayEndDate()
    {
        if (!isEndDateManuallySet &&
                selectedStartDate != null &&
                selectedMemberType != null &&
                selectedMemberType.getId() != -1) {

            if (selectedMemberType.getDurationDays() > 0) {
                selectedEndDate = selectedStartDate.plusDays(selectedMemberType.getDurationDays());
                endDateTextView.setText(selectedEndDate.format(displayDateFormatter));
            } else {
                endDateTextView.setHint("Select Valid Type");
                selectedEndDate = null;
            }
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
        Glide.with(this).clear(memberImageView2ndMember); 
        selectedImageUriForNewPartner = null; 
        setPartnerDetailsEditable(true); 
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
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        
    }
}