#
#   Copyright 2019 - 2024 Tyler Williamson
#
#   This file is part of QuickWeather.
#
#   QuickWeather is free software: you can redistribute it and/or modify
#   it under the terms of the GNU General Public License as published by
#   the Free Software Foundation, either version 3 of the License, or
#   (at your option) any later version.
#
#   QuickWeather is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#   GNU General Public License for more details.
#
#   You should have received a copy of the GNU General Public License
#   along with QuickWeather.  If not, see <https://www.gnu.org/licenses/>.
#

-dontobfuscate

-keepattributes Exceptions, InnerClasses

-keep class com.woxthebox.draglistview.* { *; }
-keep class com.mapbox.mapboxsdk.maps.AttributionDialogManager { *; }
-keep interface androidx.* { *; }
-keep class androidx.* { *; }

# Keep Annotations
-keep class com.ominous.tylerutils.annotation.*

# Inner classes get built via reflection, need to keep them
-keep class com.ominous.quickweather.api.openmeteo.OpenMeteoForecast* { *; }
-keep class com.ominous.quickweather.api.openweather.OpenWeatherOneCall* { *; }
-keep class com.ominous.quickweather.api.openweather.OpenWeatherForecast* { *; }
-keep class com.ominous.tylerutils.plugins.GithubUtils* { *; }

-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn edu.umd.cs.findbugs.annotations.NonNull
-dontwarn edu.umd.cs.findbugs.annotations.Nullable