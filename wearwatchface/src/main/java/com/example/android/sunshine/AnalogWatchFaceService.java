package com.example.android.sunshine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class AnalogWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = AnalogWatchFaceService.class.getSimpleName();
    private static final String WEATHER_INFO_PATH = "/weather_info";
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private static final String WEATHER_IMAGE_ID_KEY = "weatherImageId";
    private static final String DATE_STRING_KEY = "dateString";
    private static final String WEATHER_HIGH_TEMPERATURE = "highTemperature";
    private static final String WEATHER_LOW_TEMPERATURE = "lowTemperature";
    public static final String WEATHER_INFO_CAPABILITIES = "weather_info";
    private String weatherInfoNodeId;
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private boolean mAmbient;
    private static final int MSG_UPDATE_TIME = 0;

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
        private int width;
        private int height;
        private int weatherImageId;
        private String highTemperature;
        private String lowTemperature;
        private String dateString;
        private Calendar mCalendar;
        private boolean mRegisteredTimeZoneReceiver = false;
        private int backGroundColor;
        private int whiteTextColor;
        private int dateTextColor;
        private Paint mBackGroundPaint;
        private Paint hourPaint;
        private Paint minPaint;
        private Paint datePaint;
        private Paint weatherIconPaint;
        private Paint highTempPaint;
        private Paint lowTemperaturePaint;
        private Paint horizontalBaseLinePaint;
        private TimeZone mTimeZone;
        private Bitmap mWeatherIconBitmap;

        public Engine() {
            super();
        }


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            if (mAmbient) {
                hourPaint.setAntiAlias(false);
                datePaint.setAntiAlias(false);
                highTempPaint.setAntiAlias(false);
                lowTemperaturePaint.setAntiAlias(false);
            }
            invalidate();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            mCalendar = Calendar.getInstance();
            mTimeZone = TimeZone.getDefault();
            mCalendar.setTimeZone(mTimeZone);
            mGoogleApiClient = new GoogleApiClient.Builder(AnalogWatchFaceService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
            initComponents();

        }

        private void initComponents() {
            mBackGroundPaint = new Paint();
            backGroundColor = getResources().getColor(R.color.colorPrimaryDark);
            whiteTextColor = Color.WHITE;
            dateTextColor = Color.GRAY;

            hourPaint = new Paint();
            hourPaint.setColor(whiteTextColor);
            hourPaint.setTypeface(Typeface.SANS_SERIF);
            hourPaint.setTextSize(getResources().getDimension(R.dimen.time_text_size));

            datePaint = new Paint();
            datePaint.setColor(whiteTextColor);
            datePaint.setTypeface(Typeface.SANS_SERIF);
            datePaint.setTextSize(getResources().getDimension(R.dimen.date_text_size));

            horizontalBaseLinePaint = new Paint();
            horizontalBaseLinePaint.setColor(whiteTextColor);

            weatherIconPaint = new Paint();
            highTempPaint = new Paint();
            highTempPaint.setColor(whiteTextColor);
            highTempPaint.setTypeface(Typeface.SANS_SERIF);
            highTempPaint.setTextSize(getResources().getDimension(R.dimen.date_text_size));


            lowTemperaturePaint = new Paint();
            lowTemperaturePaint.setColor(whiteTextColor);
            lowTemperaturePaint.setTypeface(Typeface.SANS_SERIF);
            lowTemperaturePaint.setTextSize(getResources().getDimension(R.dimen.date_text_size));
            highTemperature = "00" + getResources().getString(R.string.celcius);
            lowTemperature = "00" + getResources().getString(R.string.celcius);
        }

        private void drawBackGround(Canvas canvas) {
            mBackGroundPaint.setColor(backGroundColor);
            mBackGroundPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, width, height, mBackGroundPaint);
        }


        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }
            updateTimer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            width = bounds.width();
            height = bounds.height();


            drawBackGround(canvas);
            drawTime(canvas);


        }

        private void drawTime(Canvas canvas) {
            String time = String.format("%02d", mCalendar.get(Calendar.HOUR_OF_DAY)) + ":" +
                    String.format("%02d", mCalendar.get(Calendar.MINUTE));
            int centerX = width / 2;
            int centerY = height / 2;
            int xPos = centerX / 2;
            int yPos = centerY / 2;
            int timeYPos = yPos + 20;
            int dateYPos = timeYPos + 50;
            canvas.drawText(time, xPos, timeYPos, hourPaint);
            String dayDate = getDate();
            canvas.drawText(dayDate, xPos - 20, dateYPos, datePaint);
            canvas.drawLine(centerX - 20, centerY + 40, centerX + 40, centerY + 40, horizontalBaseLinePaint);
            canvas.drawText(highTemperature, centerX - 30, centerY + 35 + 55, datePaint);
            canvas.drawText(lowTemperature, centerX - 30 + 40, centerY + 35 + 55, datePaint);
            if (null != mWeatherIconBitmap) {
                canvas.drawBitmap(mWeatherIconBitmap, centerX - 30 - mWeatherIconBitmap.getWidth(), centerY + 50, weatherIconPaint);
            }
        }

        private String getDate() {
            Date date = mCalendar.getTime();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E, MMM d yyyy");
            String dateString = simpleDateFormat.format(date).toUpperCase();
            return dateString;
        }


        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, new DataApi.DataListener() {
                @Override
                public void onDataChanged(DataEventBuffer dataEventBuffer) {
                    Log.v(TAG, "dataReceived 2");
                    for (DataEvent dataEvent : dataEventBuffer) {
                         if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                        DataItem dataItem = dataEvent.getDataItem();
                        if (dataItem.getUri().getPath().compareTo("/weatherdata") == 0) {
                            DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                            retrieveData(dataMap);
                        }
                         }
                    }
                }
            });

            new WeatherInfoMessageTask().execute();


        }

        private void retrieveData(DataMap dataMap) {
            weatherImageId = dataMap.getInt(WEATHER_IMAGE_ID_KEY);
            dateString = dataMap.getString(DATE_STRING_KEY);
            highTemperature = dataMap.getString(WEATHER_HIGH_TEMPERATURE);
            lowTemperature = dataMap.getString(WEATHER_LOW_TEMPERATURE);
            Log.v(TAG, "weatherImageId" + weatherImageId);
            Log.v(TAG, "dateString" + dateString);
            Log.v(TAG, "highTemperature" + highTemperature);
            Log.v(TAG, "lowTemperature" + lowTemperature);

            int weatherIconID = Utils.getSmallArtResourceIdForWeatherCondition(weatherImageId);
            mWeatherIconBitmap = BitmapFactory.decodeResource(getResources(), weatherIconID);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

        private void setUpWeathInfoProvider() {
            Log.v(TAG, "setUpWeathInfoProvider...");
            CapabilityApi.GetCapabilityResult result = Wearable.CapabilityApi.getCapability(mGoogleApiClient, WEATHER_INFO_CAPABILITIES, CapabilityApi.FILTER_REACHABLE).await();
            updateWeatherInfoCapability(result.getCapability());
            requestWeatherInfo();
        }

        private class WeatherInfoMessageTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... params) {
                setUpWeathInfoProvider();
                return null;
            }
        }

        private void updateWeatherInfoCapability(CapabilityInfo capabilityInfo) {
            Set<Node> connectedNodes = capabilityInfo.getNodes();
            weatherInfoNodeId = pickBestNodeId(connectedNodes);
        }

        private String pickBestNodeId(Set<Node> connectedNodes) {
            String bestNodeId = null;
            for (Node node : connectedNodes) {
                if (node.isNearby()) {
                    return node.getId();
                }
                bestNodeId = node.getId();

            }
            return bestNodeId;
        }

        private void requestWeatherInfo() {
            byte[] data = new byte[0];
            if (null != weatherInfoNodeId) {
                Wearable.MessageApi.sendMessage(mGoogleApiClient, weatherInfoNodeId, WEATHER_INFO_PATH, data).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                        if (sendMessageResult.getStatus().isSuccess()) {
                            Log.v(TAG, "Successfully sent");
                        } else {
                            Log.v(TAG, "failed..");
                        }
                    }
                });
            }
        }

        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            AnalogWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            AnalogWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }


    }


}
