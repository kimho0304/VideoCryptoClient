package com.example.videocrypto.util;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.locks.ReentrantLock;

public class Locker {
    private static boolean isLocked = false;
    private static final String TAG = "Locker_Debug";
    private static ReentrantLock lock = new ReentrantLock();

    public void lock(){
        lock.lock();
        try {
            // ... method body
        } finally {
            lock.unlock();
        }
    }

    public static synchronized void lock(Context context) throws InterruptedException {
        isLocked = true;
        //while(isLocked){
            Log.d(TAG, "Lock.");
            Thread.sleep(4000);
            isLocked = false;
        //}
        Log.d(TAG, "Unlock.");
    }
}
