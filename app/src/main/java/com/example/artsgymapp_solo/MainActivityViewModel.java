package com.example.artsgymapp_solo;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainActivityViewModel extends ViewModel
{
    private final MutableLiveData<Bitmap> bitmap1 = new MutableLiveData<>();

    public void setBitmap1(Bitmap bitmap)
    {
        bitmap1.setValue(bitmap);
    }

    public LiveData<Bitmap> getBitmap1()
    {
        return bitmap1;
    }

    private final MutableLiveData<Bitmap> bitmap2 = new MutableLiveData<>();

    public void setBitmap2(Bitmap bitmap)
    {
        bitmap2.setValue(bitmap);
    }

    public LiveData<Bitmap> getBitmap2()
    {
        return bitmap2;
    }


    private final MutableLiveData<byte []> byte1 = new MutableLiveData<>();

    public void setByte1(byte [] _byte)
    {
        byte1.setValue(_byte);
    }

    public LiveData<byte []> getByte1()
    {
        return byte1;
    }

    private final MutableLiveData<byte []> byte2 = new MutableLiveData<>();

    public void setByte2(byte [] _byte)
    {
        byte2.setValue(_byte);
    }

    public LiveData<byte []> getByte2()
    {
        return byte2;
    }


    private final MutableLiveData<Uri> uri1 = new MutableLiveData<>();

    public void setUri1(Uri uri)
    {
        uri1.setValue(uri);
    }

    public LiveData<Uri> getUri1()
    {
        return uri1;
    }

    private final MutableLiveData<Uri> uri2 = new MutableLiveData<>();

    public void setUri2(Uri uri)
    {
        uri2.setValue(uri);
    }

    public LiveData<Uri> getUri2()
    {
        return uri2;
    }


    private final MutableLiveData<String> imagePath1 = new MutableLiveData<>();

    public void setImagePath1(String imagePath)
    {
        imagePath1.setValue(imagePath);
    }

    public LiveData<String> getImagePath1()
    {
        return imagePath1;
    }

    private final MutableLiveData<String> imagePath2 = new MutableLiveData<>();

    public void setImagePath2(String imagePath)
    {
        imagePath2.setValue(imagePath);
    }

    public LiveData<String> getImagePath2()
    {
        return imagePath2;
    }

    public void clearAllData()
    {
        bitmap1.setValue(null);
        bitmap2.setValue(null);
        byte1.setValue(null);
        byte2.setValue(null);
        uri1.setValue(null);
        uri2.setValue(null);
        imagePath1.setValue(null);
        imagePath2.setValue(null);
    }

}