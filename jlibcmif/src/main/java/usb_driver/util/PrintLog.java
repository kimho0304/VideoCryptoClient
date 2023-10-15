package usb_driver.util;

import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import usb_driver.func.usblib;

public class PrintLog {
    private usblib board; // 프론트 액티비티에서 생성된 usblib 객체를 할당 받을 변수
    private String echoString = "P10000000000000000"; // echo 용도로 쓰일 문자열
    private String encryptString = "P20000000000000000"; //encrypt 용도로 쓰일 문자열

    private long echoRepeat = 1, encryptRepeat = 1, bothRepeat = 1, msgRepeat = 1; // echo와 encrypt, 그리고 둘 다의 경우 각각의 반복 횟수를 저장하는 변수
    private static String timeDiff; // 시작과 종료 시간의 차이를 문자열 형태로 저장할 변수
    private static String timeDiffBoth; // Echo와 Encrypt 측정 시간의 차이를 문자열 형태로 저장할 변수
    private static LocalTime startTime, endTime; // LocalTime형 시작과 종료 시간 변수
    private static String echoTimeString, encryptTimeString;
    //private static long startNano, endNano;
    private static int i = 0, b = 0;

    public PrintLog(usblib board) { // 생성자로서 프론트 액티비티에서 생성된 usblib 객체 가져오기.
        this.board = board;
    }

    /*public static String byteArrayToHexString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder(byteArray.length * 2);
        for (byte b : byteArray) {
            sb.append(String.format("%02X", b & 0xFF)); // 0xFF 마스크를 사용하여 강제로 2자리로 표현
        }
        return sb.toString();
    }

    // Hex string -> byte array (Considered odd length)
    private static byte[] string_to_bytes(String text) {
        int len = text.length();

        if (len % 2 != 0) {
            text += text.substring(len - 1);
            text = text.substring(0, len - 1) + "0" + text.substring(len - 1);
            len++;
        }
        len = len / 2;

        byte[] byte_arr = new byte[len];

        for (int i = 0; i < len; i++) {
            String byte_string = text.substring(i * 2, i * 2 + 2);
            byte byteValue = (byte) Integer.parseInt(byte_string, 16);
            byte_arr[i] = byteValue;
        }
        return byte_arr;
    }

    public String encrypt() {
        byte[] _pt, _ct, _mac, _iv, _aad, _key;

        int _pt_len, _mac_len, _iv_len, _aad_len, _key_len, _algo, _mode;

        String macLenText = "", ivText = "00000000000000000000000000000000", aadText = "", keyText = "00000000000000000000000000000000", alg;
        int ret = 0;
        _pt = string_to_bytes(this.data);
        _pt_len = _pt.length;
        _iv = new byte[128];
        _aad = new byte[128];
        _key = new byte[32];


        if (macLenText.equals(""))
            _mac_len = 0;
        else
            _mac_len = Integer.parseInt(macLenText);


        if (ivText.equals("")) {
            Arrays.fill(_iv, (byte) 0);
            _iv_len = 0;
        } else {
            _iv = string_to_bytes(ivText);
            _iv_len = _iv.length;
        }

        if (aadText.equals("")) {
            Arrays.fill(_aad, (byte) 0);
            _aad_len = 0;
        } else {
            _aad = string_to_bytes(aadText);
            _aad_len = _aad.length;
        }

        if (keyText.equals("")) {
            Arrays.fill(_key, (byte) 0);
            _key_len = 0;
        } else {
            _key = string_to_bytes(keyText.toString());
            _key_len = _key.length;
        }

        //_algo = algorithmToindex.getOrDefault(algorithmSpinner.getSelectedItem().toString(), 0);
        //_mode = modesToindex.getOrDefault(modesSpinner.getSelectedItem().toString(), 0);

        _algo = 2;
        _mode = 2;

        _ct = new byte[_pt_len];
        _mac = new byte[_mac_len];
        try {
            ret = board.jlibcmif_c_encrypt_bc(_ct, _pt, _pt_len, _mac, _mac_len, _iv, _iv_len, _aad, _aad_len, _key, _key_len, _algo, _mode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String recvString = String.format("0x%08X", ret);
        return recvString;
    } */

