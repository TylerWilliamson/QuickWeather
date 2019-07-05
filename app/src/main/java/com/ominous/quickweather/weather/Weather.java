package com.ominous.quickweather.weather;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.MainThread;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

public class Weather {
    private static final String weatherUriFormat = "https://api.darksky.net/forecast/%1$s/%2$f,%3$f?exclude=minutely,flags";
    private static final Map<Pair<Double, Double>, WeatherResponse> responseCache = new HashMap<>();
    private static final long CACHE_EXPIRATION = 60 * 1000; //1 minute

    private static WeakReference<Context> context;

    public static void initialize(Context context) {
        Weather.context = new WeakReference<>(context);
    }

    @SuppressWarnings("ConstantConditions")
    public static void getWeather(String apiKey, double latitude, double longitude, WeatherListener weatherListener) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        Pair<Double, Double> locationKey = new Pair<>(latitude, longitude);
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
        public DataPoint currently;
        public DataBlock daily;
        public DataBlock hourly;
        public Alert[] alerts;

        public static class DataBlock { //https://darksky.net/dev/docs#data-block
            public DataPoint[] data;
        }

        public static class Alert implements Serializable { //https://darksky.net/dev/docs#alerts
            public static final String TEXT_WATCH = "watch", TEXT_WARNING = "warning";

            public String title;
            public String severity;
            public String description;
            public String uri;
            //public long time; //no see
            public long expires;

            public int getId() {
                return uri.hashCode();
            }
        }

        public static class DataPoint { //https://darksky.net/dev/docs#data-point
            public long time; //no see
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

        void onWeatherError(String error, Throwable throwable);
    }

    private static class WeatherRequestTask extends AsyncTask<Void, Void, String> {
        private WeatherListener weatherListener;
        private Pair<Double, Double> locationKey;
        private String apiKey;

        private Throwable error;

        WeatherRequestTask(WeatherListener weatherListener, String apiKey, Pair<Double, Double> locationKey) {
            this.weatherListener = weatherListener;
            this.apiKey = apiKey;
            this.locationKey = locationKey;
        }

        @Override
        protected String doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(String.format(Locale.US, weatherUriFormat, apiKey,
                        locationKey.first,
                        locationKey.second)).openConnection();

                conn.addRequestProperty("Accept-Encoding","gzip");

                if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    GZIPInputStream inputStream = new GZIPInputStream(conn.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder stringBuilder = new StringBuilder(inputStream.available());

                    for (String line; (line = reader.readLine()) != null; ) {
                        stringBuilder.append(line).append('\n');
                    }

                    return stringBuilder.toString();
                } else {
                    error = new Exception(conn.getResponseMessage());
                    //TODO: More descriptive errors
                    return "HTTP Error: " + conn.getResponseCode() + " " + conn.getResponseMessage();
                }
            } catch (IOException e) {
                error = e;
                return context.get().getString(R.string.error_connecting_api);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        @MainThread
        protected void onPostExecute(String result) {
            if (error == null) {
                try {
                    WeatherResponse response = JsonUtils.deserialize(WeatherResponse.class, new JSONObject(result));

                    responseCache.put(locationKey, response);

                    weatherListener.onWeatherRetrieved(response);
                } catch (JSONException e) {
                    weatherListener.onWeatherError(context.get().getString(R.string.error_unexpected_api_result), e);
                } catch (IllegalAccessException iae) {
                    //will never happen
                } catch (InstantiationException ie) {
                    //will never happen
                }
            }else {
                weatherListener.onWeatherError(result, error);
            }
        }
    }
}
