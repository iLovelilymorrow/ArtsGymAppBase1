package com.example.artsgymapp_solo;
import android.graphics.Bitmap;

public interface FingerprintEnrollmentCallback
{
    // target: An integer representing which fingerprint is being enrolled (e.g., 1 for primary, 2 for partner)
    void onEnrollmentProgress(int target, int progress, int total, String message);
    void onEnrollmentComplete(int target, Bitmap capturedImage, byte[] mergedTemplate);
    void onEnrollmentFailed(int target, String errorMessage);
}