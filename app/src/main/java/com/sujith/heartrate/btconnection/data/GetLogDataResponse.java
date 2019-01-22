package com.sujith.heartrate.btconnection.data;

import java.util.List;

/**
 * Created by sujit on 04-11-2017.
 */

public class GetLogDataResponse extends ResponseData {

    public List<Byte> data;

    public GetLogDataResponse(boolean success, List<Byte> response) {
        super(success);
        data = response;
    }
}
