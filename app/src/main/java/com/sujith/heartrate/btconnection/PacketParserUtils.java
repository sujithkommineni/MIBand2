package com.sujith.heartrate.btconnection;

import com.sujith.heartrate.btconnection.data.BatteryData;
import com.sujith.heartrate.btconnection.data.ResponseData;
import com.sujith.heartrate.btconnection.data.WatchLogData;

import java.util.Arrays;

import timber.log.Timber;

/**
 * Created by sujit on 28-10-2017.
 */

public class PacketParserUtils {

    public static ResponseData parseBatteryPacket(byte[] data) {
        Timber.d("parseBatteryPacket().. data : %s", Arrays.toString(data));
        ResponseData ret = null;
        if ((ret = isValidPacket(data)) != null) {
            return ret;
        }

        // Actual payload location is 4th byte to n-1 bytes
        BatteryData bData = new BatteryData(data[3], data[4], true);
        return bData;
    }

    public static ResponseData parseAppVersion(byte[] data) {
        Timber.d("parseAppVersion()..data : %s", Arrays.toString(data));
        ResponseData ret = null;
        if ((ret = isValidPacket(data)) != null) {
            return ret;
        }
        int unsignedMajor = 0;
        int unsignedMinor = 0;
        if (data.length >= 5) {
            unsignedMajor = data[3] & 0xff;
            unsignedMinor = data[4] & 0xff;
        } else {
            Timber.e("parseAppVersion().. data is less than 5 bytes.");
        }
        return new ResponseData(true, unsignedMajor + "." + unsignedMinor);
    }

    public static ResponseData parseSetSessionDataResponse(byte[] data) {
        Timber.d("parseSetSessionDataResponse()..data : %s", Arrays.toString(data));
        if (data == null || data.length == 0) {
            return new ResponseData(true);
        }
        ResponseData ret = null;
        if ((ret = isValidPacket(data)) != null) {
            return ret;
        }
        return new ResponseData(true);
    }

    public static ResponseData parseStartSessionResponse(byte[] data) {
        Timber.d("parseStartSessionResponse()..data : %s", Arrays.toString(data));
        if (data == null || data.length == 0) {
            return new ResponseData(true);
        }
        ResponseData ret = null;
        if ((ret = isValidPacket(data)) != null) {
            return ret;
        }
        return new ResponseData(true);
    }

    public static ResponseData parseGetLogDataResponse(byte[] data) {
        Timber.d("parseGetLogDataResponse()..data : %s", Arrays.toString(data));
        // Log data comes in 3 packets. Packet1 - 8+4 byes, packet2 - 12+4 bytes, packet3 - 6+4 bytes.
        Timber.d("Total length: %s", data.length);

        byte[] pack1 = new byte[12];
        System.arraycopy(data, 0, pack1, 0, 12);

        byte[] pack2 = new byte[16];
        System.arraycopy(data, 12, pack2, 0, 16);

        byte[] pack3 = new byte[10];
        System.arraycopy(data, 28, pack3, 0, 10);

        ResponseData ret = null;
        if ((ret = isValidPacket(pack1)) != null) {
            return ret;
        }
        if ((ret = isValidPacket(pack2)) != null) {
            return ret;
        }
        if ((ret = isValidPacket(pack3)) != null) {
            return ret;
        }

        byte[] pack1D = new byte[8];
        System.arraycopy(pack1, 3, pack1D, 0, 8);

        byte[] pack2D = new byte[12];
        System.arraycopy(pack2, 3, pack2D, 0, 12);

        byte[] pack3D = new byte[6];
        System.arraycopy(pack3, 3, pack3D, 0, 6);

        return new WatchLogData(true, pack1D, pack2D, pack3D);
    }



    private static ResponseData isValidPacket(byte[] data) {
        Timber.d("isValidPacket()..");
        if (data == null) {
            Timber.e("data is NULL");
            return new ResponseData(false);
        }
        int len = data.length;

        if (len < 3) {
            Timber.e("packet data length is LESS than 3");
            return new ResponseData(false);
        }
        int packetLen = data[0];
        if (packetLen != len -1) {
            Timber.e("Error parsing packet.. Length doesn't match. length value %s, actual length %s", packetLen, len -1);
            return new ResponseData(false);
        }

        int seed = 0x3a;
        int xor = seed;
        for (int i = 1; i < len-1; i++) {
            xor ^= data[i];
        }
        if (xor != data[len - 1]) {
            Timber.d("Packet Checksum FAILED!!");
            return new ResponseData(false);
        }

        if (data[2] == PacketUtils.PacketTypes.NACK_TRANSFER) {
            Timber.d("NACK packet received!! " + data[3]);
            String nackMsg = getNackString(data[3]);
            Timber.e("NACK msg : " + nackMsg);
            return new ResponseData(false, nackMsg);
        }

        return null;
    }

    private static String getNackString(byte nackByte) {
        switch (nackByte) {
            case 0x01:
                return "Invalid length for command";
            case 0x02:
                return "Parameter out of range";
            case 0x03:
                return "Parameter failed to set";
            case 0x04:
                return "Access denied";
            case 0x05:
                return "Checksum failed";
        }
        return null;
    }

}
