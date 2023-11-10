package com.example.videocrypto;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.videocrypto.util.ExtractBitmaps;
import com.example.videocrypto.util.Locker;
import com.example.videocrypto.util.SendFramesCallback;

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
import java.util.ArrayList;
import java.util.concurrent.Executor;

public class VideoCryptoActivity extends AppCompatActivity implements SendFramesCallback {
    private static final String TAG = "VideoCryptoActivity_Debug";
    public static int MAX_FRAMES = -1;
    private int portNum, deviceId, baudRate;
    private static final int REQ_CODE = 123; // startActivityForResult에 쓰일 사용자 정의 요청 코드
    private Socket serverSocket;
    private OutputStream clientOS;
    private ObjectOutputStream clientDOS;
    private final long cnt = 0;
    public Uri selectedFile;
    int totalSize = 0;
    private static Handler mainHandler;
    private boolean mShowStopLabel = false;
    private DecodingClass.PlayTask mPlayTask;
    private final Surface surface = null;
    private final boolean mSurfaceTextureReady = false;
    private String ip = null;
    private final int port = 9090;
    private File file;
    private int videoWidth, videoHeight;

    // UI variables definition ↓
    private Button send_btn, select_file_btn, connectBtn;
    private VideoView videoView;
    private MediaPlayer mediaPlayer;
    private TextView directoryText, logText;
    private EditText ipText;
    private ScrollView logScrollView;
    private Switch isLoopingSet;
    private boolean ready = false;
    private boolean isLoop = false;
    public static ArrayList<Bitmap> decodedFrames = new ArrayList<>();
    public static int curInx = 0;
    private ExtractBitmaps extractor;

    @Override
    protected void onResume() { //화면에 나타나있고 실행 중일 때
        super.onResume();
    }

    protected void onPause() { //액티비티 화면의 일부가 다른 액티비티에 가려질 때
        super.onPause();
    }

