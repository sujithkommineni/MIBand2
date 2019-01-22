package com.sujith.heartrate.btconnection.data;

/**
 * Created by sujit on 29-10-2017.
 */

public class ResponseData {
    public ResponseData(boolean success, String msg) {
        this.success = success;
        this.msg = msg;
    }
    public ResponseData(boolean success) {
        this.success = success;
    }
    public boolean success;
    public String msg;
}
