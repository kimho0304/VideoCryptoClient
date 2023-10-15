package usb_driver.func;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import usb_driver.driver.CommonUsbSerialPort;
import usb_driver.driver.CustomProber;
import usb_driver.driver.UsbSerialDriver;
import usb_driver.driver.UsbSerialPort;
import usb_driver.util.PrintLog;
import usb_driver.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;

public class usblib {
    Context mContext;

    public usblib(Context context, int deviceId, int portNum) {
        this.mContext = context;
        this.deviceId = deviceId;
        this.portNum = portNum;
        mainLooper = new Handler(Looper.getMainLooper());;
    }

    CommonUsbSerialPort mainDevice = new CommonUsbSerialPort(null, 0) {
        @Override
        protected void openInt(UsbDeviceConnection connection) throws IOException {

        }

        @Override
        protected void closeInt() {

        }

        @Override
        public UsbSerialDriver getDriver() {
            return null;
        }

        @Override
        public void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException {

        }

        @Override
        public EnumSet<ControlLine> getControlLines() throws IOException {
            return null;
        }

        @Override
        public EnumSet<ControlLine> getSupportedControlLines() throws IOException {
            return null;
        }
    };

    private enum UsbPermission { Unknown, Requested, Granted, Denied }

    private static final int WRITE_WAIT_MILLIS = 2000;
    private static final int READ_WAIT_MILLIS = 2000;

    private int deviceId, portNum;
    private int baudRate = 921600;

    private final Handler mainLooper;

    private SerialInputOutputManager usbIoManager;
    private UsbSerialPort usbSerialPort;
    private UsbPermission usbPermission = UsbPermission.Unknown;
    private boolean connected = false;

    public void jlibcmif_u_connect() {
        UsbDevice device = null;
        UsbSerialDriver driver = null;
        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

        for(UsbDevice v : usbManager.getDeviceList().values()) {
            device = v;
            if(device == null) {
                Log.e("hello_error", "connection failed: device not found");
                return;
            }
            driver = CustomProber.getCustomProber().probeDevice(device);
            if (driver != null)
                break;
        }

        Log.d("Port___", String.valueOf(portNum));
        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());
        if(usbConnection == null && usbPermission == UsbPermission.Unknown && !usbManager.hasPermission(driver.getDevice())) {
            usbPermission = UsbPermission.Requested;
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent("kr.taemin.android.usbserial.examples.GRANT_USB"), flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if(usbConnection == null) {
            if (!usbManager.hasPermission(driver.getDevice()))
                Log.e("hello_error","connection failed: permission denied");
            else
                Log.e("hello_error", "connection failed: open failed");
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, 8, 1, UsbSerialPort.PARITY_NONE);

            Log.i("hello_info","connected");
            connected = true;
        } catch (Exception e) {
            Log.e("hello_error", "connection failed: " + e.getMessage());
            jlibcmif_u_disconnect();
        }
    }

    public void jlibcmif_u_disconnect() {
        connected = false;
        if(usbIoManager != null) {
            usbIoManager.setListener(null);
            usbIoManager.stop();
        }
        usbIoManager = null;
        try {
            usbSerialPort.close();
        } catch (IOException ignored) {}
        usbSerialPort = null;

        Log.i("hello_info","disconnected");
    }

    //    public String receive(byte[] data) throws IOException {
