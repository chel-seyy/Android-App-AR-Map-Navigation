package com.arjo129.artest.indoorLocation;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class WifiService extends Service {
    public WifiService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
