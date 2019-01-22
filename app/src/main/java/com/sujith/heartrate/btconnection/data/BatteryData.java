package com.sujith.heartrate.btconnection.data;

/**
 * Created by sujit on 28-10-2017.
 */

public class BatteryData extends ResponseData {
    public BatteryData(byte high, byte low, boolean success) {
        super(success);
        this.high = high;
        this.low = low;
    }
    public byte high;
    public byte low;
}
