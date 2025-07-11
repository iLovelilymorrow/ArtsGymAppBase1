package com.example.artsgymapp_solo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import android.os.Handler;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;

public class MemberListFragment extends Fragment {
    private static final String TAG = "MemberListFragment";
    private RecyclerView membersRecyclerView;
    private MemberAdapter memberAdapter;
    private List<MemberDisplayInfo> allMembersDisplayList;
    private DatabaseHelper databaseHelper;
    private SearchView searchView;
    private NavController navController;
    private RadioGroup radioGroupFilters;
    private TextView tvNoExpiredMembers;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private static final int REQUEST_CODE_SAVE_DB_ZIP_EXPORT = 2;
    private static final int REQUEST_CODE_PICK_DB_FILE_IMPORT = 103;

    private MaterialButton exportButton;
    private MaterialButton importButton;

    // Enum to track active filter state
    private enum ActiveFilter
    {
        NONE,
        CURRENT_MONTH_REG,
        EXPIRING_SOON
    }

    private ActiveFilter currentActiveFilter = ActiveFilter.NONE;
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_memberlist, container, false);

        membersRecyclerView = view.findViewById(R.id.membersRecyclerView);
        searchView = view.findViewById(R.id.searchBarView1);
        tvNoExpiredMembers = view.findViewById(R.id.textViewNoExpiredMembers);
        radioGroupFilters = view.findViewById(R.id.radioGroupFilters);
        exportButton = view.findViewById(R.id.exportButton);
        importButton = view.findViewById(R.id.importButton);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        if (databaseHelper == null)
        {
            databaseHelper = new DatabaseHelper(requireContext());
        }

        allMembersDisplayList = new ArrayList<>();

        setupRecyclerView();
        loadMembersFromDatabase(); // Initial load, will also apply filters
        setupSearchView();
        setupFilterRadioGroup(); // ++ Set up listeners for new buttons ++
        setupImportExportButtons();
    }

    private void setupImportExportButtons()
    {
        exportButton.setOnClickListener(v -> handleExportDatabase());
        importButton.setOnClickListener(v -> handleImportDatabase());
    }

    // --- Database Export Logic ---
    private void handleExportDatabase() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip"); // Changed to ZIP
        intent.putExtra(Intent.EXTRA_TITLE, "ArtsGymApp_Backup_" + System.currentTimeMillis() + ".zip"); // Changed extension

        try {
            // Use a new request code to differentiate if needed, or reuse if you adapt onActivityResult
            startActivityForResult(intent, REQUEST_CODE_SAVE_DB_ZIP_EXPORT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No app found to handle file creation.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "ActivityNotFoundException for ACTION_CREATE_DOCUMENT with type application/zip");
        }
    }

    // --- Database Import Logic ---
    private void handleImportDatabase() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Import Database")
                .setMessage("Importing a database will overwrite current data. Are you sure you want to proceed? It's recommended to export the current database first.")
                .setPositiveButton("Import", (dialog, which) ->
                {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/zip");
                    // Optionally, you can specify an initial URI
                    // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
                    try {
                        startActivityForResult(intent, REQUEST_CODE_PICK_DB_FILE_IMPORT);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getContext(), "No app found to pick files.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == REQUEST_CODE_SAVE_DB_ZIP_EXPORT) { // Check for the new code
                Log.d(TAG, "URI selected for ZIP export: " + uri.toString());
                // Initiate the background task for zipping
                performFullExportToZip(uri);
            } else if (requestCode == REQUEST_CODE_PICK_DB_FILE_IMPORT) { // Your existing import
                // ... (existing import logic for .db file for now, will be updated later)
                Log.d(TAG, "URI selected for DB import: " + uri.toString());
                performDatabaseImport(uri); // This will need to change to handle ZIPs
            }
            // ... any other request codes ...
        } else {
            Log.d(TAG, "onActivityResult: resultCode=" + resultCode + " or data/uri is null. RequestCode: " + requestCode);
        }
    }

    private void performFullExportToZip(Uri destinationZipUri) {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot perform export.");
            Toast.makeText(getActivity(), "Error: Context not available for export.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Show progress indicator to the user
        // e.g., progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(getContext(), "Exporting data... Please wait.", Toast.LENGTH_LONG).show();


        executorService.execute(() -> {
            boolean exportSuccess = false;
            String errorMessage = "Export failed. Unknown error.";
            File tempExportDir = null;

            try {
                // 1. Create a temporary directory for staging files
                tempExportDir = new File(getContext().getCacheDir(), "TempExportDir");
                if (tempExportDir.exists()) {
                    deleteRecursive(tempExportDir); // Clear previous temp data
                }
                if (!tempExportDir.mkdirs()) {
                    throw new IOException("Failed to create temporary export directory.");
                }
                Log.d(TAG, "Temporary export directory created: " + tempExportDir.getAbsolutePath());

                // 2. Close the database before copying its file
                if (databaseHelper != null) {
                    databaseHelper.closeDatabase();
                    Log.d(TAG, "Database closed for export.");
                }

                // 3. Copy the database file to the temporary directory
                File currentDBFile = getContext().getDatabasePath(DatabaseHelper.DATABASE_NAME);
                if (!currentDBFile.exists()) {
                    throw new FileNotFoundException("Database file not found: " + currentDBFile.getAbsolutePath());
                }
                File tempDbFile = new File(tempExportDir, DatabaseHelper.DATABASE_NAME);
                copyFile(new FileInputStream(currentDBFile), new FileOutputStream(tempDbFile));
                Log.d(TAG, "Database file copied to temp dir: " + tempDbFile.getAbsolutePath());


                // 4. Get all image paths from the database
                // Re-initialize a temporary DB helper instance to get image paths
                // This is because the main one was just closed.
                // Or, get image paths *before* closing the main databaseHelper.
                // Let's assume we fetch paths *before* closing for simplicity here.
                // (You'd call databaseHelper.getAllMemberImagePaths() *before* databaseHelper.closeDatabase())
                // For this example, let's pretend we have the list.
                // You MUST get this list BEFORE closing the databaseHelper or use a new instance.
                DatabaseHelper tempDbReader = new DatabaseHelper(getContext()); // Create a temp one to read paths
                List<String> imagePaths = tempDbReader.getAllMemberImagePaths();
                tempDbReader.close(); // Close the temp reader
                Log.d(TAG, "Retrieved " + imagePaths.size() + " image paths for export.");

                // 5. Create an "ImageBackup" subdirectory in the temp folder
                File tempImageBackupDir = new File(tempExportDir, "ImageBackup");
                if (!tempImageBackupDir.mkdirs()) {
                    throw new IOException("Failed to create temporary image backup directory.");
                }
                Log.d(TAG, "Temporary image backup directory created: " + tempImageBackupDir.getAbsolutePath());

                // 6. Copy each image file to the "ImageBackup" subdirectory
                for (String imagePath : imagePaths) {
                    if (imagePath != null && !imagePath.isEmpty()) {
                        File sourceImageFile = new File(imagePath);
                        if (sourceImageFile.exists() && sourceImageFile.isFile()) {
                            File destImageFile = new File(tempImageBackupDir, sourceImageFile.getName());
                            try {
                                copyFile(new FileInputStream(sourceImageFile), new FileOutputStream(destImageFile));
                                Log.d(TAG, "Copied image " + sourceImageFile.getName() + " to temp backup.");
                            } catch (FileNotFoundException fnfe) {
                                Log.e(TAG, "Image file not found during copy: " + imagePath, fnfe);
                                // Decide if you want to continue or fail the export for a missing image
                            } catch (IOException ioe) {
                                Log.e(TAG, "IOException copying image: " + imagePath, ioe);
                                // Decide policy
                            }
                        } else {
                            Log.w(TAG, "Image path does not exist or is not a file: " + imagePath);
                        }
                    }
                }

                // 7. Zip the entire temporary directory
                try (OutputStream out = getContext().getContentResolver().openOutputStream(destinationZipUri)) {
                    if (out == null) {
                        throw new IOException("Failed to open output stream for destination URI.");
                    }
                    ZipUtils.zipDirectory(tempExportDir, out); // Use the tempExportDir as the root for zipping
                    exportSuccess = true;
                    Log.d(TAG, "Successfully zipped contents of " + tempExportDir.getAbsolutePath() + " to " + destinationZipUri.toString());
                }

            } catch (Exception e) { // Catch generic Exception for broader error handling on background thread
                Log.e(TAG, "Error during full export to ZIP", e);
                errorMessage = "Export failed: " + e.getMessage();
                exportSuccess = false;
            } finally {
                // 8. Clean up the temporary directory
                if (tempExportDir != null && tempExportDir.exists()) {
                    deleteRecursive(tempExportDir);
                    Log.d(TAG, "Temporary export directory deleted.");
                }

                // 9. Re-initialize the app's database and reload data on the main thread
                // This is important because we closed the main databaseHelper instance.
                final boolean finalExportSuccess = exportSuccess;
                final String finalMessage = exportSuccess ? "Data exported successfully to ZIP!" : errorMessage;

                mainThreadHandler.post(() -> {
                    // progressBar.setVisibility(View.GONE);
                    reInitializeDatabaseAndReloadData(); // Your existing method
                    Toast.makeText(getContext(), finalMessage, Toast.LENGTH_LONG).show();
                    if (finalExportSuccess) {
                        Log.d(TAG, "Export process completed successfully.");
                    } else {
                        Log.e(TAG, "Export process failed. Message: " + finalMessage);
                    }
                });
            }
        });
    }

    // Helper method to copy files (add this to MemberListFragment or a utility class)
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        try {
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
            out.flush(); // Ensure all buffered data is written
            out.close();
        }
    }

    // Helper method to delete directories recursively (add this to MemberListFragment or a utility class)
    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        if (!fileOrDirectory.delete()) {
            Log.e(TAG, "Failed to delete file/directory: " + fileOrDirectory.getAbsolutePath());
        }
    }

    // Inside MemberListFragment.java

    private void performDatabaseImport(Uri sourceZipUri) { // Renamed for clarity
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot perform import.");
            Toast.makeText(getActivity(), "Error: Context not available for import.", Toast.LENGTH_SHORT).show();
            return;
        }

        File internalDbFileTarget = getContext().getDatabasePath(DatabaseHelper.DATABASE_NAME);
        File tempUnzipDir = new File(getContext().getCacheDir(), "TempImportDir"); // Temporary directory for unzipping
        File targetImageDir = new File(getContext().getFilesDir(), "MemberImages"); // Defined earlier, still correct

        Log.d(TAG, "Attempting to import from ZIP. Target DB path: " + internalDbFileTarget.getAbsolutePath());
        Toast.makeText(getContext(), "Importing data... Please wait.", Toast.LENGTH_LONG).show(); // User feedback

        executorService.execute(() -> { // Perform unzipping and DB copy on a background thread
            boolean importSuccess = false; // Assume failure until all critical steps pass
            String errorMessage = "Import failed. Unknown error.";

            try {
                // 1. Close current database connection
                if (databaseHelper != null) {
                    databaseHelper.closeDatabase();
                    databaseHelper = null; // Important to nullify
                    Log.d(TAG, "DatabaseHelper closed and nulled for import.");
                }
                System.gc(); // Suggest garbage collection
                System.runFinalization();

                // 2. Unzip the source URI to a temporary directory
                if (tempUnzipDir.exists()) {
                    ZipUtils.deleteRecursive(tempUnzipDir);
                }
                if (!tempUnzipDir.mkdirs()) {
                    throw new IOException("Failed to create temporary import directory: " + tempUnzipDir.getAbsolutePath());
                }
                Log.d(TAG, "Temporary import directory created: " + tempUnzipDir.getAbsolutePath());

                try (InputStream zipInputStream = getContext().getContentResolver().openInputStream(sourceZipUri)) {
                    if (zipInputStream == null) {
                        throw new IOException("Failed to open input stream from selected ZIP file.");
                    }
                    ZipUtils.unzip(zipInputStream, tempUnzipDir); // Perform unzipping
                    Log.d(TAG, "ZIP file unzipped successfully to " + tempUnzipDir.getAbsolutePath());
                }

                // 3. Locate the .db file within the unzipped contents
                File unzippedDbFile = new File(tempUnzipDir, DatabaseHelper.DATABASE_NAME);
                if (!unzippedDbFile.exists() || !unzippedDbFile.isFile()) {
                    throw new FileNotFoundException("Database file '" + DatabaseHelper.DATABASE_NAME + "' not found in the ZIP archive at " + unzippedDbFile.getAbsolutePath());
                }
                Log.d(TAG, "Database file found in unzipped archive: " + unzippedDbFile.getAbsolutePath());

                // 4. Copy the unzipped .db file to the app's database location
                File databasesDir = internalDbFileTarget.getParentFile();
                if (databasesDir != null && !databasesDir.exists()) {
                    if (!databasesDir.mkdirs()) {
                        throw new IOException("Failed to create databases directory: " + databasesDir.getAbsolutePath());
                    }
                }

                try (InputStream in = new FileInputStream(unzippedDbFile);
                     OutputStream out = new FileOutputStream(internalDbFileTarget, false)) { // false to overwrite
                    copyFile(in, out); // Your existing copyFile utility
                    Log.d(TAG, "Database file copied from temp to final destination: " + internalDbFileTarget.getAbsolutePath());
                }
                // At this point, the database file itself should be imported.

                // 5. Handle Image Import (NEW SECTION)
                // ===================================
                Log.d(TAG, "Starting image import process...");
                File tempImageBackupDir = new File(tempUnzipDir, "ImageBackup"); // Path to ImageBackup in the unzipped folder

                if (tempImageBackupDir.exists() && tempImageBackupDir.isDirectory()) {
                    // Ensure the target image directory in app's internal storage exists
                    if (!targetImageDir.exists()) {
                        if (targetImageDir.mkdirs()) {
                            Log.d(TAG, "Created target image directory: " + targetImageDir.getAbsolutePath());
                        } else {
                            // If target dir creation fails, this is a problem for image import
                            Log.e(TAG, "Failed to create target image directory: " + targetImageDir.getAbsolutePath() + ". Images may not be imported.");
                            // Depending on policy, you could throw new IOException here to fail the whole import.
                            // For now, we'll log and image import for this session might fail.
                        }
                    } else {
                        Log.d(TAG, "Target image directory already exists: " + targetImageDir.getAbsolutePath());
                        // Optional: Clear existing images in targetImageDir if you want a clean import
                        // ZipUtils.deleteRecursive(targetImageDir);
                        // if (!targetImageDir.mkdirs()) {
                        //     Log.e(TAG, "Failed to recreate target image directory after clearing: " + targetImageDir.getAbsolutePath());
                        // }
                    }

                    // Proceed with image copy only if targetImageDir is usable
                    if (targetImageDir.exists() && targetImageDir.isDirectory()) {
                        File[] imageFilesToImport = tempImageBackupDir.listFiles();
                        if (imageFilesToImport != null && imageFilesToImport.length > 0) {
                            Log.d(TAG, "Found " + imageFilesToImport.length + " items in ImageBackup directory.");
                            int successfulImageCopies = 0;
                            for (File sourceImageFile : imageFilesToImport) {
                                if (sourceImageFile.isFile()) { // Only copy files
                                    File destImageFile = new File(targetImageDir, sourceImageFile.getName());
                                    try (InputStream inImg = new FileInputStream(sourceImageFile);
                                         OutputStream outImg = new FileOutputStream(destImageFile, false)) { // false to overwrite
                                        copyFile(inImg, outImg); // Your existing copyFile utility
                                        Log.d(TAG, "Copied image: " + sourceImageFile.getName() + " to " + destImageFile.getAbsolutePath());
                                        successfulImageCopies++;
                                    } catch (IOException e) {
                                        Log.e(TAG, "Error copying image: " + sourceImageFile.getName(), e);
                                        // errorMessage += "\n- Failed to copy image: " + sourceImageFile.getName(); // Append to overall error message
                                    }
                                } else {
                                    Log.d(TAG, "Skipping item (not a file) in ImageBackup: " + sourceImageFile.getName());
                                }
                            }
                            Log.d(TAG, "Image import process finished. Copied " + successfulImageCopies + " images.");
                        } else if (imageFilesToImport == null) {
                            Log.w(TAG, "Could not list files in ImageBackup directory (null): " + tempImageBackupDir.getAbsolutePath());
                        } else { // length == 0
                            Log.i(TAG, "ImageBackup directory is empty: " + tempImageBackupDir.getAbsolutePath());
                        }
                    } else {
                        Log.e(TAG, "Target image directory is not usable for import: " + targetImageDir.getAbsolutePath());
                    }
                } else {
                    Log.i(TAG, "ImageBackup directory not found in ZIP or is not a directory: " + tempImageBackupDir.getAbsolutePath() + ". No images to import from this source.");
                    // This is not necessarily an error if the ZIP doesn't contain an ImageBackup folder.
                }
                // ===================================
                // END OF NEW IMAGE IMPORT SECTION

                importSuccess = true; // If we've reached here, consider the core import (DB + attempted images) successful.
                // Minor image copy failures might have occurred but are logged.

            } catch (Exception e) { // Catch generic Exception for broader error handling on background thread
                Log.e(TAG, "Critical error during data import from ZIP", e);
                errorMessage = "Import failed: " + e.getMessage(); // Overwrite with the critical error
                importSuccess = false;
            } finally {
                // 6. Clean up the temporary unzipped directory
                if (tempUnzipDir.exists()) {
                    ZipUtils.deleteRecursive(tempUnzipDir);
                    Log.d(TAG, "Temporary import directory deleted.");
                }

                // 7. Re-initialize the app's database and reload data on the main thread
                // This is important because we closed the main databaseHelper instance.
                final boolean finalImportSuccess = importSuccess;
                // Construct a more informative final message if needed based on partial successes/failures
                final String finalMessage = finalImportSuccess ? "Data import process completed!" : errorMessage;

                mainThreadHandler.post(() -> {
                    reInitializeDatabaseAndReloadData(); // This will use the new DB
                    Toast.makeText(getContext(), finalMessage, Toast.LENGTH_LONG).show();
                    if (finalImportSuccess) {
                        Log.d(TAG, "Import process concluded successfully overall.");
                    } else {
                        Log.e(TAG, "Import process failed or had critical errors. Message: " + finalMessage);
                    }
                });
            }
        });
    }

    private void reInitializeDatabaseAndReloadData() {
        Log.d(TAG, "Re-initializing DatabaseHelper and reloading data.");
        if (getContext() == null && getActivity() != null) { // Ensure context is still valid
            databaseHelper = new DatabaseHelper(requireContext());
        } else if (getContext() != null) {
            databaseHelper = new DatabaseHelper(getContext());
        } else {
            Log.e(TAG, "Context is null, cannot re-initialize database helper.");
            Toast.makeText(getActivity(), "Error: Could not re-initialize database.", Toast.LENGTH_LONG).show();
            return;
        }

        // Reset adapter with an empty list before loading to avoid crashes if schema changed.
        if (memberAdapter != null) {
            allMembersDisplayList.clear();
            memberAdapter.setMemberDisplayInfoList(new ArrayList<>()); // Clear adapter
        }

        loadMembersFromDatabase(); // This will use the new databaseHelper instance
        applyFiltersAndSearch();   // Re-apply any active filters/search
        Toast.makeText(getContext(), "Refreshing Data.", Toast.LENGTH_SHORT).show();
    }

    private void setupRecyclerView() {
        memberAdapter = new MemberAdapter(new ArrayList<>(), requireContext()); // Pass empty list initially

        membersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        membersRecyclerView.setHasFixedSize(true);
        membersRecyclerView.setAdapter(memberAdapter);

        // If your adapter has a listener like setOnItemClickListener as you've shown:
        memberAdapter.setOnItemClickListener(memberInfo -> {
            if (memberInfo != null && memberInfo.getMemberID() != null) {
                // Log the periodId to confirm it's not -1
                Log.d(TAG, "Item clicked: " + memberInfo.getFirstName() + " ID: " + memberInfo.getMemberID() +
                        " PeriodID: " + memberInfo.getPeriodId()); // Make sure getPeriodId() returns the active period's ID

                if (memberInfo.getPeriodId() == -1) { // Add this check for robustness, though ideally it shouldn't happen now
                    Log.e(TAG, "Error: Clicked member has an invalid periodId (-1). Cannot navigate to edit.");
                    Toast.makeText(getContext(), "Error: Member data incomplete for editing.", Toast.LENGTH_SHORT).show();
                    return; // Don't navigate if periodId is invalid
                }

                Bundle bundle = new Bundle();
                bundle.putString("memberID", memberInfo.getMemberID());
                // ++ THIS IS THE KEY CHANGE FOR STEP 3 ++
                bundle.putInt("periodID", memberInfo.getPeriodId()); // UNCOMMENT AND USE THIS

                if (navController != null) {
                    // Ensure R.id.action_viewFragment3_to_memberEditFragment is the correct navigation action ID
                    // from your nav_graph.xml that leads from MemberListFragment to MemberEditFragment
                    navController.navigate(R.id.action_memberListFragment_to_memberEditFragment, bundle);
                } else {
                    Log.e(TAG, "NavController is null, cannot navigate.");
                    Toast.makeText(getContext(), "Error: Cannot open edit screen.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Clicked memberInfo or memberID is null.");
                Toast.makeText(getContext(), "Error: Cannot edit member details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMembersFromDatabase() {
        if (databaseHelper != null) {
            // MODIFIED: Use getAllMembersForDisplay()
            List<MemberDisplayInfo> fetchedMembers = databaseHelper.getActiveMembersForDisplay();
            allMembersDisplayList.clear(); // MODIFIED variable name
            if (fetchedMembers != null && !fetchedMembers.isEmpty()) {
                membersRecyclerView.setVisibility(View.VISIBLE);
                tvNoExpiredMembers.setVisibility(View.GONE);
                allMembersDisplayList.addAll(fetchedMembers);
                Log.d(TAG, "Members loaded for display: " + fetchedMembers.size());
            } else
            {
                membersRecyclerView.setVisibility(View.GONE);
                tvNoExpiredMembers.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e(TAG, "DatabaseHelper is null, cannot load members.");
        }

        if (radioGroupFilters.getCheckedRadioButtonId() == -1) {
            radioGroupFilters.check(R.id.radioButtonNoFilter);
        }
        applyFiltersAndSearch(); // Apply current filters and search to the newly loaded list
    }

    // ++ NEW: Setup RadioGroup Listener ++
    private void setupFilterRadioGroup() {
        // Set the initial filter based on the default checked RadioButton
        updateActiveFilterState(radioGroupFilters.getCheckedRadioButtonId());

        radioGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            updateActiveFilterState(checkedId);
            applyFiltersAndSearch();
        });
    }

    private void updateActiveFilterState(int checkedId) {
        if (checkedId == R.id.radioButtonFilterCurrentMonth) {
            currentActiveFilter = ActiveFilter.CURRENT_MONTH_REG;
        } else if (checkedId == R.id.radioButtonFilterExpiringSoon) {
            currentActiveFilter = ActiveFilter.EXPIRING_SOON;
        } else { // Defaults to radioButtonNoFilter or if somehow no valid ID is found
            currentActiveFilter = ActiveFilter.NONE;
        }
        Log.d(TAG, "Active filter changed to: " + currentActiveFilter);
    }


    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                applyFiltersAndSearch();
                return false; // Typically false if you want default behavior (e.g. hide keyboard)
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                applyFiltersAndSearch();
                return true;
            }
        });
    }

    private void applyFiltersAndSearch() {
        if (allMembersDisplayList == null) {
            allMembersDisplayList = new ArrayList<>();
        }
        List<MemberDisplayInfo> processingList = new ArrayList<>(allMembersDisplayList);

        // 1. Apply active pre-filter (current month, expiring soon)
        if (currentActiveFilter == ActiveFilter.CURRENT_MONTH_REG) {
            processingList = filterByCurrentMonthRegistration(processingList);
        } else if (currentActiveFilter == ActiveFilter.EXPIRING_SOON) {
            processingList = filterByExpiringSoon(processingList);
        }

        List<MemberDisplayInfo> finalList = new ArrayList<>();
        if (currentSearchQuery == null || currentSearchQuery.trim().isEmpty()) {
            finalList.addAll(processingList);
        } else {
            String filterPattern = currentSearchQuery.toLowerCase().trim();
            List<String> memberIdsFromReceiptSearch = new ArrayList<>();

            // --- New: Perform DB search for receipt number ---
            if (databaseHelper != null) {
                // You might want to add a check here to see if filterPattern could be a receipt number
                // e.g., if it's all digits or matches a certain pattern, to avoid unnecessary DB queries.
                // For simplicity now, we'll search always if there's a query.
                memberIdsFromReceiptSearch = databaseHelper.getMemberIdsByReceiptNumber(filterPattern);
            }
            // --- End New ---

            for (MemberDisplayInfo memberInfo : processingList) {
                boolean matchesTextSearch = false;
                boolean matchesReceiptSearch = false;

                // Standard text search on MemberDisplayInfo fields
                if ((memberInfo.getFirstName() != null && memberInfo.getFirstName().toLowerCase().contains(filterPattern)) ||
                        (memberInfo.getLastName() != null && memberInfo.getLastName().toLowerCase().contains(filterPattern)) ||
                        (memberInfo.getMemberID() != null && memberInfo.getMemberID().toLowerCase().contains(filterPattern)) ||
                        (memberInfo.getMemberTypeName() != null && memberInfo.getMemberTypeName().toLowerCase().contains(filterPattern)) ||
                        // Also check the *active* receipt number in MemberDisplayInfo
                        (memberInfo.getReceiptNumber() != null && memberInfo.getReceiptNumber().toLowerCase().contains(filterPattern))) {
                    matchesTextSearch = true;
                }

                // Check if the current member's ID was found in the receipt number search
                if (memberInfo.getMemberID() != null && memberIdsFromReceiptSearch.contains(memberInfo.getMemberID())) {
                    matchesReceiptSearch = true;
                }

                if (matchesTextSearch || matchesReceiptSearch) {
                    finalList.add(memberInfo);
                }
            }
        }

        if (memberAdapter != null) {
            memberAdapter.setMemberDisplayInfoList(finalList);
            Log.d(TAG, "Adapter updated. Displaying " + finalList.size() + " member infos. ActiveFilter: " +
                    currentActiveFilter + ", Query: '" + currentSearchQuery + "'");
        }

        if (finalList.isEmpty() && (currentSearchQuery != null && !currentSearchQuery.trim().isEmpty())) {
            Toast.makeText(getContext(), "No members found matching your criteria.", Toast.LENGTH_SHORT).show();
        } else if (finalList.isEmpty() && currentActiveFilter != ActiveFilter.NONE) {
            Toast.makeText(getContext(), "No members found for the selected filter.", Toast.LENGTH_SHORT).show();
        }
    }

    // MODIFIED: Filter Logic Methods now operate on List<MemberDisplayInfo>
    private List<MemberDisplayInfo> filterByCurrentMonthRegistration(List<MemberDisplayInfo> membersToFilter) {
        if (membersToFilter == null) return new ArrayList<>();
        YearMonth currentYearMonth = YearMonth.now();
        return membersToFilter.stream()
                .filter(memberInfo -> { // MODIFIED: parameter is memberInfo
                    if (memberInfo.getRegistrationDate() == null) return false;
                    YearMonth registrationYearMonth = YearMonth.from(memberInfo.getRegistrationDate());
                    return registrationYearMonth.equals(currentYearMonth);
                })
                .collect(Collectors.toList());
    }

    private List<MemberDisplayInfo> filterByExpiringSoon(List<MemberDisplayInfo> membersToFilter) {
        if (membersToFilter == null) return new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate fiveDaysFromNow = today.plusDays(5); // Consider making "5" a constant or configurable
        return membersToFilter.stream()
                .filter(memberInfo -> { // MODIFIED: parameter is memberInfo
                    if (memberInfo.getExpirationDate() == null) return false;
                    return !memberInfo.getExpirationDate().isBefore(today) &&
                            !memberInfo.getExpirationDate().isAfter(fiveDaysFromNow);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify views to prevent memory leaks
        membersRecyclerView = null;
        searchView = null;
        tvNoExpiredMembers = null;
        radioGroupFilters = null;
        exportButton = null;
        importButton = null;
        // Don't nullify databaseHelper here if you want it to survive view recreation,
        // unless you are sure you want a new one every time.
        // For import, we explicitly nullify and recreate it.
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // Refresh the list when the fragment becomes visible,
        // in case new members were added from another screen.
        loadMembersFromDatabase();
    }
}