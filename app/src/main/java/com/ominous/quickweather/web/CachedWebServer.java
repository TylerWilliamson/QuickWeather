/*
 *   Copyright 2019 - 2023 Tyler Williamson
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

package com.ominous.quickweather.web;

import android.util.Log;

import com.ominous.quickweather.R;
import com.ominous.quickweather.util.SnackbarHelper;
import com.ominous.tylerutils.http.HttpRequest;
import com.ominous.tylerutils.util.ApiUtils;

import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class CachedWebServer extends NanoHTTPD {
    private final static int PORT = 4234;
    private final static String STADIA_URI = "https://tiles.stadiamaps.com";
    private final static String RAINVIEWER_TILECACHE_URI = "https://tilecache.rainviewer.com";
    private final static String RAINVIEWER_API_URI = "https://api.rainviewer.com";

    private final static String STADIA_SUFFIX = "/s";
    private final static String RAINVIEWER_SUFFIX = "/r";

    private final String CACHE_SERVER_URL;

    private static CachedWebServer instance;

    private WeakReference<SnackbarHelper> snackbarHelperRef;

    public static CachedWebServer getInstance() {
        if (instance == null) {
            instance = new CachedWebServer();
        }

        return instance;
    }

    private CachedWebServer() {
        super(PORT);

        CACHE_SERVER_URL = "http://localhost:" + PORT;

        disableLogs();

        this.setHTTPHandler(session -> {
            Response response = Response.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Bad Request");

            try {
                if (session.getUri().startsWith(STADIA_SUFFIX)) {
                    String stadiaUri = STADIA_URI + session.getUri().substring(STADIA_SUFFIX.length());

                    if (stadiaUri.endsWith(".json")) {
                        response = Response.newFixedLengthResponse(Status.OK,
                                "text/json",
                                new HttpRequest(stadiaUri)
                                        .fetchAsync()
                                        .await()
                                        .replaceAll(STADIA_URI, getStadiaUrl()));
                    } else {
                        HttpURLConnection conn = (HttpURLConnection) new URL(stadiaUri).openConnection();
                        conn.setRequestMethod(session.getMethod().name());

                        addRequestHeaders(conn);

                        response = Response.newChunkedResponse(getResponseStatus(conn.getResponseCode()), conn.getHeaderField("Content-Type"), conn.getInputStream());
                        response.addHeader("content-encoding", "gzip");
                        response.addHeader("Cache-Control", "public,max-age=86400");
                    }
                } else if (session.getUri().startsWith(RAINVIEWER_SUFFIX)) {
                    String rainviewerUrl =
                            (session.getUri().endsWith(".json") ? RAINVIEWER_API_URI : RAINVIEWER_TILECACHE_URI) +
                                    session.getUri().substring(RAINVIEWER_SUFFIX.length());

                    HttpURLConnection conn = (HttpURLConnection) new URL(rainviewerUrl).openConnection();
                    conn.setRequestMethod(session.getMethod().name());

                    response = Response.newChunkedResponse(getResponseStatus(conn.getResponseCode()), conn.getHeaderField("Content-Type"), conn.getInputStream());

                    if (!rainviewerUrl.endsWith(".json")) {
                        response.addHeader("Cache-Control", "public,max-age=86400");
                    }
                }

                return response;
            } catch (Exception e) {
                if (snackbarHelperRef != null && snackbarHelperRef.get() != null) {
                    snackbarHelperRef.get().notifyError(R.string.error_radar_data, e);
                } else {
                    Log.e("CachedWebServer", "Error loading radar data", e);
                }

                return Response.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Bad Request");
            }
        });
    }

    @Override
    public void start() {
        try {
            super.start();
        } catch (BindException e) {
            //Only throws exception if port in use
            //Unless we are REALLY unlucky, we should be fine
            Log.w("CachedWebServer", "Address in use - Another instance probably already started", e);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void setSnackbarHelper(SnackbarHelper snackbarHelper) {
        this.snackbarHelperRef = new WeakReference<>(snackbarHelper);
    }

    private void addRequestHeaders(HttpURLConnection conn) {
        conn.addRequestProperty("Origin", CACHE_SERVER_URL);
        conn.addRequestProperty("Accept-Encoding", "gzip");
    }

    public String getStadiaUrl() {
        return CACHE_SERVER_URL + STADIA_SUFFIX;
    }

    public String getRainviewerUrl() {
        return CACHE_SERVER_URL + RAINVIEWER_SUFFIX;
    }

    private Status getResponseStatus(int responseCode) {
        for (Status status : Status.values()) {
            if (status.getRequestStatus() == responseCode) {
                return status;
            }
        }
        return Status.NOT_FOUND; //hehe
    }

    private void disableLogs() {
        ((Logger) ApiUtils.getPrivateField(NanoHTTPD.class, this, "LOG"))
                .setFilter(record -> false);
    }
}