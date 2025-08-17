package com.example.artsgymapp_solo.ZKUSBManager;

import android.hardware.usb.UsbDevice;

public interface ZKUSBManagerListener
{
    
    
    
    void onCheckPermission(int result);

    void onUSBArrived(UsbDevice device);

    void onUSBRemoved(UsbDevice device);
}
