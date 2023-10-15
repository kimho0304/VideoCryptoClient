package usb_driver.driver;

/**
 * add devices here, that are not known to DefaultProber
 *
 * if the App should auto start for these devices, also
 * add IDs to app/src/main/res/xml/device_filter.xml
 */
public class CustomProber {
    public static UsbSerialProber getCustomProber() {
        ProbeTable customTable = new ProbeTable();
        //customTable.addProduct(0x16d0, 0x087e, CdcAcmSerialDriver.class); // e.g. Digispark CDC
        customTable.addProduct(0x04D8, 0x000A, CdcAcmSerialDriver.class); // e.g. SAM BOARD
        return new UsbSerialProber(customTable);
    }
}