//        String spn = new String();
//        spn += "receive " + data.length + " bytes\n";
//        if(data.length > 0)
//            spn += String.valueOf(usbSerialPort.read(data, 2000));
//            //spn += (HexDump.dumpHexString(data)) + ("\n");
//        }
//        return spn;
//    }
    public byte[] jlibcmif_u_receive() throws IOException {
        if (!connected) {
            // Log : Not connected
            return null;
        }

        byte[] data = new byte[8000];
        int read_len = 0;
        try {
            read_len = usbSerialPort.read(data, READ_WAIT_MILLIS);
            PrintLog.getCurrentTimeExternal();
        } catch (Exception e) {
            Log.e("hello_error_read", e.getMessage());
        }
        return Arrays.copyOf(data, read_len);
    }

    public int jlibcmif_u_send(byte[] data) {
        if (!connected) {
            // Log : Not connected
            return 1;
        }

        try {
            PrintLog.getCurrentTimeExternal();
            usbSerialPort.write(data, WRITE_WAIT_MILLIS);
        } catch (Exception e) {
            Log.e("hello_error", e.getMessage());
        }
        return 0;
    }

    // output에 결과 받아오고
    // return값은 오류 같이 전달
    public int jlibcmif_c_encrypt_bc(byte[]  output,
                                     byte[]  input, int input_len,
                                     byte[]  mac, int mac_len,
                                     byte[]  iv, int iv_len,
                                     byte[]  aad, int aad_len,
                                     byte[]  key, int key_len,
                                     int     alg_choice, int mode_choice) throws IOException {

        String bc_enc_code = "1";
        String open_braket = "(";
        String close_braket = ")";
        String comma = ",";

        String out_cmd = new String();

        out_cmd = bc_enc_code + open_braket + comma + comma;    // 1(,,

        //  mac_len
        if (mac_len != 0) {
            out_cmd += Integer.toString(mac_len);
        }
        out_cmd += comma;

        // pt + pt_len
        if (input_len != 0){
            out_cmd += byteArrayToHexString(input) + comma;
            out_cmd += Integer.toString(input_len) + comma;
        } else
            out_cmd += comma + comma;

        // iv + iv_len
        if (iv_len != 0) {
            out_cmd += byteArrayToHexString(iv) + comma;
            out_cmd += Integer.toString(iv_len) + comma;
        } else
            out_cmd += comma + comma;

        // aad + aad_len
        if (aad_len != 0) {
            out_cmd += byteArrayToHexString(aad) + comma;
            out_cmd += Integer.toString(aad_len) + comma;
        } else
            out_cmd += comma + comma;

        // Key + Key_len
        out_cmd += byteArrayToHexString(key) + comma + Integer.toString(key_len) + comma;

        // algorithm
        out_cmd += Integer.toString(alg_choice) + comma;

        // modes of operation
        out_cmd += Integer.toString(mode_choice) + close_braket;

        Log.d("hello_", "Send String : " + out_cmd);

        //long startTime = System.currentTimeMillis(); // 시간 측정 시작
        // GetTime.getCurrentTimeExternal();

        if (jlibcmif_u_send(out_cmd.getBytes()) != 0) {
            Log.e("hello_", "send() failed");
            return 1;
        }

        try {
            byte[] recv_buf = jlibcmif_u_receive();

           // long endTime = System.currentTimeMillis(); // 시간 측정 종료
           // GetTime.getCurrentTimeExternal();

            byte[] recv_data = new byte[recv_buf.length - 1];
            System.arraycopy(recv_buf, 1, recv_data, 0, recv_buf.length - 1);

            if (recv_buf[0] == 's')
            {
                recv_data = hexStringTobyteArray(recv_data);
                System.arraycopy(recv_data, 0, output, 0, input_len);
                if (mode_choice > 3)
                    System.arraycopy(recv_data, input_len, mac, 0, mac_len);

                Log.d("hello_", "CT : " + byteArrayToHexString(output));

                if (mode_choice > 3)
                    Log.d("hello_", "MAC : " + byteArrayToHexString(mac));
            }
            else if (recv_buf[0] == 'r')
            {
                recv_data = hexStringTobyteArray(recv_data);
                Log.d("hello_", "err ret : " + hexToDecimal(recv_data));
                return hexToDecimal(recv_data);
            }

        } catch (IOException e) {
            // log
            Log.e("hello_", "receive() failed");
            return 0x00000001;
        }

        return 0x00000000;
    }

    public int jlibcmif_c_decrypt_bc(byte[]  output,
                                     byte[]  input, int input_len,
                                     byte[]  mac, int mac_len,
                                     byte[]  iv, int iv_len,
                                     byte[]  aad, int aad_len,
                                     byte[]  key, int key_len,
                                     int alg_choice, int mode_choice) {
        String bc_dec_code = "2";
        String open_braket = "(";
        String close_braket = ")";
        String comma = ",";

        String out_cmd = new String();

        out_cmd = bc_dec_code + open_braket + comma;        // 2(,


        // ct + ct_len
        if (input_len != 0){
            out_cmd += byteArrayToHexString(input) + comma;
            out_cmd += Integer.toString(input_len) + comma;
        } else
            out_cmd += comma + comma;

        //  mac_len
        if (mac_len != 0) {
            out_cmd += byteArrayToHexString(mac) + comma;
            out_cmd += Integer.toString(mac_len) + comma;
        } else
            out_cmd += comma + comma;

        // iv + iv_len
        if (iv_len != 0) {
            out_cmd += byteArrayToHexString(iv) + comma;
            out_cmd += Integer.toString(iv_len) + comma;
        } else
            out_cmd += comma + comma;

        // aad + aad_len
        if (aad_len != 0) {
            out_cmd += byteArrayToHexString(aad) + comma;
            out_cmd += Integer.toString(aad_len) + comma;
        } else
            out_cmd += comma + comma;

        // Key + Key_len
        out_cmd += byteArrayToHexString(key) + comma + Integer.toString(key_len) + comma;

        // algorithm
        out_cmd += Integer.toString(alg_choice) + comma;

        // modes of operation
        out_cmd += Integer.toString(mode_choice) + close_braket;

        Log.d("hello_", "Send String : " + out_cmd);

        if (jlibcmif_u_send(out_cmd.getBytes()) != 0) {
            Log.e("hello_", "send() failed");
            return 1;
        }

        try {
            byte[] recv_buf = jlibcmif_u_receive();
            byte[] recv_data = new byte[recv_buf.length - 1];
            System.arraycopy(recv_buf, 1, recv_data, 0, recv_buf.length - 1);

            if (recv_buf[0] == 's')
            {
                recv_data = hexStringTobyteArray(recv_data);
                System.arraycopy(recv_data, 0, output, 0, input_len);
                Log.d("hello_", "PT : " + byteArrayToHexString(output));
            }
            else if (recv_buf[0] == 'r')
            {
                recv_data = hexStringTobyteArray(recv_data);
                Log.d("hello_", "err ret : " + hexToDecimal(recv_data));
                return hexToDecimal(recv_data);
            }

        }catch (IOException e) {
            Log.e("hello_", "receive() failed");
            return 0x00000001;
        }

        return 0x00000000;
    }

    public int jlibcmif_c_msg_digest(byte[]  digest,
                                     byte[]  msg, int msg_len,
                                     int     hash_type, int hash_size) {
        String hash_code = "3";
        String open_braket = "(";
        String close_braket = ")";
        String comma = ",";

        String out_cmd = new String();

        out_cmd = hash_code + open_braket + comma;    // 3(,

        // msg + msg_len
        if (msg_len != 0){
            out_cmd += byteArrayToHexString(msg) + comma;
            out_cmd += Integer.toString(msg_len) + comma;
        } else
            out_cmd += comma + comma;

        // hash algorithm
        out_cmd += Integer.toString(hash_type) + comma;

        // hash size
        out_cmd += Integer.toString(hash_size) + close_braket;


        Log.d("hello_", "Send String : " + out_cmd);

        if (jlibcmif_u_send(out_cmd.getBytes()) != 0) {
            Log.e("hello_", "send() failed");
            return 1;
        }

        try {
            byte[] recv_buf = jlibcmif_u_receive();
            byte[] recv_data = new byte[recv_buf.length - 1];
            System.arraycopy(recv_buf, 1, recv_data, 0, recv_buf.length - 1);

            if (recv_buf[0] == 's')
            {
                recv_data = hexStringTobyteArray(recv_data);
                System.arraycopy(recv_data, 0, digest, 0, hash_size / 8);
                Log.d("hello_", "Digest : " + byteArrayToHexString(digest));
            }
            else if (recv_buf[0] == 'r')
            {
                recv_data = hexStringTobyteArray(recv_data);
                Log.d("hello_", "err ret : " + hexToDecimal(recv_data));
                return hexToDecimal(recv_data);
            }

        } catch (IOException e) {
            Log.e("hello_", "receive() failed");
            return 0x00000001;
        }

        return 0x00000000;
    }

    public int jlibcmif_c_gen_mac(byte[] mac, int mac_len,
                                  byte[] msg, int msg_len,
                                  byte[] key, int key_len,
                                  byte[] iv, int iv_len,
                                  byte[] aad, int aad_len,
                                  int    alg_choice) {

        String mac_gen_code = "4";
        String open_braket = "(";
        String close_braket = ")";
        String comma = ",";

        String out_cmd = new String();

        out_cmd = mac_gen_code + open_braket + comma;    // 4(,

        //  mac_len
        if (mac_len != 0) {
            out_cmd += Integer.toString(mac_len);
        }
        out_cmd += comma;

        // msg + msg_len
        if (msg_len != 0){
            out_cmd += byteArrayToHexString(msg) + comma;
            out_cmd += Integer.toString(msg_len) + comma;
        } else
            out_cmd += comma + comma;

        // Key + Key_len
        out_cmd += byteArrayToHexString(key) + comma + Integer.toString(key_len) + comma;

        // iv + iv_len
        if (iv_len != 0) {
            out_cmd += byteArrayToHexString(iv) + comma;
            out_cmd += Integer.toString(iv_len) + comma;
        } else
            out_cmd += comma + comma;

        // aad + aad_len
        if (aad_len != 0) {
            out_cmd += byteArrayToHexString(aad) + comma;
            out_cmd += Integer.toString(aad_len) + comma;
        } else
            out_cmd += comma + comma;

        // algorithm
        out_cmd += Integer.toString(alg_choice) + close_braket;

        Log.d("hello_", "Send String : " + out_cmd);

        if (jlibcmif_u_send(out_cmd.getBytes()) != 0) {
            Log.e("hello_", "send() failed");
            return 1;
        }

        try {
            byte[] recv_buf = jlibcmif_u_receive();
            byte[] recv_data = new byte[recv_buf.length - 1];
            System.arraycopy(recv_buf, 1, recv_data, 0, recv_buf.length - 1);

            if (recv_buf[0] == 's')
            {
                recv_data = hexStringTobyteArray(recv_data);
                System.arraycopy(recv_data, 0, mac, 0, mac_len);
                Log.d("hello_", "MAC : " + byteArrayToHexString(mac));
            }
            else if (recv_buf[0] == 'r')
            {
                recv_data = hexStringTobyteArray(recv_data);
                Log.d("hello_", "err ret : " + hexToDecimal(recv_data));
                return hexToDecimal(recv_data);
            }

        } catch (IOException e) {
            Log.e("hello_", "receive() failed");
            return 0x00000001;
        }

        return 0x00000000;
    }

    public int jlibcmif_c_ver_mac(byte[] msg, int msg_len,
                                  byte[] mac, int mac_len,
                                  byte[] key, int key_len,
                                  byte[] iv, int iv_len,
                                  byte[] aad, int aad_len,
                                  int    alg_choice) {
        String mac_veri_code = "5";
        String open_braket = "(";
        String close_braket = ")";
        String comma = ",";
        String success_msg = "Verify valid";


        String out_cmd = new String();

        out_cmd = mac_veri_code + open_braket;    // 4(


        // msg + msg_len
        if (msg_len != 0){
            out_cmd += byteArrayToHexString(msg) + comma;
            out_cmd += Integer.toString(msg_len) + comma;
        } else
            out_cmd += comma + comma;

        //  mac_len
        if (mac_len != 0) {
            out_cmd += byteArrayToHexString(mac) + comma;
            out_cmd += Integer.toString(mac_len) + comma;
        } else
            out_cmd += comma + comma;

        // Key + Key_len
        out_cmd += byteArrayToHexString(key) + comma + Integer.toString(key_len) + comma;

        // iv + iv_len
        if (iv_len != 0) {
            out_cmd += byteArrayToHexString(iv) + comma;
            out_cmd += Integer.toString(iv_len) + comma;
        } else
            out_cmd += comma + comma;

        // aad + aad_len
        if (aad_len != 0) {
            out_cmd += byteArrayToHexString(aad) + comma;
            out_cmd += Integer.toString(aad_len) + comma;
        } else
            out_cmd += comma + comma;

        // algorithm
        out_cmd += Integer.toString(alg_choice) + close_braket;


        Log.d("hello_", "Send String : " + out_cmd);

        if (jlibcmif_u_send(out_cmd.getBytes()) != 0) {
            Log.e("hello_", "send() failed");
            return 0x00000001;
        }

        try {
            byte[] recv_buf = jlibcmif_u_receive();
            byte[] recv_data = new byte[recv_buf.length - 1];
            System.arraycopy(recv_buf, 1, recv_data, 0, recv_buf.length - 1);

            if (recv_buf[0] == 's')
            {
                String ret = new String(recv_data, StandardCharsets.UTF_8);
                Log.d("hello_", "Ret : " + ret);
                boolean isEqual = success_msg.equals(ret);
                if (isEqual != true) {
                    Log.d("hello_", "Verify result : " + "Verify invalid");
                    return 0x00203021;
                }
            }
            else if (recv_buf[0] == 'r')
            {
                recv_data = hexStringTobyteArray(recv_data);
                Log.d("hello_", "err ret : " + hexToDecimal(recv_data));
                return hexToDecimal(recv_data);
            }

        } catch (IOException e) {
            Log.e("hello_", "receive() failed");
            return 0x00000003;
        }

        Log.d("hello_", "Verify result : " + success_msg);
        return 0x00000000;
    }

    public static String byteArrayToHexString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder(byteArray.length * 2);
        for (byte b : byteArray) {
            sb.append(String.format("%02X", b & 0xFF)); // 0xFF 마스크를 사용하여 강제로 2자리로 표현
        }
        return sb.toString();
    }

    // input    : A[0] = '5', A[1] = 'F', ..
    // output   : B[0] = 0x5F , ...
    public static byte[] hexStringTobyteArray(byte[] byteArray) {
        int len = byteArray.length;
        byte[] hexArray = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            byte highNibble = (byte) Character.digit(byteArray[i], 16);
            byte lowNibble = (byte) Character.digit(byteArray[i + 1], 16);
            byte hexValue = (byte) ((highNibble << 4) | lowNibble);
            hexArray[i / 2] = hexValue;
        }

        return hexArray;
    }

    public int hexToDecimal(byte[] hex) {
        int result = 0;

        for (int i = 0; i < 4; i++) {
            result |= (hex[i] & 0xff) << (24 - 8 * i);
        }
        return result;
    }

    public boolean getConnectStatus() {
        return connected;
    }
}

