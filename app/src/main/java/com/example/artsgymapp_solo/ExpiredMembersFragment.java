package com.example.artsgymapp_solo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Looper; 
import android.os.Handler; 
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpiredMembersFragment extends Fragment implements ExpiredMemberAdapter.OnExpiredMemberClickListener {

    private RecyclerView recyclerViewExpiredMembers;
    private ExpiredMemberAdapter adapter;
    private List<MemberDisplayInfo> expiredMemberList;
    private DatabaseHelper dbHelper;
    private SearchView searchViewExpiredMembers;
    private TextView textViewNoExpiredMembers;
    private NavController navController;

    
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    public ExpiredMembersFragment() {
        
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(getContext());
        expiredMemberList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_expiredmembers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        recyclerViewExpiredMembers = view.findViewById(R.id.recyclerViewExpiredMembers);
        searchViewExpiredMembers = view.findViewById(R.id.searchBarView);
        textViewNoExpiredMembers = view.findViewById(R.id.textViewNoExpiredMembers);

        recyclerViewExpiredMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ExpiredMemberAdapter(getContext(), expiredMemberList, this);
        recyclerViewExpiredMembers.setAdapter(adapter);

        setupSearchView();
        
        
    }

    @Override
    public void onResume() {
        super.onResume();
        
        
        loadExpiredMembers();
    }

    private void setupSearchView() {
        searchViewExpiredMembers.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }

    private void loadExpiredMembers() {
        
        executorService.execute(() -> {
            List<MemberDisplayInfo> members = dbHelper.getExpiredMembersForDisplay();
            mainThreadHandler.post(() -> {
                
                if (members.isEmpty()) {
                    textViewNoExpiredMembers.setVisibility(View.VISIBLE);
                    recyclerViewExpiredMembers.setVisibility(View.GONE);
                } else {
                    textViewNoExpiredMembers.setVisibility(View.GONE);
                    recyclerViewExpiredMembers.setVisibility(View.VISIBLE);
                }
                adapter.updateList(members);
            });
        });
    }

    @Override
    public void onExpiredMemberClick(MemberDisplayInfo memberDisplayInfo) {
        
        Bundle bundle = new Bundle();
        bundle.putString("memberID", memberDisplayInfo.getMemberID());

        navController.navigate(R.id.action_expiredMembersFragment_to_renewMembershipFragment, bundle);
        
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        recyclerViewExpiredMembers = null;
        searchViewExpiredMembers = null;
        textViewNoExpiredMembers = null;
        adapter = null; 
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}