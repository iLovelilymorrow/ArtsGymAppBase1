package com.example.artsgymapp_solo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map; // Import Map
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors; // For more advanced formatting if needed later

public class RecordsFragment extends Fragment implements RecordsAdapter.OnRecordActionListener {
    private static final String TAG = "RecordsFragment";
    private RecyclerView recyclerViewRecords;
    private RecordsAdapter adapter;
    private DatabaseHelper dbHelper;
    private SearchView searchViewRecords;
    private TextView textViewNoRecords;

    private TextView textViewStatusCounts; // For detailed status by type
    private TextView textViewTotalCounts;  // For total counts per membership type
    private MaterialButton clearRecordsButton;
    private Spinner filterByMonthSpinner;

    private int selectedMonthFilter = 0;
    private String currentSearchQuery = "";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public RecordsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewRecords = view.findViewById(R.id.recyclerViewExpiredMembers);
        searchViewRecords = view.findViewById(R.id.searchBarView);
        textViewNoRecords = view.findViewById(R.id.textViewNoExpiredMembers);
        clearRecordsButton = view.findViewById(R.id.clearRecordsButton);
        filterByMonthSpinner = view.findViewById(R.id.filterByMonthSpinner);
        textViewStatusCounts = view.findViewById(R.id.textViewStatusCounts);
        textViewTotalCounts = view.findViewById(R.id.textViewTotalCounts); // Initialize this

        recyclerViewRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecordsAdapter(getContext(), new ArrayList<>(), this);
        recyclerViewRecords.setAdapter(adapter);

        setupSearchView();
        setupClearRecordsButton();
        setupMonthSpinner();

