/*
 *     Copyright 2019 - 2021 Tyler Williamson
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

package com.ominous.quickweather.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;

import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.MainActivity;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.data.WeatherResponseOneCall;
import com.ominous.quickweather.receiver.WeatherReceiver;
import com.ominous.quickweather.weather.Weather;
import com.ominous.tylerutils.util.BitmapUtils;
import com.ominous.tylerutils.util.StringUtils;

import androidx.core.content.ContextCompat;

public class NotificationUtils {
    private static final String ALERTS_SORT_WARNING = "0";
    private static final String ALERTS_SORT_WATCH = "1";
    private static final String ALERTS_SORT_ADVISORY = "2";
    private static final int PENDING_INTENT_FLAGS = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
    //Unless we're really unlucky this should work
    private static final int PERSISTENT_ID = 0;
    private static final int SUMMARY_ID = 1;
    private static final int ERROR_ID = 2;
    private static final String ALERTS_CHANNEL_ID = "weatherAlerts";
    private static final String ALERTS_GROUP_KEY = "com.ominous.quickweather.alerts_group";
    private static final String PERSISTENT_CHANNEL_ID = "persistentWeather";
    private static final String PERSISTENT_GROUP_KEY = "com.ominous.quickweather.persist_group";
    private static final String ERRORS_CHANNEL_ID = "notificationErrors";
    private static final String ERRORS_GROUP_KEY = "com.ominous.quickweather.errors_group";

    public static void initialize(Context context) {

        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26 && notificationManager != null) {
            if (notificationManager.getNotificationChannel(ALERTS_CHANNEL_ID) == null) {
                NotificationChannel alertsChannel = new NotificationChannel(ALERTS_CHANNEL_ID, context.getString(R.string.channel_alerts_name), NotificationManager.IMPORTANCE_HIGH);

                alertsChannel.enableLights(true);
                alertsChannel.enableVibration(true);
                alertsChannel.setLightColor(ContextCompat.getColor(context, R.color.color_yellow));
                alertsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                alertsChannel.setDescription(context.getString(R.string.channel_alerts_description));
                alertsChannel.setShowBadge(true);

                notificationManager.createNotificationChannel(alertsChannel);
            }

            if (notificationManager.getNotificationChannel(PERSISTENT_CHANNEL_ID) == null) {
                NotificationChannel persistentChannel = new NotificationChannel(PERSISTENT_CHANNEL_ID, context.getString(R.string.channel_persistent_name), NotificationManager.IMPORTANCE_MIN);

                persistentChannel.setDescription(context.getString(R.string.channel_persistent_description));
                persistentChannel.setShowBadge(false);

                notificationManager.createNotificationChannel(persistentChannel);
            }

            if (notificationManager.getNotificationChannel(ERRORS_CHANNEL_ID) == null) {
                NotificationChannel errorsChannel = new NotificationChannel(ERRORS_CHANNEL_ID, context.getString(R.string.channel_errors_name), NotificationManager.IMPORTANCE_DEFAULT);

                errorsChannel.setDescription(context.getString(R.string.channel_errors_description));
                errorsChannel.enableLights(true);
                errorsChannel.enableVibration(true);
                errorsChannel.setLightColor(ContextCompat.getColor(context, R.color.color_yellow));
                errorsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                errorsChannel.setShowBadge(true);

                notificationManager.createNotificationChannel(errorsChannel);
            }
        }
    }

    public static ColorStateList getNotificationTextColor(Context context) {
        final TypedArray b = context.obtainStyledAttributes(android.R.style.TextAppearance_Material_Notification_Title, new int[]{android.R.attr.textColor});
        final ColorStateList textColors = b.getColorStateList(0);
        b.recycle();

        return textColors;
    }

    public static void updatePersistentNotification(Context context, WeatherDatabase.WeatherLocation weatherLocation, WeatherResponseOneCall responseOneCall) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager != null) {
            String weatherDesc = StringUtils.capitalizeEachWord(WeatherUtils.getCurrentShortWeatherDesc(responseOneCall));

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_current);

            remoteViews.setTextViewText(R.id.main_location, weatherLocation.name);
            remoteViews.setTextViewText(R.id.main_temperature, WeatherUtils.getTemperatureString(responseOneCall.current.temp, 1));
            remoteViews.setTextViewText(R.id.main_description, weatherDesc);
            remoteViews.setImageViewBitmap(R.id.main_icon,
                    BitmapUtils.drawableToBitmap(
                            ContextCompat.getDrawable(context, WeatherUtils.getIconFromCode(responseOneCall.current.weather[0].icon, responseOneCall.current.weather[0].id)),
                            getNotificationTextColor(context).getDefaultColor() | 0xFF000000));

            Notification.Builder notificationBuilder = makeNotificationBuilder(context, PERSISTENT_CHANNEL_ID, Notification.PRIORITY_MIN)
                    .setContent(remoteViews)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PENDING_INTENT_FLAGS))
                    .setOngoing(true)
                    .setShowWhen(true)
                    .setSmallIcon(WeatherUtils.getIconFromCode(responseOneCall.current.weather[0].icon, responseOneCall.current.weather[0].id))
                    .setColor(context.getResources().getColor(R.color.color_accent_emphasis))
                    .setContentTitle(WeatherUtils.getTemperatureString(responseOneCall.current.temp, 1) + " • " + weatherDesc);

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

        if (notificationManager != null) {
            notificationManager.cancel(PERSISTENT_ID);
        }
    }

    public static void makeAlert(Context context, WeatherResponseOneCall.Alert alert) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        int alertId = alert.getId();

        if (notificationManager != null) {
            WeatherDatabase weatherDatabase = WeatherDatabase.getInstance(context);

            boolean notificationExists = false;

            if (Build.VERSION.SDK_INT >= 23) {
                for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
                    if (statusBarNotification.getId() == alertId) {
                        notificationExists = true;
                    }
                }
            }

            if (!notificationExists) {
                WeatherDatabase.WeatherNotification weatherNotification = weatherDatabase.notificationDao().findByHashCode(alertId);

                notificationExists = weatherNotification != null && weatherNotification.uri.equals(alert.getUri());
            }

            if (!notificationExists) {
                Notification.Builder notificationBuilder = makeNotificationBuilder(context, ALERTS_CHANNEL_ID, Notification.PRIORITY_HIGH);

                Weather.AlertSeverity severity = alert.getSeverity();
                if (Build.VERSION.SDK_INT >= 24) {
                    notificationBuilder
                            .setGroup(ALERTS_GROUP_KEY)
                            .setSortKey(
                                    severity == Weather.AlertSeverity.WARNING ? ALERTS_SORT_WARNING :
                                            severity == Weather.AlertSeverity.WATCH ? ALERTS_SORT_WATCH :
                                                    ALERTS_SORT_ADVISORY);
                }

                if (Build.VERSION.SDK_INT >= 26) {
                    notificationBuilder.setGroupAlertBehavior(Notification.GROUP_ALERT_SUMMARY);
                }

                notificationBuilder
                        .setStyle(new Notification.BigTextStyle())
                        .setContentIntent(
                                PendingIntent.getActivity(context, alertId,
                                        new Intent(context, MainActivity.class)
                                                .setAction(MainActivity.ACTION_OPENALERT)
                                                .putExtra(MainActivity.EXTRA_ALERT, alert),
                                        PENDING_INTENT_FLAGS))
                        .setDeleteIntent(
                                PendingIntent.getBroadcast(context, alertId,
                                        new Intent(context, WeatherReceiver.class)
                                                .setAction(WeatherReceiver.ACTION_DISMISSALERT)
                                                .putExtra(WeatherReceiver.EXTRA_ALERT, alert),
                                        PENDING_INTENT_FLAGS))
                        .setOnlyAlertOnce(true)
                        .setShowWhen(true)
                        .setAutoCancel(true)
                        .setContentTitle(
                                WeatherDatabase.getInstance(context).locationDao().getSelected().name +
                                        " - " +
                                        alert.event)
                        .setContentText(alert.getPlainFormattedDescription())
                        .setSmallIcon(R.drawable.ic_error_outline_white_24dp)
                        .setColor(
                                severity == Weather.AlertSeverity.WARNING ? ContextCompat.getColor(context, R.color.color_red) :
                                        severity == Weather.AlertSeverity.WATCH ? ContextCompat.getColor(context, R.color.color_yellow) :
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
                    .setBigContentTitle(notifications.length + " Weather Alerts")
                    .setSummaryText(context.getString(R.string.channel_alerts_name));

            String sortKey = ALERTS_SORT_ADVISORY;

            for (StatusBarNotification notification : notifications) {
                Bundle extras = notification.getNotification().extras;

                if (notification.getNotification().getSortKey() != null && notification.getNotification().getSortKey().compareTo(sortKey) < 0) {
                    sortKey = notification.getNotification().getSortKey();
                }

                inboxStyle.addLine(extras.getString(Notification.EXTRA_TITLE) + " " + extras.getString(Notification.EXTRA_TEXT));
            }

            notificationManager.notify(SUMMARY_ID, notificationBuilder
                    .setColor(
                            sortKey.equals(ALERTS_SORT_WARNING) ? ContextCompat.getColor(context, R.color.color_red) :
                                    sortKey.equals(ALERTS_SORT_WATCH) ? ContextCompat.getColor(context, R.color.color_yellow) :
                                            ContextCompat.getColor(context, R.color.color_blue))
                    .setStyle(inboxStyle).build());
        }
    }

    public static void makeError(Context context, String title, String content) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager != null) {
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

    @SuppressWarnings("deprecation")
    private static Notification.Builder makeNotificationBuilder(Context context, String channelId, int priority) {
        if (Build.VERSION.SDK_INT >= 26) {
            return new Notification.Builder(context, channelId);
        } else {
            return new Notification.Builder(context).setPriority(priority);
        }
    }
}
