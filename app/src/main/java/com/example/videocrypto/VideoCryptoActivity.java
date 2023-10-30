package com.example.videocrypto;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;

public class VideoCryptoActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, DecodingClass.PlayerFeedback {
    private static final String TAG = "VideoCryptoActivity_Debug";
    private static int sendCnt = 0;
    private final BroadcastReceiver permission = new com.example.videocrypto.UsbPermissionBroadcastReceiver();
    private int portNum, deviceId, baudRate;
    private static final int REQ_CODE = 123; // startActivityForResult에 쓰일 사용자 정의 요청 코드
    private final Thread transport = null;
    private Socket serverSocket;
    private OutputStream clientOS;
    private ObjectOutputStream clientDOS;
    private DataOutputStream clientDOSFlag;
    private long cnt = 0;
    public Uri selectedFile;
    long totalRead = 0;
    int totalSize = 0;
    private Handler mainHandler;
    private static final int decodeCount = 0;
    private boolean mShowStopLabel = false;
    private DecodingClass.PlayTask mPlayTask;
    private Surface surface = null;
    private boolean mSurfaceTextureReady = false;
    private String ip = null;
    private final int port = 9090;
    private File file;

    // UI variables definition ↓
    private Button send_btn, select_file_btn, connectBtn;
    private TextureView kl, videoView;
    private TextView directoryText, logText;
    private EditText ipText;
    private ScrollView logScrollView;
    private boolean ready = false;

    @Override
    protected void onResume() { //화면에 나타나있고 실행 중일 때
        super.onResume();
    }

    protected void onPause() { //액티비티 화면의 일부가 다른 액티비티에 가려질 때
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) { // 액티비티 생성될 때
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videocrypto);

        // portNum = getIntent().getIntExtra("portNum", -1); // 포트 번호 받아오기.
        // deviceId = getIntent().getIntExtra("deviceId", -1); // 디바이스 ID 받아오기.
        //baudRate = getIntent().getIntExtra("baudRate", 115200); // 보레이트 받아오기.

        // 메인 스레드를 통해 UI 작업을 위한 메인 루퍼 등록.
        mainHandler = new Handler(Looper.getMainLooper());

        logScrollView = findViewById(R.id.logScrollView);

        videoView = findViewById(R.id.videoView);
        videoView.setSurfaceTextureListener(this);

        directoryText = findViewById(R.id.fileDirectoryText);
        logText = findViewById(R.id.logText);
        ipText = findViewById(R.id.ipText);

        connectBtn = findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener(view -> {
            ip = String.valueOf(ipText.getText());
            isServerOnline().start();
        });

        select_file_btn = findViewById(R.id.selectFileBtn); // 파일 선택 버튼 등록.
        select_file_btn.setOnClickListener(view -> {
                    openFile(); //openFile() 실행.
                }
        );

