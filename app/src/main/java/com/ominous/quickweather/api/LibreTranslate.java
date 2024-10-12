/*
 *   Copyright 2019 - 2024 Tyler Williamson
 *
 *   This file is part of QuickWeather.
 *
 *   QuickWeather is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   QuickWeather is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.quickweather.api;

import com.ominous.tylerutils.http.HttpException;
import com.ominous.tylerutils.http.HttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LibreTranslate {
    private final static String DEFAULT_INSTANCE = "https://libretranslate.com";

    private final static LibreTranslate instance = new LibreTranslate();

    private LibreTranslate() {

    }

    public static LibreTranslate getInstance() {
        return instance;
    }

    private static String getUrl(String instance, String endpoint) {
        return (instance.isEmpty() ? DEFAULT_INSTANCE : instance) + endpoint;
    }

    public String[] translate(String instance, String apiKey, String targetLanguage,
                              String... input)
            throws HttpException, IOException, JSONException {
        JSONArray translatedText = new JSONObject(new HttpRequest(getUrl(instance, "/translate"))
                .setMethod(HttpRequest.METHOD_POST)
                .setBodyFormat(HttpRequest.FORMAT_JSON)
                .addBodyParam("q", new JSONArray(input))
                .addBodyParam("source", "auto")
                .addBodyParam("target", targetLanguage)
                .addBodyParam("api_key", apiKey)
                .fetch()).getJSONArray("translatedText");

        String[] output = new String[translatedText.length()];

        for (int i = 0, l = translatedText.length(); i < l; i++) {
            output[i] = translatedText.getString(i);
        }

        return output;
    }

    public String detect(String instance, String apiKey, String input)
            throws HttpException, IOException, JSONException {
        return new JSONArray(new HttpRequest(getUrl(instance, "/detect"))
                .setMethod(HttpRequest.METHOD_POST)
                .setBodyFormat(HttpRequest.FORMAT_JSON)
                .addBodyParam("q", input)
                .addBodyParam("api_key", apiKey)
                .fetch()).getJSONObject(0).getString("language");
    }

    public String[] getSupportedLanguages(String instance, String sourceLanguage)
            throws HttpException, IOException, JSONException {
        JSONArray supportedLanguageArray = new JSONArray(new HttpRequest(getUrl(instance, "/languages"))
                .setMethod(HttpRequest.METHOD_GET)
                .fetch());

        for (int i = 0, l = supportedLanguageArray.length(); i < l; i++) {
            JSONObject jsonObject = supportedLanguageArray.getJSONObject(i);

            if (jsonObject.getString("code").equals(sourceLanguage)) {
                JSONArray languageArray = jsonObject.getJSONArray("targets");

                String[] languages = new String[languageArray.length()];

                for (int ii = 0, ll = languageArray.length(); ii < ll; ii++) {
                    languages[ii] = languageArray.getString(ii);
                }

                return languages;
            }
        }
        return new String[]{};
    }
}
