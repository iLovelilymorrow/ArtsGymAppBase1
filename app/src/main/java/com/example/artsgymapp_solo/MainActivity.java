package com.example.artsgymapp_solo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputLayout;

import androidx.lifecycle.ViewModelProvider;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.os.SystemClock; // For triple tap
import com.google.android.material.switchmaterial.SwitchMaterial; // Import SwitchMaterial
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // PRE-FINGERPRINT CODES (START)
    private static MainActivity instance;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    Toolbar toolbar;
    DrawerLayout drawerLayout;
    NavigationView navigationView;

    private SettingsViewModel settingsViewModel;
    private String currentStoredAdminUsername = "";

    private SwitchMaterial adminModeToggleSwitch;

    // For Triple Tap to change credentials
    private static final long TRIPLE_TAP_TIMEOUT = 500L; // Milliseconds
    private long lastTapTime = 0L;
    private int tapCount = 0;

    private static final long IDLE_TIMEOUT_MS = 3 * 60 * 1000L;
    private Handler idleHandler;
    private Runnable idleRunnable;
    private boolean isIdleModeActive = false; // Track if app is in idle mode

    private boolean isScannerConnected = false; // Track scanner connection status

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;
        databaseHelper = new DatabaseHelper(this);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // Observe admin username for pre-filling dialogs
        settingsViewModel.getAdminUsernameLiveData().observe(this, username -> { // Updated to getAdminUsernameLiveData
            currentStoredAdminUsername = username;
            Log.d("MainActivity", "Admin Username from ViewModel: " + username);
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            throw new IllegalStateException("NavHostFragment not found. Check your layout file for R.id.nav_host_fragment.");
        }

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment, R.id.addMemberFragment, R.id.memberListFragment, R.id.memberTypeFragment, R.id.expiredMembersFragment)
                .setOpenableLayout(drawerLayout)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        if (navigationView != null) {
            setupAdminToggleAndNavigation(navigationView); // Call new setup method
            setupTripleTapForCredentials(navigationView);
        }

        // Observe admin mode state to update the switch if changed programmatically
        settingsViewModel.isAdminModeActiveLiveData().observe(this, isActive -> {
            if (adminModeToggleSwitch != null) {
                adminModeToggleSwitch.setChecked(isActive);
                Log.d("MainActivity", "Admin mode LiveData changed. Switch set to: " + isActive);
            }
        });

        idleHandler = new Handler(Looper.getMainLooper());
        idleRunnable = () -> {
            Log.d("MainActivityIdle", "User Idle, launching persistent idle screen (activity_idle).");
            Intent intent = new Intent(MainActivity.this, activity_idle.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(0, 0);
        };

        zkusbManager = new ZKUSBManager(this.getApplicationContext(), zkusbManagerListener);
        zkusbManager.registerUSBPermissionReceiver();

        attemptAutoStartDevice();
    }

    @Override
    protected void onStart() {
        super.onStart();
        resetIdleTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetIdleTimer();
        if (!bRegister)
        {
            startIdentification();
        }
        if (isIdleModeActive)
        {
            Log.d("MainActivity", "Returning from idle screen. Starting identification.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIdleTimer();

        if (isIdentificationMode)
        {
            resetIdentificationState();
        }

        Log.d("MainActivityIdle", "onPause - Idle timer stopped.");
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
            Log.d("MainActivityIdle", "Idle timer reset.");
        }
    }

    private void stopIdleTimer() {
        if (idleHandler != null && idleRunnable != null) {
            idleHandler.removeCallbacks(idleRunnable);
            Log.d("MainActivityIdle", "Idle timer stopped.");
        }
    }

    private void setupAdminToggleAndNavigation(NavigationView navView) {
        View headerView = navView.getHeaderView(0);
        if (headerView != null) {
            adminModeToggleSwitch = headerView.findViewById(R.id.adminModeToggle);
            if (adminModeToggleSwitch != null) {
                Boolean initialAdminMode = settingsViewModel.isAdminModeActiveLiveData().getValue();
                if (initialAdminMode != null) {
                    adminModeToggleSwitch.setChecked(initialAdminMode);
                }

                adminModeToggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    // Prevent recursive calls if setChecked is called from observer
                    if (buttonView.isPressed()) { // Only react to user interaction
                        if (isChecked) {
                            // User wants to turn ON admin mode
                            promptForAdminLogin();
                        } else {
                            // User wants to turn OFF admin mode
                            settingsViewModel.logoutAdmin(); // ViewModel updates its state
                            Toast.makeText(this, "Admin mode OFF", Toast.LENGTH_SHORT).show();

                            if (navController != null && navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.homeFragment) {
                                NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.homeFragment, true) // Pops everything up to and including viewFragment1, then navigates
                                        .build();
                                navController.navigate(R.id.homeFragment, null, navOptions);

                            }
                        }
                    }
                });
            } else {
                Log.e("MainActivity", "AdminModeToggle Switch not found in nav header!");
            }
        } else {
            Log.e("MainActivity", "Nav header view is null!");
        }

        // Handle Navigation Item Clicks to control access based on admin mode
        navView.setNavigationItemSelectedListener(item -> {
            boolean isAdminActive = Boolean.TRUE.equals(settingsViewModel.isAdminModeActiveLiveData().getValue());
            int itemId = item.getItemId();

            // Allow navigation to home (viewFragment1) regardless of admin mode
            if (itemId == R.id.homeFragment) { // Assuming viewFragment1 is your "Home"
                if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != itemId) {
                    navController.navigate(itemId);
                }
                drawerLayout.closeDrawers();
                return true;
            }

            // For other fragments, check admin mode
            // Add all your restricted fragment IDs here
            if (itemId == R.id.addMemberFragment || itemId == R.id.memberListFragment || itemId == R.id.memberTypeFragment || itemId == R.id.expiredMembersFragment) {
                if (isAdminActive) {
                    if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != itemId) {
                        navController.navigate(itemId);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Not in admin mode. Access denied.", Toast.LENGTH_SHORT).show();
                    // Do not navigate
                }
                drawerLayout.closeDrawers();
                return true; // Return true to indicate the item selection was handled
            }

            // Fallback for any other items, though typically you'd handle all menu items explicitly
            // boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            // if (handled) drawerLayout.closeDrawers();
            // return handled;
            drawerLayout.closeDrawers(); // Close drawer even if not navigating
            return false; // Item not handled by this custom logic
        });
    }

    private void promptForAdminLogin() {
        if (getSupportFragmentManager().findFragmentByTag("AdminLoginDialog") != null) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_admin_login, null); // Ensure this layout exists

        final EditText usernameEditText = dialogView.findViewById(R.id.editTextAdminLoginUsername);
        final EditText passwordEditText = dialogView.findViewById(R.id.editTextAdminLoginPassword);
        final TextInputLayout usernameLayout = dialogView.findViewById(R.id.textInputLayoutAdminLoginUsername);
        final TextInputLayout passwordLayout = dialogView.findViewById(R.id.textInputLayoutAdminLoginPassword);
        Button loginButton = dialogView.findViewById(R.id.buttonAdminLogin);
        Button cancelButton = dialogView.findViewById(R.id.buttonAdminCancelLogin);

        // Pre-fill username
        if (currentStoredAdminUsername != null && !currentStoredAdminUsername.isEmpty()) {
            usernameEditText.setText(currentStoredAdminUsername);
        } else {
            usernameEditText.setText(UserCredentialsRepository.DEFAULT_ADMIN_USERNAME); // Fallback
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Admin Login")
                .setView(dialogView)
                .setCancelable(false)
                .create();

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
                // Use the ViewModel's attemptAdminLogin method with a callback
                settingsViewModel.attemptAdminLogin(enteredUsername, enteredPassword, loginSuccess -> {
                    runOnUiThread(() -> { // Ensure UI updates on main thread
                        if (loginSuccess) {
                            Toast.makeText(MainActivity.this, "Admin mode ON", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            // The switch will be updated by the LiveData observer for isAdminModeActiveLiveData
                        } else {
                            passwordLayout.setError("Invalid credentials");
                            if (adminModeToggleSwitch != null) {
                                adminModeToggleSwitch.setChecked(false); // Revert switch if login fails
                            }
                        }
                    });
                    return null; // For Kotlin Unit -> Java Function in SAM conversion
                });
            }
        });

        cancelButton.setOnClickListener(v -> {
            if (adminModeToggleSwitch != null) {
                adminModeToggleSwitch.setChecked(false); // Revert switch if cancelled
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupTripleTapForCredentials(NavigationView navView) {
        View headerView = navView.getHeaderView(0);
        if (headerView != null) {
            ImageView logoImageView = headerView.findViewById(R.id.artsGymLogo); // Your logo ID
            if (logoImageView != null) {
                logoImageView.setClickable(true); // Ensure it's clickable
                logoImageView.setFocusable(true);  // Ensure it's focusable for accessibility
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
            } else {
                Log.e("MainActivity", "artsGymLogo ImageView not found in nav header for triple tap.");
            }
        } else {
            Log.e("MainActivity", "Nav header view is null for triple tap setup.");
        }
    }

    private void showChangeCredentialsDialog() {
        if (getSupportFragmentManager().findFragmentByTag("ChangeCredDialog") != null) {
            return; // Dialog already showing
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

        // Pre-fill with current admin username
        if (currentStoredAdminUsername != null && !currentStoredAdminUsername.isEmpty()) {
            usernameEditText.setText(currentStoredAdminUsername);
        } else {
            // Fallback to default if no custom username is set yet
            usernameEditText.setText(UserCredentialsRepository.DEFAULT_ADMIN_USERNAME);
        }

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change Admin Credentials")
                .setView(dialogView)
                .setCancelable(false) // Good practice for this dialog
                .create();

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
            } else if (newPassword.length() < 4) { // Example: min password length
                passwordLayout.setError("Password must be at least 4 characters");
                isValid = false;
            }
            if (!newPassword.equals(confirmPassword)) {
                confirmPasswordLayout.setError("Passwords do not match");
                isValid = false;
            }

            if (isValid) {
                // Call the updated ViewModel method for plain text
                settingsViewModel.updateAdminCredentials(newUsername, newPassword);
                Toast.makeText(MainActivity.this, "Admin credentials updated!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && appBarConfiguration != null &&
                NavigationUI.navigateUp(navController, appBarConfiguration))
                || super.onSupportNavigateUp();
    }
    // PRE-FINGERPRINT CODES (END)

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

    // Enrollment specific variables
    private final static int ENROLL_COUNT = 3; // Number of scans for enrollment
    private int enroll_index = 0; // Current scan count for enrollment
    private boolean bRegister = false; // Flag to indicate if enrollment is active
    private byte[][] regTemplates = new byte[ENROLL_COUNT][]; // Array to store intermediate templates
    private byte[] mergedTemplate = null; // The final merged template
    private Bitmap finalEnrollmentBitmap = null; // The final image captured during enrollment
    private DatabaseHelper databaseHelper;
    private boolean isIdentificationMode = false;
    private static FingerprintEnrollmentCallback currentEnrollmentCallback;
    private static int activeEnrollmentTarget;

    private FingerprintCaptureListener fingerprintCaptureListener = new FingerprintCaptureListener() {
        @Override
        public void captureOK(byte[] fpImage) {
            // This is called when an image is successfully captured.
            // If we are in enrollment mode, we can show this intermediate image.
            Bitmap bitmap = ToolUtils.renderCroppedGreyScaleBitmap(fpImage, fingerprintSensor.getImageWidth(), fingerprintSensor.getImageHeight());
            Log.d("MainActivity", "Capture OK called, image received.");

            // Store the last captured image for final enrollment result
            finalEnrollmentBitmap = bitmap;
            runOnUiThread(() -> {
                if (currentEnrollmentCallback != null) {
                    currentEnrollmentCallback.onEnrollmentProgress(activeEnrollmentTarget, enroll_index + 1, ENROLL_COUNT, "Image captured.");
                }
            });
        }

        @Override
        public void captureError(FingerprintException e) {

        }

        @Override
        public void extractOK(byte[] fpTemplate) {
            Log.d("MainActivity", "Extract OK called, template received. Size: " + fpTemplate.length);

            if (bRegister) { // If in enrollment mode
                // --- Check if finger is already enrolled in DB (before new enrollment) ---
                byte[] bufids = new byte[256];
                int retIdentify = ZKFingerService.identify(fpTemplate, bufids, 70, 1);
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

                // --- Same Finger Verification for Enrollment ---
                if (enroll_index > 0) {
                    int retVerify = ZKFingerService.verify(regTemplates[enroll_index - 1], fpTemplate);
                    Log.d("MainActivity", "Verification status for scan " + (enroll_index + 1) + ": " + retVerify);
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
                    Log.w("MainActivity", "Enrollment index out of bounds. Resetting.");
                    resetEnrollmentState();
                    return;
                }

                // --- Deep Copy the template ---
                if (regTemplates[enroll_index] == null || regTemplates[enroll_index].length < fpTemplate.length) {
                    regTemplates[enroll_index] = new byte[fpTemplate.length];
                }
                System.arraycopy(fpTemplate, 0, regTemplates[enroll_index], 0, fpTemplate.length);
                Log.d("MainActivity", "Template for enroll_index " + enroll_index + " copied. Length: " + regTemplates[enroll_index].length);

                enroll_index++;

                runOnUiThread(() -> {
                    if (currentEnrollmentCallback != null) {
                        if (enroll_index < ENROLL_COUNT) {
                            currentEnrollmentCallback.onEnrollmentProgress(activeEnrollmentTarget, enroll_index, ENROLL_COUNT, "Place same finger again (" + enroll_index + "/" + ENROLL_COUNT + ")");
                        } else {
                            // All templates collected, now merge
                            byte[] tempMergedTemplate = new byte[2048];
                            int mergedLen = ZKFingerService.merge(regTemplates[0], regTemplates[1], regTemplates[2], tempMergedTemplate);
                            if (mergedLen > 0) {
                                byte[] finalMergedTemplate = new byte[mergedLen];
                                System.arraycopy(tempMergedTemplate, 0, finalMergedTemplate, 0, mergedLen);
                                Log.d("MainActivity", "Fingerprint merged successfully. Final template size: " + finalMergedTemplate.length);
                                currentEnrollmentCallback.onEnrollmentComplete(activeEnrollmentTarget, finalEnrollmentBitmap, finalMergedTemplate); // Pass finalMergedTemplate
                                resetEnrollmentState();
                            } else {
                                Log.e("MainActivity", "Fingerprint merge failed. Error code/length: " + mergedLen);
                                currentEnrollmentCallback.onEnrollmentFailed(activeEnrollmentTarget, "Enrollment failed (merge error: " + mergedLen + ")");
                                resetEnrollmentState();
                            }
                        }
                    }
                });

            } else if (isIdentificationMode) { // If in identification mode
                Log.d("MainActivity", "Processing template for identification.");
                // --- Perform identification ---
                byte[] bufids = new byte[256]; // Buffer to receive the matched ID
                int ret = ZKFingerService.identify(fpTemplate, bufids, 70, 1);
                if (ret > 0) { // Match found
                    // Log the raw buffer content for debugging
                    Log.d("MainActivity", "Buffer content: " + Arrays.toString(bufids));
                    // Convert buffer to string and extract the member ID
                    String identifiedMemberId = new String(bufids).trim(); // Trim whitespace
                    identifiedMemberId = identifiedMemberId.split("\\s+")[0]; // Get the first part
                    Log.d("MainActivity", "Fingerprint identified: Member ID = " + identifiedMemberId);
                    String finalIdentifiedMemberId = identifiedMemberId;
                    runOnUiThread(() -> handleIdentifiedMember(finalIdentifiedMemberId));
                } else { // No match found
                    Log.d("MainActivity", "No matching fingerprint found. Ret: " + ret);
                    runOnUiThread(() -> handleIdentifiedMember(null)); // Pass null for no match
                }
                resetIdentificationState(); // Clear the identification state
            }
        }

            @Override
        public void extractError(int i)
        {

        }
    };

    private FingerprintExceptionListener fingerprintExceptionListener = new FingerprintExceptionListener() {
        @Override
        public void onDeviceException() {
            LogHelper.e("usb exception!!!");
            if (!isReseted) {
                try {
                    fingerprintSensor.openAndReboot(deviceIndex);
                } catch (FingerprintException e) {
                    e.printStackTrace();
                }
                isReseted = true;
            }
        }
    };

    private ZKUSBManagerListener zkusbManagerListener = new ZKUSBManagerListener() {
        @Override
        public void onCheckPermission(int result) {
            // This is called after permission check, whether granted or not.
            // If permission is granted (result == 0), openDevice() will be called.
            // If not, openDevice() won't be called, and the UI will reflect the permission issue.
            afterGetUsbPermission();
        }
        @Override
        public void onUSBArrived(UsbDevice device) {
            LogHelper.d("USB device arrived: " + device.getDeviceName());
            // If the device is already started, close it first to re-initialize cleanly.
            // This handles cases where the device might have been unplugged and re-plugged.
            if (bStarted) {
                closeDevice();
            }
            // Now, attempt to get permission and open the newly arrived device.
            // This will trigger afterGetUsbPermission() -> openDevice() if permission is granted.
            tryGetUSBPermission();
        }
        @Override
        public void onUSBRemoved(UsbDevice device) {
            LogHelper.d("USB device removed: " + device.getDeviceName());
            // --- NEW: Automatically close the device when it's unplugged ---
            if (bStarted) {
                closeDevice();
                Log.d("IVAN LOG SCANNER C/P", "scanner disconnected patayin kona");
                Toast.makeText(getApplicationContext(), "scanner disconnected patayin kona", Toast.LENGTH_LONG).show();
            } else {
                Log.d("IVAN LOG SCANNER C/P", "scanner disconnected lang");
                Toast.makeText(getApplicationContext(), "scanner disconnected lang", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                boolean granted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        granted = false;
                    }
                }
                if (granted) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied,The application can't run on this device", Toast.LENGTH_SHORT).show();
                }
            default:
                break;
        }
    }

    private void attemptAutoStartDevice() {
        if (bStarted) {
            // Device is already connected, no need to re-attempt
            Log.d("IVAN LOG SCANNER C/P", "connected na tol kanina pa");
            Toast.makeText(getApplicationContext(), "connected na tol kanina pa", Toast.LENGTH_LONG).show();
            return;
        }
        if (!enumSensor()) {
            Log.d("IVAN LOG SCANNER C/P", "wala ata tol");
            Toast.makeText(getApplicationContext(), "wala ata tol", Toast.LENGTH_LONG).show();
            return;
        }
        // Device found, now try to get USB permission (or confirm it's already granted)
        tryGetUSBPermission();
    }

    private void createFingerprintSensor()
    {
        if (null != fingerprintSensor)
        {
            FingprintFactory.destroy(fingerprintSensor);
            fingerprintSensor = null;
        }
        // Define output log level
        LogHelper.setLevel(Log.VERBOSE);
        LogHelper.setNDKLogLevel(Log.ASSERT);
        // Start fingerprint sensor
        Map deviceParams = new HashMap();
        deviceParams.put(ParameterHelper.PARAM_KEY_VID, usb_vid);
        deviceParams.put(ParameterHelper.PARAM_KEY_PID, usb_pid);
        fingerprintSensor = FingprintFactory.createFingerprintSensor(getApplicationContext(), TransportType.USB, deviceParams);
    }

    private boolean enumSensor()
    {
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
            // --- NEW: Load existing templates into ZKFingerService's internal database ---
            // This should be done once the sensor is open and ready.
            loadAllTemplatesIntoZKFingerService();
            // --- END NEW ---
            {
                // device parameter
                LogHelper.d("sdk version" + fingerprintSensor.getSDK_Version());
                LogHelper.d("firmware version" + fingerprintSensor.getFirmwareVersion());
                LogHelper.d("serial:" + fingerprintSensor.getStrSerialNumber());
                LogHelper.d("width=" + fingerprintSensor.getImageWidth() + ", height=" + fingerprintSensor.getImageHeight());
            }
            fingerprintSensor.setFingerprintCaptureListener(deviceIndex, fingerprintCaptureListener);
            fingerprintSensor.SetFingerprintExceptionListener(fingerprintExceptionListener);
            fingerprintSensor.startCapture(deviceIndex);
            bStarted = true;
            Log.d("IVAN LOG SCANNER C/P", "connected the scanner cuhh");
            Toast.makeText(getApplicationContext(), "connected the scanner cuhh", Toast.LENGTH_LONG).show();
        } catch (FingerprintException e) {
            e.printStackTrace();
            try {
                fingerprintSensor.openAndReboot(deviceIndex);
            } catch (FingerprintException ex) {
                ex.printStackTrace();
            }
            Log.d("IVAN LOG SCANNER C/P", "fuck bro");
            Toast.makeText(getApplicationContext(), "fuck bro", Toast.LENGTH_LONG).show();
        }
    }

    public void loadAllTemplatesIntoZKFingerService() {
        if (databaseHelper == null) {
            Log.e("MainActivity", "DatabaseHelper is null, cannot load templates.");
            return;
        }
        // Clear any previously loaded templates in ZKFingerService to avoid duplicates
        // Assuming a method like clearTemplateDB() exists. If not, you might need to restart ZKFingerService.
        // ZKFingerService.clearTemplateDB(); // Uncomment if such a method exists
        HashMap<String, byte[]> existingTemplates = databaseHelper.getAllFingerprintTemplates();
        if (existingTemplates.isEmpty()) {
            Log.d("MainActivity", "No existing fingerprint templates to load into ZKFingerService.");
            return;
        }
        int loadedCount = 0;
        for (Map.Entry<String, byte[]> entry : existingTemplates.entrySet()) {
            String memberId = entry.getKey();
            byte[] template = entry.getValue();
            // Assuming ZKFingerService.addTemplate(String uid, byte[] template) exists
            // The 'uid' here is your memberId
            int ret = ZKFingerService.save(template, memberId);
            if (ret == 0) { // Assuming 0 indicates success
                loadedCount++;
            } else {
                Log.w("MainActivity", "Failed to add template for member " + memberId + " to ZKFingerService. Ret: " + ret);
            }
        }
        Log.d("MainActivity", "Loaded " + loadedCount + " templates into ZKFingerService's internal database.");
    }

    public static void startFingerprintEnrollment(int target, FingerprintEnrollmentCallback callback) {
        if (instance != null) {
            instance.startEnrollmentInternal(target, callback);
        } else {
            Log.e("MainActivity", "MainActivity instance is null, cannot start enrollment.");
            if (callback != null) {
                callback.onEnrollmentFailed(target, "App not ready.");
            }
        }
    }

    public static void stopFingerprintEnrollment() {
        if (instance != null) {
            instance.stopEnrollmentInternal();
        }
    }

    private void startEnrollmentInternal(int target, FingerprintEnrollmentCallback callback) {
        if (fingerprintSensor != null && bStarted) {
            bRegister = true; // Set registration mode
            enroll_index = 0; // Reset enrollment progress
            // Clear previous templates for new enrollment
            for (int i = 0; i < ENROLL_COUNT; i++) {
                regTemplates[i] = null;
            }
            currentEnrollmentCallback = callback; // Set the active callback
            activeEnrollmentTarget = target; // Set the target
            Log.d("MainActivity", "Fingerprint enrollment started for target: " + target);

            if (currentEnrollmentCallback != null) {
                currentEnrollmentCallback.onEnrollmentProgress(activeEnrollmentTarget, 0, ENROLL_COUNT, "Place finger on scanner (1/3)");
            }
        } else {
            Log.e("MainActivity", "Cannot start enrollment: Sensor not connected or started.");
            if (callback != null) {
                callback.onEnrollmentFailed(target, "Sensor not ready.");
            }
            // Optionally try to open device again if it's not started
            // openDevice();
        }
    }

    private void stopEnrollmentInternal() {
        bRegister = false; // Exit registration mode
        enroll_index = 0; // Reset enrollment progress
        currentEnrollmentCallback = null; // Clear the callback
        activeEnrollmentTarget = 0; // Clear the target
        Log.d("MainActivity", "Enrollment state reset.");
    }

    private void resetEnrollmentState() {
        stopEnrollmentInternal();
    }

    public void startIdentification() {
        if (fingerprintSensor != null && bStarted) { // Check if sensor is connected and started
            isIdentificationMode = true;
            Log.d("MainActivity", "Starting fingerprint identification...");
            // You might want to show a Toast or update a TextView here
            // Toast.makeText(this, "Place finger for identification...", Toast.LENGTH_SHORT).show();
            // If you have a status TextView in MainActivity, update it here.
        } else {
            Log.e("MainActivity", "Cannot start identification: Sensor not connected or started.");
            // Optionally try to open device again if it's not started
            // openDevice();
        }
    }

    private void handleIdentifiedMember(String memberId) {
        if (memberId != null && !memberId.isEmpty()) {
            // Member identified, now check membership status
            MemberDisplayInfo memberInfo = databaseHelper.getMemberDisplayInfo(memberId);
            if (memberInfo != null) {
                if (databaseHelper.isMembershipActive(memberId)) {
                    // Membership is active, navigate to HomeFragment with details
                    Log.d("MainActivity", "Active member identified: " + memberId);
                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
                    Bundle bundle = new Bundle();
                    bundle.putString("identifiedMemberId", memberId);
                    navController.navigate(R.id.homeFragment, bundle); // Navigate to HomeFragment
                    Toast.makeText(this, "Welcome, " + memberInfo.getFullName() + "!", Toast.LENGTH_LONG).show();
                } else {
                    // Membership inactive
                    Log.d("MainActivity", "Member " + memberId + " identified, but membership is inactive.");
                    Toast.makeText(this, "Membership for " + memberInfo.getFullName() + " is inactive.", Toast.LENGTH_LONG).show();
                    // Optionally, navigate to HomeFragment but clear details or show a specific message
                    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
                    navController.navigate(R.id.homeFragment); // Navigate to HomeFragment without ID to clear it
                    startIdentification();
                }
            } else {
                // Member ID found in ZKFingerService but not in our database (shouldn't happen if sync is good)
                Log.e("MainActivity", "Identified Member ID " + memberId + " not found in local database.");
                Toast.makeText(this, "Member data not found for ID: " + memberId, Toast.LENGTH_LONG).show();
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
                navController.navigate(R.id.homeFragment); // Navigate to HomeFragment without ID to clear it
            }
        } else {
            // No match found
            Log.d("MainActivity", "No matching fingerprint found in database.");
            Toast.makeText(this, "Fingerprint not recognized. Please try again or register.", Toast.LENGTH_LONG).show();
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            navController.navigate(R.id.homeFragment); // Navigate to HomeFragment without ID to clear it
            startIdentification();
        }
    }

    private void resetIdentificationState()
    {
        isIdentificationMode = false;
        Log.d("MainActivity", "Fingerprint identification stopped.");
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