package com.example.artsgymapp_solo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
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
import android.widget.Toast;

import android.os.SystemClock; // For triple tap
import com.google.android.material.switchmaterial.SwitchMaterial; // Import SwitchMaterial

import android.os.Handler;

// Import ZKTeco SDK classes
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.LogHelper;
import com.zkteco.android.biometric.module.fingerprintreader.FingprintFactory;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor;
import com.zkteco.android.biometric.module.fingerprintreader.exception.FingerprintException;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener;
import com.zkteco.android.biometric.module.fingerprintreader.ZKFingerService;

public class MainActivity extends AppCompatActivity {

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

    private static final long IDLE_TIMEOUT_MS = 1 * 60 * 1000L;
    private Handler idleHandler;
    private Runnable idleRunnable;

    private static final String TAG = "MainActivity_USB";
   // private UsbManager usbManager;
    //private FingerprintSensor fingerprintSensorInstance;
    //private String currentScannerDeviceName;
    //private static final int SCANNER_VID = 6997;
    //private static final int SCANNER_PID = 288;
    //private static final String ACTION_USB_PERMISSION = "com.example.artsgymapp_solo.USB_PERMISSION";
/*
    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) { // Good practice for broadcast receivers
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.d(TAG, "USB Permission GRANTED for device: " + device.getDeviceName());
                            // Now that permission is granted, try to initialize
                            tryInitializeScanner(device);
                        }
                    } else {
                        Log.e(TAG, "USB Permission DENIED for device: " + (device != null ? device.getDeviceName() : "null"));
                        Toast.makeText(context, "Scanner permission denied. Cannot use scanner.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };

    private final BroadcastReceiver usbDeviceDetachReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getVendorId() == SCANNER_VID && device.getProductId() == SCANNER_PID) {
                        // More robust check if currentScannerDeviceName matches device.getDeviceName()
                        if (currentScannerDeviceName != null && currentScannerDeviceName.equals(device.getDeviceName())) {
                            Log.i(TAG, "Target ZKTeco scanner DETACHED: " + device.getDeviceName());
                            Toast.makeText(context, "Scanner disconnected.", Toast.LENGTH_SHORT).show();
                            closeScannerResources(); // We will define this method soon
                        }
                    }
                }
            }
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        if (navHostFragment != null)
        {
            navController = navHostFragment.getNavController();
        }
        else
        {
            throw new IllegalStateException("NavHostFragment not found. Check your layout file for R.id.nav_host_fragment.");
        }

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment, R.id.addMemberFragment, R.id.memberListFragment, R.id.memberTypeFragment, R.id.expiredMembersFragment)
                .setOpenableLayout(drawerLayout)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        if (navigationView != null) {
            // NavigationUI.setupWithNavController(navigationView, navController); // We'll handle item selection manually for admin mode
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
            finishAffinity();
        };
    }

    private void resetIdleTimer()
    {
        if(idleHandler != null && idleRunnable != null)
        {
            idleHandler.removeCallbacks(idleRunnable);
            idleHandler.postDelayed(idleRunnable, IDLE_TIMEOUT_MS);
        }
    }

    private void stopIdleTimer()
    {
        if(idleHandler != null && idleRunnable != null)
        {
            idleHandler.removeCallbacks(idleRunnable);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        stopIdleTimer();
        Log.d("MainActivityIdle", "onDestroy - Idle timer stopped and cleaned up.");
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        stopIdleTimer();
        Log.d("MainActivityIdle", "onPause - Idle timer stopped.");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        resetIdleTimer();
        Log.d("MainActivityIdle", "onResume - Idle timer reset.");
    }

    @Override
    public void onUserInteraction()
    {
        super.onUserInteraction();
        resetIdleTimer();
    }

    private void setupAdminToggleAndNavigation(NavigationView navView) {
        View headerView = navView.getHeaderView(0);
        if (headerView != null) {
            adminModeToggleSwitch = headerView.findViewById(R.id.adminModeToggle); // Make sure this ID exists in your nav_header.xml
            if (adminModeToggleSwitch != null) {
                // Initialize switch state from ViewModel's LiveData current value (if available)
                // or rely on the observer to set it shortly.
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

                            if(navController != null && navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() != R.id.homeFragment)
                            {
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
            return; // Dialog already showing
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
            // Consider using UserCredentialsRepository.DEFAULT_ADMIN_USERNAME if currentStoredAdminUsername is null/empty
            // String defaultUser = settingsViewModel.getAdminUsernameLiveData().getValue(); // Another way to get it
            // usernameEditText.setText(defaultUser != null ? defaultUser : UserCredentialsRepository.DEFAULT_ADMIN_USERNAME);
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
    public boolean onSupportNavigateUp()
    {
        return (navController != null && appBarConfiguration != null &&
        NavigationUI.navigateUp(navController, appBarConfiguration))
                || super.onSupportNavigateUp();
    }
}