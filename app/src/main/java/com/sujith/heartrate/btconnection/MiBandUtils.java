package com.sujith.heartrate.btconnection;

import java.util.UUID;

/**
 * Created by sujit on 21-12-2017.
 */

public class MiBandUtils {

    public static class HeartRate {
        public static UUID service = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
        public static UUID measurementCharacteristic = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
        public static UUID descriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        public static UUID controlCharacteristic = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");
    }

    public static byte[] getHeartRate() {
        byte[] heartRate = {21, 2, 1};
        return heartRate;
    }
}
