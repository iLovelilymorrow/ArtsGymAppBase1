package com.example.artsgymapp_solo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;

import androidx.lifecycle.ViewModelProvider;

import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import android.os.SystemClock; 
import com.google.android.material.switchmaterial.SwitchMaterial; 
import com.zkteco.android.biometric.FingerprintExceptionListener;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.core.utils.ToolUtils;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor;
import com.zkteco.android.biometric.module.fingerprintreader.FingprintFactory;
import com.zkteco.android.biometric.module.fingerprintreader.ZKFingerService;
import com.zkteco.android.biometric.module.fingerprintreader.exception.FingerprintException;
import com.example.artsgymapp_solo.ZKUSBManager.ZKUSBManager;
import com.example.artsgymapp_solo.ZKUSBManager.ZKUSBManagerListener;

import android.os.Handler;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
{
    private static MainActivity instance;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    private FrameLayout idleOverlayView;

    private SettingsViewModel settingsViewModel;
    private String currentStoredAdminUsername = "";

    private SwitchMaterial adminModeToggleSwitch;
    
    private static final long TRIPLE_TAP_TIMEOUT = 500L; 
    private long lastTapTime = 0L;
    private int tapCount = 0;

    private static final long IDLE_TIMEOUT_MS =  30 * 1000L;
    private Handler idleHandler;
    private Runnable idleRunnable;

    private MainActivityViewModel mainActivityViewModel;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /// ////////////////////////////////////////////////////////////
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        float densityDpi = metrics.densityDpi;
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;

        // Approximate physical size in inches
        double widthInches = (double) widthPixels / densityDpi;
        double heightInches = (double) heightPixels / densityDpi;
        double diagonalInches = Math.sqrt(widthInches * widthInches + heightInches * heightInches);

        Log.d("ScreenInfo", "Density: " + densityDpi + " dpi, Resolution: " + widthPixels + "x" + heightPixels +
                ", Approx. Size: " + String.format("%.2f", diagonalInches) + " inches");

        /// ///////////////////////////////////////////////////////

        mainActivityViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        instance = this;
        databaseHelper = new DatabaseHelper(this);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        settingsViewModel.getAdminUsernameLiveData().observe(this, username -> { 
            currentStoredAdminUsername = username;
            
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            throw new IllegalStateException("NavHostFragment not found. Check your layout file for R.id.nav_host_fragment.");
        }

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment, R.id.addMemberFragment, R.id.memberListFragment, R.id.memberTypeFragment, R.id.expiredMembersFragment, R.id.recordsFragment)
                .setOpenableLayout(drawerLayout)
                .build();

        ImageButton imageButton = findViewById(R.id.imageButton_open_drawer);
        imageButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        if (navigationView != null)
        {
            setupAdminToggleAndNavigation(navigationView); 
            setupTripleTapForCredentials(navigationView);
        }

        settingsViewModel.isAdminModeActiveLiveData().observe(this, isActive ->
        {
            if (adminModeToggleSwitch != null)
            {
                adminModeToggleSwitch.setChecked(isActive);
            }
        });

        ViewGroup rootView = findViewById(R.id.drawer_layout);

        idleOverlayView = (FrameLayout) LayoutInflater.from(this).inflate(R.layout.idle_overlay, rootView, false);
        rootView.addView(idleOverlayView);

        idleOverlayView.setOnClickListener(v -> {
            hideIdleOverlay();
            resetIdleTimer();
        });

        idleOverlayView.setVisibility(View.GONE);
        idleHandler = new Handler(Looper.getMainLooper());

        idleRunnable = () -> {

            if (settingsViewModel != null && Boolean.TRUE.equals(settingsViewModel.isAdminModeActiveLiveData().getValue())) {
                settingsViewModel.logoutAdmin();
            }

            showIdleOverlay();
        };

        zkusbManager = new ZKUSBManager(this.getApplicationContext(), zkusbManagerListener);
        zkusbManager.registerUSBPermissionReceiver();

        attemptAutoStartDevice();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) ->
        {
            Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            rootView.setPadding(0, 0, 0, navBarInsets.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        resetIdleTimer();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        resetIdleTimer();
        startIdentificationForMainActivity();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIdleTimer();
        stopIdentification();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetIdleTimer();
    }

    private void resetIdleTimer() {
        if (idleHandler != null && idleRunnable != null) {
            idleHandler.removeCallbacks(idleRunnable);
            idleHandler.postDelayed(idleRunnable, IDLE_TIMEOUT_MS);
        }
    }

    private void stopIdleTimer()
    {
        if (idleHandler != null && idleRunnable != null)
        {
            idleHandler.removeCallbacks(idleRunnable);
        }
    }

    private void showIdleOverlay()
    {
        if (idleOverlayView != null)
        {
            ViewGroup rootView = findViewById(R.id.drawer_layout);

            drawerLayout.closeDrawers();

            if (idleOverlayView.getParent() == null && rootView != null)
            {
                rootView.addView(idleOverlayView);
                mainActivityViewModel.clearAllData();
            }

            idleOverlayView.setVisibility(View.VISIBLE);
            idleOverlayView.bringToFront();
        }
    }

    private void hideIdleOverlay() {
        if (idleOverlayView != null) {

            if (navController != null)
            {
                NavDestination currentDestination = navController.getCurrentDestination();
                if (currentDestination == null || currentDestination.getId() != R.id.homeFragment)
                {
                    NavOptions navOptions = new NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true)
                            .setLaunchSingleTop(true)
                            .build();
                    navController.navigate(R.id.homeFragment, null, navOptions);
                    navigationView.setCheckedItem(R.id.homeFragment);
                }
            }

            idleOverlayView.setVisibility(View.GONE);

            ViewGroup parent = (ViewGroup) idleOverlayView.getParent();
            if (parent != null) {
                parent.removeView(idleOverlayView);

            }

            if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START))
            {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
        }
    }

    private void setupAdminToggleAndNavigation(NavigationView navView)
    {
        int currentVisibleFragmentId = navController.getCurrentDestination() != null ? navController.getCurrentDestination().getId() : 0;

        navView.setCheckedItem(currentVisibleFragmentId);

        View headerView = navView.getHeaderView(0);
        if (headerView != null) {
            adminModeToggleSwitch = headerView.findViewById(R.id.adminModeToggle);
            if (adminModeToggleSwitch != null) {
                Boolean initialAdminMode = settingsViewModel.isAdminModeActiveLiveData().getValue();
                if (initialAdminMode != null) {
                    adminModeToggleSwitch.setChecked(initialAdminMode);
                }

                adminModeToggleSwitch.setOnCheckedChangeListener(null);
                adminModeToggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {

                    if (buttonView.isPressed()) {
                        if (isChecked) {
                            promptForAdminLogin();
                        } else {
                            settingsViewModel.logoutAdmin();
                            Toast.makeText(this, "Admin mode OFF", Toast.LENGTH_SHORT).show();
                            if (navController != null && navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.homeFragment) {
                                NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.homeFragment, true)
                                        .build();
                                navController.navigate(R.id.homeFragment, null, navOptions);
                            }
                        }
                    }
                });
            }
        }

        navView.setNavigationItemSelectedListener(item -> {
            boolean isAdminActive = Boolean.TRUE.equals(settingsViewModel.isAdminModeActiveLiveData().getValue());
            int itemId = item.getItemId();

            if (itemId == R.id.homeFragment) {

                if (navController.getCurrentDestination() == null || navController.getCurrentDestination().getId() != itemId) {
                    NavOptions navOptions = new NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true)
                            .setLaunchSingleTop(true)
                            .build();
                    navController.navigate(itemId, null, navOptions);
                }
                drawerLayout.closeDrawers();
                return true;
            }
            
            if (itemId == R.id.addMemberFragment || itemId == R.id.memberListFragment || itemId == R.id.memberTypeFragment || itemId == R.id.expiredMembersFragment || itemId == R.id.recordsFragment)
            {
                if (isAdminActive) {
                    if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != itemId)
                    {
                        navController.navigate(itemId);
                    }
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Not in admin mode. Access denied.", Toast.LENGTH_SHORT).show();

                    if (currentVisibleFragmentId != 0) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            navView.setCheckedItem(currentVisibleFragmentId);
                        });
                    }
                }
                drawerLayout.closeDrawers();
                return true;
            }

            drawerLayout.closeDrawers(); 
            return false; 
        });
    }

    private void promptForAdminLogin()
    {
        if (getSupportFragmentManager().findFragmentByTag("AdminLoginDialog") != null)
        {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_admin_login, null); 

        final EditText usernameEditText = dialogView.findViewById(R.id.editTextAdminLoginUsername);
        final EditText passwordEditText = dialogView.findViewById(R.id.editTextAdminLoginPassword);
        final TextInputLayout usernameLayout = dialogView.findViewById(R.id.textInputLayoutAdminLoginUsername);
        final TextInputLayout passwordLayout = dialogView.findViewById(R.id.textInputLayoutAdminLoginPassword);
        Button loginButton = dialogView.findViewById(R.id.buttonAdminLogin);
        Button cancelButton = dialogView.findViewById(R.id.buttonAdminCancelLogin);

        if (currentStoredAdminUsername != null && !currentStoredAdminUsername.isEmpty()) {
            usernameEditText.setText(currentStoredAdminUsername);
        } else {
            usernameEditText.setText(UserCredentialsRepository.DEFAULT_ADMIN_USERNAME); 
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Admin Login")
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialog.setOnDismissListener(dialogInterface ->
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        });

        loginButton.setOnClickListener(v -> {
            String enteredUsername = usernameEditText.getText().toString().trim();
            String enteredPassword = passwordEditText.getText().toString().trim();
            boolean valid = true;
            usernameLayout.setError(null);
            passwordLayout.setError(null);

            if (enteredUsername.isEmpty()) {
                usernameLayout.setError("Username required");
                valid = false;
            }
            if (enteredPassword.isEmpty()) {
                passwordLayout.setError("Password required");
                valid = false;
            }

            if (valid) {
                
                settingsViewModel.attemptAdminLogin(enteredUsername, enteredPassword, loginSuccess -> {
                    runOnUiThread(() -> { 
                        if (loginSuccess) {
                            Toast.makeText(MainActivity.this, "Admin mode ON", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            
                        } else {
                            passwordLayout.setError("Invalid credentials");
                            if (adminModeToggleSwitch != null) {
                                adminModeToggleSwitch.setChecked(false); 
                            }
                        }
                    });
                    return null; 
                });
            }
        });

        cancelButton.setOnClickListener(v -> {
            if (adminModeToggleSwitch != null) {
                adminModeToggleSwitch.setChecked(false); 
            }
            dialog.dismiss();
        });

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        dialog.show();
    }

    private void setupTripleTapForCredentials(NavigationView navView) {
        View headerView = navView.getHeaderView(0);
        if (headerView != null) {
            ImageView logoImageView = headerView.findViewById(R.id.artsGymLogo); 
            if (logoImageView != null) {
                logoImageView.setClickable(true); 
                logoImageView.setFocusable(true);  
                logoImageView.setOnClickListener(v -> {
                    long currentTime = SystemClock.uptimeMillis();
                    if (currentTime - lastTapTime < TRIPLE_TAP_TIMEOUT) {
                        tapCount++;
                    } else {
                        tapCount = 1;
                    }
                    lastTapTime = currentTime;

                    if (tapCount >= 3) {
                        tapCount = 0;
                        showChangeCredentialsDialog();
                    }
                });
            }
        }
    }

    private void showChangeCredentialsDialog() {
        if (getSupportFragmentManager().findFragmentByTag("ChangeCredDialog") != null) {
            return; 
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_change_credentials, null);

        final TextInputLayout usernameLayout = dialogView.findViewById(R.id.textInputLayoutChangeUsername);
        final EditText usernameEditText = dialogView.findViewById(R.id.editTextChangeUsername);
        final TextInputLayout passwordLayout = dialogView.findViewById(R.id.textInputLayoutChangePassword);
        final EditText passwordEditText = dialogView.findViewById(R.id.editTextChangePassword);
        final TextInputLayout confirmPasswordLayout = dialogView.findViewById(R.id.textInputLayoutChangeConfirmPassword);
        final EditText confirmPasswordEditText = dialogView.findViewById(R.id.editTextChangeConfirmPassword);
        Button saveButton = dialogView.findViewById(R.id.buttonSaveCredentials);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancelCredentials);

        
        if (currentStoredAdminUsername != null && !currentStoredAdminUsername.isEmpty()) {
            usernameEditText.setText(currentStoredAdminUsername);
        } else {
            
            usernameEditText.setText(UserCredentialsRepository.DEFAULT_ADMIN_USERNAME);
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Admin Credentials")
                .setView(dialogView)
                .setCancelable(false) 
                .create();

        dialog.setOnDismissListener(dialogInterface ->
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        });

        saveButton.setOnClickListener(v -> {
            String newUsername = usernameEditText.getText().toString().trim();
            String newPassword = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();
            boolean isValid = true;

            usernameLayout.setError(null);
            passwordLayout.setError(null);
            confirmPasswordLayout.setError(null);

            if (newUsername.isEmpty()) {
                usernameLayout.setError("Username cannot be empty");
                isValid = false;
            }
            if (newPassword.isEmpty()) {
                passwordLayout.setError("Password cannot be empty");
                isValid = false;
            } else if (newPassword.length() < 4) { 
                passwordLayout.setError("Password must be at least 4 characters");
                isValid = false;
            }
            if (!newPassword.equals(confirmPassword)) {
                confirmPasswordLayout.setError("Passwords does not match");
                isValid = false;
            }

            if (isValid)
            {
                settingsViewModel.updateAdminCredentials(newUsername, newPassword);
                Toast.makeText(MainActivity.this, "Admin credentials updated!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && appBarConfiguration != null &&
                NavigationUI.navigateUp(navController, appBarConfiguration))
                || super.onSupportNavigateUp();
    }

    private static final int ZKTECO_VID = 0x1b55;
    private static final int LIVE20R_PID = 0x0120;
    private static final int LIVE10R_PID = 0x0124;
    private final int REQUEST_PERMISSION_CODE = 9;
    private ZKUSBManager zkusbManager = null;
    private FingerprintSensor fingerprintSensor = null;
    private int usb_vid = ZKTECO_VID;
    private int usb_pid = 0;
    private boolean bStarted = false;
    private int deviceIndex = 0;
    private boolean isReseted = false;

    
    private final static int ENROLL_COUNT = 3; 
    private int enroll_index = 0; 
    private boolean bRegister = false; 
    private byte[][] regTemplates = new byte[ENROLL_COUNT][];
    private Bitmap finalEnrollmentBitmap = null; 
    private DatabaseHelper databaseHelper;
    private static FingerprintEnrollmentCallback currentEnrollmentCallback;
    private static FingerprintIdentificationCallback currentIdentificationCallback;
    private static int activeEnrollmentTarget;

    private byte[] tempMember1TemplateForVerification;

    private FingerprintCaptureListener fingerprintCaptureListener = new FingerprintCaptureListener()
    {
        @Override
        public void captureOK(byte[] fpImage)
        {
            Bitmap bitmap = ToolUtils.renderCroppedGreyScaleBitmap(fpImage, fingerprintSensor.getImageWidth(), fingerprintSensor.getImageHeight());
            
            finalEnrollmentBitmap = bitmap;
            runOnUiThread(() ->
            {
                if (currentEnrollmentCallback != null)
                {
                    currentEnrollmentCallback.onEnrollmentProgress(activeEnrollmentTarget, enroll_index + 1, ENROLL_COUNT, "Image captured.");
                }
            });
        }

        @Override
        public void captureError(FingerprintException e) {

        }

        @Override
        public void extractOK(byte[] fpTemplate) {
            if (bRegister) {
                byte[] bufids = new byte[256];
                int retIdentify = ZKFingerService.identify(fpTemplate, bufids, 110, 1);
                if (retIdentify > 0) {
                    String matchedMemberId = new String(bufids).trim();
                    runOnUiThread(() -> {
                        if (currentEnrollmentCallback != null) {
                            currentEnrollmentCallback.onEnrollmentFailed(activeEnrollmentTarget,
                                    "Finger already enrolled by Member ID: " + matchedMemberId + ". Enrollment cancelled.");
                        }
                        resetEnrollmentState();
                    });
                    return;
                }

                if (activeEnrollmentTarget == 2 && tempMember1TemplateForVerification != null) {
                    int retVerifyAgainstMember1 = ZKFingerService.verify(tempMember1TemplateForVerification, fpTemplate);
                    if (retVerifyAgainstMember1 > 0) { // If it matches Member 1's template
                        runOnUiThread(() -> {
                            if (currentEnrollmentCallback != null) {
                                currentEnrollmentCallback.onEnrollmentFailed(activeEnrollmentTarget,
                                        "This finger is already used for Member 1. Please use a different finger.");
                            }
                            resetEnrollmentState();
                        });
                        return;
                    }
                }
                
                if (enroll_index > 0) {
                    int retVerify = ZKFingerService.verify(regTemplates[enroll_index - 1], fpTemplate);
                    
                    if (retVerify <= 0) {
                        runOnUiThread(() -> {
                            if (currentEnrollmentCallback != null) {
                                currentEnrollmentCallback.onEnrollmentFailed(activeEnrollmentTarget, "Finger mismatch or poor quality! Please use the same finger. Status: " + retVerify);
                            }
                            resetEnrollmentState();
                        });
                        return;
                    }
                }

                if (enroll_index >= ENROLL_COUNT) {
                    resetEnrollmentState();
                    return;
                }

                
                if (regTemplates[enroll_index] == null || regTemplates[enroll_index].length < fpTemplate.length)
                {
                    regTemplates[enroll_index] = new byte[fpTemplate.length];
                }

                System.arraycopy(fpTemplate, 0, regTemplates[enroll_index], 0, fpTemplate.length);

                enroll_index++;

                runOnUiThread(() -> {
                    if (currentEnrollmentCallback != null)
                    {
                        if (enroll_index < ENROLL_COUNT)
                        {
                            currentEnrollmentCallback.onEnrollmentProgress(activeEnrollmentTarget, enroll_index, ENROLL_COUNT, "Place same finger again (" + enroll_index + "/" + ENROLL_COUNT + ")");
                        }
                        else
                        {
                            byte[] tempMergedTemplate = new byte[2048];
                            int mergedLen = ZKFingerService.merge(regTemplates[0], regTemplates[1], regTemplates[2], tempMergedTemplate);
                            if (mergedLen > 0) {
                                byte[] finalMergedTemplate = new byte[mergedLen];
                                System.arraycopy(tempMergedTemplate, 0, finalMergedTemplate, 0, mergedLen);
                                
                                currentEnrollmentCallback.onEnrollmentComplete(activeEnrollmentTarget, finalEnrollmentBitmap, finalMergedTemplate); 
                                resetEnrollmentState();
                            }
                            else
                            {
                                currentEnrollmentCallback.onEnrollmentFailed(activeEnrollmentTarget, "Enrollment failed (merge error: " + mergedLen + ")");
                                resetEnrollmentState();
                            }
                        }
                    }
                });

            } else if (currentIdentificationCallback != null) {
                // --- Perform identification ---
                byte[] bufids = new byte[256];
                int ret = ZKFingerService.identify(fpTemplate, bufids, 89, 1);

                if (ret > 0) { // Match found
                    String identifiedMemberId = new String(bufids).trim();
                    identifiedMemberId = identifiedMemberId.split("\\s+")[0];
                    String finalIdentifiedMemberId = identifiedMemberId;
                    runOnUiThread(() -> currentIdentificationCallback.onIdentificationResult(finalIdentifiedMemberId));
                } else { // No match found
                    runOnUiThread(() -> currentIdentificationCallback.onIdentificationResult(null));
                }
            }
        }

            @Override
        public void extractError(int i) {

        }
    };

    private FingerprintExceptionListener fingerprintExceptionListener = () -> {
        LogHelper.e("usb exception!!!");
        if (!isReseted) {
            try {
                fingerprintSensor.openAndReboot(deviceIndex);
            } catch (FingerprintException e) {
                e.printStackTrace();
            }
            isReseted = true;
        }
    };

    private ZKUSBManagerListener zkusbManagerListener = new ZKUSBManagerListener() {
        @Override
        public void onCheckPermission(int result) {
            afterGetUsbPermission();
        }
        @Override
        public void onUSBArrived(UsbDevice device) {
            LogHelper.d("USB device arrived: " + device.getDeviceName());
            
            if (bStarted) {
                closeDevice();
            }

            tryGetUSBPermission();
        }
        @Override
        public void onUSBRemoved(UsbDevice device) {
            LogHelper.d("USB device removed: " + device.getDeviceName());
            
            if (bStarted) {
                closeDevice();
                Toast.makeText(getApplicationContext(), "Disconnecting Connection.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Incomplete Disconnection, Please Restart App.", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied,The application can't run on this device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void attemptAutoStartDevice()
    {
        int runCount = settingsViewModel.getRunCountValue();

        if (bStarted)
        {
            if (runCount == 0)
            {
                Toast.makeText(getApplicationContext(), "Scanner Already Connected.", Toast.LENGTH_LONG).show();
                settingsViewModel.incrementRunCount();
            }
            return;
        }

        if (!enumSensor())
        {
            if (runCount == 0)
            {
                Toast.makeText(getApplicationContext(), "Scanner Not Found.", Toast.LENGTH_LONG).show();
                settingsViewModel.incrementRunCount();
            }
            return;
        }

        tryGetUSBPermission();
    }

    private void createFingerprintSensor()
    {
        if (null != fingerprintSensor)
        {
            FingprintFactory.destroy(fingerprintSensor);
            fingerprintSensor = null;
        }
        
        LogHelper.setLevel(Log.VERBOSE);
        LogHelper.setNDKLogLevel(Log.ASSERT);
        
        Map deviceParams = new HashMap();
        deviceParams.put(ParameterHelper.PARAM_KEY_VID, usb_vid);
        deviceParams.put(ParameterHelper.PARAM_KEY_PID, usb_pid);
        fingerprintSensor = FingprintFactory.createFingerprintSensor(getApplicationContext(), TransportType.USB, deviceParams);
    }

    private boolean enumSensor() {
        UsbManager usbManager = (UsbManager)this.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            int device_vid = device.getVendorId();
            int device_pid = device.getProductId();
            if (device_vid == ZKTECO_VID && (device_pid == LIVE20R_PID || device_pid == LIVE10R_PID))
            {
                usb_pid = device_pid;
                return true;
            }
        }
        return false;
    }

    private void tryGetUSBPermission()
    {
        zkusbManager.initUSBPermission(usb_vid, usb_pid);
    }

    private void afterGetUsbPermission()
    {
        openDevice();
    }

    private void openDevice() {
        createFingerprintSensor();
        isReseted = false;
        try {
            fingerprintSensor.open(deviceIndex);
            
            
            loadAllTemplatesIntoZKFingerService();
            
            {
                LogHelper.d("sdk version" + fingerprintSensor.getSDK_Version());
                LogHelper.d("firmware version" + fingerprintSensor.getFirmwareVersion());
                LogHelper.d("serial:" + fingerprintSensor.getStrSerialNumber());
                LogHelper.d("width=" + fingerprintSensor.getImageWidth() + ", height=" + fingerprintSensor.getImageHeight());
            }
            fingerprintSensor.setFingerprintCaptureListener(deviceIndex, fingerprintCaptureListener);
            fingerprintSensor.SetFingerprintExceptionListener(fingerprintExceptionListener);
            fingerprintSensor.startCapture(deviceIndex);
            bStarted = true;
            
            Toast.makeText(getApplicationContext(), "Scanner Connected!", Toast.LENGTH_LONG).show();
        } catch (FingerprintException e) {
            e.printStackTrace();
            try {
                fingerprintSensor.openAndReboot(deviceIndex);
            } catch (FingerprintException ex) {
                ex.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "Scanner Failed to Connect.", Toast.LENGTH_LONG).show();
        }
    }

    public void loadAllTemplatesIntoZKFingerService() {
        if (databaseHelper == null) {
            return;
        }

        ZKFingerService.clear();
        
        HashMap<String, byte[]> existingTemplates = databaseHelper.getAllFingerprintTemplates();
        if (existingTemplates.isEmpty()) {
            
            return;
        }
        int loadedCount = 0;
        for (Map.Entry<String, byte[]> entry : existingTemplates.entrySet()) {
            String memberId = entry.getKey();
            byte[] template = entry.getValue();
            
            
            int ret = ZKFingerService.save(template, memberId);
            if (ret == 0) { 
                loadedCount++;
            }
        }
    }

    public static void startFingerprintEnrollment(int target, FingerprintEnrollmentCallback callback, @Nullable byte[] member1Template)
    {
        if (instance != null)
        {
            instance.startEnrollmentInternal(target, callback, member1Template);
        }
        else
        {
            if (callback != null)
            {
                callback.onEnrollmentFailed(target, "App not ready.");
            }
        }
    }

    public static void stopFingerprintEnrollment() {
        if (instance != null) {
            instance.stopEnrollmentInternal();
        }
    }

    private void startEnrollmentInternal(int target, FingerprintEnrollmentCallback callback, @Nullable byte[] member1Template)
    {
        if (fingerprintSensor != null && bStarted)
        {
            bRegister = true; 
            enroll_index = 0; 
            
            for (int i = 0; i < ENROLL_COUNT; i++)
            {
                regTemplates[i] = null;
            }

            currentEnrollmentCallback = callback; 
            activeEnrollmentTarget = target;
            this.tempMember1TemplateForVerification = member1Template;

            if (currentEnrollmentCallback != null)
            {
                currentEnrollmentCallback.onEnrollmentProgress(activeEnrollmentTarget, 0, ENROLL_COUNT, "Place finger on scanner (1/3)");
            }
        }
        else
        {
            if (callback != null)
            {
                callback.onEnrollmentFailed(target, "Sensor not ready.");
            }
        }
    }

    private void stopEnrollmentInternal()
    {
        bRegister = false; 
        enroll_index = 0; 
        currentEnrollmentCallback = null; 
        activeEnrollmentTarget = 0;
        this.tempMember1TemplateForVerification = null;
    }

    private void resetEnrollmentState() {
        stopEnrollmentInternal();
    }


    private void startIdentificationForMainActivity() {
        startIdentificationInternal(new FingerprintIdentificationCallback() {
            @Override
            public void onIdentificationResult(String memberId) {
                Log.d("MainActivity", "Identification result for MainActivity: " + (memberId != null ? memberId : "No match"));
                handleIdentifiedMember(memberId);
            }

            @Override
            public void onIdentificationError(String errorMessage)
            {

            }
        });
    }

    private void startIdentificationInternal(FingerprintIdentificationCallback callback) {
        if (fingerprintSensor != null && bStarted) {
            currentIdentificationCallback = callback;

            try {
                fingerprintSensor.startCapture(deviceIndex);
            } catch (FingerprintException e) {
                if (callback != null) {
                    callback.onIdentificationError("Failed to start scanner.");
                }
            }
        } else {
            if (callback != null) {
                callback.onIdentificationError("Scanner not ready.");
            }
        }
    }

    private void stopIdentificationInternal() {
        currentIdentificationCallback = null;

        if (fingerprintSensor != null && bStarted) {
            try {
                fingerprintSensor.stopCapture(deviceIndex);
            } catch (FingerprintException ignored) {
            }
        }
    }

    private void stopIdentification() {
        stopIdentificationInternal();
    }

    @SuppressLint("RestrictedApi")
    private void handleIdentifiedMember(String memberId) {
        NavDestination currentDestination = navController.getCurrentDestination();

        Log.d("NavDebug", "Current NavController destination before navigation: " + (navController.getCurrentDestination() != null ? navController.getCurrentDestination().getDisplayName() : "null"));
        Log.d("NavDebug", "NavController graph: " + (navController.getGraph() != null ? navController.getGraph().getDisplayName() : "null"));

        if (currentDestination != null && currentDestination.getId() == R.id.homeFragment) {

            hideIdleOverlay();
            resetIdleTimer();

            if (memberId != null && !memberId.isEmpty()) {
                MemberDisplayInfo memberInfo = databaseHelper.getMemberDisplayInfo(memberId);
                if (memberInfo != null) {
                    if (databaseHelper.isMembershipActive(memberId))
                    {
                        Bundle bundle = new Bundle();
                        bundle.putString("identifiedMemberId", memberId);
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.homeFragment, true)
                                .build();
                        navController.navigate(R.id.homeFragment, bundle, navOptions);
                    }
                    else
                    {
                        Toast.makeText(this, "Membership for " + memberInfo.getFullName() + " is EXPIRED.", Toast.LENGTH_LONG).show();
                        NavOptions navOptions = new NavOptions.Builder()
                                .setPopUpTo(R.id.homeFragment, true)
                                .build();
                        navController.navigate(R.id.homeFragment, null, navOptions);
                    }
                }
                else
                {
                    Toast.makeText(this, "Member data not found for ID: " + memberId, Toast.LENGTH_LONG).show();
                    NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.homeFragment, true).build();
                    navController.navigate(R.id.homeFragment, null, navOptions);
                }
            }
            else
            {
                Toast.makeText(this, "Fingerprint not recognized. Please try again or register.", Toast.LENGTH_LONG).show();
                NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.homeFragment, true).build();
                navController.navigate(R.id.homeFragment, null, navOptions);
            }
        }
        else if (currentDestination != null)
        {
            Toast.makeText(this, "Please finish current processes.", Toast.LENGTH_SHORT).show();
            Log.w("NavDebug", "Navigation blocked by 'Please finish current processes.' toast.");
        }
        else {
            Log.e("NavDebug", "NavController.getCurrentDestination() is null, cannot navigate.");
        }
    }

    private void closeDevice()
    {
        if (bStarted)
        {
            try {
                fingerprintSensor.stopCapture(deviceIndex);
                fingerprintSensor.close(deviceIndex);
            } catch (FingerprintException e) {
                e.printStackTrace();
            }
            bStarted = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bStarted)
        {
            closeDevice();
        }
        if (instance == this)
        {
            instance = null;
        }

        zkusbManager.unRegisterUSBPermissionReceiver();
    }
}