    public static String getCurrentDate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 현재 연월일 날짜
            LocalDate dates = LocalDate.now();

            // 연월일 날짜 포맷 정의
            DateTimeFormatter datesFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

            // 연월일 날짜 포맷 적용
            String datesFormat = dates.format(datesFormatter);

            return datesFormat;
        }
        return null;
    }

    public static String getCurrentTime(LocalTime times) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 현재 시분초 시간
            LocalTime tmpTime = null;

            if (times == null)
                tmpTime = LocalTime.now();
            else
                tmpTime = times;

            // 시분초 시간 포맷 정의
            DateTimeFormatter timesFormatter = DateTimeFormatter.ofPattern("hh:mm:ss");

            // 시분초 시간 포맷 적용
            String timesFormat = tmpTime.format(timesFormatter);

            return timesFormat;
        }

        return null;
    }

    public static String getDifferenceTime(LocalTime startTime, LocalTime endTime) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Duration diff = null; // 시작과 종료 시간 차이 구하기
            diff = Duration.between(startTime, endTime);

            Log.d("getCurrentTimeExternal", "diff: " + diff.toString() + ", diff.toNanos: " + diff.toNanos());
            //Log.d("getCurrentTimeExternal", "System.nanoTime: " + String.valueOf(startNano) + " " + String.valueOf(endNano) + ", System.nanoTime diff: " + String.valueOf(endNano - startNano));

            String diffNanoSecStr = "";
            //String diffMilliSecStr = null;
            String diffSecStr = null;
            String diffMinStr = null;
            String diffHourStr = null;

            long diffNanoSec = diff.toNanos(); // 차이 시간의 나노초 구하기
            if ((diffNanoSec / 1000000000) < 1) { // 1초 = 10^9나노초. 차이 시간의 나노초가 10^9보다 작을 시:
                int t = 10 - 1 - String.valueOf(diffNanoSec).length(); // "1000000000"와 나노초 문자열 길이 차이 구하기
                for (int i = 0; i < t; i++) // 차이 만큼 나노초 문자열 앞에 "0" 붙이기. 이 과정을 수행하는 이유: 만약 나노초 값이 12345678이라면 예컨대 1시간:30분:0초.0012345678나노초, 즉 앞에 소숫점 0이 붙어야하기 때문.
                    diffNanoSecStr += "0";
                diffNanoSecStr += String.valueOf(diffNanoSec/1000);
            } else {
                diffNanoSecStr = String.valueOf((diffNanoSec % 1000000000)/100000);
            }

            //long diffMilliSec = diff.toMillis();
            //diffMilliSecStr =  String.valueOf(diffMilliSec);

            long diffSec = diff.getSeconds() % 60; // 60초로 나누기
            if (diffSec < 10)
                diffSecStr = "0" + String.valueOf(diffSec);
            else
                diffSecStr = String.valueOf(diffSec);

            long diffMin = diff.toMinutes() % 60; // 60분으로 나누기
            if (diffSec < 10)
                diffMinStr = "0" + String.valueOf(diffMin);
            else
                diffMinStr = String.valueOf(diffMin);

            long diffHour = diff.toHours() % 24; // 24시간으로 나누기
            if (diffSec < 10)
                diffHourStr = "0" + String.valueOf(diffHour);
            else
                diffHourStr = String.valueOf(diffHour);

            Log.d("getCurrentTimeExternal", diffHourStr + " " + diffMinStr + " " + diffSecStr + " " + diffNanoSecStr);

            return diffHourStr + ":" + diffMinStr + ":" + diffSecStr + "." + diffNanoSecStr; //[시:분:초.나노초] 포맷으로 저장

        }

        return null;
    }

    public static void getCurrentTimeExternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // SDK 버전 확인
            if (i == 0) {
                startTime = LocalTime.now();
                //startNano = System.nanoTime();
            } else {
                endTime = LocalTime.now();
                //endNano = System.nanoTime();

                Log.d("getCurrentTimeExternal", String.valueOf(startTime) + " " + String.valueOf(endTime));

                timeDiff = getDifferenceTime(startTime, endTime);
            }
        }
        i = (++i) % 2;
    }

    public String printLog(String msg) throws IOException {
        // 최종 반환할 문자열 result
        String result = "[Echo test]" + "\n";

        // Log 왼쪽에 표시할 "[연월일 날짜] :"
        String leftLog = null;

        byte[] msgToByte = msg.getBytes(); // jlibcmif_u_send의 인자 자료형이 byte[]이므로, 전달할 문자열을 byte[]로 변환.
        board.jlibcmif_u_send(msgToByte); // jlibcmif_u_send 수행.
        byte[] recvByte = board.jlibcmif_u_receive(); // jlibcmif_u_receive 수행.
        String recvString = new String(recvByte); // jlibcmif_u_send의 결괏값을 String으로 다시 변환.

        if(recvString == null){
            Log.e("jlibcmifErr", "recvString null err.");
            return null;
        }

        Log.d("printLog", "recvString: " + recvString);

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(startTime) + "] : ";
        result += leftLog + "Send - [" + msg + "]" + "\n";

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(endTime) + "] : ";
        result += leftLog + "Recv - [" + recvString + "]" + "\n";

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(null) + "] : ";
        result += leftLog + "Echo time - [" + timeDiff + "]" + "\n\n";

        echoTimeString = timeDiff;

        result += "---------" + String.valueOf(msgRepeat) + " - test --------------\n\n";

        msgRepeat++;

        return result;
    }

    public String printLog() throws IOException {
        // 최종 반환할 문자열 result
        String result = "[Echo test]" + "\n";

        // Log 왼쪽에 표시할 "[연월일 날짜] :"
        String leftLog = null;

        byte[] echoStringToByte = echoString.getBytes(); // jlibcmif_u_send의 인자 자료형이 byte[]이므로, 전달할 문자열을 byte[]로 변환
        board.jlibcmif_u_send(echoStringToByte); // jlibcmif_u_send 수행
        byte[] recvByte = board.jlibcmif_u_receive(); // jlibcmif_u_receive 수행
        String recvString = new String(recvByte); // jlibcmif_u_send의 결괏값을 String으로 다시 변환.

        if(recvString == null){
            Log.e("jlibcmifErr", "recvString null err.");
            return null;
        }

        Log.d("printLog", "recvString: " + recvString);

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(startTime) + "] : ";
        result += leftLog + "Send - [" + echoString + "]" + "\n";

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(endTime) + "] : ";
        result += leftLog + "Recv - [" + recvString + "]" + "\n";

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(null) + "] : ";
        result += leftLog + "Echo time - [" + timeDiff + "]" + "\n\n";

        echoTimeString = timeDiff;

        result += "[Encrypt test]" + "\n";

        //String recvString = encrypt();
        byte[] encryptStringToByte = encryptString.getBytes(); // jlibcmif_u_send의 인자 자료형이 byte[]이므로, 전달할 문자열을 byte[]로 변환
        board.jlibcmif_u_send(encryptStringToByte); // jlibcmif_u_send 수행
        recvByte = board.jlibcmif_u_receive(); // jlibcmif_u_receive 수행
        recvString = new String(recvByte); // jlibcmif_u_send의 결괏값을 String으로 다시 변환.

        if(recvString == null){
            Log.e("jlibcmifErr", "recvString null err.");
            return null;
        }

        Log.d("printLog", "recvString: " + recvString);

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(startTime) + "] : ";
        result += leftLog + "Send - [" + encryptString + "]" + "\n";

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(endTime) + "] : ";
        result += leftLog + "Recv - [" + recvString + "]" + "\n";

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(null) + "] : ";
        result += leftLog + "Encrypt time - [" + timeDiff + "]" + "\n\n";

        encryptTimeString = timeDiff;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalTime echoTime = LocalTime.parse(echoTimeString);
            LocalTime encryptTime = LocalTime.parse(encryptTimeString);

            timeDiffBoth = getDifferenceTime(echoTime, encryptTime);
        }

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(null) + "] : ";
        result += leftLog + "Time difference - [" + timeDiffBoth + "]" + "\n";

        result += "---------" + String.valueOf(bothRepeat) + " - test --------------\n\n";

        bothRepeat++;

        return result;
    }

    public String printLogEcho() throws IOException {
        //this.data = data;

        // 로그 첫 번째 줄: [ - yyyy.mm.dd: hh.mm.ss ]
        String logFirstLine = "[" + String.valueOf(echoRepeat) + " - ";

        // 최종 반환할 문자열 result
        String result = null;

        // Log 왼쪽에 표시할 "[연월일 날짜] :"
        String leftLog = null;

        // 문자열 병합
        logFirstLine += getCurrentDate() + ": " + getCurrentTime(null) + "]";

        result = logFirstLine + "\n";

        byte[] echoStringToByte = echoString.getBytes(); // jlibcmif_u_send의 인자 자료형이 byte[]이므로, 전달할 문자열을 byte[]로 변환
        board.jlibcmif_u_send(echoStringToByte); // jlibcmif_u_send 수행
        byte[] recvByte = board.jlibcmif_u_receive(); // jlibcmif_u_receive 수행.
        String recvString = new String(recvByte); // jlibcmif_u_send의 결괏값을 String으로 다시 변환.

        if(recvString == null){
            Log.e("jlibcmifErr", "recvString null err.");
            return null;
        }

        Log.d("printLogEcho", "recvString: " + recvString);

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(startTime) + "] : ";
        result += leftLog + "Send - [" + echoString + "]" + "\n";

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(endTime) + "] : ";
        result += leftLog + "Recv - [" + recvString + "]" + "\n";

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(null) + "] : ";
        result += leftLog + "Echo time - [" + timeDiff + "]" + "\n";

        result += "---------" + String.valueOf(echoRepeat) + " - test --------------\n\n";

        echoRepeat++;

        return result;
    }

    public String printLogEncrypt() throws IOException {
        //this.data = data;

        // 로그 첫 번째 줄: [ - yyyy.mm.dd: hh.mm.ss ]
        String logFirstLine = "[" + String.valueOf(encryptRepeat) + " - ";

        // 최종 반환할 문자열 result
        String result = null;

        // Log 왼쪽에 표시할 "[연월일 날짜] :"
        String leftLog = null;

        // 문자열 병합
        logFirstLine += getCurrentDate() + ": " + getCurrentTime(null) + "]";

        result = logFirstLine + "\n";

        //String recvString = encrypt();
        byte[] encryptStringToByte = encryptString.getBytes(); // jlibcmif_u_send의 인자 자료형이 byte[]이므로, 전달할 문자열을 byte[]로 변환
        board.jlibcmif_u_send(encryptStringToByte); // jlibcmif_u_send 수행
        byte[] recvByte = board.jlibcmif_u_receive(); // jlibcmif_u_receive 수행
        String recvString = new String(recvByte); // jlibcmif_u_send의 결괏값을 String으로 다시 변환.

        if(recvString == null){
            Log.e("jlibcmifErr", "recvString null err.");
            return null;
        }

        Log.d("printLogEncrypt", "recvString: " + recvString);

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(startTime) + "] : ";
        result += leftLog + "Send - [" + encryptString + "]" + "\n";

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(endTime) + "] : ";
        result += leftLog + "Recv - [" + recvString + "]" + "\n";

        // Log 왼쪽에 표시할 "[시분초 시간] :"
        leftLog = "[" + getCurrentTime(null) + "] : ";
        result += leftLog + "Echo time - [" + timeDiff + "]" + "\n";

        result += "---------" + String.valueOf(encryptRepeat) + " - test --------------\n\n";

        encryptRepeat++;

        return result;
    }
}
