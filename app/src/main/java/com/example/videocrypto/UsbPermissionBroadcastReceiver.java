package com.example.videocrypto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

public class UsbPermissionBroadcastReceiver extends BroadcastReceiver {
    protected static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    protected final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    protected final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";

    public UsbDevice device = null;
    public static String msg;
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("hello_receive", "onReceive of Main");

        if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
            synchronized (this) {

                device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if(device != null){
                        //call method to set up device communication
                        Log.d("hello_receive", "Device lists: "+device);
                        msg = "Device name: "+device.getDeviceName()+", PID: "+String.valueOf(device.getProductId())+", VID: "+String.valueOf(device.getVendorId());
                    }
                    else{
                        Log.d("hello_receive", "NULL");
                        msg = "NULL";
                    }
                }
                else {
                    Log.d("hello_receive", "Permission denied for device " + device);
                }
            }
        }
        else if(ACTION_USB_ATTACHED.equals(intent.getAction())){
            UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

            try {
                while (deviceIterator.hasNext()) {
                    device = deviceIterator.next();
                    msg = "Device name: " + device.getDeviceName() + ", PID: " + String.valueOf(device.getProductId()) + ", VID: " + String.valueOf(device.getVendorId());
                }
            }
            catch(NullPointerException e){
            }

            if(device != null){
                msg = "Device name: "+device.getDeviceName()+", PID: "+String.valueOf(device.getProductId())+", VID: "+String.valueOf(device.getVendorId());
//                Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
            else{
                msg = "NULL";
//                Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }
        else if(ACTION_USB_DETACHED.equals(intent.getAction())){
            msg = "NULL";
//            Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}
