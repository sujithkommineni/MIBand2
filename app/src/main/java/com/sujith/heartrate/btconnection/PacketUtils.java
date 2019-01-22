package com.sujith.heartrate.btconnection;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import timber.log.Timber;

/**
 * Created by sujit on 28-10-2017.
 */

public class PacketUtils {

    public static class Commands {
        public static byte GET_VERSION = 0x30;
        public static byte SET_TRAINING_SESSION = 0x32;
        public static byte GET_LOG_DATA = 0x34;
        public static byte GET_BATTERY = 0x36;
        public static byte SET_SESSION_DATA = 0x38;
    }

    public static class ResponseCommands {
        public static byte GET_VERSION = 0x31;
        public static byte SET_TRAINING_SESSION = 0x33;
        public static byte GET_LOG_DATA = 0x35;
        public static byte GET_BATTERY = 0x37;
    }

    public static class PacketTypes {
        public static byte COMMAND = 0x43;
        public static byte DATA_TRANSFER = 0x44;
        public static byte NACK_TRANSFER = 0x4E;
    }

    public static byte[] batteryStatusPacket() {
        byte[] battery = {0x03, Commands.GET_BATTERY, PacketTypes.COMMAND, 0x4F};
        return battery;
    }

    public static byte[] versionQueryPacket() {
        byte[] version = {0x03, Commands.GET_VERSION, PacketTypes.COMMAND, 0x00};
        fillCRC(version);
        return version;
    }

    /**
     *
     * @param date
     * @param sessionId - should be 0 - 7
     * @param swimmerId - max 2 bytes.
     * @return
     */
    public static byte[] setSessionData(Date date, int sessionId, int swimmerId) {
        Calendar now = Calendar.getInstance();
        now.setTime(date);
        int year = now.get(Calendar.YEAR);
        year %= 100;
        int month = now.get(Calendar.MONTH) + 1;
        int day = now.get(Calendar.DAY_OF_MONTH);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        Timber.d("setSessionData() %s, %s, %s, %s, %s", year, month, day, hour, minute);
        // size of swimmer ID is 2 bytes, session id is 1 byte (max 8 sessions)
        BigInteger integer = BigInteger.valueOf(swimmerId);
        byte[] swimmerIdBytes = integer.toByteArray();
        if (swimmerIdBytes.length == 1) {
            byte temp = swimmerIdBytes[0];
            swimmerIdBytes = new byte[2];
            swimmerIdBytes[0] = 0;
            swimmerIdBytes[1] = temp;
        }
        byte[] sessionData = {0xB, Commands.SET_SESSION_DATA, PacketTypes.COMMAND, (byte)day,
                (byte)month, (byte)year, (byte)hour, (byte)minute, (byte)sessionId,
                 swimmerIdBytes[1], swimmerIdBytes[0], 0x00};
        fillCRC(sessionData);
        Timber.d("setSessionData, data : %s", Arrays.toString(sessionData));
        return sessionData;
    }

    public static byte[] startTrainingSession(boolean start) {
        byte startStop = start ? (byte)1 : (byte)0;
        byte[] startSession = {0x04, Commands.SET_TRAINING_SESSION, PacketTypes.COMMAND, startStop, 0x00};
        fillCRC(startSession);
        return startSession;
    }

    /**
     *  session id should be 0 - 7
      */
    public static byte[] getLogData(int sessionId) {
        byte[] startSession = {0x04, Commands.GET_LOG_DATA, PacketTypes.COMMAND, (byte)sessionId, 0x00};
        fillCRC(startSession);
        return startSession;
    }

    /**
     * Discounts first byte & fills CRC value in the last byte of this array
     * @param data
     * @return
     */
    public static void fillCRC(byte[] data) {
        byte seed = 0x3a;
        for (int i = 1; i < data.length -1; i++) {
            seed ^= data[i];
        }
        data[data.length-1] = seed;
    }



}
