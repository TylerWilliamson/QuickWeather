package com.ominous.quickweather.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.receiver.WeatherReceiver;
import com.ominous.quickweather.weather.Weather;
import com.ominous.quickweather.weather.Weather.WeatherResponse.Alert;
import com.ominous.quickweather.weather.WeatherLocationManager;

public class NotificationUtil {
    private static final String
            ALERTS_CHANNEL_ID = "weatherAlerts",
            PERSISTENT_CHANNEL_ID = "persistentWeather";

    public static void initialize(Context context) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26 && notificationManager != null) {
            if (notificationManager.getNotificationChannel(ALERTS_CHANNEL_ID) == null) {
                NotificationChannel alertsChannel = new NotificationChannel(ALERTS_CHANNEL_ID, context.getString(R.string.channel_alerts_name), NotificationManager.IMPORTANCE_HIGH);

                alertsChannel.enableLights(true);
                alertsChannel.enableVibration(true);
                alertsChannel.setLightColor(context.getColor(R.color.color_yellow));
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
        }
    }

    public static void updatePersistentNotification(Context context, Weather.WeatherResponse.DataPoint currently) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager != null) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),R.layout.notification_current);

            remoteViews.setTextViewText(R.id.current_temperature,WeatherUtils.getTemperatureString(currently.temperature,1));
            remoteViews.setTextViewText(R.id.current_description,currently.summary);
            remoteViews.setImageViewResource(R.id.current_icon,WeatherUtils.getIconFromCode(currently.icon));

            Notification.Builder notificationBuilder;

            if (Build.VERSION.SDK_INT >= 26) {
                notificationBuilder = new Notification.Builder(context, PERSISTENT_CHANNEL_ID);
            } else {
                notificationBuilder = new Notification.Builder(context)
                        .setPriority(Notification.PRIORITY_MIN);
            }

            notificationBuilder
                    .setContent(remoteViews)
                    .setOngoing(true)
                    .setSmallIcon(WeatherUtils.getIconFromCode(currently.icon))
                    .setColor(context.getResources().getColor(R.color.color_accent_emphasis))
                    .setContentTitle(context.getString(R.string.format_notification_title,WeatherUtils.getTemperatureString(currently.temperature,1),currently.summary));

            if (Build.VERSION.SDK_INT >= 24) {
                notificationBuilder
                        .setStyle(new Notification.DecoratedCustomViewStyle())
                        .setCustomContentView(remoteViews);
            }

            notificationManager.notify(0, notificationBuilder.build());
        }
    }

    public static void cancelPersistentNotification(Context context) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        if (notificationManager != null) {
            notificationManager.cancel(0);
        }
    }

    public static void makeAlert(Context context, Alert alert) {
        NotificationManager notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);

        int hashCode = alert.getId();

        if (notificationManager != null) {
            WeatherDatabase weatherDatabase = WeatherDatabase.getInstance(context);

            boolean notificationExists = false;

            if (Build.VERSION.SDK_INT >= 23) {
                for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
                    if (statusBarNotification.getId() == hashCode) {
                        notificationExists = true;
                    }
                }
            }

            if (!notificationExists) {
                WeatherDatabase.WeatherNotification weatherNotification = weatherDatabase.notificationDao().findByHashCode(hashCode);

                notificationExists = weatherNotification != null && weatherNotification.uri.equals(alert.uri);
            }

            if (!notificationExists) {
                Notification.Builder notificationBuilder;

                if (Build.VERSION.SDK_INT >= 26) {
                    notificationBuilder = new Notification.Builder(context, ALERTS_CHANNEL_ID);
                } else {
                    notificationBuilder = new Notification.Builder(context)
                            .setPriority(Notification.PRIORITY_HIGH);
                }

                notificationBuilder
                        .setStyle(new Notification.BigTextStyle())
                        .setContentIntent(
                                PendingIntent.getBroadcast(context, hashCode, new Intent(context, WeatherReceiver.class).setAction(WeatherReceiver.ACTION_OPENALERT).putExtra(WeatherReceiver.EXTRA_ALERT, alert), 0))
                        .setDeleteIntent(
                                PendingIntent.getBroadcast(context, hashCode, new Intent(context, WeatherReceiver.class).setAction(WeatherReceiver.ACTION_DISMISSALERT).putExtra(WeatherReceiver.EXTRA_ALERT, alert), 0))
                        .setOnlyAlertOnce(true)
                        .setShowWhen(true)
                        .setAutoCancel(true)
                        .setContentTitle(
                                context.getString(R.string.text_notification_alert_title,
                                        WeatherLocationManager.getLocationFromPreferences().location,
                                        alert.title))
                        .setContentText(alert.description)
                        .setSmallIcon(R.drawable.ic_error_outline_white_24dp)
                        .setColor(
                                alert.severity.equals(Weather.WeatherResponse.Alert.TEXT_WARNING) ?
                                        ContextCompat.getColor(context, R.color.color_red) :
                                        alert.severity.equals(Alert.TEXT_WATCH) ?
                                                ContextCompat.getColor(context, R.color.color_yellow) :
                                                ContextCompat.getColor(context, R.color.color_blue));

                notificationManager.notify(hashCode, notificationBuilder.build());
            }
        }
    }
}
