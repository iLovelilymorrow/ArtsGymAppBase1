package com.example.artsgymapp_solo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class activity_idle extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_splash), (v, insets) ->
        {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // THIS IS THE CORRECT PLACEMENT FOR onTouchEvent
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            Log.d("ActivityIdle", "Idle screen tapped. Returning to MainActivity.");
            Intent intent = new Intent(activity_idle.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(0, 0); // No animation for this transition
            finish(); // Finish activity_idle so it's not on the back stack
            return true; // Event was handled
        }
        return super.onTouchEvent(event); // Default handling for other touch events
    }
}