    @Override
    protected void onDestroy() { //화면에 나타나있고 실행 중일 때
        super.onDestroy();
        if (clientOS != null && clientDOS != null) {
            try {
                clientOS.close();
                clientDOS.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close OuputStreams: " + e);
                e.printStackTrace();
            }
        }
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
        videoView.setOnPreparedListener(mediaPlayer -> {
            //비디오 시작
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        });


        /*if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            Log.i(TAG, "mediaPlayer constructor set.");
            //mediaPlayer.setSurface(videoView.getHolder().getSurface());
        } else {
            mediaPlayer.reset();
        }
*/
        mediaPlayer = new MediaPlayer();
        Log.i(TAG, "mediaPlayer constructor set.");

        directoryText = findViewById(R.id.fileDirectoryText);
        logText = findViewById(R.id.logText);
        ipText = findViewById(R.id.ipText);
        isLoopingSet = findViewById(R.id.isLooping);
        isLoopingSet.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isLoop = isChecked;
        });

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
        send_btn.setOnClickListener(view -> {
            videoView.getLayoutParams().width = 720;
            videoView.getLayoutParams().height = 1280;

            Log.i(TAG, "Changed size of surface: " + videoView.getLayoutParams().width + ", " + videoView.getLayoutParams().height);

            try {
                /*Log.i(TAG, "mediaPlayer path set: " + fileDir);
                mediaPlayer.prepareAsync();
                Log.i(TAG, "prepareAsync called.");

                ready = true;
                mediaPlayer.setOnPreparedListener(mp -> {
                    mediaPlayer.setLooping(isLoop);
                    mediaPlayer.start();
                });

                mediaPlayer.setOnCompletionListener(mp -> {
                    mediaPlayer.reset();
                    //mediaPlayer.prepareAsync();
                });*/

                ThreadPool threadPool = new ThreadPool();
                extractor = new ExtractBitmaps(this);
                ExtractBitmaps.inputFile = file;
                Thread extract = null;
                try {
                    extract = new Thread(() -> {
                        try {
                            extractor.testExtractMpegFrames();
                            Log.d(TAG, "extractor called.");
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Throwable e) {
                    Log.e(TAG, "Failed to run ExtractBimap: " + e);
                    e.printStackTrace();
                }

                threadPool.execute(extract);
                // sendThread.run();
            } catch (Exception e) {
                //Log.e(TAG, "surface view error : " + e.getMessage());
                e.printStackTrace();
            }
        });
        // send_btn.setOnClickListener(view -> new ClientTask().execute());
    }

    @Override
    public void callback(Bitmap bmp, int inx) {
        Log.i(TAG + "_Callback", "callback() called.");
        new Thread(() -> {
            // Log.i(TAG, "sendFrames called: " + inx);
            try {
                //BitmapDataObject bitmapObject = new BitmapDataObject(bmp);
                //byte[] serializedBitmap = bitmapObject.compressBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] serializedBitmap = stream.toByteArray();
                bmp.recycle(); // Should be recycled status.
                Log.i(TAG, "Start the transmission: " + serializedBitmap.length);
                // printLog("Start the transmission: " + inx);

                clientDOS.writeObject(serializedBitmap);
                clientDOS.flush();
            } catch (MalformedURLException e) {
                printLog("MalformedURLException: " + e);
                Log.e(TAG, "MalformedURLException: " + e);
                e.printStackTrace();
            } catch (IOException e) {
                printLog("IOException: " + e);
                Log.e(TAG, "IOException: " + e);
                e.printStackTrace();
            }
        }).start();
    }

    private static class ThreadPool implements Executor {
        @Override
        public void execute(final Runnable command) {
            new Thread(command).start();
        }
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
        // logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
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

    //@Override
    /* public void sendFrames(Bitmap bmp, int inx) {
        new Thread(() -> {
            Log.i(TAG, "sendFrames called: " + inx);
            try {
                BitmapDataObject bitmapObject = new BitmapDataObject(bmp);
                byte[] serializedBitmap = bitmapObject.compressBitmap();
                Log.i(TAG, "Start the transmission: " + serializedBitmap.length);

                clientDOS.write(serializedBitmap);
                clientDOS.flush();
            } catch (MalformedURLException e) {
                printLog("MalformedURLException: " + e);
                Log.e(TAG, "MalformedURLException: " + e);
                e.printStackTrace();
            } catch (IOException e) {
                printLog("IOException: " + e);
                Log.e(TAG, "IOException: " + e);
                e.printStackTrace();
            }
        }).start();
    }*/

    private class ExtractFrames implements Runnable {
        @Override
        public void run() {
            ExtractBitmaps.inputFile = file;
            try {
                //extractor.testExtractMpegFrames();
            } catch (Throwable e) {
                Log.e(TAG, "Failed to run ExtractBimap: " + e);
                e.printStackTrace();
            }
        }
    }

    /* private class SendFrames implements Runnable {
        @Override
        public void run() {

            Locker locker = new Locker();
            try {
                locker.lock(getApplicationContext());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "ExtractBitmaps.decodeCountFlag has been true now.");
            int i = 0;
            while (i < ExtractBitmaps.MAX_FRAMES) {
                //Log.i(TAG, "curInx looping.");
                try {
                    Bitmap tmp = decodedFrames.get(i);
                    BitmapDataObject bitmapObject = new BitmapDataObject(tmp);
                    byte[] serializedBitmap = bitmapObject.compressBitmap();
                    Log.i(TAG, "Start the transmission: " + serializedBitmap.length);
                    //clientDOS.writeInt(serializedBitmap.length);
                    clientDOS.write(serializedBitmap);
                    clientDOS.flush();

                    //Log.i(TAG, "The end of the send: " + (++sendCnt));
                    //printLog("The end of the send: " + (++sendCnt));
                } catch (MalformedURLException e) {
                    printLog("MalformedURLException: " + e);
                    Log.e(TAG, "MalformedURLException: " + e);
                    e.printStackTrace();
                } catch (IOException e) {
                    printLog("IOException: " + e);
                    Log.e(TAG, "IOException: " + e);
                    e.printStackTrace();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                i++;
            }
        }
    } */

    // https://github.com/rantibi/MyFirstApp/blob/master/src/com/sharedpaint/serializables/SerializableBitmap.java
    public class BitmapDataObject implements Serializable {

        private final Bitmap currentImage;

        public BitmapDataObject(Bitmap bitmap) {
            currentImage = bitmap;
        }

        private byte[] compressBitmap() throws IOException {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            currentImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            currentImage.recycle();

            /*OutputStream os = null;

            boolean f = false;
            try {
                File tmpBitmap = File.createTempFile("texture", ".png");
                os = new FileOutputStream(tmpBitmap);
                f = currentImage.compress(Bitmap.CompressFormat.PNG, 100, os);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            if (f) {
                os.flush();
                os.close();
            }*/
            return byteArray;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { // startActivityForResult 실행 후, 값을 전달 받는 액티비티 함수. startActivityForResult 실행 후 파일 탐색기 액티비티에서 파일을 선택 하면, 이 선택 된 파일이 전달할 값이 됨.
        // 그러므로 onActivityResult에 이 선택 된 파일을 전달함.
        super.onActivityResult(requestCode, resultCode, data); // 생성자 호출
        Log.i(TAG, "onActivityResult: " + requestCode + ", " + resultCode + ", " + data.toString());

        if (requestCode == REQ_CODE && resultCode == Activity.RESULT_OK) { // 요청 코드가 사용자가 정의한 값이랑 동일하고, 결과가 OK 상태(정상적)인 경우:
            if (data != null) { // 전달 받은 값(파일 탐색기에서 선택한 파일)이 존재할 경우:
                selectedFile = data.getData(); // URI 객체인 selectedFile에 그 값을 할당.
                String filename = queryName(getContentResolver(), selectedFile);
                Log.i(TAG, "File name: " + filename);
                String fileDir = null;

                try {
                    file = getFile(getApplicationContext(), selectedFile);
                    fileDir = file.getAbsolutePath();
                } catch (IOException e) {
                    printLog("Failed to set the file: " + e);
                    Log.e(TAG, "Failed to set the file: " + e);
                }

                Log.i(TAG, "The size of video: " + videoWidth + ", " + videoHeight);

                directoryText.setText(fileDir); // fileName의 경로를 directoryText에 할당.
                totalSize = Integer.parseInt(getRealSizeFromUri(this, selectedFile));
                Log.i(TAG, "size: " + totalSize);

                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(fileDir);
                MAX_FRAMES = Integer.parseInt(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT));
                Log.i(TAG, "MAX_FRAMES: " + MAX_FRAMES);

                videoView.setVideoPath(fileDir);
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
