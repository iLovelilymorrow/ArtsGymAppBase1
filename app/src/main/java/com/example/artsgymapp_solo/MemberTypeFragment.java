package com.example.artsgymapp_solo; // Your package

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog; // For confirmation dialog
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

// Implement the adapter's delete listener interface
public class MemberTypeFragment extends Fragment implements MemberTypeAdapter.OnMemberTypeDeleteListener {

    private static final String TAG = "MemberTypeFragment";

    private EditText editTextMemberTypeName;
    private EditText editTextDurationDays;
    private androidx.appcompat.widget.AppCompatCheckBox isTwoInOneCheckbox;
    private Button buttonSaveMemberType;
    private RecyclerView recyclerViewMemberTypes;
    private MemberTypeAdapter memberTypeAdapter;
    private List<MemberType> currentMemberTypesList;
    private DatabaseHelper databaseHelper;

    public MemberTypeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(requireContext());
        currentMemberTypesList = new ArrayList<>(); // Initialize the list
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_membertype, container, false); // Your updated layout name

        editTextMemberTypeName = view.findViewById(R.id.editTextMemberTypeName);
        editTextDurationDays = view.findViewById(R.id.editTextDurationDays);
        isTwoInOneCheckbox = view.findViewById(R.id.isTwoInOneCheckbox);
        buttonSaveMemberType = view.findViewById(R.id.buttonSaveMemberType);
        recyclerViewMemberTypes = view.findViewById(R.id.recyclerViewMemberTypes); // Added

        buttonSaveMemberType.setOnClickListener(v -> saveMemberType());

        setupRecyclerView();
        loadMemberTypesFromDb(); // Load initially

        return view;
    }

    private void setupRecyclerView() {
        // Pass 'this' as the delete listener
        memberTypeAdapter = new MemberTypeAdapter(requireContext(), currentMemberTypesList, this);
        recyclerViewMemberTypes.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewMemberTypes.setAdapter(memberTypeAdapter);
    }

    private void loadMemberTypesFromDb() {
        List<MemberType> types = databaseHelper.getAllMemberTypes();
        if (types != null) {
            currentMemberTypesList.clear();
            currentMemberTypesList.addAll(types);
            memberTypeAdapter.notifyDataSetChanged(); // Or adapter.updateData(types);
            Log.d(TAG, "Loaded " + types.size() + " member types from DB.");
        } else {
            Log.d(TAG, "No member types found in DB or error loading.");
        }
    }

    private void saveMemberType() {
        String name = editTextMemberTypeName.getText().toString().trim();
        String durationStr = editTextDurationDays.getText().toString().trim();
        boolean isTwoInOne = isTwoInOneCheckbox.isChecked();

        if (TextUtils.isEmpty(name)) {
            editTextMemberTypeName.setError("Name is required");
            editTextMemberTypeName.requestFocus();
            return;
        } else {
            editTextMemberTypeName.setError(null);
        }

        if (TextUtils.isEmpty(durationStr)) {
            editTextDurationDays.setError("Duration is required");
            editTextDurationDays.requestFocus();
            return;
        } else {
            editTextDurationDays.setError(null);
        }

        int durationDays;
        try {
            durationDays = Integer.parseInt(durationStr);
            if (durationDays <= 0) {
                editTextDurationDays.setError("Duration must be greater than 0");
                editTextDurationDays.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            editTextDurationDays.setError("Invalid number for duration");
            editTextDurationDays.requestFocus();
            return;
        }

        if (databaseHelper.addMemberType(name, durationDays, isTwoInOne)) {
            Toast.makeText(getContext(), "Membership Type '" + name + "' added!", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Membership Type added: " + name + ", Duration: " + durationDays + ", TwoInOne: " + isTwoInOne);
            editTextMemberTypeName.setText("");
            editTextDurationDays.setText("");
            isTwoInOneCheckbox.setChecked(false);
            editTextMemberTypeName.requestFocus();
            loadMemberTypesFromDb(); // Refresh the list after adding
        } else {
            Toast.makeText(getContext(), "Failed to add Membership Type. It might already exist.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to add Membership Type: " + name);
        }
    }

    // Implementation of OnMemberTypeDeleteListener
    @Override
    public void onDeleteClicked(MemberType memberType) {
        // Show a confirmation dialog before deleting
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Membership Type")
                .setMessage("Are you sure you want to delete '" + memberType.getName() + "'?\nThis action cannot be undone and might affect existing members if this type is in use (consider implications).") // Add warning
                .setPositiveButton("Delete", (dialog, which) -> {
                    performDeleteMemberType(memberType);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.ic_delete) // Optional: show delete icon in dialog
                .show();
    }

    private void performDeleteMemberType(MemberType memberType) {
        // You'll need a delete method in DatabaseHelper by ID or name
        // For now, let's assume delete by ID (which is better)
        if (databaseHelper.deleteMemberType(memberType.getId())) {
            Toast.makeText(getContext(), "'" + memberType.getName() + "' deleted successfully.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Member type deleted: ID " + memberType.getId() + ", Name: " + memberType.getName());
            loadMemberTypesFromDb(); // Refresh the list
        } else {
            Toast.makeText(getContext(), "Failed to delete '" + memberType.getName() + "'. It might be in use or an error occurred.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to delete member type: ID " + memberType.getId());
        }
    }
}