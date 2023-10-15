package com.example.videocrypto;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class Main extends AppCompatActivity {
    // Used to load the 'application' library on application startup.
    protected final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    protected final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    private BroadcastReceiver permission = new com.example.videocrypto.UsbPermissionBroadcastReceiver();
    private PendingIntent permissionIntent;
    private int portNum, deviceId, baudRate;


    @Override
    protected void onResume() { //화면에 나타나있고 실행 중일 때
        super.onResume();

        permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(permission, filter);
    }

    protected void onPause() { //액티비티 화면의 일부가 다른 액티비티에 가려질 때
        super.onPause();
        unregisterReceiver(permission);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //액션바 속 메뉴 설정
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        if (item.getItemId() == R.id.videoCrypto) {
            Intent toVideoCrypto = new Intent(getApplicationContext(), VideoCryptoActivity.class);
            toVideoCrypto.putExtra("deviceId", deviceId);
            toVideoCrypto.putExtra("portNum", portNum);
            toVideoCrypto.putExtra("baudRate", baudRate);
            Log.d("main_activity", "Unit_test Intent");
            startActivity(toVideoCrypto);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) { //액티비티 생성될 때
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        portNum = getIntent().getIntExtra("portNum", -1);
        deviceId = getIntent().getIntExtra("deviceId", -1);
        baudRate = getIntent().getIntExtra("baudRate", 115200);

        ActionBar actionBar = getSupportActionBar();
        //actionBar.show();
    }
}