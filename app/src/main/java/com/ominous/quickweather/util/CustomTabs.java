package com.ominous.quickweather.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.core.content.ContextCompat;

import com.ominous.quickweather.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CustomTabs {
    private static CustomTabs instance = null;
    private static CustomTabsServiceConnection customTabsServiceConnection = new CustomTabsServiceConnection() {
        @Override
        public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
            instance.onClientObtained(client);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            instance.client = null;
        }
    };

    private WeakReference<Context> context;
    private CustomTabsClient client = null;
    private CustomTabsSession session = null;
    private String customTabsPackageName;
    private long lastLaunch = 0;
    private CustomTabsIntent customTabsIntent;

    private Uri[] likelyUris = new Uri[0];

    private CustomTabs(Context context) {
        try {
            this.context = new WeakReference<>(context);
            customTabsPackageName = getCustomTabsPackages(context).get(0).activityInfo.packageName;

            this.bind(context);

            this.setColor(0);
        } catch (Exception e) {
            //
        }
    }

    public static CustomTabs getInstance(Context context, Uri... likelyUris) {
        if (instance == null) {
            instance = new CustomTabs(context.getApplicationContext());
        }

        instance.addLikelyUris(likelyUris);

        return instance;
    }

    private void bind(Context context) {
        if (customTabsPackageName != null) {
            CustomTabsClient.bindCustomTabsService(context, customTabsPackageName, customTabsServiceConnection);
        }
    }

    @Override
    public void finalize() throws Throwable {
        if (customTabsServiceConnection != null) {
            context.get().unbindService(customTabsServiceConnection);
            customTabsServiceConnection = null;
        }

        super.finalize();
    }

    private void onClientObtained(CustomTabsClient client) {
        this.client = client;

        client.warmup(0);

        session = client.newSession(null);

        warmUpLikelyUris();
    }

    public void setColor(int color) {
        this.customTabsIntent = new CustomTabsIntent.Builder(session)
                .addDefaultShareMenuItem()
                .enableUrlBarHiding()
                .setCloseButtonIcon(getBackArrow(context.get(),ColorUtils.getTextColor(color)))
                .setToolbarColor(color)
                .setShowTitle(true)
                .setStartAnimations(context.get(), R.anim.slide_right_in, R.anim.slide_left_out)
                .setExitAnimations(context.get(), R.anim.slide_left_in, R.anim.slide_right_out)
                .build();
    }

    public void addLikelyUris(Uri... uris) {
        likelyUris = uris;

        if (client != null) {
            warmUpLikelyUris();
        }
    }

    private void warmUpLikelyUris() {
        if (session != null && likelyUris.length > 0) {

            List<Bundle> otherLikelyBundles = new ArrayList<>();

            for (int i = 1, l = likelyUris.length; i < l; i++) {
                Bundle likelyUrl = new Bundle();

                likelyUrl.putParcelable(CustomTabsService.KEY_URL, likelyUris[i]);

                otherLikelyBundles.add(likelyUrl);
            }
            session.mayLaunchUrl(likelyUris[0], null, otherLikelyBundles);
        }
    }

    public void launch(Context context, Uri uri) {
        long currentTime = Calendar.getInstance().getTimeInMillis();

        if (currentTime - lastLaunch > 300) { //rate limit
            lastLaunch = currentTime;

            Intent urlIntent = new Intent(Intent.ACTION_VIEW).setData(uri);

            String nativePackageName = context.getPackageManager().queryIntentActivities(urlIntent, 0).get(0).activityInfo.packageName;

            if (client == null || !customTabsPackageName.equals(nativePackageName)) {
                context.startActivity(urlIntent);
            } else {
                customTabsIntent.launchUrl(context, uri);
            }
        }
    }

    private static List<ResolveInfo> getCustomTabsPackages(Context context) {
        PackageManager pm = context.getPackageManager();
        // Get default VIEW intent handler.
        Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.darksky.net"));

        // Get all apps that can handle VIEW intents.
        List<ResolveInfo> resolvedActivityList = pm.queryIntentActivities(activityIntent, 0);
        ArrayList<ResolveInfo> packagesSupportingCustomTabs = new ArrayList<>();
        for (ResolveInfo info : resolvedActivityList) {
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
            serviceIntent.setPackage(info.activityInfo.packageName);
            // Check if this package also resolves the Custom Tabs service.
            if (pm.resolveService(serviceIntent, 0) != null) {
                packagesSupportingCustomTabs.add(info);
            }
        }
        return packagesSupportingCustomTabs;
    }

    private static Bitmap getBackArrow(Context context, int color) {
        VectorDrawable drawable = (VectorDrawable) ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_white_24dp);
        Bitmap bitmap = null;

        if (drawable != null) {
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }

        return bitmap;
    }
}
