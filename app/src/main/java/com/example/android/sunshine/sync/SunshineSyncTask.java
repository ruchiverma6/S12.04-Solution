/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;

import com.example.android.sunshine.MainActivity;
import com.example.android.sunshine.data.SunshinePreferences;
import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.NetworkUtils;
import com.example.android.sunshine.utilities.NotificationUtils;
import com.example.android.sunshine.utilities.OpenWeatherJsonUtils;
import com.example.android.sunshine.utilities.SunshineDateUtils;
import com.example.android.sunshine.utilities.SunshineWeatherUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.net.URL;

public class SunshineSyncTask implements  GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String WEATHER_IMAGE_ID_KEY = "weatherImageId";
    private static final String DATE_STRING_KEY = "dateString";
    private static final String WEATHER_HIGH_TEMPERATURE = "highTemperature";
    private static final String WEATHER_LOW_TEMPERATURE = "lowTemperature";
    static GoogleApiClient mGoogleApiClient;
    private static final String TAG = SunshineSyncTask.class.getSimpleName();
    static int weatherImageId;
    static String highString;
    static String dateString;
    static String lowString;


    /**
     * Performs the network request for updated weather, parses the JSON from that request, and
     * inserts the new weather information into our ContentProvider. Will notify the user that new
     * weather has been loaded if the user hasn't been notified of the weather within the last day
     * AND they haven't disabled notifications in the preferences screen.
     *
     * @param context Used to access utility methods and the ContentResolver
     */
    synchronized public static void syncWeather(Context context) {

        try {
            /*
             * The getUrl method will return the URL that we need to get the forecast JSON for the
             * weather. It will decide whether to create a URL based off of the latitude and
             * longitude or off of a simple location as a String.
             */
            URL weatherRequestUrl = NetworkUtils.getUrl(context);

            /* Use the URL to retrieve the JSON */
            String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);

            /* Parse the JSON into a list of weather values */
            ContentValues[] weatherValues = OpenWeatherJsonUtils
                    .getWeatherContentValuesFromJson(context, jsonWeatherResponse);

            /*
             * In cases where our JSON contained an error code, getWeatherContentValuesFromJson
             * would have returned null. We need to check for those cases here to prevent any
             * NullPointerExceptions being thrown. We also have no reason to insert fresh data if
             * there isn't any to insert.
             */
            if (weatherValues != null && weatherValues.length != 0) {
                /* Get a handle on the ContentResolver to delete and insert data */
                ContentResolver sunshineContentResolver = context.getContentResolver();

                /* Delete old weather data because we don't need to keep multiple days' data */
                sunshineContentResolver.delete(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        null,
                        null);

                /* Insert our new weather data into Sunshine's ContentProvider */
                sunshineContentResolver.bulkInsert(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        weatherValues);



                sendWeatherDataToWear(context);

                /*
                 * Finally, after we insert data into the ContentProvider, determine whether or not
                 * we should notify the user that the weather has been refreshed.
                 */
                boolean notificationsEnabled = SunshinePreferences.areNotificationsEnabled(context);

                /*
                 * If the last notification was shown was more than 1 day ago, we want to send
                 * another notification to the user that the weather has been updated. Remember,
                 * it's important that you shouldn't spam your users with notifications.
                 */
                long timeSinceLastNotification = SunshinePreferences
                        .getEllapsedTimeSinceLastNotification(context);

                boolean oneDayPassedSinceLastNotification = false;

                if (timeSinceLastNotification >= DateUtils.DAY_IN_MILLIS) {
                    oneDayPassedSinceLastNotification = true;
                }

                /*
                 * We only want to show the notification if the user wants them shown and we
                 * haven't shown a notification in the past day.
                 */
                if (notificationsEnabled && oneDayPassedSinceLastNotification) {
                    NotificationUtils.notifyUserOfNewWeather(context);
                }

            /* If the code reaches this point, we have successfully performed our sync */

            }

        } catch (Exception e) {
            /* Server probably invalid */
            e.printStackTrace();
        }
    }

    private static void sendWeatherDataToWear(final Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        getWeatherDataForWear(context);
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private static void getWeatherDataForWear(Context context){

        Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        String selection = WeatherContract.WeatherEntry.getSqlSelectForTodayOnwards();

        Cursor weatherCursor = context.getContentResolver().query(forecastQueryUri, MainActivity.MAIN_FORECAST_PROJECTION,selection,null,sortOrder);
        int weatherId=-1;
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
    }
}