        send_btn = findViewById(R.id.sendBtn); // 데이터 전송 버튼 등록.
        send_btn.setOnClickListener(view -> clickPlayStop());
        // send_btn.setOnClickListener(view -> new ClientTask().execute());
    }

    private void toastMsg(String s) {
        mainHandler.post(() -> { // 메인 Handler를 통해 메인 스레드에 "Server is offline." 내용의 Toast Message를 띄움.
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        });
    }

    private void printLog(String s) {
        mainHandler.post(() -> { // 메인 Handler를 통해 메인 스레드에 "Server is offline." 내용의 Toast Message를 띄움.
            logText.append(s + '\n');
            int delta = logScrollView.getBottom() - logScrollView.getScrollY() - logScrollView.getScrollX();
            logScrollView.smoothScrollBy(0, delta);
        });

        logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
    }

    protected Thread isServerOnline() { // isReachable 메소드를 통해, 소켓 통신 전에 서버에 접속할 수 있는지, 즉 서버가 열려 있는지를 확인하려고 생성한 함수.
        return new Thread(() -> { // 새로운 스레드 객체 생성; isReachble 메소드는 메인 스레드가 아닌 별도의 스레드에서 실행되어야 하기 때문.
            try { // 서버 ip(192.168.0.22)에 대해, isReachble 메소드를 통해 해당 서버가 열려있는지 확인. 해당 메소드의 반환값은 Boolean형임.
                if (!(InetAddress.getByName(ip).isReachable(5000))) { // 만약 서버가 닫혀 있어 접속이 불가능하면(false 반환):
                    printLog("Server is offline.");
                } else { // 만약 서버에 접속 가능하다면(true 반환):
                    printLog("Connecting to the server.");

                    try { // 앞서 정의된 ip(192.168.0.XX)와 port(9090)로 새로운 소켓 객체를 할당. new Socket 구문은 try/catch문 작성이 필수이므로 try/catch문도 추가함.
                        serverSocket = new Socket(ip, port);
                        Log.i(TAG, String.valueOf(serverSocket.isConnected()));
                        InetAddress clientHost = serverSocket.getLocalAddress();
                        int clientPort = serverSocket.getPort();

                        clientOS = serverSocket.getOutputStream();
                        clientDOS = new ObjectOutputStream(clientOS);
                        //clientDOSFlag = new DataOutputStream(serverSocket.getOutputStream());

                        printLog("Connection Success.");
                        printLog("The Client is connected: " + clientHost + ":" + clientPort + ", host: " + clientHost.getHostName());
                        Log.i(TAG, "Server is connected: " + clientHost + ":" + clientPort + ", host: " + clientHost.getHostName());

                    } catch (IOException e) {
                        printLog("Failed to allocate the serverSocket: " + e);
                        Log.e("isServerOnline", "Failed to allocate the serverSocket: " + e);
                        e.printStackTrace();
                    }
                }
            } catch (UnknownHostException e) { // 해당 호스트 정보(ip)를 찾지 못해 발생하는 예외:
                printLog("Failed to connect the server(UnknownHostException): " + e);
                Log.e(TAG, "Failed to connect the server(UnknownHostException): " + e);
                e.printStackTrace();
            } catch (IOException e) { //입출력 에러 발생 시 발생하는 예외:
                printLog("Failed to allocate the serverSocket: " + e);
                Log.e(TAG, "Failed to allocate the serverSocket: " + e);
                e.printStackTrace();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //액션바 속 메뉴 설정
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "SurfaceTexture ready (" + width + "x" + height + ")");
        mSurfaceTextureReady = true;
        //updateControls();
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        if (ready == true) {
            sendData task = new sendData();
            task.execute();
        }
    }

    private class sendData extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Bitmap tmp = videoView.getBitmap();
                BitmapDataObject bitmapObject = new BitmapDataObject(tmp);
                //Uri bitmapToURI = getImageUri(getApplicationContext(), tmp);
                //Log.i(TAG,"uri info: " + bitmapToURI.toString());

                //printLog("Start the transmission.");
               // Log.i(TAG, "Start the transmission.");

                //clientDOS.defaultWriteObject();
                byte[] serializedBitmap = bitmapObject.compressBitmap();
                Log.i(TAG, "Start the transmission: " + serializedBitmap.length);
                //clientDOS.writeInt(serializedBitmap.length);
                clientDOS.writeObject(serializedBitmap);
                clientDOS.flush();

                Log.i(TAG, "The end of the send: " + (++sendCnt));
                //printLog("The end of the send: " + (++sendCnt));
            } catch (MalformedURLException e) {
                printLog("MalformedURLException: " + e);
                Log.e(TAG, "MalformedURLException: " + e);
                e.printStackTrace();
            } catch (IOException e) {
                printLog("IOException: " + e);
                Log.e(TAG, "IOException: " + e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Boolean result){
            Log.i(TAG, "Current Async status: " + this.getStatus().toString());
        }

        /*@Override
        protected void onProgressUpdate(java.lang.Void... values) {
            super.onProgressUpdate(values);
        }*/
    }

    // https://github.com/rantibi/MyFirstApp/blob/master/src/com/sharedpaint/serializables/SerializableBitmap.java
    public class BitmapDataObject implements Serializable {

        private Bitmap currentImage;

        public BitmapDataObject(Bitmap bitmap) {
            currentImage = bitmap;
        }

        private byte[] compressBitmap() throws IOException {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            currentImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();OutputStream os = null;

            /*boolean f = false;
            try {
                File tmpBitmap = File.createTempFile("example", ".png");
                os = new FileOutputStream(tmpBitmap);
                f = currentImage.compress(Bitmap.CompressFormat.PNG, 100, os);
            }catch (NullPointerException e){
                e.printStackTrace();
            }
            if(f) {
                os.flush();
                os.close();
            }*/
            return byteArray;
        }
    }

    @Override
    public void playbackStopped() {
        Log.d(TAG, "playback stopped");
        mShowStopLabel = false;
        mPlayTask = null;
    }

    private void stopPlayback() {
        if (mPlayTask != null) {
            mPlayTask.requestStop();
        }
    }

    public void clickPlayStop() {
        if (mShowStopLabel) {
            Log.d(TAG, "stopping movie");
            stopPlayback();
            // Don't update the controls here -- let the task thread do it after the movie has
            // actually stopped.
            //mShowStopLabel = false;
            //updateControls();
        } else {
            if (mPlayTask != null) {
                Log.w(TAG, "movie already playing");
                return;
            }
            Log.d(TAG, "starting movie");
            SpeedControlCallback callback = new SpeedControlCallback();
            /*if (((CheckBox) findViewById(R.id.locked60fps_checkbox)).isChecked()) {
                // TODO: consider changing this to be "free running" mode
                callback.setFixedPlaybackRate(60);
            }*/
            SurfaceTexture st = videoView.getSurfaceTexture();
            surface = new Surface(st);
            DecodingClass player = null;
            try {
                player = new DecodingClass(
                        file, surface, callback);
            } catch (IOException ioe) {
                Log.e(TAG, "Unable to play movie", ioe);
                surface.release();
                return;
            }
            // adjustAspectRatio(player.getVideoWidth(), player.getVideoHeight());

            mPlayTask = new DecodingClass.PlayTask(player, this);

            mShowStopLabel = true;
            //updateControls();
            mPlayTask.execute();
            ready = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { // startActivityForResult 실행 후, 값을 전달 받는 액티비티 함수. startActivityForResult 실행 후 파일 탐색기 액티비티에서 파일을 선택 하면, 이 선택 된 파일이 전달할 값이 됨.
        // 그러므로 onActivityResult에 이 선택 된 파일을 전달함.
        super.onActivityResult(requestCode, resultCode, data); // 생성자 호출
        Log.i("videocrypto", "onActivityResult");

        if (requestCode == REQ_CODE && resultCode == Activity.RESULT_OK) { // 요청 코드가 사용자가 정의한 값이랑 동일하고, 결과가 OK 상태(정상적)인 경우:
            if (data != null) { // 전달 받은 값(파일 탐색기에서 선택한 파일)이 존재할 경우:
                selectedFile = data.getData(); // URI 객체인 selectedFile에 그 값을 할당.
                String filename = queryName(getContentResolver(), selectedFile);
                Log.i("videocrypto_filename", filename);
                String fileDir = "null";

                try {
                    file = getFile(getApplicationContext(), selectedFile);
                    fileDir = file.getAbsolutePath();
                } catch (IOException e) {
                    printLog("Failed to set the file: " + e);
                    Log.e(TAG, "Failed to set the file: " + e);
                }

                directoryText.setText(fileDir); // fileName의 경로를 directoryText에 할당.
                totalSize = Integer.parseInt(getRealSizeFromUri(this, selectedFile));
                Log.i(TAG, "size: " + totalSize);
            }
        }
    }

    private Uri getImageUri(Context context, Bitmap inImage) throws IOException {
        File f = File.createTempFile("tmp", ".png");
        OutputStream os = new FileOutputStream(f);
        inImage.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.close();
        //String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Title", null);
        return Uri.fromFile(f);
        //return Uri.parse(path);
    }

    public static File getFile(Context context, Uri uri) throws IOException {
        File destinationFilename = new File(context.getFilesDir().getPath() + File.separatorChar + queryName(context.getContentResolver(), uri));
        try (InputStream ins = context.getContentResolver().openInputStream(uri)) {
            createFileFromStream(ins, destinationFilename);
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
        }
        return destinationFilename;
    }

    public static void createFileFromStream(InputStream ins, File destination) {
        try (OutputStream os = new FileOutputStream(destination)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = ins.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
        }
    }

    // https://stackoverflow.com/questions/45589736/uri-file-size-is-always-0
    private String getRealSizeFromUri(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Audio.Media.SIZE};
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    protected void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); // 파일 탐색기를 여는 intent 등록
        intent.addCategory(Intent.CATEGORY_OPENABLE); //onActivityResult 내에서 사용할 ContentProvider로 URI 객체를 접근하기 위해 쓰이는 intent 등록.
        //ContetProvider는 app 사이에서 data를 공유하는 역할을 함. ContentResolver는 ContentProvider를 통해 데이터에 접근해서 값을 가져옴.
        intent.setType("*/*"); //파일 형식 지정: *은 모든 타입을 의미

        startActivityForResult(intent, REQ_CODE); //위의 intent를 통해 파일 탐색 액티비티 실행.
    }

    private static String queryName(ContentResolver resolver, Uri uri) { // https://stackoverflow.com/a/38304115
        Cursor returnCursor = resolver.query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();

        String[] result = name.split("\\."); // '.'을 기준으로 확장명 분리 ex) ".txt"
        return result[0];
    }
}
