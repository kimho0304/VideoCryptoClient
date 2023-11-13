package com.example.videocrypto.util;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Locker {
    private static boolean isLocked = false;
    private static final String TAG = "Locker_Debug";
    private static final boolean VERBOSE = true;
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition condition = lock.newCondition();
    private static boolean sentFlag = false;
    private static int cnt = 0;

    public  void lock() {
        if (VERBOSE)
            Log.d(TAG, "locked: " + cnt);
        lock.lock();
        try {
            if (sentFlag == false)
                condition.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException: " + e);
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void unlock() {
        if (VERBOSE)
            Log.d(TAG, "unlocked: " + cnt++);
        lock.lock();
        sentFlag = true;
        try {
            condition.signal();
        } finally {
            lock.unlock();
        }
    }
}
