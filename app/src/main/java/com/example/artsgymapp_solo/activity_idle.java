package com.example.artsgymapp_solo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log; // Import Log

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class activity_idle extends AppCompatActivity {

    private static final String TAG = "activity_idle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash); // Assuming activity_splash is your idle screen layout

        View rootView = findViewById(R.id.activity_splash); // Assuming this is the root view of your idle screen

        if (rootView != null) {
            rootView.setOnClickListener(v -> {
                Log.d(TAG, "Idle screen tapped. Returning to MainActivity.");
                returnToMainActivity();
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // When idle activity resumes, ensure MainActivity is aware it's coming back
        // and can re-engage identification if needed.
        // The MainActivity's onResume will handle the identification start.
        Log.d(TAG, "activity_idle onResume. Scanner should be in identification mode.");
    }

    private void returnToMainActivity() {
        Intent intent = new Intent(activity_idle.this, MainActivity.class);
        // Use FLAG_ACTIVITY_SINGLE_TOP or FLAG_ACTIVITY_REORDER_TO_FRONT
        // to bring MainActivity to front without recreating it,
        // allowing it to process the identification result.
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish(); // Finish this idle activity
    }
}