        loadRecords();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecords();
    }

    private void setupSearchView() {
        searchViewRecords.setQuery(currentSearchQuery, false);
        searchViewRecords.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                applySearchToAdapter();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                applySearchToAdapter();
                return true;
            }
        });
    }

    private void setupMonthSpinner() {
        if (getContext() == null) return;
        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.month_array, android.R.layout.simple_spinner_item);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterByMonthSpinner.setAdapter(monthAdapter);
        filterByMonthSpinner.setSelection(selectedMonthFilter, false);
        filterByMonthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (selectedMonthFilter != position) {
                    selectedMonthFilter = position;
                    loadRecords();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (selectedMonthFilter != 0) {
                    selectedMonthFilter = 0;
                    loadRecords();
                }
            }
        });
    }

    private void setupClearRecordsButton() {
        clearRecordsButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Clear Old Records")
                    .setMessage("Are you sure you want to permanently delete all records. This action cannot be undone.")
                    .setPositiveButton("Clear Old Records", (dialog, which) -> clearRecordsOlderThanThreeMonthsWindow())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void clearRecordsOlderThanThreeMonthsWindow() {
        executorService.execute(() -> {
            boolean success = dbHelper.deleteMembershipPeriodsStartedBeforeThreeMonthsWindow();
            mainThreadHandler.post(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Old membership records cleared.", Toast.LENGTH_SHORT).show();
                    selectedMonthFilter = 0;
                    if (filterByMonthSpinner != null) {
                        filterByMonthSpinner.setSelection(0, false);
                    }
                    searchViewRecords.setQuery("", false);
                    currentSearchQuery = "";
                    loadRecords();
                } else {
                    Toast.makeText(getContext(), "Failed to clear old records. Check logs.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void loadRecords() {
        Log.d(TAG, "loadRecords called. Month filter: " + selectedMonthFilter + ", Search Query: '" + currentSearchQuery + "'");
        executorService.execute(() -> {
            List<MemberDisplayInfo> membersFromDb = dbHelper.getMembershipsStartedInLastThreeMonthsForDisplay(selectedMonthFilter);
            mainThreadHandler.post(() -> {
                adapter.updateList(membersFromDb);
                applySearchToAdapter(); // This will also call updateStatusCounts
            });
        });
    }

    private void updateStatusCounts(List<MemberDisplayInfo> currentList) {
        if (currentList == null) {
            if (textViewStatusCounts != null) textViewStatusCounts.setText("");
            if (textViewTotalCounts != null) textViewTotalCounts.setText("");
            return;
        }

        // Map: MemberTypeName -> (Map: Status -> Count)
        Map<String, Map<MemberDisplayInfo.MembershipStatus, Integer>> statusByTypeCounts = new HashMap<>();
        // Map: MemberTypeName -> TotalCount
        Map<String, Integer> totalTypeCounts = new HashMap<>();

        for (MemberDisplayInfo member : currentList) {
            String memberType = member.getMemberTypeName() != null ? member.getMemberTypeName() : "Unknown Type";
            MemberDisplayInfo.MembershipStatus status = member.getCurrentMembershipStatus();

            // Increment total count for this membership type
            totalTypeCounts.put(memberType, totalTypeCounts.getOrDefault(memberType, 0) + 1);

            // Increment count for this specific status within this membership type
            statusByTypeCounts.putIfAbsent(memberType, new HashMap<>());
            Map<MemberDisplayInfo.MembershipStatus, Integer> typeStatusMap = statusByTypeCounts.get(memberType);
            if (typeStatusMap != null) { // Should not be null due to putIfAbsent
                typeStatusMap.put(status, typeStatusMap.getOrDefault(status, 0) + 1);
            }
        }

        // --- Build string for textViewStatusCounts (Detailed: Type (Status): Count) ---
        StringBuilder detailedStatusText = new StringBuilder();
        if (statusByTypeCounts.isEmpty()) {
            detailedStatusText.append("No members in current view.");
        } else {
            List<String> sortedMemberTypes = new ArrayList<>(statusByTypeCounts.keySet());
            Collections.sort(sortedMemberTypes); // Sort for consistent order

            for (String type : sortedMemberTypes) {
                Map<MemberDisplayInfo.MembershipStatus, Integer> countsForType = statusByTypeCounts.get(type);
                if (countsForType != null && !countsForType.isEmpty()) {
                    List<String> typeStatusDetails = new ArrayList<>();
                    // Define preferred order for statuses
                    MemberDisplayInfo.MembershipStatus[] statusOrder = {
                            MemberDisplayInfo.MembershipStatus.ACTIVE,
                            MemberDisplayInfo.MembershipStatus.EXPIRING_SOON,
                            MemberDisplayInfo.MembershipStatus.EXPIRED
                    };

                    for (MemberDisplayInfo.MembershipStatus statKey : statusOrder) {
                        if (countsForType.containsKey(statKey)) {
                            String statusName = "";
                            switch(statKey) {
                                case ACTIVE: statusName = "Active"; break;
                                case EXPIRING_SOON: statusName = "Expiring"; break;
                                case EXPIRED: statusName = "Expired"; break;
                            }
                            typeStatusDetails.add(String.format("%s (%s): %d", type, statusName, countsForType.get(statKey)));
                        }
                    }
                    if (!typeStatusDetails.isEmpty()) {
                        detailedStatusText.append(String.join(" | ", typeStatusDetails)).append("\n");
                    }
                }
            }
        }
        if (textViewStatusCounts != null) {
            textViewStatusCounts.setText(detailedStatusText.toString().trim());
        }

        // --- Build string for textViewTotalCounts (Total: Type: Count) ---
        StringBuilder totalCountsText = new StringBuilder();
        if (totalTypeCounts.isEmpty()) {
            totalCountsText.append("No members in current view.");
        } else {
            List<String> sortedTotalMemberTypes = new ArrayList<>(totalTypeCounts.keySet());
            Collections.sort(sortedTotalMemberTypes); // Sort for consistent order

            List<String> totalDetails = new ArrayList<>();
            for (String type : sortedTotalMemberTypes) {
                totalDetails.add(String.format("%s: %d", type, totalTypeCounts.get(type)));
            }
            totalCountsText.append(String.join(" | ", totalDetails));
        }
        if (textViewTotalCounts != null) {
            textViewTotalCounts.setText(totalCountsText.toString().trim());
        }
    }


    private void applySearchToAdapter() {
        if (adapter != null) {
            adapter.filter(currentSearchQuery);
            updateNoRecordsTextView();
            updateStatusCounts(adapter.getCurrentFilteredList()); // This is key
        }
    }

    private void updateNoRecordsTextView() {
        if (adapter == null || textViewNoRecords == null || recyclerViewRecords == null) {
            return;
        }
        if (adapter.getItemCount() == 0) {
            String message;
            boolean isStatusSearch = currentSearchQuery.equalsIgnoreCase("expired") ||
                    currentSearchQuery.equalsIgnoreCase("expiring") ||
                    currentSearchQuery.equalsIgnoreCase("expiring soon") ||
                    currentSearchQuery.equalsIgnoreCase("active");

            if (!currentSearchQuery.isEmpty()) {
                if (isStatusSearch) {
                    message = "No records with status: " + currentSearchQuery;
                } else {
                    message = "No records match your search: '" + currentSearchQuery + "'";
                }
                if (selectedMonthFilter > 0) {
                    String[] months = getResources().getStringArray(R.array.month_array);
                    String monthName = (selectedMonthFilter < months.length) ? months[selectedMonthFilter] : "selected month";
                    message += " for " + monthName;
                }
            } else if (selectedMonthFilter > 0) {
                String[] months = getResources().getStringArray(R.array.month_array);
                String monthName = (selectedMonthFilter < months.length) ? months[selectedMonthFilter] : "the selected month";
                message = "No memberships started in " + monthName + " within the current display window.";
            } else {
                // More generic message if no filters and no search led to empty list
                message = "No membership records found matching the current criteria.";
            }
            textViewNoRecords.setText(message);
            textViewNoRecords.setVisibility(View.VISIBLE);
            recyclerViewRecords.setVisibility(View.GONE);
        } else {
            textViewNoRecords.setVisibility(View.GONE);
            recyclerViewRecords.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDeleteClick(MemberDisplayInfo memberDisplayInfo) {
        if (getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Member Record")
                .setMessage("Are you sure you want to delete " + memberDisplayInfo.getFullName() + "'s record (ID: " + memberDisplayInfo.getMemberID() + ")? This will delete ALL their membership periods and cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    executorService.execute(() -> {
                        boolean success = dbHelper.deleteMember(memberDisplayInfo.getMemberID());
                        mainThreadHandler.post(() -> {
                            if (success) {
                                Toast.makeText(getContext(), memberDisplayInfo.getFullName() + "'s record deleted.", Toast.LENGTH_SHORT).show();
                                loadRecords();
                            } else {
                                Toast.makeText(getContext(), "Failed to delete " + memberDisplayInfo.getFullName() + "'s record.", Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerViewRecords = null;
        searchViewRecords = null;
        textViewNoRecords = null;
        clearRecordsButton = null;
        filterByMonthSpinner = null;
        textViewStatusCounts = null;
        textViewTotalCounts = null;
        adapter = null;
        Log.d(TAG, "onDestroyView called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        Log.d(TAG, "onDestroy called");
    }
}
