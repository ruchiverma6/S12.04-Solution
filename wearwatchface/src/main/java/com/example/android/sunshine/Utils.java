package com.example.android.sunshine;

import android.util.Log;

/**
 * Created by v-ruchd on 3/1/2017.
 */

public class Utils {

    private static final String LOG_TAG = Utils.class.getSimpleName() ;

    /**
     * Helper method to provide the art resource ID according to the weather condition ID returned
     * by the OpenWeatherMap call. This method is very similar to
     *
     *   {@link #getSmallArtResourceIdForWeatherCondition(int)}.
     *
     * The difference between these two methods is that this method provides larger assets, used
     * in the "today view" of the list, as well as in the DetailActivity.
     *
     * @param weatherId from OpenWeatherMap API response
     *                  See http://openweathermap.org/weather-conditions for a list of all IDs
     *
     * @return resource ID for the corresponding icon. -1 if no relation is found.
     */
    public static int getLargeArtResourceIdForWeatherCondition(int weatherId) {

        /*
         * Based on weather code data for Open Weather Map.
         */
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.art_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.art_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.art_rain;
        } else if (weatherId == 511) {
            return R.drawable.art_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.art_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.art_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.art_fog;
        } else if (weatherId == 761 || weatherId == 771 || weatherId == 781) {
            return R.drawable.art_storm;
        } else if (weatherId == 800) {
            return R.drawable.art_clear;
        } else if (weatherId == 801) {
            return R.drawable.art_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.art_clouds;
        } else if (weatherId >= 900 && weatherId <= 906) {
            return R.drawable.art_storm;
        } else if (weatherId >= 958 && weatherId <= 962) {
            return R.drawable.art_storm;
        } else if (weatherId >= 951 && weatherId <= 957) {
            return R.drawable.art_clear;
        }

        Log.e(LOG_TAG, "Unknown Weather: " + weatherId);
        return R.drawable.art_storm;
    }

    public static int getSmallArtResourceIdForWeatherCondition(int weatherId) {

        /*
         * Based on weather code data for Open Weather Map.
         */
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 771 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        } else if (weatherId >= 900 && weatherId <= 906) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 958 && weatherId <= 962) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 951 && weatherId <= 957) {
            return R.drawable.ic_clear;
        }

        Log.e(LOG_TAG, "Unknown Weather: " + weatherId);
        return R.drawable.ic_storm;
    }
}
