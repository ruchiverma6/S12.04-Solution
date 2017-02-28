package com.example.android.sunshine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.Set;

public class AnalogWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = AnalogWatchFaceService.class.getSimpleName();
    private static final String WEATHER_INFO_PATH = "/weather_info";
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private static final String WEATHER_IMAGE_ID_KEY = "weatherImageId";
    private static final String DATE_STRING_KEY = "dateString";
    private static final String WEATHER_HIGH_TEMPERATURE = "highTemperature";
    private static final String WEATHER_LOW_TEMPERATURE = "lowTemperature";
    public static final String WEATHER_INFO_CAPABILITIES="weather_info";
    private String weatherInfoNodeId;

    public AnalogWatchFaceService() {
        super();
        mContext = this;
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

        private int weatherImageId;
        private String highTemperature;
        private String lowTemperature;
        private String dateString;

        public Engine() {
            super();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mGoogleApiClient = new GoogleApiClient.Builder(AnalogWatchFaceService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {


            Wearable.DataApi.addListener(mGoogleApiClient, new DataApi.DataListener() {
                @Override
                public void onDataChanged(DataEventBuffer dataEventBuffer) {
                    Log.v(TAG, "dataReceived 2");
                    for (DataEvent dataEvent : dataEventBuffer) {
                        //= if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                        DataItem dataItem=dataEvent.getDataItem();
                        if(dataItem.getUri().getPath().compareTo("/weatherdata")==0){
                            DataMap dataMap= DataMapItem.fromDataItem(dataItem).getDataMap();
                            retrieveData(dataMap);
                        }
                        //   }
                    }
                }
            });

          new WeatherInfoMessageTask().execute();


        }

        private void retrieveData(DataMap dataMap) {
             weatherImageId=dataMap.getInt(WEATHER_IMAGE_ID_KEY);
             dateString=dataMap.getString(DATE_STRING_KEY);
             highTemperature=dataMap.getString(WEATHER_HIGH_TEMPERATURE);
             lowTemperature=dataMap.getString(WEATHER_LOW_TEMPERATURE);
            Log.v(TAG, "weatherImageId"+weatherImageId);
            Log.v(TAG, "dateString"+dateString);
            Log.v(TAG, "highTemperature"+highTemperature);
            Log.v(TAG, "lowTemperature"+lowTemperature);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

        private void setUpWeathInfoProvider(){
            Log.v(TAG,"setUpWeathInfoProvider...");
            CapabilityApi.GetCapabilityResult result=Wearable.CapabilityApi.getCapability(mGoogleApiClient,WEATHER_INFO_CAPABILITIES,CapabilityApi.FILTER_REACHABLE).await();
            updateWeatherInfoCapability(result.getCapability());
            requestWeatherInfo();
        }

        private class WeatherInfoMessageTask extends AsyncTask<Void,Void,Void>{
            @Override
            protected Void doInBackground(Void... params) {
                setUpWeathInfoProvider();
                return null;
            }
        }

        private void updateWeatherInfoCapability(CapabilityInfo capabilityInfo) {
              Set<Node> connectedNodes=capabilityInfo.getNodes();
            weatherInfoNodeId=pickBestNodeId(connectedNodes);
        }

        private String pickBestNodeId(Set<Node> connectedNodes) {
            String bestNodeId=null;
            for(Node node:connectedNodes){
                if(node.isNearby()){
                    return node.getId();
                }
                bestNodeId=node.getId();

            }
            return bestNodeId;
        }
        private void requestWeatherInfo(){
            byte[] data=new byte[0];
if(null!=weatherInfoNodeId){
    Wearable.MessageApi.sendMessage(mGoogleApiClient,weatherInfoNodeId,WEATHER_INFO_PATH,data).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
        @Override
        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
            if(sendMessageResult.getStatus().isSuccess()){
                Log.v(TAG,"Successfully sent");
            }else {
                Log.v(TAG,"failed..");
            }
        }
    });
}
        }


    }
}
