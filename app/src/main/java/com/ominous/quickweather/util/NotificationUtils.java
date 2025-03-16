/*
 *   Copyright 2019 - 2025 Tyler Williamson
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

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.MainActivity;
import com.ominous.quickweather.data.AlertSeverity;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.data.CurrentWeather;
import com.ominous.quickweather.pref.WeatherPreferences;
import com.ominous.quickweather.view.CurrentWeatherRemoteViews;

//TODO dependency injection
public class NotificationUtils {
    private final static int PENDING_INTENT_FLAGS = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
    //Unless we're really unlucky this should work
    private final static int PERSISTENT_ID = 0;
    private final static int SUMMARY_ID = 1;
    private final static int ERROR_ID = 2;
    private final static String ALERTS_CHANNEL_ID = "weatherAlerts";
    private final static String ALERTS_GROUP_KEY = "com.ominous.quickweather.alerts_group";
    private final static String PERSISTENT_CHANNEL_ID = "persistentWeather";
    private final static String PERSISTENT_GROUP_KEY = "com.ominous.quickweather.persist_group";
    private final static String ERRORS_CHANNEL_ID = "notificationErrors";
    private final static String ERRORS_GROUP_KEY = "com.ominous.quickweather.errors_group";

    public static void updatePersistentNotification(Context context, WeatherDatabase.WeatherLocation weatherLocation, CurrentWeather currentWeather) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager != null && canShowNotifications(context)) {
            if (Build.VERSION.SDK_INT >= 26 && notificationManager.getNotificationChannel(PERSISTENT_CHANNEL_ID) == null) {
                NotificationChannel persistentChannel = new NotificationChannel(PERSISTENT_CHANNEL_ID, context.getString(R.string.channel_persistent_name), NotificationManager.IMPORTANCE_MIN);

                persistentChannel.setDescription(context.getString(R.string.channel_persistent_description));
                persistentChannel.setShowBadge(false);

                notificationManager.createNotificationChannel(persistentChannel);
            }

            WeatherUtils weatherUtils = WeatherUtils.getInstance(context);
            WeatherPreferences weatherPreferences = WeatherPreferences.getInstance(context);

            String weatherDesc = currentWeather.current.weatherDescription;

            //TODO reuse remoteViews
            CurrentWeatherRemoteViews remoteViews = new CurrentWeatherRemoteViews(context);

            remoteViews.update(weatherLocation, currentWeather);

            Notification.Builder notificationBuilder = makeNotificationBuilder(context, PERSISTENT_CHANNEL_ID, Notification.PRIORITY_MIN)
                    .setContent(remoteViews)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PENDING_INTENT_FLAGS))
                    .setOngoing(true)
                    .setShowWhen(true)
                    .setWhen(currentWeather.timestamp)
                    .setSmallIcon(currentWeather.current.weatherIconRes)
                    .setColor(context.getResources().getColor(R.color.color_app_accent))
                    .setContentTitle(weatherUtils.getTemperatureString(weatherPreferences.getTemperatureUnit(), currentWeather.current.temp, 1) + " • " + weatherDesc);

            if (Build.VERSION.SDK_INT >= 24) {
                notificationBuilder
                        .setStyle(new Notification.DecoratedCustomViewStyle())
                        .setCustomContentView(remoteViews)
                        .setGroup(PERSISTENT_GROUP_KEY);
            }

            notificationManager.notify(PERSISTENT_ID, notificationBuilder.build());
        }
    }

    public static void cancelPersistentNotification(Context context) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager != null && canShowNotifications(context)) {
            notificationManager.cancel(PERSISTENT_ID);
        }
    }

    public static void makeAlert(Context context, CurrentWeather.Alert alert) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        int alertId = alert.getId();

        if (notificationManager != null && canShowNotifications(context)) {
            if (Build.VERSION.SDK_INT >= 26 && notificationManager.getNotificationChannel(ALERTS_CHANNEL_ID) == null) {
                NotificationChannel alertsChannel = new NotificationChannel(ALERTS_CHANNEL_ID, context.getString(R.string.channel_alerts_name), NotificationManager.IMPORTANCE_HIGH);

                alertsChannel.enableLights(true);
                alertsChannel.enableVibration(true);
                alertsChannel.setLightColor(ContextCompat.getColor(context, R.color.color_yellow));
                alertsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                alertsChannel.setDescription(context.getString(R.string.channel_alerts_description));
                alertsChannel.setShowBadge(true);

                notificationManager.createNotificationChannel(alertsChannel);
            }

            WeatherDatabase weatherDatabase = WeatherDatabase.getInstance(context);

            if (!wasNotificationShown(context, alert)) {
                weatherDatabase.insertAlert(alert);

                Notification.Builder notificationBuilder = makeNotificationBuilder(context, ALERTS_CHANNEL_ID, Notification.PRIORITY_HIGH);

                AlertSeverity severity = alert.getSeverity();
                if (Build.VERSION.SDK_INT >= 24) {
                    notificationBuilder
                            .setGroup(ALERTS_GROUP_KEY)
                            .setSortKey(severity.getSortKey());
                }

                if (Build.VERSION.SDK_INT >= 26) {
                    notificationBuilder.setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY);
                }

                WeatherDatabase.WeatherLocation weatherLocation = WeatherDatabase.getInstance(context).locationDao().getSelected();

                notificationBuilder
                        .setStyle(new Notification.BigTextStyle())
                        .setContentIntent(
                                PendingIntent.getActivity(context, alertId,
                                        new Intent(context, MainActivity.class)
                                                .setAction(MainActivity.ACTION_OPENALERT)
                                                .putExtra(MainActivity.EXTRA_ALERT, alert),
                                        PENDING_INTENT_FLAGS))
                        .setOnlyAlertOnce(true)
                        .setShowWhen(true)
                        .setAutoCancel(true)
                        .setContentTitle(
                                (weatherLocation.isCurrentLocation ? context.getString(R.string.text_current_location) : weatherLocation.name) +
                                        " - " +
                                        alert.event)
                        .setContentText(alert.getPlainFormattedDescription())
                        .setSmallIcon(R.drawable.ic_error_outline_white_24dp)
                        .setColor(
                                severity == AlertSeverity.WARNING ? ContextCompat.getColor(context, R.color.color_red) :
                                        severity == AlertSeverity.WATCH ? ContextCompat.getColor(context, R.color.color_yellow) :
                                                ContextCompat.getColor(context, R.color.color_blue));

                notificationManager.notify(alertId, notificationBuilder.build());

                makeAlertSummary(context);
            }
        }
    }

    private static void makeAlertSummary(Context context) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager != null && Build.VERSION.SDK_INT >= 24) {
            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();

            Notification.Builder notificationBuilder = makeNotificationBuilder(context, ALERTS_CHANNEL_ID, Notification.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.ic_error_outline_white_24dp)
                    .setGroup(ALERTS_GROUP_KEY)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PENDING_INTENT_FLAGS));

            Notification.InboxStyle inboxStyle = new Notification.InboxStyle()
                    .setBigContentTitle(context.getResources().getQuantityString(R.plurals.format_notification_bigtitle, notifications.length))
                    .setSummaryText(context.getString(R.string.channel_alerts_name));

            String sortKey = AlertSeverity.ADVISORY.getSortKey();

            for (StatusBarNotification notification : notifications) {
                Bundle extras = notification.getNotification().extras;
                String sortKey2 = notification.getNotification().getSortKey();

                if (sortKey.compareTo(sortKey2) > 0) {
                    sortKey = sortKey2;
                }

                inboxStyle.addLine(
                        extras.getString(Notification.EXTRA_TITLE) +
                                " " +
                                extras.getString(Notification.EXTRA_TEXT));
            }

            int summaryColor;

            switch (AlertSeverity.from(sortKey, AlertSeverity.ADVISORY)) {
                case WARNING:
                    summaryColor = ContextCompat.getColor(context, R.color.color_red);
                    break;
                case WATCH:
                    summaryColor = ContextCompat.getColor(context, R.color.color_yellow);
                    break;
                default:
                    summaryColor = ContextCompat.getColor(context, R.color.color_blue);
            }

            notificationManager.notify(SUMMARY_ID, notificationBuilder
                    .setColor(summaryColor)
                    .setStyle(inboxStyle).build());
        }
    }

    public static void makeError(Context context, String title, String content) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager != null && canShowNotifications(context)) {
            if (Build.VERSION.SDK_INT >= 26 && notificationManager.getNotificationChannel(ERRORS_CHANNEL_ID) == null) {
                NotificationChannel errorsChannel = new NotificationChannel(ERRORS_CHANNEL_ID, context.getString(R.string.channel_errors_name), NotificationManager.IMPORTANCE_DEFAULT);

                errorsChannel.setDescription(context.getString(R.string.channel_errors_description));
                errorsChannel.enableLights(true);
                errorsChannel.enableVibration(true);
                errorsChannel.setLightColor(ContextCompat.getColor(context, R.color.color_yellow));
                errorsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                errorsChannel.setShowBadge(true);

                notificationManager.createNotificationChannel(errorsChannel);
            }

            Notification.Builder notificationBuilder = makeNotificationBuilder(context, ERRORS_CHANNEL_ID, Notification.PRIORITY_HIGH);

            if (Build.VERSION.SDK_INT >= 24) {
                notificationBuilder
                        .setGroup(ERRORS_GROUP_KEY);
            }

            if (Build.VERSION.SDK_INT >= 26) {
                notificationBuilder.setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY);
            }

            notificationBuilder
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PENDING_INTENT_FLAGS))
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(R.drawable.ic_error_outline_white_24dp)
                    .setColor(ContextCompat.getColor(context, R.color.color_yellow));

            notificationManager.notify(ERROR_ID, notificationBuilder.build());
        }
    }

    private static boolean wasNotificationShown(Context context, CurrentWeather.Alert alert) {
        boolean notificationExists = false;
        int alertId = alert.getId();
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (Build.VERSION.SDK_INT >= 23 && notificationManager != null) {
            for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
                if (statusBarNotification.getId() == alertId) {
                    notificationExists = true;
                }
            }
        }

        if (!notificationExists) {
            WeatherDatabase.WeatherNotification weatherNotification = WeatherDatabase.getInstance(context).notificationDao().findByHashCode(alertId);

            notificationExists = weatherNotification != null && weatherNotification.uri.equals(alert.getUri());
        }

        return notificationExists;
    }

    public static boolean canShowNotifications(Context context) {
        return Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("deprecation")
    private static Notification.Builder makeNotificationBuilder(Context context, String channelId, int priority) {
        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(context, channelId);
        } else {
            return new Notification.Builder(context).setPriority(priority);
        }
    }
}
