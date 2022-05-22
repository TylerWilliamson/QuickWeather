/*
 *     Copyright 2019 - 2022 Tyler Williamson
 *
 *     This file is part of QuickWeather.
 *
 *     QuickWeather is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     QuickWeather is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ominous.quickweather.web;

import android.content.Context;
import android.content.res.Resources;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class FileWebServer extends NanoHTTPD {
    private final Resources resources;

    public FileWebServer(Context context, int port) {
        super(port);

        this.resources = context.getResources();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri().substring(1);

        if (uri.equals("favicon.ico")) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
        } else {
            try {
                return newChunkedResponse(Response.Status.OK,
                        getMimeTypeForFile(uri),
                        resources.getAssets().open(uri));
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + e.getMessage());
            }
        }
    }

    @Override
    public void start() {
        try {
            super.start();
        } catch (IOException ioe) {
            //Only throws exception if port in use
            //Unless we are REALLY unlucky, we should be fine
        }
    }
}
