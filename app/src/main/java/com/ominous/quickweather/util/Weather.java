package com.ominous.quickweather.util;

import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

public class Weather {
    private static final String weatherUriFormat = "https://api.darksky.net/forecast/%1$s/%2$f,%3$f?exclude=minutely,hourly,flags";
    private static final Map<Pair<Double,Double>,WeatherResponse> responseCache = new HashMap<>();
    private static final long CACHE_EXPIRATION = 60 * 1000; //1 minute
    private static final int MAX_DAYS = 5;

    @SuppressWarnings("ConstantConditions")
    public static void getWeather(String apiKey, double latitude, double longitude, WeatherListener weatherListener) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Pair<Double, Double> locationKey = new Pair<>(latitude,longitude);
        WeatherResponse previousWeather;

        if (responseCache.containsKey(locationKey) && now.getTimeInMillis() - (previousWeather = responseCache.get(locationKey)).currently.time * 1000 < CACHE_EXPIRATION) {
            weatherListener.onWeatherRetrieved(previousWeather);
        } else {
            new WeatherRequestTask(weatherListener, apiKey, locationKey).execute();
        }
    }

    public static class WeatherResponse {
        public double latitude;
        public double longitude;
        public WeatherObj currently;
        public DailyDataObj daily;
        public AlertsDataObj alerts;

        public static class DailyDataObj { //TODO: convert to DataObj<WeatherObj>
            public WeatherObj[] data;
        }

        public static class AlertsDataObj {
            public AlertObj[] data;
        }

        public static class AlertObj {
            public String title;
            public String severity;
            //public String description;
            public String uri;
            //public long time; //no see
            public long expires;
        }

        public static class WeatherObj {
            long time; //no see
            public String summary;
            public String icon;
            public double precipProbability;
            public double temperature;
            public double temperatureMax;
            public double temperatureMin;
            public double humidity;
            //public double pressure;
            public double windSpeed;
            public int windBearing;
        }
    }

    public interface WeatherListener {
        void onWeatherRetrieved(WeatherResponse weatherResponse);

        void onWeatherError(String error);
    }

    private static class WeatherRequestTask extends AsyncTask<Void, Void, WeatherRequestTask.WeatherRequestResult> {
        private WeatherListener weatherListener;
        private Pair<Double, Double> locationKey;
        private String apiKey;

        WeatherRequestTask(WeatherListener weatherListener, String apiKey, Pair<Double, Double> locationKey) {
            this.weatherListener = weatherListener;
            this.apiKey = apiKey;
            this.locationKey = locationKey;
        }

        @Override
        protected WeatherRequestResult doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(String.format(Locale.US,weatherUriFormat,apiKey,
                        locationKey.first,
                        locationKey.second)).openConnection();

                if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {

                    //TODO: apparently there are faster ways to do this
                    InputStream inputStream = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder stringBuilder = new StringBuilder(inputStream.available());

                    for (String line; (line = reader.readLine()) != null; ) {
                        stringBuilder.append(line).append('\n');
                    }

                    return new WeatherRequestResult(WeatherRequestResult.ResultType.SUCCESS, stringBuilder.toString());
                } else {
                    return new WeatherRequestResult(WeatherRequestResult.ResultType.FAILURE, "HTTP Error: " + conn.getResponseCode() + " " + conn.getResponseMessage());
                }
            } catch (IOException e) {
                return new WeatherRequestResult(WeatherRequestResult.ResultType.FAILURE, "IOException: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        @MainThread
        protected void onPostExecute(WeatherRequestResult result) {
            switch (result.resultType) {
                case SUCCESS:
                    try {
                        WeatherResponse response = JsonUtils.deserialize(WeatherResponse.class,new JSONObject(result.result));

                        responseCache.put(locationKey,response);

                        weatherListener.onWeatherRetrieved(response);
                    } catch (JSONException e) {
                        weatherListener.onWeatherError("JSONException: " + e.getMessage());
                    } catch (IllegalAccessException iae) {
                        //will never happen
                    } catch (InstantiationException ie) {
                        //will never happen
                    }
                    break;
                case FAILURE:
                    weatherListener.onWeatherError(result.result);
                    break;
            }
        }

        static class WeatherRequestResult {
            ResultType resultType;
            String result;

            WeatherRequestResult(ResultType resultType, String result) {
                this.resultType = resultType;
                this.result = result;
            }

            enum ResultType {
                SUCCESS,
                FAILURE
            }
        }
    }
}
