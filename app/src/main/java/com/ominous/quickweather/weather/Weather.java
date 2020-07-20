package com.ominous.quickweather.weather;

import android.content.Context;
import android.util.Pair;

import com.ominous.tylerutils.http.HttpRequest;
import com.ominous.tylerutils.http.HttpException;
import com.ominous.quickweather.util.LocaleUtils;
import com.ominous.quickweather.util.WeatherPreferences;
import com.ominous.quickweather.util.WeatherUtils;
import com.ominous.quickweather.work.WeatherTask;
import com.ominous.tylerutils.util.JsonUtils;
import com.ominous.tylerutils.work.ParallelThreadManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class Weather {
    private static final String weatherUriFormatDS = "https://api.darksky.net/forecast/%1$s/%2$f,%3$f?exclude=minutely,flags";
    private static final String weatherUriFormatOWM = "https://api.openweathermap.org/data/2.5/onecall?appid=%1$s&lat=%2$f&lon=%3$f&lang=%4$s&exclude=minutely&units=imperial";
    private static final String weatherUriFormatWGOV = "https://api.weather.gov/alerts/active?point=%1$f,%2$f";
    private static final Map<Pair<Double, Double>, WeatherResponse> responseCache = new HashMap<>();
    private static final long CACHE_EXPIRATION = 60 * 1000; //1 minute
    private static final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);

    private static WeakReference<Context> context;

    public static void initialize(Context context) {
        Weather.context = new WeakReference<>(context);
    }

    public static void getWeatherAsync(String provider, String apiKey, double latitude, double longitude, final WeatherListener weatherListener) {
        new WeatherTask(context.get(), weatherListener, provider, apiKey, new Pair<>(latitude, longitude)).execute();
    }

    private static class RawWeatherResponses {
        WGOVWeatherResponse wgovWeatherResponse;
        OWMWeatherResponse oWMWeatherResponse;
        Throwable t;
    }

    public static WeatherResponse getWeather(String provider, String apiKey, Pair<Double, Double> locationKey) throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        WeatherResponse newWeather = null, previousWeather;
        Pair<Double, Double> newLocationKey = new Pair<>(
                BigDecimal.valueOf(locationKey.first).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue(),
                BigDecimal.valueOf(locationKey.second).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue()
        );

        if (responseCache.containsKey(newLocationKey) && now.getTimeInMillis() - (previousWeather = responseCache.get(newLocationKey)).currently.time * 1000 < CACHE_EXPIRATION) {
            return previousWeather;
        } else if (provider.equals(WeatherPreferences.PROVIDER_DS)) {
            newWeather = getDSWeather(String.format(Locale.US, weatherUriFormatDS, apiKey, newLocationKey.first, newLocationKey.second));
        } else if (provider.equals(WeatherPreferences.PROVIDER_OWM)) {
            RawWeatherResponses rawWeatherResponses = new RawWeatherResponses();

            try {
                ParallelThreadManager.execute(() -> {
                    try {
                        rawWeatherResponses.wgovWeatherResponse = getResponse(WGOVWeatherResponse.class, String.format(Locale.US, weatherUriFormatWGOV,
                                newLocationKey.first,
                                newLocationKey.second));
                    } catch (HttpException t) {
                        if (t.getMessage() == null || !t.getMessage().contains("400 Bad Request")) {
                            rawWeatherResponses.t = t;
                        }
                    } catch (Throwable t) {
                        rawWeatherResponses.t = t;
                    }
                }, () -> {
                    try {
                        rawWeatherResponses.oWMWeatherResponse = getResponse(OWMWeatherResponse.class, String.format(Locale.US, weatherUriFormatOWM, apiKey,
                                newLocationKey.first,
                                newLocationKey.second,
                                LocaleUtils.getOWMLang(Locale.getDefault())));
                    } catch (Throwable t) {
                        rawWeatherResponses.t = t;
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (rawWeatherResponses.oWMWeatherResponse == null) {
                return null;
            } else if (rawWeatherResponses.t == null) {
                newWeather = convertResponse(
                        rawWeatherResponses.oWMWeatherResponse,
                        rawWeatherResponses.wgovWeatherResponse
                );
            } else if (rawWeatherResponses.t instanceof IOException) {
                throw (IOException) rawWeatherResponses.t;
            } else if (rawWeatherResponses.t instanceof JSONException) {
                throw (JSONException) rawWeatherResponses.t;
            } else if (rawWeatherResponses.t instanceof InstantiationException) {
                throw (InstantiationException) rawWeatherResponses.t;
            } else if (rawWeatherResponses.t instanceof IllegalAccessException) {
                throw (IllegalAccessException) rawWeatherResponses.t;
            } else if (rawWeatherResponses.t instanceof HttpException) {
                throw (HttpException) rawWeatherResponses.t;
            } else {
                throw new RuntimeException("Uncaught Exception occurred");
            }
        }

        responseCache.put(newLocationKey, newWeather);
        return newWeather;
    }

    private static <T> T getResponse(Class<T> responseClass, String url) throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        return JsonUtils.deserialize(responseClass, new JSONObject(
                new HttpRequest(url)
                        .addHeader("User-Agent", "QuickWeather - https://play.google.com/store/apps/details?id=com.ominous.quickweather")
                        .fetch()));
    }

    private static WeatherResponse getDSWeather(String url) throws IOException, JSONException, InstantiationException, IllegalAccessException, HttpException {
        return JsonUtils.deserialize(WeatherResponse.class, new JSONObject(
                new HttpRequest(url)
                        .addHeader("Accept-Encoding", "gzip")
                        .setCompression(HttpRequest.COMPRESSION_GZIP)
                        .fetch()));
    }

    //TODO: Remove DS WeatherResponse, convert everything to OWM
    private static WeatherResponse convertResponse(OWMWeatherResponse owmWeatherResponse, WGOVWeatherResponse wgovWeatherResponse) {
        WeatherResponse response = new WeatherResponse();
        double snow, rain;

        response.latitude = owmWeatherResponse.lat;
        response.longitude = owmWeatherResponse.lon;
        response.timezone = owmWeatherResponse.timezone;

        rain = owmWeatherResponse.current.rain == null ? 0 : owmWeatherResponse.current.rain.volume;
        snow = owmWeatherResponse.current.snow == null ? 0 : owmWeatherResponse.current.snow.volume;

        response.currently = new WeatherResponse.DataPoint();
        response.currently.humidity = owmWeatherResponse.current.humidity / 100.0;
        response.currently.temperature = owmWeatherResponse.current.temp;
        response.currently.uvIndex = owmWeatherResponse.current.uvi;
        response.currently.cloudCover = owmWeatherResponse.current.clouds / 100.0;
        response.currently.pressure = owmWeatherResponse.current.pressure;
        response.currently.precipIntensity = rain + snow;
        response.currently.precipType =
                snow == 0 ? WeatherResponse.DataPoint.PRECIP_RAIN :
                        rain == 0 ? WeatherResponse.DataPoint.PRECIP_SNOW :
                                WeatherResponse.DataPoint.PRECIP_MIX;
        response.currently.icon = WeatherUtils.getCodeFromOWMCode(owmWeatherResponse.current.weather[0].icon);
        response.currently.summary = owmWeatherResponse.current.weather[0].description;
        response.currently.windBearing = owmWeatherResponse.current.wind_deg + 180;
        response.currently.windSpeed = owmWeatherResponse.current.wind_speed;
        response.currently.dewPoint = owmWeatherResponse.current.dew_point;

        response.daily = new WeatherResponse.DataBlock();
        response.daily.data = new WeatherResponse.DataPoint[owmWeatherResponse.daily.length];

        for (int i = 0, l = owmWeatherResponse.daily.length; i < l; i++) {
            response.daily.data[i] = new WeatherResponse.DataPoint();
            response.daily.data[i].precipIntensity = owmWeatherResponse.daily[i].rain + owmWeatherResponse.daily[i].snow;
            response.daily.data[i].precipType =
                    owmWeatherResponse.daily[i].snow == 0 ? WeatherResponse.DataPoint.PRECIP_RAIN :
                            owmWeatherResponse.daily[i].rain == 0 ? WeatherResponse.DataPoint.PRECIP_SNOW :
                                    WeatherResponse.DataPoint.PRECIP_MIX;
            response.daily.data[i].temperatureMin = owmWeatherResponse.daily[i].temp.min;
            response.daily.data[i].temperatureMax = owmWeatherResponse.daily[i].temp.max;
            response.daily.data[i].summary = owmWeatherResponse.daily[i].weather[0].description;
            response.daily.data[i].icon = WeatherUtils.getCodeFromOWMCode(owmWeatherResponse.daily[i].weather[0].icon);
        }

        response.hourly = new WeatherResponse.DataBlock();
        response.hourly.data = new WeatherResponse.DataPoint[owmWeatherResponse.hourly.length];

        for (int i = 0, l = owmWeatherResponse.hourly.length; i < l; i++) {
            rain = owmWeatherResponse.hourly[i].rain == null ? 0 : owmWeatherResponse.hourly[i].rain.volume;
            snow = owmWeatherResponse.hourly[i].snow == null ? 0 : owmWeatherResponse.hourly[i].snow.volume;

            response.hourly.data[i] = new WeatherResponse.DataPoint();
            response.hourly.data[i].precipIntensity = rain + snow;
            response.hourly.data[i].precipType =
                    snow == 0 ? WeatherResponse.DataPoint.PRECIP_RAIN :
                            rain == 0 ? WeatherResponse.DataPoint.PRECIP_SNOW :
                                    WeatherResponse.DataPoint.PRECIP_MIX;
            response.hourly.data[i].temperature = owmWeatherResponse.hourly[i].temp;
            response.hourly.data[i].time = owmWeatherResponse.hourly[i].dt;
        }

        if (wgovWeatherResponse != null) {
            response.alerts = new WeatherResponse.Alert[wgovWeatherResponse.features.length];

            for (int i = 0, l = wgovWeatherResponse.features.length; i < l; i++) {
                response.alerts[i] = new WeatherResponse.Alert();

                //English-only right now
                response.alerts[i].description = (wgovWeatherResponse.features[i].properties.description +
                        " <br>* INSTRUCTIONS..." +
                        wgovWeatherResponse.features[i].properties.instruction)
                        .replaceAll("\\n\\n", "<br>")
                        .replaceAll("\\n", " ");
                response.alerts[i].uri = wgovWeatherResponse.features[i].id;
                response.alerts[i].title = wgovWeatherResponse.features[i].properties.event;
                response.alerts[i].severity =
                        wgovWeatherResponse.features[i].properties.event.toLowerCase().contains(WeatherResponse.Alert.TEXT_WARNING) ? WeatherResponse.Alert.TEXT_WARNING :
                                wgovWeatherResponse.features[i].properties.event.toLowerCase().contains(WeatherResponse.Alert.TEXT_WATCH) ? WeatherResponse.Alert.TEXT_WATCH : WeatherResponse.Alert.TEXT_ADVISORY;

                try {
                    Date expireDate = isoDateFormat.parse(
                            wgovWeatherResponse.features[i].properties.ends == null ?
                                    wgovWeatherResponse.features[i].properties.expires :
                                    wgovWeatherResponse.features[i].properties.ends);

                    if (expireDate != null) {
                        response.alerts[i].expires = expireDate.getTime() / 1000;
                    }
                } catch (Exception e) {
                    //Nothing
                }
            }
        } else {
            response.alerts = new WeatherResponse.Alert[0];
        }

        return response;
    }

    public interface WeatherListener {
        void onWeatherRetrieved(WeatherResponse weatherResponse);

        void onWeatherError(String error, Throwable throwable);
    }
}
