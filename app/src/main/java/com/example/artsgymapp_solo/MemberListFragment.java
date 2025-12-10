package com.example.artsgymapp_solo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
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
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import android.os.Handler;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;

public class MemberListFragment extends Fragment
{
    private static final String TAG = "MemberListFragment";
    private RecyclerView membersRecyclerView;
    private MemberAdapter memberAdapter;
    private List<MemberDisplayInfo> allMembersDisplayList;
    private DatabaseHelper databaseHelper;
    private SearchView searchBarView1;
    private NavController navController;
    private RadioGroup radioGroupFilters;
    private TextView memberCountByMembershipType_TextView;
    private TextView textViewNoExpiredMembers;
    private Spinner filterByMonthSpinner;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private static final int REQUEST_CODE_SAVE_DB_ZIP_EXPORT = 2;
    private static final int REQUEST_CODE_PICK_DB_FILE_IMPORT = 103;

    private MaterialButton exportButton;
    private MaterialButton importButton;

    private int selectedMonthFilter = 0;
    private RadioGroup.OnCheckedChangeListener radioGroupListener;

    private enum ActiveFilter
    {
        NONE,
        CURRENT_MONTH_REG,
        EXPIRING_SOON,
        BY_SELECTED_MONTH_REG
    }

    private ActiveFilter currentActiveFilter = ActiveFilter.NONE;
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_memberlist, container, false);

        membersRecyclerView = view.findViewById(R.id.membersRecyclerView);
        searchBarView1 = view.findViewById(R.id.searchBarView1);
        radioGroupFilters = view.findViewById(R.id.radioGroupFilters);
        exportButton = view.findViewById(R.id.exportButton);
        importButton = view.findViewById(R.id.importButton);
        filterByMonthSpinner = view.findViewById(R.id.filterByMonthSpinner);
        memberCountByMembershipType_TextView = view.findViewById(R.id.memberCountByMembershipType_TextView);
        textViewNoExpiredMembers = view.findViewById(R.id.textViewNoExpiredMembers);

        int searchSrcTextId = getResources().getIdentifier("search_src_text", "id", requireContext().getPackageName());
        TextView searchText = searchBarView1.findViewById(searchSrcTextId);

        if (searchText != null) {
            int hintColor = ContextCompat.getColor(requireContext(), R.color.white);
            searchText.setHintTextColor(hintColor);
        }

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
        setupSearchView();
        setupFilterRadioGroup(); 
        setupImportExportButtons();
        loadMembersFromDatabase();
        setupMonthSpinner();

        if (allMembersDisplayList.isEmpty())
        {
            loadMembersFromDatabase();
        } else {
            applyFiltersAndSearch();
        }
    }

    private void setupMonthSpinner()
    {
        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.month_array, R.layout.spinner_item_months_collapsed);

        monthAdapter.setDropDownViewResource(R.layout.spinner_item_months);
        filterByMonthSpinner.setAdapter(monthAdapter);

        LocalDate today = LocalDate.now();
        int currentMonthValue = today.getMonthValue(); // This is 1-12

        if (currentMonthValue >= 1 && currentMonthValue <= 12)
        {
            filterByMonthSpinner.setSelection(currentMonthValue, false);
            selectedMonthFilter = currentMonthValue;

            currentActiveFilter = ActiveFilter.BY_SELECTED_MONTH_REG;
            if (radioGroupFilters.getCheckedRadioButtonId() != -1) {
                if (radioGroupListener != null) {
                    radioGroupFilters.setOnCheckedChangeListener(null);
                }
                radioGroupFilters.clearCheck();
                if (radioGroupListener != null) {
                    radioGroupFilters.setOnCheckedChangeListener(radioGroupListener);
                }
            }
        }
        else
        {
            filterByMonthSpinner.setSelection(0);
            selectedMonthFilter = 0;
        }

        filterByMonthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonthFilter = position;

                if (selectedMonthFilter > 0) {
                    currentActiveFilter = ActiveFilter.BY_SELECTED_MONTH_REG;

                    if (radioGroupFilters.getCheckedRadioButtonId() != -1) {
                        if (radioGroupListener != null) {
                            radioGroupFilters.setOnCheckedChangeListener(null);
                        }
                        radioGroupFilters.clearCheck();
                        if (radioGroupListener != null) {
                            radioGroupFilters.setOnCheckedChangeListener(radioGroupListener);
                        }
                    }
                } else
                {
                    updateActiveFilterState(radioGroupFilters.getCheckedRadioButtonId());
                }
                Log.d(TAG, "Month Spinner onItemSelected - Position: " + position + ", ActiveFilter: " + currentActiveFilter);
                applyFiltersAndSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedMonthFilter = 0;
                updateActiveFilterState(radioGroupFilters.getCheckedRadioButtonId());
                Log.d(TAG, "Month Spinner onNothingSelected - ActiveFilter: " + currentActiveFilter);
                applyFiltersAndSearch();
            }
        });

        if (currentMonthValue >= 1 && currentMonthValue <= 12)
        {
           applyFiltersAndSearch();
        }
    }

    private void setupImportExportButtons()
    {
        exportButton.setOnClickListener(v -> handleExportDatabase());
        importButton.setOnClickListener(v -> handleImportDatabase());
    }

    private void handleExportDatabase() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip"); 
        intent.putExtra(Intent.EXTRA_TITLE, "ArtsGymApp_Backup_" + System.currentTimeMillis() + ".zip"); 

        try {
            startActivityForResult(intent, REQUEST_CODE_SAVE_DB_ZIP_EXPORT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "No app found to handle file creation.", Toast.LENGTH_LONG).show();
            
        }
    }
    
    private void handleImportDatabase() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Import Database")
                .setMessage("Importing a database will overwrite current data. Are you sure you want to proceed? It's recommended to export the current database first.")
                .setPositiveButton("Import", (dialog, which) ->
                {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/zip");
                    
                    try
                    {
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
            if (requestCode == REQUEST_CODE_SAVE_DB_ZIP_EXPORT) { 

                performFullExportToZip(uri);
            } else if (requestCode == REQUEST_CODE_PICK_DB_FILE_IMPORT) {
                performDatabaseImport(uri); 
            }
        }
    }

    private void performFullExportToZip(Uri destinationZipUri) {
        if (getContext() == null) {
            
            Toast.makeText(getActivity(), "Error: Context not available for export.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(getContext(), "Exporting data... Please wait.", Toast.LENGTH_LONG).show();

        executorService.execute(() -> {
            boolean exportSuccess = false;
            String errorMessage = "Export failed. Unknown error.";
            File tempExportDir = null;

            try {
                tempExportDir = new File(getContext().getCacheDir(), "TempExportDir");
                if (tempExportDir.exists()) {
                    deleteRecursive(tempExportDir); 
                }
                if (!tempExportDir.mkdirs()) {
                    throw new IOException("Failed to create temporary export directory.");
                }
                
                if (databaseHelper != null) {
                    databaseHelper.closeDatabase();
                    
                }
                
                File currentDBFile = getContext().getDatabasePath(DatabaseHelper.DATABASE_NAME);
                if (!currentDBFile.exists()) {
                    throw new FileNotFoundException("Database file not found: " + currentDBFile.getAbsolutePath());
                }
                File tempDbFile = new File(tempExportDir, DatabaseHelper.DATABASE_NAME);
                copyFile(new FileInputStream(currentDBFile), new FileOutputStream(tempDbFile));

                DatabaseHelper tempDbReader = new DatabaseHelper(getContext()); 
                List<String> imagePaths = tempDbReader.getAllMemberImagePaths();
                tempDbReader.close();
                
                File tempImageBackupDir = new File(tempExportDir, "ImageBackup");
                if (!tempImageBackupDir.mkdirs()) {
                    throw new IOException("Failed to create temporary image backup directory.");
                }
                

                
                for (String imagePath : imagePaths) {
                    if (imagePath != null && !imagePath.isEmpty()) {
                        File sourceImageFile = new File(imagePath);
                        if (sourceImageFile.exists() && sourceImageFile.isFile()) {
                            File destImageFile = new File(tempImageBackupDir, sourceImageFile.getName());
                            try {
                                copyFile(new FileInputStream(sourceImageFile), new FileOutputStream(destImageFile));
                                
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }

                try (OutputStream out = getContext().getContentResolver().openOutputStream(destinationZipUri)) {
                    if (out == null) {
                        throw new IOException("Failed to open output stream for destination URI.");
                    }
                    ZipUtils.zipDirectory(tempExportDir, out); 
                    exportSuccess = true;
                    
                }

            } catch (Exception e) { 
                
                errorMessage = "Export failed: " + e.getMessage();
                exportSuccess = false;
            } finally {
                
                if (tempExportDir != null && tempExportDir.exists()) {
                    deleteRecursive(tempExportDir);
                    
                }

                final String finalMessage = exportSuccess ? "Data exported successfully to ZIP!" : errorMessage;

                mainThreadHandler.post(() -> {
                    
                    reInitializeDatabaseAndReloadData(); 
                    Toast.makeText(getContext(), finalMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        try {
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
            out.flush(); 
            out.close();
        }
    }
    
    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void performDatabaseImport(Uri sourceZipUri) { 
        if (getContext() == null) {
            
            Toast.makeText(getActivity(), "Error: Context not available for import.", Toast.LENGTH_SHORT).show();
            return;
        }

        File internalDbFileTarget = getContext().getDatabasePath(DatabaseHelper.DATABASE_NAME);
        File tempUnzipDir = new File(getContext().getCacheDir(), "TempImportDir"); 
        File targetImageDir = new File(getContext().getFilesDir(), "MemberImages"); 

        Toast.makeText(getContext(), "Importing data... Please wait.", Toast.LENGTH_LONG).show(); 

        executorService.execute(() -> { 
            boolean importSuccess = false; 
            String errorMessage = "Import failed. Unknown error.";

            try {
                if (databaseHelper != null) {
                    databaseHelper.closeDatabase();
                    databaseHelper = null; 
                    
                }
                System.gc(); 
                System.runFinalization();

                if (tempUnzipDir.exists()) {
                    ZipUtils.deleteRecursive(tempUnzipDir);
                }
                if (!tempUnzipDir.mkdirs()) {
                    throw new IOException("Failed to create temporary import directory: " + tempUnzipDir.getAbsolutePath());
                }

                try (InputStream zipInputStream = getContext().getContentResolver().openInputStream(sourceZipUri)) {
                    if (zipInputStream == null) {
                        throw new IOException("Failed to open input stream from selected ZIP file.");
                    }
                    ZipUtils.unzip(zipInputStream, tempUnzipDir); 
                    
                }

                File unzippedDbFile = new File(tempUnzipDir, DatabaseHelper.DATABASE_NAME);
                if (!unzippedDbFile.exists() || !unzippedDbFile.isFile()) {
                    throw new FileNotFoundException("Database file '" + DatabaseHelper.DATABASE_NAME + "' not found in the ZIP archive at " + unzippedDbFile.getAbsolutePath());
                }
                
                File databasesDir = internalDbFileTarget.getParentFile();
                if (databasesDir != null && !databasesDir.exists()) {
                    if (!databasesDir.mkdirs()) {
                        throw new IOException("Failed to create databases directory: " + databasesDir.getAbsolutePath());
                    }
                }

                try (InputStream in = new FileInputStream(unzippedDbFile);
                     OutputStream out = new FileOutputStream(internalDbFileTarget, false)) { 
                    copyFile(in, out); 
                    
                }
                
                File tempImageBackupDir = new File(tempUnzipDir, "ImageBackup"); 

                if (tempImageBackupDir.exists() && tempImageBackupDir.isDirectory()) {
                    
                    if (!targetImageDir.exists()) {
                        targetImageDir.mkdirs();
                    }

                    if (targetImageDir.exists() && targetImageDir.isDirectory()) {
                        File[] imageFilesToImport = tempImageBackupDir.listFiles();
                        if (imageFilesToImport != null && imageFilesToImport.length > 0) {
                            
                            int successfulImageCopies = 0;
                            for (File sourceImageFile : imageFilesToImport) {
                                if (sourceImageFile.isFile()) { 
                                    File destImageFile = new File(targetImageDir, sourceImageFile.getName());
                                    try (InputStream inImg = new FileInputStream(sourceImageFile);
                                         OutputStream outImg = new FileOutputStream(destImageFile, false)) { 
                                        copyFile(inImg, outImg); 
                                        
                                        successfulImageCopies++;
                                    } catch (IOException ignored) {
                                    }
                                }
                            }
                            
                        }
                    }
                }
                importSuccess = true;
            } catch (Exception e) { 
                
                errorMessage = "Import failed: " + e.getMessage(); 
                importSuccess = false;

            } finally {
                
                if (tempUnzipDir.exists()) {
                    ZipUtils.deleteRecursive(tempUnzipDir);
                }

                final boolean finalImportSuccess = importSuccess;
                
                final String finalMessage = finalImportSuccess ? "Data import process completed!" : errorMessage;

                mainThreadHandler.post(() -> {
                    reInitializeDatabaseAndReloadData(); 
                    Toast.makeText(getContext(), finalMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void reInitializeDatabaseAndReloadData() {
        
        if (getContext() == null && getActivity() != null) { 
            databaseHelper = new DatabaseHelper(requireContext());
        } else if (getContext() != null) {
            databaseHelper = new DatabaseHelper(getContext());
        } else {
            Toast.makeText(getActivity(), "Error: Could not re-initialize database.", Toast.LENGTH_LONG).show();
            return;
        }

        
        if (memberAdapter != null)
        {
            allMembersDisplayList.clear();
            memberAdapter.setMemberDisplayInfoList(new ArrayList<>()); 
        }

        loadMembersFromDatabase(); 
        applyFiltersAndSearch();   
        Toast.makeText(getContext(), "Refreshing Data.", Toast.LENGTH_SHORT).show();
    }

    private void setupRecyclerView()
    {
        memberAdapter = new MemberAdapter(new ArrayList<>(), requireContext()); 

        membersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        membersRecyclerView.setHasFixedSize(true);
        membersRecyclerView.setAdapter(memberAdapter);
        
        memberAdapter.setOnItemClickListener(memberInfo ->
        {
            if (memberInfo != null && memberInfo.getMemberID() != null) {
                
                Log.d(TAG, "Item clicked: " + memberInfo.getFirstName() + " ID: " + memberInfo.getMemberID() +
                        " PeriodID: " + memberInfo.getPeriodId()); 

                if (memberInfo.getPeriodId() == -1) { 
                    
                    Toast.makeText(getContext(), "Error: Member data incomplete for editing.", Toast.LENGTH_SHORT).show();
                    return; 
                }

                Bundle bundle = new Bundle();
                bundle.putString("memberID", memberInfo.getMemberID());
                
                bundle.putInt("periodID", memberInfo.getPeriodId()); 

                if (navController != null)
                {
                    navController.navigate(R.id.action_memberListFragment_to_memberEditFragment, bundle);
                }
                else
                {
                    Toast.makeText(getContext(), "Error: Cannot open edit screen.", Toast.LENGTH_SHORT).show();
                }
            } else {
                
                Toast.makeText(getContext(), "Error: Cannot edit member details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMembersFromDatabase() {
        if (databaseHelper != null) {
            List<MemberDisplayInfo> fetchedMembers = databaseHelper.getActiveMembersForDisplay();
            allMembersDisplayList.clear();
            if (fetchedMembers != null && !fetchedMembers.isEmpty())
            {
                allMembersDisplayList.addAll(fetchedMembers);
            }
        }
    }

    private void setupFilterRadioGroup()
    {
        updateActiveFilterState(radioGroupFilters.getCheckedRadioButtonId());

        radioGroupListener = (group, checkedId) ->
        {
            updateActiveFilterState(checkedId);
            applyFiltersAndSearch();
        };

        radioGroupFilters.setOnCheckedChangeListener(radioGroupListener);
    }

    private void updateActiveFilterState(int checkedId)
    {
        if (checkedId == R.id.radioButtonFilterExpiringSoon)
        {
            currentActiveFilter = ActiveFilter.EXPIRING_SOON;
            filterByMonthSpinner.setSelection(0);
            selectedMonthFilter = 0;
        }
        else if (checkedId == -1 && selectedMonthFilter > 0)
        {
            currentActiveFilter = ActiveFilter.BY_SELECTED_MONTH_REG;
        }
        else
        {
            currentActiveFilter = ActiveFilter.NONE;

            if (checkedId != -1)
            {
                filterByMonthSpinner.setSelection(0);
                selectedMonthFilter = 0;
            }
        }
    }

    private void setupSearchView()
    {
        searchBarView1.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                applyFiltersAndSearch();
                return false; 
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                applyFiltersAndSearch();
                return true;
            }
        });
    }

    private void updateMemberTypeCounts(List<MemberDisplayInfo> displayedMembers)
    {
        if (memberCountByMembershipType_TextView == null)
        {
            return;
        }

        if (displayedMembers == null || displayedMembers.isEmpty()) {
            memberCountByMembershipType_TextView.setText("Member Count: 0");
            memberCountByMembershipType_TextView.setVisibility(View.GONE);
            return;
        }

        Map<String, Integer> typeCounts = new HashMap<>();
        for (MemberDisplayInfo member : displayedMembers) {
            String typeName = member.getMemberTypeName();
            if (typeName == null || typeName.trim().isEmpty()) {
                typeName = "Unknown Type";
            }
            typeCounts.put(typeName, typeCounts.getOrDefault(typeName, 0) + 1);
        }

        if (typeCounts.isEmpty()) {
            memberCountByMembershipType_TextView.setText("Member Count: 0");
            memberCountByMembershipType_TextView.setVisibility(View.GONE);
            return;
        }

        StringBuilder countsTextBuilder = new StringBuilder("Member Count: ");
        boolean firstType = true;
        for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
            if (!firstType) {
                countsTextBuilder.append(" | ");
            }
            countsTextBuilder.append(String.format(Locale.getDefault(), "%s: %d", entry.getKey(), entry.getValue()));
            firstType = false;
        }
        memberCountByMembershipType_TextView.setText(countsTextBuilder.toString());
        memberCountByMembershipType_TextView.setVisibility(View.VISIBLE);
    }

    private void applyFiltersAndSearch()
    {
        if (allMembersDisplayList == null) {
            allMembersDisplayList = new ArrayList<>();
        }
        List<MemberDisplayInfo> processingList = new ArrayList<>(allMembersDisplayList);

        if (currentActiveFilter == ActiveFilter.CURRENT_MONTH_REG) {
            processingList = filterByCurrentMonthRegistration(processingList);
        } else if (currentActiveFilter == ActiveFilter.EXPIRING_SOON) {
            processingList = filterByExpiringSoon(processingList);
        } else if (currentActiveFilter == ActiveFilter.BY_SELECTED_MONTH_REG && selectedMonthFilter > 0) {
            processingList = filterBySelectedMonthRegistration(processingList, selectedMonthFilter);
        }

        List<MemberDisplayInfo> finalList = new ArrayList<>();
        if (currentSearchQuery == null || currentSearchQuery.trim().isEmpty()) {
            finalList.addAll(processingList);
        } else {
            String filterPattern = currentSearchQuery.toLowerCase().trim();
            List<String> memberIdsFromReceiptSearch = new ArrayList<>();

            if (databaseHelper != null) {
                memberIdsFromReceiptSearch = databaseHelper.getMemberIdsByReceiptNumber(filterPattern);
            }

            for (MemberDisplayInfo memberInfo : processingList) {
                boolean matchesTextSearch = false;
                boolean matchesReceiptSearch = false;

                if ((memberInfo.getFirstName() != null && memberInfo.getFirstName().toLowerCase().contains(filterPattern)) ||
                        (memberInfo.getLastName() != null && memberInfo.getLastName().toLowerCase().contains(filterPattern)) ||
                        (memberInfo.getMemberID() != null && memberInfo.getMemberID().toLowerCase().contains(filterPattern)) ||
                        (memberInfo.getMemberTypeName() != null && memberInfo.getMemberTypeName().toLowerCase().contains(filterPattern)) ||
                        (memberInfo.getReceiptNumber() != null && memberInfo.getReceiptNumber().toLowerCase().contains(filterPattern))) {
                    matchesTextSearch = true;
                }

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
        }

        updateMemberTypeCounts(finalList);

        if (textViewNoExpiredMembers != null)
        {
            if (finalList.isEmpty())
            {
                textViewNoExpiredMembers.setVisibility(View.VISIBLE);
            }
            else
            {
                textViewNoExpiredMembers.setVisibility(View.GONE);
            }
        }

        if (finalList.isEmpty() && (currentSearchQuery != null && !currentSearchQuery.trim().isEmpty()))
        {
            Toast.makeText(getContext(), "No members found matching your criteria.", Toast.LENGTH_SHORT).show();

            if (memberCountByMembershipType_TextView != null) {
                memberCountByMembershipType_TextView.setText("Member Count: 0");
                memberCountByMembershipType_TextView.setVisibility(View.GONE);
            }
        } else if (finalList.isEmpty() && currentActiveFilter != ActiveFilter.NONE) {
            Toast.makeText(getContext(), "No members found for the selected filter.", Toast.LENGTH_SHORT).show();

            if (memberCountByMembershipType_TextView != null) {
                memberCountByMembershipType_TextView.setText("Member Count: 0");
                memberCountByMembershipType_TextView.setVisibility(View.GONE);
            }
        } else if (!finalList.isEmpty()){
            if (memberCountByMembershipType_TextView != null)
            {
                memberCountByMembershipType_TextView.setVisibility(View.VISIBLE);
            }
        }
    }

    private List<MemberDisplayInfo> filterByCurrentMonthRegistration(List<MemberDisplayInfo> membersToFilter) {
        if (membersToFilter == null) return new ArrayList<>();
        YearMonth currentYearMonth = YearMonth.now();
        return membersToFilter.stream()
                .filter(memberInfo -> { 
                    if (memberInfo.getRegistrationDate() == null) return false;
                    YearMonth registrationYearMonth = YearMonth.from(memberInfo.getRegistrationDate());
                    return registrationYearMonth.equals(currentYearMonth);
                })
                .collect(Collectors.toList());
    }

    private List<MemberDisplayInfo> filterByExpiringSoon(List<MemberDisplayInfo> membersToFilter) {
        if (membersToFilter == null) return new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate fiveDaysFromNow = today.plusDays(5); 
        return membersToFilter.stream()
                .filter(memberInfo -> { 
                    if (memberInfo.getExpirationDate() == null) return false;
                    return !memberInfo.getExpirationDate().isBefore(today) &&
                            !memberInfo.getExpirationDate().isAfter(fiveDaysFromNow);
                })
                .collect(Collectors.toList());
    }

    private List<MemberDisplayInfo> filterBySelectedMonthRegistration(List<MemberDisplayInfo> membersToFilter, int monthOfYear)
    {
        if (membersToFilter == null || monthOfYear <= 0 || monthOfYear > 12) {
            return new ArrayList<>(membersToFilter);
        }
        if (membersToFilter.isEmpty()) return new ArrayList<>();

        Month selectedMonthEnum = Month.of(monthOfYear);

        return membersToFilter.stream()
                .filter(memberInfo -> {
                    if (memberInfo.getRegistrationDate() == null) return false;
                    return memberInfo.getRegistrationDate().getMonth() == selectedMonthEnum;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        membersRecyclerView = null;
        searchBarView1 = null;
        radioGroupFilters = null;
        exportButton = null;
        importButton = null;
        memberCountByMembershipType_TextView = null;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        loadMembersFromDatabase();
    }
}