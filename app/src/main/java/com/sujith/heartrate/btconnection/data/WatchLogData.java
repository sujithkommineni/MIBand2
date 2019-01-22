package com.sujith.heartrate.btconnection.data;

import java.util.Arrays;

/**
 * Created by sujit on 30-10-2017.
 */

public class WatchLogData extends ResponseData {

    public byte[] userData, accGyro, mag;
    public WatchLogData(boolean success, byte[] userData, byte[] accGyro, byte[] mag) {
        super(success);
        this.userData = userData;
        this.accGyro = accGyro;
        this.mag = mag;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Success: " + success).append(", userData : ").append(Arrays.toString(userData))
                .append(", accGyro: ").append(Arrays.toString(accGyro))
                .append(", mag: ").append(Arrays.toString(mag));
        return sb.toString();
    }
}
