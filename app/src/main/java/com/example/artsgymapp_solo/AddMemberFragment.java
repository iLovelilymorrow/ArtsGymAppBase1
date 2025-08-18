package com.example.artsgymapp_solo;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
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
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
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

public class AddMemberFragment extends Fragment implements FingerprintEnrollmentCallback
{
    private int activeImageTarget = 1;

    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText ageEditText;
    private Spinner genderSpinner;
    private EditText phoneNumberEditText;
    private Spinner memberTypeSpinner;
    private ImageView memberPreviewImageView;
    private MaterialButton buttonAddMember, buttonCancel;

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

    private NavController navController;

    private ImageView fingerprintImageView1;
    private ImageView fingerprintImageView2;
    private TextView fingerprintStatusTextView1; 
    private TextView fingerprintStatusTextView2; 

    private byte[] capturedFingerprintTemplate1; 
    private byte[] capturedFingerprintTemplate2; 
    private Bitmap capturedFingerprintBitmap1; 
    private Bitmap capturedFingerprintBitmap2; 

    private int activeFingerprintTarget = 0;

    private MainActivityViewModel mainActivityViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(getContext());
        mainActivityViewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);
        
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

                    if (permissions.containsKey(Manifest.permission.CAMERA)) 
                    {
                        if (cameraGranted) {
                            openCamera();
                        } else {
                            Toast.makeText(getContext(), "Camera permission is required to take photos.", Toast.LENGTH_LONG).show();
                        }
                    }

                    if (permissions.containsKey(readPermissionNeeded)) {
                        if (readStorageGranted) {
                            openGallery();
                        } else {
                            Toast.makeText(getContext(), "Storage permission is required to choose from gallery.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (activeImageTarget == 1) {
                            selectedImageUri1 = null;
                            if (capturedImageUri1 != null && memberPreviewImageView != null) {
                                Glide.with(AddMemberFragment.this).load(capturedImageUri1).into(memberPreviewImageView);
                            } 
                        } else {
                            selectedImageUri2 = null;
                            if (capturedImageUri2 != null && memberPreviewImageView2 != null) {
                                Glide.with(AddMemberFragment.this).load(capturedImageUri2).into(memberPreviewImageView2);
                            }
                        }
                    } else {
                        if (activeImageTarget == 1) {
                            if (currentPhotoPath1 != null)
                            {
                                File photoFile = new File(currentPhotoPath1);
                                if (photoFile.exists() && photoFile.length() == 0) 
                                {
                                    photoFile.delete();
                                }
                            }
                            currentPhotoPath1 = null;
                            capturedImageUri1 = null;
                        } else {
                            if (currentPhotoPath2 != null) 
                            {
                                File photoFile = new File(currentPhotoPath2);
                                if (photoFile.exists() && photoFile.length() == 0) 
                                {
                                    photoFile.delete();
                                }
                            }
                            currentPhotoPath2 = null;
                            capturedImageUri2 = null;
                        }
                    }
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) { //todo 3 data survival
                        if (activeImageTarget == 1) {
                            selectedImageUri1 = uri;
                            mainActivityViewModel.setUri1(selectedImageUri1);
                            capturedImageUri1 = null;
                            currentPhotoPath1 = null;

                            if (memberPreviewImageView != null)
                            {
                                Glide.with(AddMemberFragment.this).load(selectedImageUri1).into(memberPreviewImageView);
                            }
                        } else {
                            selectedImageUri2 = uri;
                            mainActivityViewModel.setUri2(selectedImageUri2);
                            capturedImageUri2 = null;
                            currentPhotoPath2 = null;
                            if (memberPreviewImageView2 != null) {
                                Glide.with(AddMemberFragment.this).load(selectedImageUri2).into(memberPreviewImageView2);
                            }
                        }
                    } 
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        
        fingerprintImageView1 = view.findViewById(R.id.fingerprintImageView1);
        fingerprintImageView2 = view.findViewById(R.id.fingerprintImageView2);
        fingerprintStatusTextView1 = view.findViewById(R.id.fingerprintStatusTextView1);
        fingerprintStatusTextView2 = view.findViewById(R.id.fingerprintStatusTextView2);
        
        if (fingerprintStatusTextView1 != null) fingerprintStatusTextView1.setText(R.string.StartCapture);
        if (fingerprintStatusTextView2 != null) fingerprintStatusTextView2.setText(R.string.StartCapture);
        
        fingerprintImageView1.setOnClickListener(v ->
        {
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null)
            {
                activeFingerprintTarget = 1;
                mainActivity.startFingerprintEnrollment(activeFingerprintTarget, this, null);

                if(capturedFingerprintTemplate1 != null)
                {
                    capturedFingerprintTemplate1 = null;
                    capturedFingerprintBitmap1 = null;
                    if (fingerprintImageView1 != null) fingerprintImageView1.setImageResource(R.drawable.addfingerprint);
                }
            }
        });

        fingerprintImageView2.setOnClickListener(v ->
        {
            if(capturedFingerprintTemplate1 != null)
            {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity != null) {
                    activeFingerprintTarget = 2;
                    mainActivity.startFingerprintEnrollment(activeFingerprintTarget, this, capturedFingerprintTemplate1);

                    if(capturedFingerprintTemplate2 != null)
                    {
                        capturedFingerprintTemplate2 = null;
                        capturedFingerprintBitmap2 = null;
                        if (fingerprintImageView2 != null) fingerprintImageView2.setImageResource(R.drawable.addfingerprint);
                    }
                }
            }
            else
            {
                Toast.makeText(getContext(), "Please capture fingerprint for Member 1 first.", Toast.LENGTH_LONG).show();
            }
        });

        // DATA SURVIVAL
        String imagePath1 = mainActivityViewModel.getImagePath1().getValue();
        String imagePath2 = mainActivityViewModel.getImagePath2().getValue();
        Bitmap capturedFingerprintBitmap1 = mainActivityViewModel.getBitmap1().getValue();
        Bitmap capturedFingerprintBitmap2 = mainActivityViewModel.getBitmap2().getValue();
        byte[] capturedFingerprintTemplate1 = mainActivityViewModel.getByte1().getValue();
        byte[] capturedFingerprintTemplate2 = mainActivityViewModel.getByte2().getValue();
        Uri image1 = mainActivityViewModel.getUri1().getValue();
        Uri image2 = mainActivityViewModel.getUri2().getValue();

        if (capturedFingerprintBitmap1 != null)
        {
            fingerprintImageView1.setImageBitmap(capturedFingerprintBitmap1);
        }

        if (capturedFingerprintBitmap2 != null)
        {
            fingerprintImageView2.setImageBitmap(capturedFingerprintBitmap2);
        }

        if (capturedFingerprintTemplate1 != null)
        {
            this.capturedFingerprintTemplate1 = capturedFingerprintTemplate1;
        }

        if (capturedFingerprintTemplate2 != null)
        {
            this.capturedFingerprintTemplate2 = capturedFingerprintTemplate2;
        }

        if (image1 != null)
        {
            Glide.with(this).load(image1).into(memberPreviewImageView);

            if (imagePath1 != null && !imagePath1.isEmpty())
            {
                this.currentPhotoPath1 = imagePath1;
                this.capturedImageUri1 = image1;
                this.selectedImageUri1 = null;
            }
            else
            {
                this.selectedImageUri1 = image1;
                this.currentPhotoPath1 = null;
                this.capturedImageUri1 = null;
            }
        }

        if (image2 != null)
        {
            Glide.with(this).load(image2).into(memberPreviewImageView2);

            if (imagePath2 != null && !imagePath2.isEmpty())
            {
                this.currentPhotoPath2 = imagePath2;
                this.capturedImageUri2 = image2;
                this.selectedImageUri2 = null;
            }
            else
            {
                this.selectedImageUri2 = image2;
                this.currentPhotoPath2 = null;
                this.capturedImageUri2 = null;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
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
        buttonCancel = view.findViewById(R.id.buttonCancel);

        if (genderSpinner != null) {
            defaultGenderSpinnerBackground = genderSpinner.getBackground();
        }
        if (memberTypeSpinner != null) {
            defaultMemberTypeSpinnerBackground = memberTypeSpinner.getBackground();
        }

        setupSpinners();
        setupSpinnerErrorClearing();
        setupDatePickers();

        memberPreviewImageView.setOnClickListener(v -> {
            this.activeImageTarget = 1;
            showImagePickDialog();
        });

        memberPreviewImageView2.setOnClickListener(v -> {
            this.activeImageTarget = 2;
            showImagePickDialog();
        });

        if (buttonAddMember != null) {
            buttonAddMember.setOnClickListener(v ->
            {
                attemptSaveMember();
            });
        }

        if (buttonCancel != null) {
            buttonCancel.setOnClickListener(v -> {
                navController.popBackStack();
            });
        }

        return view;
    }

    @Override
    public void onEnrollmentProgress(int targetMember, int currentScan, int totalScans, String message) {
        
        if (targetMember == 1) {
            if (fingerprintStatusTextView1 != null) fingerprintStatusTextView1.setText(message);
        } else if (targetMember == 2) {
            if (fingerprintStatusTextView2 != null) fingerprintStatusTextView2.setText(message);
        }
    }
    @Override
    public void onEnrollmentComplete(int targetMember, Bitmap finalImage, byte[] finalTemplate)
    {
        //todo data survival fingerprint
        if (targetMember == 1)
        {
            capturedFingerprintTemplate1 = finalTemplate;
            capturedFingerprintBitmap1 = finalImage; 
            if (fingerprintImageView1 != null) fingerprintImageView1.setImageBitmap(finalImage);
            if (fingerprintStatusTextView1 != null) fingerprintStatusTextView1.setText(R.string.FingerprintCaptured);
            mainActivityViewModel.setByte1(capturedFingerprintTemplate1);
            mainActivityViewModel.setBitmap1(capturedFingerprintBitmap1);
            Toast.makeText(getContext(), "Fingerprint enrolled for Member 1!", Toast.LENGTH_SHORT).show();
        }
        else if (targetMember == 2)
        {
            capturedFingerprintTemplate2 = finalTemplate;
            capturedFingerprintBitmap2 = finalImage; 
            if (fingerprintImageView2 != null) fingerprintImageView2.setImageBitmap(finalImage);
            if (fingerprintStatusTextView2 != null) fingerprintStatusTextView2.setText(R.string.FingerprintCaptured);
            mainActivityViewModel.setByte2(capturedFingerprintTemplate2);
            mainActivityViewModel.setBitmap2(capturedFingerprintBitmap2);
            Toast.makeText(getContext(), "Fingerprint enrolled for Member 2!", Toast.LENGTH_SHORT).show();
        }
        activeFingerprintTarget = 0; 
    }

    @Override
    public void onEnrollmentFailed(int targetMember, String errorMessage) {
        
        if (targetMember == 1) {
            capturedFingerprintTemplate1 = null; 
            capturedFingerprintBitmap1 = null; 
            if (fingerprintImageView1 != null) fingerprintImageView1.setImageResource(R.drawable.addfingerprint); 
            if (fingerprintStatusTextView1 != null) fingerprintStatusTextView1.setText("Enrollment Failed: " + errorMessage);
        } else if (targetMember == 2) {
            capturedFingerprintTemplate2 = null; 
            capturedFingerprintBitmap2 = null; 
            if (fingerprintImageView2 != null) fingerprintImageView2.setImageResource(R.drawable.addfingerprint); 
            if (fingerprintStatusTextView2 != null) fingerprintStatusTextView2.setText("Enrollment Failed: " + errorMessage);
        }
        Toast.makeText(getContext(), "Fingerprint enrollment failed for Member " + targetMember + ": " + errorMessage, Toast.LENGTH_LONG).show();
        activeFingerprintTarget = 0; 
    }

    @Override
    public void onResume() {
        super.onResume();
        
        
        if (capturedFingerprintTemplate1 == null) {
            
            if (fingerprintImageView1 != null) fingerprintImageView1.setImageResource(R.drawable.addfingerprint);
            if (fingerprintStatusTextView1 != null) fingerprintStatusTextView1.setText(R.string.StartCapture);
        } else {
            
            if (fingerprintImageView1 != null && capturedFingerprintBitmap1 != null) {
                fingerprintImageView1.setImageBitmap(capturedFingerprintBitmap1);
            }
            if (fingerprintStatusTextView1 != null) fingerprintStatusTextView1.setText(R.string.FingerprintCaptured);
        }
        
        if (capturedFingerprintTemplate2 == null) {
            
            if (fingerprintImageView2 != null) fingerprintImageView2.setImageResource(R.drawable.addfingerprint);
            if (fingerprintStatusTextView2 != null) fingerprintStatusTextView2.setText(R.string.StartCapture);
        } else {
            if (fingerprintImageView2 != null && capturedFingerprintBitmap2 != null) {
                fingerprintImageView2.setImageBitmap(capturedFingerprintBitmap2);
            }
            if (fingerprintStatusTextView2 != null) fingerprintStatusTextView2.setText(R.string.FingerprintCaptured);
        }
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        clearForm();

        capturedFingerprintTemplate1 = null;
        capturedFingerprintTemplate2 = null;
        capturedFingerprintBitmap1 = null;
        capturedFingerprintBitmap2 = null;
        activeFingerprintTarget = 0;

        MainActivity.stopFingerprintEnrollment();

        if (executorService != null && !executorService.isShutdown())
        {
            executorService.shutdown();
        }
    }

    private void attemptSaveMember()
    {
        if (firstNameEditText != null) firstNameEditText.setError(null);
        if (lastNameEditText != null) lastNameEditText.setError(null);
        if (ageEditText != null) ageEditText.setError(null);
        if (receiptNumberEditText != null) receiptNumberEditText.setError(null);
        if (genderSpinner != null && defaultGenderSpinnerBackground != null) genderSpinner.setBackground(defaultGenderSpinnerBackground);
        if (memberTypeSpinner != null && defaultMemberTypeSpinnerBackground != null) memberTypeSpinner.setBackground(defaultMemberTypeSpinnerBackground);

        String firstName1 = firstNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(firstName1)) {
            firstNameEditText.setError("First name is required");
            firstNameEditText.requestFocus();
            
            return;
        }

        String lastName1 = lastNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(lastName1)) {
            lastNameEditText.setError("Last name is required");
            lastNameEditText.requestFocus();
            
            return;
        }

        String ageStr1 = ageEditText.getText().toString().trim();
        int age1 = 0;
        if (TextUtils.isEmpty(ageStr1)) {
            ageEditText.setError("Age is required");
            ageEditText.requestFocus();
            
            return;
        } else {
            try {
                age1 = Integer.parseInt(ageStr1);
                if (age1 <= 0 || age1 > 120) {
                    ageEditText.setError("Enter a valid age (1-120)");
                    ageEditText.requestFocus();
                    
                    return;
                }
            } catch (NumberFormatException e) {
                ageEditText.setError("Enter a valid age (number)");
                ageEditText.requestFocus();
                
                return;
            }
        }

        String phoneNumber1 = phoneNumberEditText.getText().toString().trim();

        String receiptNumberStr = receiptNumberEditText.getText().toString().trim();
        if (TextUtils.isEmpty(receiptNumberStr)) {
            receiptNumberEditText.setError("Receipt number is required");
            receiptNumberEditText.requestFocus();
            
            return;
        }

        String gender1;
        if (genderSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Please select a gender.", Toast.LENGTH_SHORT).show();
            genderSpinner.setBackgroundResource(R.drawable.spinner_error_background);
            
            return;
        } else {
            gender1 = genderSpinner.getSelectedItem().toString();
        }

        int selectedMemberTypeIdFromSpinner = -1;
        MemberType selectedTypeObject = null;
        if (memberTypeSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Please select a membership type.", Toast.LENGTH_SHORT).show();
            memberTypeSpinner.setBackgroundResource(R.drawable.spinner_error_background);
            
            return;
        } else {
            int actualListPosition = memberTypeSpinner.getSelectedItemPosition() - 1;
            if (memberTypeList != null && actualListPosition >= 0 && actualListPosition < memberTypeList.size()) {
                selectedTypeObject = memberTypeList.get(actualListPosition);
                selectedMemberTypeIdFromSpinner = selectedTypeObject.getId();
            } else {
                Toast.makeText(getContext(), "Invalid membership type selected.", Toast.LENGTH_SHORT).show();
                memberTypeSpinner.setBackgroundResource(R.drawable.spinner_error_background);
                
                return;
            }
        }

        if (selectedRegistrationDate == null) {
            Toast.makeText(getContext(), "Please select or confirm a registration date.", Toast.LENGTH_LONG).show();
            
            return;
        }
        if (selectedExpirationDate == null) {
            Toast.makeText(getContext(), "Please select or confirm an expiration date.", Toast.LENGTH_LONG).show();
            
            return;
        }
        if (selectedExpirationDate.isBefore(selectedRegistrationDate)) {
            Toast.makeText(getContext(), "Expiration date cannot be before the registration date.", Toast.LENGTH_LONG).show();
            
            return;
        }

        if (selectedExpirationDate.isBefore(selectedRegistrationDate) || selectedExpirationDate.isEqual(selectedRegistrationDate)) {
            Toast.makeText(getContext(), "End date must be after the start date.", Toast.LENGTH_SHORT).show();
            expirationDateTextView.setError("Invalid dates");
            startDateTextView.setError("Invalid dates");
            return;
        }

        String imagePathToSave1 = null;
        if (capturedImageUri1 != null && currentPhotoPath1 != null && !currentPhotoPath1.isEmpty())
        {
            imagePathToSave1 = currentPhotoPath1;
        }
        else if (selectedImageUri1 != null)
        {
            if (getContext() != null) {
                String timeStamp1 = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_M1", java.util.Locale.getDefault()).format(new java.util.Date());
                String imageFileName1 = "MEMBER_IMG_" + timeStamp1 + ".jpg";
                imagePathToSave1 = saveImageFromUriToInternalStorage(selectedImageUri1, imageFileName1);
                if (imagePathToSave1 == null) {
                    Toast.makeText(getContext(), "Failed to save image for Member 1.", Toast.LENGTH_LONG).show();
                    
                    return;
                }
            } else {
                
                Toast.makeText(getActivity(), "Error saving image for Member 1.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        if (capturedFingerprintTemplate1 == null)
        {
            Toast.makeText(getContext(), "Fingerprint scan is required for Member 1.", Toast.LENGTH_LONG).show();
            if (fingerprintStatusTextView1 != null) fingerprintStatusTextView1.setText("Fingerprint required!");
            
            return;
        }

        boolean isTwoInOneMembership = selectedTypeObject != null && selectedTypeObject.isTwoInOne();

        String firstName2 = null;
        String lastName2 = null;
        int age2 = 0;
        String gender2 = null;
        String phoneNumber2 = null;
        String imagePathToSave2 = null;

        if (isTwoInOneMembership) {
            

            if (firstNameEditText2 != null) firstNameEditText2.setError(null);
            if (lastNameEditText2 != null) lastNameEditText2.setError(null);
            if (ageEditText2 != null) ageEditText2.setError(null);
            if (genderSpinner2 != null && defaultGenderSpinnerBackground != null) genderSpinner2.setBackground(defaultGenderSpinnerBackground);

            firstName2 = firstNameEditText2.getText().toString().trim();
            if (TextUtils.isEmpty(firstName2)) {
                firstNameEditText2.setError("First name is required");
                firstNameEditText2.requestFocus();
                
                return;
            }

            lastName2 = lastNameEditText2.getText().toString().trim();
            if (TextUtils.isEmpty(lastName2)) {
                lastNameEditText2.setError("Last name is required");
                lastNameEditText2.requestFocus();
                
                return;
            }

            String ageStr2 = ageEditText2.getText().toString().trim();
            if (TextUtils.isEmpty(ageStr2)) {
                ageEditText2.setError("Age is required");
                ageEditText2.requestFocus();
                
                return;
            } else {
                try {
                    age2 = Integer.parseInt(ageStr2);
                    if (age2 <= 0 || age2 > 120) {
                        ageEditText2.setError("Enter a valid age (1-120)");
                        ageEditText2.requestFocus();
                        
                        return;
                    }
                } catch (NumberFormatException e) {
                    ageEditText2.setError("Enter a valid age (number)");
                    ageEditText2.requestFocus();
                    return;
                }
            }

            if (genderSpinner2.getSelectedItemPosition() == 0) {
                Toast.makeText(getContext(), "Please select a gender.", Toast.LENGTH_SHORT).show();
                genderSpinner2.setBackgroundResource(R.drawable.spinner_error_background);
                
                return;
            } else {
                gender2 = genderSpinner2.getSelectedItem().toString();
            }

            phoneNumber2 = phoneNumberEditText2.getText().toString().trim();

            if (capturedImageUri2 != null && currentPhotoPath2 != null && !currentPhotoPath2.isEmpty())
            {
                imagePathToSave2 = currentPhotoPath2;
            } else if (selectedImageUri2 != null) {
                if (getContext() != null) {
                    String timeStamp2 = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_M2", java.util.Locale.getDefault()).format(new java.util.Date());
                    String imageFileName2 = "MEMBER_IMG_" + timeStamp2 + ".jpg";
                    imagePathToSave2 = saveImageFromUriToInternalStorage(selectedImageUri2, imageFileName2);
                    if (imagePathToSave2 == null) {
                        Toast.makeText(getContext(), "Failed to save image for Member 2.", Toast.LENGTH_LONG).show();
                        return;
                    }
                } else {
                    Toast.makeText(getActivity(), "Error saving image for Member 2.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            
            if (capturedFingerprintTemplate2 == null) {
                Toast.makeText(getContext(), "Fingerprint scan is required for Member 2.", Toast.LENGTH_LONG).show();
                if (fingerprintStatusTextView2 != null) fingerprintStatusTextView2.setText("Fingerprint required!");
                return;
            }
            
        }

        final String finalFirstName1 = firstName1;
        final String finalLastName1 = lastName1;
        final String finalPhoneNumber1 = phoneNumber1;
        final String finalGender1 = gender1;
        final int finalAge1 = age1;
        final String finalImagePath1 = imagePathToSave1;
        final byte[] finalFingerprintTemplate1 = capturedFingerprintTemplate1; 

        final int finalSelectedMemberTypeId = selectedMemberTypeIdFromSpinner;
        final LocalDate finalRegDate = selectedRegistrationDate;
        final LocalDate finalExpDate = selectedExpirationDate;
        final String finalReceiptNumber = receiptNumberStr;
        final boolean finalIsTwoInOneMembership = isTwoInOneMembership;

        final String finalFirstName2 = firstName2;
        final String finalLastName2 = lastName2;
        final String finalPhoneNumber2 = phoneNumber2;
        final String finalGender2 = gender2;
        final int finalAge2 = age2;
        final String finalImagePath2 = imagePathToSave2;
        final byte[] finalFingerprintTemplate2 = capturedFingerprintTemplate2; 

        executorService.execute(() -> {
            boolean localProceedWithSave = true;
            String localReceiptErrorMessage = null;

            if (!finalIsTwoInOneMembership) {
                boolean receiptExists = databaseHelper.isReceiptNumberExists(finalReceiptNumber, -1);
                if (receiptExists) {
                    localProceedWithSave = false;
                    localReceiptErrorMessage = "This receipt number is already in use.";
                }
            }

            final boolean finalProceedWithSave = localProceedWithSave;
            final String finalReceiptErrMessageForPost = localReceiptErrorMessage;

            mainThreadHandler.post(() ->
            {
                if (!finalProceedWithSave && finalReceiptErrMessageForPost != null)
                {
                    receiptNumberEditText.setError(finalReceiptErrMessageForPost);
                    receiptNumberEditText.requestFocus();
                    Toast.makeText(getContext(), finalReceiptErrMessageForPost, Toast.LENGTH_SHORT).show();
                    
                }
                else
                {
                    saveMemberData(
                            finalFirstName1, finalLastName1, finalPhoneNumber1, finalGender1, finalAge1, finalImagePath1,
                            finalSelectedMemberTypeId, finalRegDate, finalExpDate, finalReceiptNumber,
                            finalIsTwoInOneMembership,
                            finalFirstName2, finalLastName2, finalPhoneNumber2, finalGender2, finalAge2, finalImagePath2,
                            finalFingerprintTemplate1, finalFingerprintTemplate2 
                    );
                }
            });
        });
    }

    private void saveMemberData(String firstName1, String lastName1, String phoneNumber1, String gender1, int age1,
                                String imagePathToSave1, int selectedMemberTypeId, LocalDate registrationDate,
                                LocalDate expirationDate, String receiptNumber, boolean isTwoInOneMembership,
                                String firstName2, String lastName2, String phoneNumber2, String gender2, int age2,
                                String imagePathToSave2, byte[] fingerprintTemplate1, byte[] fingerprintTemplate2
    )
    {
        mainActivityViewModel.clearAllData();
        SQLiteDatabase db = null;
        boolean overallSuccess = false;
        String member1DbId = null;
        String member2DbId = null;

        try {
            db = databaseHelper.getWritableDatabase();
            db.beginTransaction();

            String member1GeneratedId = databaseHelper.generateNewMemberId(db);
            if (member1GeneratedId == null) {
                
            } else {
                
                member1DbId = databaseHelper.addMemberInExternalTransaction(db, member1GeneratedId,
                        firstName1, lastName1, phoneNumber1, gender1, age1, imagePathToSave1,
                        fingerprintTemplate1, 
                        selectedMemberTypeId, registrationDate, expirationDate, receiptNumber);

                if (member1DbId != null) {
                    

                    if (isTwoInOneMembership) {
                        String member2GeneratedId = databaseHelper.generateNewMemberId(db);
                        if (member2GeneratedId == null) {
                            
                        } else {
                            
                            member2DbId = databaseHelper.addMemberInExternalTransaction(db, member2GeneratedId,
                                    firstName2, lastName2, phoneNumber2, gender2, age2, imagePathToSave2,
                                    fingerprintTemplate2, 
                                    selectedMemberTypeId, registrationDate, expirationDate, receiptNumber);

                            if (member2DbId != null) {
                                

                                db.setTransactionSuccessful();
                                overallSuccess = true;
                            } else {
                                
                            }
                        }
                    } else {
                        db.setTransactionSuccessful();
                        overallSuccess = true;
                    }
                } else {
                    
                }
            }
        } catch (Exception e)
        {
            overallSuccess = false;
        }
        finally {
            if (db != null && db.inTransaction()) {
                db.endTransaction();
            }
        }

        if (overallSuccess) {
            if (isTwoInOneMembership) {
                showToast("Both members added successfully!");
                
            } else {
                showToast("Member added successfully!");
            }
            clearForm();
            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null)
            {
                mainActivity.loadAllTemplatesIntoZKFingerService();
            }
        } else {
            if (isTwoInOneMembership) {
                showToast("Failed to add one or both members. Please check details and try again.");
            } else {
                showToast("Failed to add member. Please try again.");
            }
        }
    }

    private void setupDatePickers() {
        if (startDateTextView != null) {
            startDateTextView.setOnClickListener(v ->
            {
                startDateTextView.setError(null);
                expirationDateTextView.setError(null);
                final java.util.Calendar c = java.util.Calendar.getInstance();
                int year, month, day;

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

                            autoCalculateAndSetDefaultExpirationDate();
                        }, year, month, day);

                java.util.Calendar today = java.util.Calendar.getInstance();

                Calendar minDateCalendar = getCalendar(today);
                datePickerDialog.getDatePicker().setMinDate(minDateCalendar.getTimeInMillis());

                java.util.Calendar maxDateCalendar = java.util.Calendar.getInstance();
                maxDateCalendar.setTime(today.getTime());
                maxDateCalendar.set(java.util.Calendar.DAY_OF_MONTH, maxDateCalendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
                maxDateCalendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
                maxDateCalendar.set(java.util.Calendar.MINUTE, 59);
                maxDateCalendar.set(java.util.Calendar.SECOND, 59);
                maxDateCalendar.set(java.util.Calendar.MILLISECOND, 999);
                datePickerDialog.getDatePicker().setMaxDate(maxDateCalendar.getTimeInMillis());

                datePickerDialog.show();
            });
        }

        if (expirationDateTextView != null) {
            expirationDateTextView.setOnClickListener(v ->
            {
                startDateTextView.setError(null);
                expirationDateTextView.setError(null);
                final java.util.Calendar c = java.util.Calendar.getInstance();
                int year, month, day;

                if (selectedExpirationDate != null) {
                    year = selectedExpirationDate.getYear();
                    month = selectedExpirationDate.getMonthValue() - 1;
                    day = selectedExpirationDate.getDayOfMonth();
                } else if (selectedRegistrationDate != null) {
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

                        }, year, month, day);

                if (selectedRegistrationDate != null) {
                    java.util.Calendar expMinCal = java.util.Calendar.getInstance();
                    expMinCal.set(selectedRegistrationDate.getYear(),
                            selectedRegistrationDate.getMonthValue() - 1,
                            selectedRegistrationDate.getDayOfMonth());

                    datePickerDialog.getDatePicker().setMinDate(expMinCal.getTimeInMillis());
                } else
                {
                    java.util.Calendar earliestAllowedStartDate = java.util.Calendar.getInstance();
                    earliestAllowedStartDate.add(java.util.Calendar.MONTH, -1);
                    earliestAllowedStartDate.set(java.util.Calendar.DAY_OF_MONTH, earliestAllowedStartDate.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
                    earliestAllowedStartDate.add(java.util.Calendar.DAY_OF_MONTH, -4);
                    earliestAllowedStartDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    earliestAllowedStartDate.set(java.util.Calendar.MINUTE, 0);
                    earliestAllowedStartDate.set(java.util.Calendar.SECOND, 0);
                    earliestAllowedStartDate.set(java.util.Calendar.MILLISECOND, 0);
                    datePickerDialog.getDatePicker().setMinDate(earliestAllowedStartDate.getTimeInMillis());
                }

                datePickerDialog.show();
            });
        }
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

    private void autoCalculateAndSetDefaultExpirationDate() {
        if (expirationDateTextView == null || selectedRegistrationDate == null) {
            if (expirationDateTextView != null) {
                expirationDateTextView.setText("Select End Date");
            }
            selectedExpirationDate = null;
            return;
        }

        int selectedSpinnerPosition = memberTypeSpinner.getSelectedItemPosition();
        if (selectedSpinnerPosition == 0 || memberTypeList == null || memberTypeList.isEmpty()) {
            expirationDateTextView.setText("Select type for auto-calc");
            selectedExpirationDate = null;
            return;
        }

        int actualListPosition = selectedSpinnerPosition - 1;
        LocalDate calculatedExpDate = null;

        if (actualListPosition >= 0 && actualListPosition < memberTypeList.size()) {
            MemberType selectedType = memberTypeList.get(actualListPosition);
            int durationDays = selectedType.getDurationDays();

            if (durationDays > 0) {
                calculatedExpDate = selectedRegistrationDate.plusDays(durationDays);
            }
        }

        if (calculatedExpDate != null) {
            selectedExpirationDate = calculatedExpDate;
            expirationDateTextView.setText(selectedExpirationDate.format(uiDateFormatter));
            
        } else {
            expirationDateTextView.setText("Select End Date");
            selectedExpirationDate = null;
        }
    }

    private void setupSpinners()
    {
        if (getContext() == null) return;

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.gender_array, R.layout.spinner_item_collapsed);

        genderAdapter.setDropDownViewResource(R.layout.spinner_item);
        genderSpinner.setAdapter(genderAdapter);
        genderSpinner2.setAdapter(genderAdapter);

        memberTypeList = databaseHelper.getAllMemberTypes();
        List<MemberType> displayMemberTypes = new ArrayList<>();

        displayMemberTypes.add(new MemberType(0, "Select Membership Type*", 0, false));
        displayMemberTypes.addAll(memberTypeList);

        ArrayAdapter<MemberType> memberTypeAdapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_collapsed, displayMemberTypes);
        memberTypeAdapter.setDropDownViewResource(R.layout.spinner_item);
        memberTypeSpinner.setAdapter(memberTypeAdapter);
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
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        if (memberTypeSpinner != null) {
            memberTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0) {
                        memberTypeSpinner.setBackground(defaultMemberTypeSpinnerBackground);
                        selectedRegistrationDate = LocalDate.now();
                        if (startDateTextView != null) {
                            startDateTextView.setText(selectedRegistrationDate.format(uiDateFormatter)); //startDateTextView.setText(selectedRegistrationDate.format(uiDateFormatter));
                            
                        }
                        autoCalculateAndSetDefaultExpirationDate();

                        MemberType selectedType = null;
                        if (parent.getItemAtPosition(position) instanceof MemberType) {
                            selectedType = (MemberType) parent.getItemAtPosition(position);
                        }

                        if (selectedType != null && secondMemberContainer != null) {
                            if (selectedType.isTwoInOne()) {
                                secondMemberContainer.setVisibility(View.VISIBLE);
                                
                            } else {
                                secondMemberContainer.setVisibility(View.GONE);
                                
                            }
                        }
                    } else {
                        memberTypeSpinner.setBackground(defaultMemberTypeSpinnerBackground);
                        if (secondMemberContainer != null) {
                            secondMemberContainer.setVisibility(View.GONE);
                        }
                        
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
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

    private void showImagePickDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
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

        boolean allPermissionsGranted = true;
        for (String perm : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            
            openCamera();
        } else {
            requestPermissionsLauncher.launch(permissionsToRequest);
        }
    }

    private void openCamera()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null)
        {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            }
            catch (IOException ex)
            {
                Toast.makeText(getContext(), "Could not create image file.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null)
            {
                Uri photoURI;

                if (getContext() == null)
                {
                    return;
                }

                String authority = requireContext().getPackageName() + ".provider";

                //todo 2 data survival
                if (activeImageTarget == 1)
                {
                    capturedImageUri1 = FileProvider.getUriForFile(requireContext(), authority, photoFile);
                    mainActivityViewModel.setUri1(capturedImageUri1);
                    photoURI = capturedImageUri1;
                }
                else
                {
                    capturedImageUri2 = FileProvider.getUriForFile(requireContext(), authority, photoFile);
                    mainActivityViewModel.setUri2(capturedImageUri2);
                    photoURI = capturedImageUri2;
                }

                if (photoURI != null)
                {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    takePictureLauncher.launch(takePictureIntent);
                }
                else
                {
                    Toast.makeText(getContext(), "Failed to prepare image for camera.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else
        {
            Toast.makeText(getContext(), "No camera app found.", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                
                throw new IOException("Failed to create directory: " + storageDir.getAbsolutePath());
            }
        }

        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        //todo 4 data survival
        if (activeImageTarget == 1)
        {
            currentPhotoPath1 = image.getAbsolutePath();
            mainActivityViewModel.setImagePath1(currentPhotoPath1);
        }
        else
        {
            currentPhotoPath2 = image.getAbsolutePath();
            mainActivityViewModel.setImagePath2(currentPhotoPath2);
        }
        return image;
    }

    private void checkStoragePermissionAndOpenGallery() {
        String permissionNeeded;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionNeeded = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permissionNeeded = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permissionNeeded) == PackageManager.PERMISSION_GRANTED) {
            
            openGallery();
        } else {
            
            requestPermissionsLauncher.launch(new String[]{permissionNeeded});
        }
    }

    private void openGallery() {
        pickImageLauncher.launch("image/*");
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
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
            memberPreviewImageView.setImageResource(R.drawable.addimage);
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
            memberPreviewImageView2.setImageResource(R.drawable.addimage);
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

        
        if (fingerprintImageView1 != null) fingerprintImageView1.setImageResource(R.drawable.addfingerprint);
        if (fingerprintStatusTextView1 != null) fingerprintStatusTextView1.setText(R.string.StartCapture);
        capturedFingerprintTemplate1 = null;
        capturedFingerprintBitmap1 = null;

        if (fingerprintImageView2 != null) fingerprintImageView2.setImageResource(R.drawable.addfingerprint);
        if (fingerprintStatusTextView2 != null) fingerprintStatusTextView2.setText(R.string.StartCapture);
        capturedFingerprintTemplate2 = null;
        capturedFingerprintBitmap2 = null;

        currentPhotoPath1 = null;
        capturedImageUri1 = null;
        selectedImageUri1 = null;

        currentPhotoPath2 = null;
        capturedImageUri2 = null;
        selectedImageUri2 = null;

        activeFingerprintTarget = 0;
    }



    private String saveImageFromUriToInternalStorage(Uri contentUri, String fileName) {
        if (getContext() == null || contentUri == null) {
            
            return null;
        }

        File internalStorageDir = new File(getContext().getFilesDir(), "MemberImages");
        if (!internalStorageDir.exists()) {
            if (!internalStorageDir.mkdirs()) {
                
                return null;
            }
        }

        File destinationFile = new File(internalStorageDir, fileName);

        try (java.io.InputStream inputStream = getContext().getContentResolver().openInputStream(contentUri);
             java.io.OutputStream outputStream = new java.io.FileOutputStream(destinationFile)) {

            if (inputStream == null) {
                
                return null;
            }

            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            
            return destinationFile.getAbsolutePath();

        }
        catch(java.io.FileNotFoundException e)
        {
            Toast.makeText(getContext(), "Error: Source image file not found.", Toast.LENGTH_LONG).show();
        }
        catch(IOException e)
        {
            Toast.makeText(getContext(), "Error saving image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        catch (SecurityException e)
        {
            Toast.makeText(getContext(), "Security error: Cannot access image file.", Toast.LENGTH_LONG).show();
        }
        return null;
    }
}