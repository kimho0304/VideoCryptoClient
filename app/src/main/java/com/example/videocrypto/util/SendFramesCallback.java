package com.example.videocrypto.util;

import android.graphics.Bitmap;

public interface SendFramesCallback {
   // void sendFrames(Bitmap bmp, int inx);
    void callback(Bitmap bmp, int inx) throws InterruptedException;
}
