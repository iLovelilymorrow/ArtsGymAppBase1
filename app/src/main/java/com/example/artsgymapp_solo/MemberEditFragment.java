package com.example.artsgymapp_solo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import com.google.android.material.button.MaterialButton; 

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MemberEditFragment extends Fragment implements FingerprintEnrollmentCallback
{
    private ImageView memberImageView, fingerprintImageView;
    private TextView memberIdTextView, fingerprintStatusTextView, membershipTypeTextView;
    private MaterialButton buttonDeleteMember;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private Spinner genderSpinner;
    private EditText ageEditText;
    private EditText phoneNumberEditText;
    private EditText receiptNumberEditText;
    private MaterialButton buttonConfirmEdit;
    private MaterialButton buttonCancelEdit;
    private MaterialButton buttonRenewMembership;
    private DatabaseHelper databaseHelper;
    private String currentMemberIdToEdit;
    private int currentPeriodIdToEdit = -1;
    private Member currentMemberDetails; 
    private MembershipPeriod currentMembershipPeriodDetails;
    private String originalReceiptNumber; 

    private String originalImageFilePath; 
    private String newSelectedPhotoPath; 
    private Uri capturedImageUri;      
    private Uri newSelectedGalleryImageUri; 

    
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    private NavController navController;

    private Drawable defaultGenderSpinnerBackground;

    private TextView registrationDateTextView;
    private TextView expirationDateTextView;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private byte[] capturedFingerprintTemplate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(requireContext());

        if (getArguments() != null) {
            currentMemberIdToEdit = getArguments().getString("memberID");
            currentPeriodIdToEdit = getArguments().getInt("periodID", -1); 
            
        }
        setupActivityLaunchers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_memberedit, container, false);
        initializeViews(view);

        if (genderSpinner != null) {
            defaultGenderSpinnerBackground = genderSpinner.getBackground();
        }

        setupSpinners(); 
        setupSpinnerErrorClearing(); 
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        if (currentMemberIdToEdit != null && currentPeriodIdToEdit != -1)
        {
            loadMemberData(currentMemberIdToEdit, currentPeriodIdToEdit);
            
        } else {
            Toast.makeText(getContext(), "Error: Member or Period ID not found.", Toast.LENGTH_LONG).show();
            if (navController != null) navController.popBackStack();
        }
        setClickListeners();
    }

    @Override
    public void onEnrollmentProgress(int target, int progress, int total, String message) {
        if (target == 1)
        {
            if (fingerprintStatusTextView != null) {
                fingerprintStatusTextView.setText(message);
            }
        }
    }

    @Override
    public void onEnrollmentComplete(int target, Bitmap capturedImage, byte[] mergedTemplate) {
        if (target == 1) { 
            
            capturedFingerprintTemplate = mergedTemplate; 
            Toast.makeText(getContext(), "Fingerprint recaptured successfully!", Toast.LENGTH_LONG).show();

            if (fingerprintStatusTextView != null) {
                fingerprintStatusTextView.setText("Fingerprint Recaptured!");
            }

            if (capturedImage != null) {
                fingerprintImageView.setImageBitmap(capturedImage);
            }
        }
    }

    @Override
    public void onEnrollmentFailed(int target, String errorMessage) {
        if (target == 1) { 
            
            capturedFingerprintTemplate = null; 
            Toast.makeText(getContext(), "Fingerprint recapture failed: " + errorMessage, Toast.LENGTH_LONG).show();

            if (fingerprintStatusTextView != null) {
                fingerprintStatusTextView.setText("Recapture Failed: " + errorMessage);
            }
        }
    }

    private void initializeViews(View view)
    {
        memberImageView = view.findViewById(R.id.memberImageView);
        memberIdTextView = view.findViewById(R.id.memberIdTextView);
        firstNameEditText = view.findViewById(R.id.firstNameEditText);
        lastNameEditText = view.findViewById(R.id.lastNameEditText);
        membershipTypeTextView = view.findViewById(R.id.membershipTypeTextView);
        genderSpinner = view.findViewById(R.id.genderSpinner2ndMember);
        ageEditText = view.findViewById(R.id.ageEditText);
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        receiptNumberEditText = view.findViewById(R.id.receiptNumberEditText);
        buttonConfirmEdit = view.findViewById(R.id.buttonConfirmEdit);
        buttonCancelEdit = view.findViewById(R.id.buttonCancelEdit);
        buttonDeleteMember = view.findViewById(R.id.buttonDeleteMember);
        registrationDateTextView = view.findViewById(R.id.registrationDateTextView);
        expirationDateTextView = view.findViewById(R.id.expirationDateTextView);
        buttonRenewMembership = view.findViewById(R.id.buttonRenewMembership);

        fingerprintImageView = view.findViewById(R.id.fingerprintImageView);
        fingerprintStatusTextView = view.findViewById(R.id.fingerprintStatusTextView);
    }

    private void setupSpinners()
    {
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.gender_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

    }

    private void setupSpinnerErrorClearing() {
        if (genderSpinner != null) {
            genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) { 
                        genderSpinner.setBackground(defaultGenderSpinnerBackground);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void setupActivityLaunchers() {
        
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    
                    
                    boolean cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                    String readPermissionNeeded = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU ?
                            Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
                    boolean readStorageGranted = permissions.getOrDefault(readPermissionNeeded, false);

                    
                    
                    if (permissions.containsKey(Manifest.permission.CAMERA)) {
                        if (cameraGranted) openCamera();
                        else Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
                    }
                    if (permissions.containsKey(readPermissionNeeded)) {
                        if (readStorageGranted) openGallery();
                        else Toast.makeText(getContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        newSelectedGalleryImageUri = null; 
                        if (capturedImageUri != null) {
                            Glide.with(this).load(capturedImageUri).into(memberImageView);
                        }
                    } else {
                        
                        if (newSelectedPhotoPath != null) { 
                            File photoFile = new File(newSelectedPhotoPath);
                            if (photoFile.exists() && photoFile.length() == 0) {
                                photoFile.delete();
                            }
                        }
                        newSelectedPhotoPath = null;
                        capturedImageUri = null;
                    }
                });

        
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        newSelectedGalleryImageUri = uri;
                        capturedImageUri = null; 
                        newSelectedPhotoPath = null; 
                        Glide.with(this).load(newSelectedGalleryImageUri).into(memberImageView);
                        
                    }
                });
    }

    private void loadMemberData(String memberId, int periodId) {

        currentMemberDetails = databaseHelper.getMemberById(memberId);
        currentMembershipPeriodDetails = databaseHelper.getMembershipPeriodById(periodId);

        if (currentMemberDetails != null && currentMembershipPeriodDetails != null) {
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

            MemberType typeDetailsForPeriod = databaseHelper.getMemberTypeById(currentMembershipPeriodDetails.getFkMemberTypeId());
            if (typeDetailsForPeriod != null) {
                membershipTypeTextView.setText(typeDetailsForPeriod.getName());
            } else {
                membershipTypeTextView.setText("N/A");
            }

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

            originalReceiptNumber = currentMembershipPeriodDetails.getReceiptNumber();
            receiptNumberEditText.setText(Objects.requireNonNullElse(originalReceiptNumber, ""));

        } else {
            Toast.makeText(getContext(), "Failed to load member or period details.", Toast.LENGTH_LONG).show();
            if (navController != null) navController.popBackStack();
        }
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

        fingerprintImageView.setOnClickListener(v -> {
            MainActivity.startFingerprintEnrollment(1, this, null);
        });

        if (buttonRenewMembership != null) {
            buttonRenewMembership.setOnClickListener(v -> {
                if (currentMemberDetails != null && currentMembershipPeriodDetails != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("memberID", currentMemberDetails.getMemberID());
                    bundle.putInt(RenewMembershipFragment.ARG_SELECTED_MEMBER_TYPE_ID, currentMembershipPeriodDetails.getFkMemberTypeId());

                    if (navController != null) {
                        navController.navigate(R.id.action_memberEditFragment_to_renewMembershipFragment, bundle);
                    }
                } else {
                    Toast.makeText(getContext(), "Member data not fully loaded. Cannot renew.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void confirmDeleteMember()
    {
        if (currentMemberDetails == null || currentMemberIdToEdit == null) {
            Toast.makeText(getContext(), "Cannot delete: Member data not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Member")
                .setMessage("Are you sure you want to delete " + currentMemberDetails.getFirstName() + " " +
                        currentMemberDetails.getLastName() + "? This action and all associated membership records cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) ->
                {
                    if (databaseHelper != null) {
                        boolean deleted = databaseHelper.deleteMember(currentMemberIdToEdit); 
                        if (deleted) {
                            Toast.makeText(getContext(), "Member and their records deleted successfully.", Toast.LENGTH_SHORT).show();
                            
                            if (originalImageFilePath != null && !originalImageFilePath.isEmpty()) {
                                File imageFile = new File(originalImageFilePath);
                                if (imageFile.exists() && imageFile.isFile()) {
                                    imageFile.delete();
                                }
                            }

                            MainActivity mainActivity = (MainActivity) getActivity();
                            if (mainActivity != null)
                            {
                                mainActivity.loadAllTemplatesIntoZKFingerService();
                            }

                            if (navController != null) {
                                navController.popBackStack();
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to delete member from database.", Toast.LENGTH_SHORT).show();
                            
                        }
                    } else {
                        Toast.makeText(getContext(), "Database error. Cannot delete member.", Toast.LENGTH_SHORT).show();
                        
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void attemptUpdateMember() {
        
        if (currentMemberDetails == null || currentMembershipPeriodDetails == null) {
            Toast.makeText(getContext(), "Error: Original member or period data not loaded. Cannot update.", Toast.LENGTH_LONG).show();
            return;
        }

        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String ageStr = ageEditText.getText().toString().trim();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        String newReceiptNumber = receiptNumberEditText.getText().toString().trim(); 

        boolean isValid = true;
        
        firstNameEditText.setError(null);
        lastNameEditText.setError(null);
        ageEditText.setError(null);
        receiptNumberEditText.setError(null); 
        
        if (getContext() != null) {
            if (defaultGenderSpinnerBackground != null) genderSpinner.setBackground(defaultGenderSpinnerBackground);
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

        if (!isValid) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please correct the errors in the form.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        
        final String finalNewReceiptNumber = newReceiptNumber;
        
        final String finalFirstName = firstName;
        final String finalLastName = lastName;
        final int finalAge = age; 
        final String finalPhoneNumber = phoneNumber;
        final String finalGender = gender;
        
        executorService.execute(() -> {
            
            int periodIdToExclude = (currentMembershipPeriodDetails != null) ? currentMembershipPeriodDetails.getPeriodId() : -1;
            boolean receiptExists = databaseHelper.isReceiptNumberExists(finalNewReceiptNumber, periodIdToExclude);

            mainThreadHandler.post(() -> {
                if (receiptExists) {
                    receiptNumberEditText.setError("Receipt number already exists for another record.");
                    receiptNumberEditText.requestFocus();
                    Toast.makeText(getContext(), "This receipt number is already in use by another record.", Toast.LENGTH_SHORT).show();
                    
                } else {
                    updateMemberData(finalFirstName, finalLastName, finalAge, finalPhoneNumber, finalGender,
                            finalNewReceiptNumber, capturedFingerprintTemplate);
                }
            });
        });
        
    }

    private void updateMemberData(String firstName, String lastName, int age, String phoneNumber, String gender,
                                  String newReceiptNumber, byte[] newFingerprintTemplate) {

        String finalImageFilePath = originalImageFilePath;

        if (newSelectedPhotoPath != null && capturedImageUri != null) {
            finalImageFilePath = newSelectedPhotoPath;

            if (originalImageFilePath != null && !originalImageFilePath.equals(finalImageFilePath) && !originalImageFilePath.isEmpty()) {
                File oldImageFile = new File(originalImageFilePath);
                if (oldImageFile.exists()) {
                    if (!oldImageFile.delete()) {
                        Log.w("UpdateMemberData", "Failed to delete old camera image: " + originalImageFilePath);
                    }
                }
            }
        } else if (newSelectedGalleryImageUri != null) {
            if (getContext() != null) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String imageFileName = "JPEG_EDIT_GALLERY_" + timeStamp + ".jpg";
                String copiedGalleryPath = saveImageFromUriToInternalStorage(newSelectedGalleryImageUri, imageFileName);
                if (copiedGalleryPath != null) {
                    finalImageFilePath = copiedGalleryPath;
                    if (originalImageFilePath != null && !originalImageFilePath.equals(finalImageFilePath) && !originalImageFilePath.isEmpty()) {
                        File oldImageFile = new File(originalImageFilePath);
                        if (oldImageFile.exists()) {
                            if (!oldImageFile.delete()) {
                                Log.w("UpdateMemberData", "Failed to delete old gallery image: " + originalImageFilePath);
                            }
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Error saving new gallery image. Image not changed.", Toast.LENGTH_LONG).show();
                }
            }
        }

        currentMemberDetails.setFirstName(firstName);
        currentMemberDetails.setLastName(lastName);
        currentMemberDetails.setPhoneNumber(phoneNumber);
        currentMemberDetails.setGender(gender);
        currentMemberDetails.setAge(age);
        currentMemberDetails.setImageFilePath(finalImageFilePath);

        if (newFingerprintTemplate != null) {
            currentMemberDetails.setFingerprintTemplate(newFingerprintTemplate);
        }

        currentMembershipPeriodDetails.setReceiptNumber(newReceiptNumber);

        boolean success = false;
        if (databaseHelper != null) {
            success = databaseHelper.updateMemberAndPeriod(currentMemberDetails, currentMembershipPeriodDetails);

            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.loadAllTemplatesIntoZKFingerService();
            }
        } else {
            Toast.makeText(getContext(), "Database helper not available. Update failed.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (success) {
            Toast.makeText(getContext(), "Member details updated successfully.", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                navController.popBackStack();
            }
        } else {
            Toast.makeText(getContext(), "Failed to update member details.", Toast.LENGTH_SHORT).show();
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

    
    private void checkCameraPermissionAndOpenCamera() {
        if (getContext() == null) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            
            
            requestPermissionsLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
    }

    private void openCamera() {
        if (getContext() == null) {
            
            Toast.makeText(getActivity(), "Error: Cannot access camera.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            
            File photoFile = null;
            try {
                photoFile = createImageFile(); 
            } catch (IOException ex) {
                
                
                Toast.makeText(getContext(), "Error creating image file.", Toast.LENGTH_SHORT).show();
                return;
            }

            capturedImageUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);

            takePictureLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(getContext(), "No camera app found.", Toast.LENGTH_SHORT).show();
            
        }
    }

    private File createImageFile() throws IOException {
        
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_EDIT_" + timeStamp + "_"; 
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir == null) {
            
            throw new IOException("Cannot access external storage for pictures.");
        }
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            
            throw new IOException("Failed to create directory for pictures.");
        }

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        
        newSelectedPhotoPath = image.getAbsolutePath(); 
        
        return image;
    }

    
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
            
            requestPermissionsLauncher.launch(new String[]{permissionToRequest});
        }
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*"); 
    }

    private String saveImageFromUriToInternalStorage(Uri contentUri, String desiredFileName) {
        if (getContext() == null || contentUri == null) {
            
            return null;
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        File outputFile = null;

        try {
            inputStream = requireContext().getContentResolver().openInputStream(contentUri);
            if (inputStream == null) {
                
                return null;
            }

            
            
            File internalImageDir = new File(requireContext().getFilesDir(), "images");
            if (!internalImageDir.exists()) {
                if (!internalImageDir.mkdirs()) {
                    
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
            
            return outputFile.getAbsolutePath(); 

        } catch (IOException e) {
            
            if (outputFile != null && outputFile.exists()) { 
                outputFile.delete();
            }
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException ignored) {
                
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        MainActivity.stopFingerprintEnrollment();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}