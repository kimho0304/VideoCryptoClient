package usb_driver.driver;

import java.io.InterruptedIOException;

public class SerialTimeoutException extends InterruptedIOException {
    public SerialTimeoutException(String s) {
        super(s);
    }
}

