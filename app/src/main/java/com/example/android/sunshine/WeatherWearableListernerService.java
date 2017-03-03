package com.example.android.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.sync.SunshineSyncUtils;
import com.example.android.sunshine.utilities.SunshineDateUtils;
import com.example.android.sunshine.utilities.SunshineWeatherUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WeatherWearableListernerService extends WearableListenerService {

    private static final String TAG = WeatherWearableListernerService.class.getSimpleName();
    private Context context;
    static GoogleApiClient mGoogleApiClient;

    static int weatherImageId;
    static String highString;
    static String dateString;
    static String lowString;
    private static final String WEATHER_IMAGE_ID_KEY = "weatherImageId";
    private static final String DATE_STRING_KEY = "dateString";
    private static final String WEATHER_HIGH_TEMPERATURE = "highTemperature";
    private static final String WEATHER_LOW_TEMPERATURE = "lowTemperature";
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.v(TAG, "onMessageReceived....");
        context = this;
        if (messageEvent.getPath().compareTo("/weather_info") == 0) {
            fetchWeatherDataForWear();
        }
    }

    private void fetchWeatherDataForWear() {
        Thread checkForEmpty = new Thread(new Runnable() {
            @Override
            public void run() {

                /* URI for every row of weather data in our weather table*/
                Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;

                /*
                 * Since this query is going to be used only as a check to see if we have any
                 * data (rather than to display data), we just need to PROJECT the ID of each
                 * row. In our queries where we display data, we need to PROJECT more columns
                 * to determine what weather details need to be displayed.
                 */
                String[] projectionColumns = {WeatherContract.WeatherEntry._ID};
                String selectionStatement = WeatherContract.WeatherEntry
                        .getSqlSelectForTodayOnwards();

                /* Here, we perform the query to check to see if we have any weather data */
                Cursor cursor = getContentResolver().query(
                        forecastQueryUri,
                        projectionColumns,
                        selectionStatement,
                        null,
                        null);
                /*
                 * A Cursor object can be null for various different reasons. A few are
                 * listed below.
                 *
                 *   1) Invalid URI
                 *   2) A certain ContentProvider's query method returns null
                 *   3) A RemoteException was thrown.
                 *
                 * Bottom line, it is generally a good idea to check if a Cursor returned
                 * from a ContentResolver is null.
                 *
                 * If the Cursor was null OR if it was empty, we need to sync immediately to
                 * be able to display data to the user.
                 */
                if (null == cursor || cursor.getCount() == 0) {
                    SunshineSyncUtils.startImmediateSync(context);
                    SunshineSyncUtils.startImmediateSync(context);
                      /* Make sure to close the Cursor to avoid memory leaks! */
                    cursor.close();
                }else{
                    sendWeatherDataToWear(context,cursor);
                }

                /* Make sure to close the Cursor to avoid memory leaks! */
                cursor.close();
            }
        });

        /* Finally, once the thread is prepared, fire it off to perform our checks. */
        checkForEmpty.start();
    }


    private static void getWeatherDataForWear(Context context,Cursor cursor){

        Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        String selection = WeatherContract.WeatherEntry.getSqlSelectForTodayOnwards();

        Cursor weatherCursor = context.getContentResolver().query(forecastQueryUri, MainActivity.MAIN_FORECAST_PROJECTION,selection,null,sortOrder);
        long dateInMillis=0;
        double highInCelsius=0;
        double lowInCelsius=0;
        if(null!=weatherCursor && weatherCursor.moveToFirst()) {
            weatherImageId = weatherCursor.getInt(MainActivity.INDEX_WEATHER_CONDITION_ID);
            dateInMillis = weatherCursor.getLong(MainActivity.INDEX_WEATHER_DATE);
            highInCelsius = weatherCursor.getDouble(MainActivity.INDEX_WEATHER_MAX_TEMP);
            lowInCelsius = weatherCursor.getDouble(MainActivity.INDEX_WEATHER_MIN_TEMP);
        }

        /* Get human readable string using our utility method */
        dateString = SunshineDateUtils.getFriendlyDateString(context, dateInMillis, false);

        /*
          * If the user's preference for weather is fahrenheit, formatTemperature will convert
          * the temperature. This method will also append either °C or °F to the temperature
          * String.
          */
        highString = SunshineWeatherUtils.formatTemperature(context, highInCelsius);

        /*
          * If the user's preference for weather is fahrenheit, formatTemperature will convert
          * the temperature. This method will also append either °C or °F to the temperature
          * String.
          */
        lowString = SunshineWeatherUtils.formatTemperature(context, lowInCelsius);

        cursor.close();
    }

    private static void sendWeatherDataToWear(final Context context,final Cursor cursor) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        getWeatherDataForWear(context,cursor);
                        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/weatherdata");
                        DataMap dataMap= putDataMapReq.getDataMap();
                        dataMap.putInt(WEATHER_IMAGE_ID_KEY,weatherImageId);
                        dataMap.putString(DATE_STRING_KEY,dateString);
                        dataMap.putString(WEATHER_HIGH_TEMPERATURE,highString);
                        dataMap.putString(WEATHER_LOW_TEMPERATURE,lowString);
                        dataMap.putLong("timestamp",System.currentTimeMillis());
                        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                        putDataReq.setUrgent();
                        PendingResult<DataApi.DataItemResult> pendingResult =
                                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }
}
