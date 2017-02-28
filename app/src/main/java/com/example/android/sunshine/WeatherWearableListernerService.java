package com.example.android.sunshine;

import android.util.Log;

import com.example.android.sunshine.sync.SunshineSyncUtils;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WeatherWearableListernerService extends WearableListenerService {

    private static final String TAG = WeatherWearableListernerService.class.getSimpleName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.v(TAG,"onMessageReceived....");
        if(messageEvent.getPath().compareTo("/weather_info")==0){
            SunshineSyncUtils.startImmediateSync(getApplicationContext());
        }
    }
}
