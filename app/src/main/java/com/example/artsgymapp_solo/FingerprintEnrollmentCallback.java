package com.example.artsgymapp_solo;
import android.graphics.Bitmap;

public interface FingerprintEnrollmentCallback
{
    void onEnrollmentProgress(int target, int progress, int total, String message);
    void onEnrollmentComplete(int target, Bitmap capturedImage, byte[] mergedTemplate);
    void onEnrollmentFailed(int target, String errorMessage);
}