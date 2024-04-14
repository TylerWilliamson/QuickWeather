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

package com.ominous.quickweather.util;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SSLHelper {
    public static void addLetsEncryptRootCA(Context context) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X509");

            try (BufferedInputStream caInput = new BufferedInputStream(
                    context.getAssets().open("letsencrypt.der"))) {
                keyStore.setCertificateEntry("letsencrypt", cf.generateCertificate(caInput));
            }

            // Based on https://stackoverflow.com/a/53456027
            KeyStore defaultCAs = KeyStore.getInstance("AndroidCAStore");
            if (defaultCAs != null) {
                defaultCAs.load(null, null);

                for (Enumeration<String> keyAliases = defaultCAs.aliases(); keyAliases.hasMoreElements();) {
                    String alias = keyAliases.nextElement();
                    Certificate cert = defaultCAs.getCertificate(alias);
                    if (!keyStore.containsAlias(alias)) {
                        keyStore.setCertificateEntry(alias, cert);
                    }
                }
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (IOException | KeyManagementException | CertificateException |
                 KeyStoreException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
