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

package com.ominous.quickweather.activity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textview.MaterialTextView;
import com.ominous.quickweather.R;
import com.ominous.quickweather.card.RadarCardView;
import com.ominous.quickweather.data.WeatherDataManager;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.quickweather.data.WeatherModel;
import com.ominous.quickweather.util.ColorHelper;
import com.ominous.quickweather.util.FullscreenHelper;
import com.ominous.tylerutils.anim.OpenCloseState;
import com.ominous.tylerutils.async.Promise;
import com.ominous.tylerutils.browser.CustomTabs;
import com.ominous.tylerutils.plugins.ApkUtils;
import com.ominous.tylerutils.plugins.GithubUtils;
import com.ominous.tylerutils.util.ColorUtils;
import com.ominous.tylerutils.util.LocaleUtils;
import com.ominous.tylerutils.util.ViewUtils;
import com.ominous.tylerutils.util.WindowUtils;

import java.net.URLDecoder;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends BaseActivity {
    public final static String EXTRA_ALERT = "EXTRA_ALERT";
    public final static String ACTION_OPENALERT = "com.ominous.quickweather.ACTION_OPENALERT";
    private NavigationView navigationView;
    private RadarCardView radarCardView;
    private ActionBarDrawerToggle drawerToggle;
    private FrameLayout fullscreenContainer;

    private FullscreenHelper fullscreenHelper;

    private final OnBackPressedCallback drawerBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    };
    private final OnBackPressedCallback fullscreenBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            weatherViewModel.getFullscreenModel().postValue(OpenCloseState.CLOSING);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initActivity();
        initViewLogic();

        onReceiveIntent(getIntent());
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        drawerToggle.syncState();
    }

    private void initActivity() {
        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(drawerBackPressedCallback);
        onBackPressedDispatcher.addCallback(fullscreenBackPressedCallback);
    }

    protected void onReceiveIntent(Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_VIEW)) {
                WeatherDatabase.WeatherLocation weatherLocation;

                if ("geo".equals(intent.getScheme()) &&
                        (weatherLocation = getWeatherLocationFromGeoUri(intent.getDataString())) != null) {
                    ContextCompat.startActivity(this, new Intent(this, SettingsActivity.class)
                            .putExtra(SettingsActivity.EXTRA_WEATHERLOCATION, weatherLocation), null);
                }
            }
        }
    }

    private WeatherDatabase.WeatherLocation getWeatherLocationFromGeoUri(String geoUri) {
        Matcher matcher;

        //geo:0,0?q=37.78918,-122.40335
        matcher = Pattern.compile("geo:0,0\\?.*?q=([0-9.\\-]+),([0-9.\\-]+).*").matcher(geoUri);
        if (matcher.matches()) {
            try {
                String latStr = matcher.group(1);
                String lonStr = matcher.group(2);
                double lat = Double.parseDouble(latStr == null ? "0" : latStr);
                double lon = Double.parseDouble(lonStr == null ? "0" : lonStr);

                if (lat != 0 && lon != 0) {
                    return new WeatherDatabase.WeatherLocation(lat, lon, null);
                }
            } catch (Throwable t) {
                //Pattern did not match, should never happen
            }
        }

        //geo:0,0?q=my+street+address
        matcher = Pattern.compile("geo:0,0\\?.*?q=([^&]*).*").matcher(geoUri);
        if (matcher.matches()) {
            try {
                return new WeatherDatabase.WeatherLocation(0, 0, URLDecoder.decode(matcher.group(1), "UTF-8"));
            } catch (Throwable t) {
                //
            }
        }

        //geo:12.34,56.78
        matcher = Pattern.compile("geo:([0-9.\\-]+),([0-9.\\-]+)(\\?.*)?").matcher(geoUri);

        if (matcher.matches()) {
            try {
                double lat = LocaleUtils.parseDouble(Locale.getDefault(), matcher.group(1));
                double lon = LocaleUtils.parseDouble(Locale.getDefault(), matcher.group(2));

                if (lat != 0 && lon != 0) {
                    return new WeatherDatabase.WeatherLocation(lat, lon, null);
                }
            } catch (Throwable t) {
                //Pattern did not match, should never happen
            }
        }

        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        weatherViewModel.getFullscreenModel().postValue(OpenCloseState.CLOSED);

        drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    protected void initViewModel() {
        super.initViewModel();

        weatherViewModel.getFullscreenModel().observe(this, fullscreenState -> {
            if (fullscreenHelper != null) {
                fullscreenHelper.fullscreenify(fullscreenState);
            }

            boolean isFullscreen = fullscreenState == OpenCloseState.OPEN || fullscreenState == OpenCloseState.OPENING;

            fullscreenBackPressedCallback.setEnabled(isFullscreen);

            if (radarCardView != null) {
                radarCardView.getRadarView().setFullscreen(isFullscreen);
            }
        });

        weatherViewModel.getLocationModel().observe(this, weatherLocations -> {
            if (!weatherLocations.isEmpty()) {
                SubMenu locationSubMenu = navigationView.getMenu().getItem(0).getSubMenu();

                if (locationSubMenu != null) {
                    locationSubMenu.clear();

                    int selectedId = 0;

                    for (WeatherDatabase.WeatherLocation weatherLocation : weatherLocations) {
                        locationSubMenu.add(0, weatherLocation.id, 0, weatherLocation.isCurrentLocation ? MainActivity.this.getString(R.string.text_current_location) : weatherLocation.name)
                                .setCheckable(true)
                                .setIcon(R.drawable.navigation_item_icon);

                        if (weatherLocation.isSelected) {
                            selectedId = weatherLocation.id;
                        }
                    }

                    if (selectedId == 0) {
                        selectedId = weatherLocations.get(0).id;

                        Promise
                                .create(selectedId)
                                .then(defaultId -> {
                                    WeatherDatabase.getInstance(MainActivity.this).locationDao().setDefaultLocation(defaultId);
                                });
                    }

                    locationSubMenu.setGroupCheckable(0, true, true);
                    locationSubMenu.findItem(selectedId).setChecked(true);
                }

                Promise.create(a -> {WeatherDataManager.getInstance().removeUnneededFileCache(getApplicationContext(), weatherLocations); });
            }
        });

        weatherViewModel.getCurrentLayoutCardsModel().observe(this,
                cards -> weatherCardRecyclerView.setCardSections(cards));
    }

    @Override
    protected void updateWeather(WeatherModel weatherModel) {
        super.updateWeather(weatherModel);

        toolbar.setTitle(weatherModel.weatherLocation.isCurrentLocation ?
                getString(R.string.text_current_location) :
                weatherModel.weatherLocation.name);
        toolbar.setSubtitle(LocaleUtils.formatDateTime(
                this,
                Locale.getDefault(),
                new Date(weatherModel.currentWeather.timestamp),
                weatherModel.currentWeather.timezone));

        ColorHelper colorHelper = ColorHelper.getInstance(this);

        int color = colorHelper.getColorFromTemperature(
                weatherModel.currentWeather.current.temp,
                false,
                ColorUtils.isNightModeActive(this));
        int darkColor = ColorUtils.getDarkenedColor(color);
        int textColor = colorHelper.getTextColor(color);

        toolbar.setBackgroundColor(color);
        toolbar.setTitleTextColor(textColor);
        toolbar.setSubtitleTextColor(textColor);

        baseLayout.setBackgroundColor(darkColor);

        if (weatherModel.weatherLocation.isCurrentLocation) {
            toolbarMyLocation.setImageTintList(ColorStateList.valueOf(textColor));
            toolbarMyLocation.setVisibility(View.VISIBLE);
        } else {
            toolbarMyLocation.setVisibility(View.GONE);
        }

        drawerToggle.getDrawerArrowDrawable().setColor(textColor);

        if (Build.VERSION.SDK_INT < 35) {
            getWindow().setStatusBarColor(darkColor);
            getWindow().setNavigationBarColor(color);
        }

        CustomTabs.getInstance(this).setColor(color);

        WindowUtils.setLightNavBar(getWindow(), textColor == colorHelper.COLOR_TEXT_BLACK);
    }

    @Override
    protected void initViews() {
        super.initViews();

        fullscreenContainer = findViewById(R.id.fullscreen_container);
        navigationView = findViewById(R.id.navigationView);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.drawer_open_desc,
                R.string.drawer_close_desc);
    }

    public void initViewLogic() {
        weatherCardRecyclerView.setOnRadarWebViewCreatedListener(radarCardView -> {
            radarCardView.attachToActivity(MainActivity.this);

            MainActivity.this.radarCardView = radarCardView;
            fullscreenHelper = new FullscreenHelper(getWindow(), radarCardView.getRadarView(), fullscreenContainer);

            radarCardView.setOnFullscreenClickedListener((expand) -> weatherViewModel
                    .getFullscreenModel()
                    .postValue(expand ?
                            OpenCloseState.OPENING :
                            OpenCloseState.CLOSING));
        });

        addAccessibilityLabelToNavMenuItems();
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            private final GithubUtils.GitHubRepo quickWeatherRepo = GithubUtils.getRepo("TylerWilliamson", "QuickWeather");

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (item.getGroupId() == R.id.settings_group) {
                    if (itemId == R.id.menu_settings) {
                        ContextCompat.startActivity(MainActivity.this,
                                new Intent(MainActivity.this, SettingsActivity.class),
                                ActivityOptions.makeCustomAnimation(MainActivity.this, R.anim.slide_left_in, R.anim.slide_right_out).toBundle());
                    } else if (itemId == R.id.menu_check_for_updates) {
                        Promise.create(quickWeatherRepo)
                                .then((repo) -> {
                                    final String currentVersion = ApkUtils.getReleaseVersion(MainActivity.this).split("-")[0];
                                    final GithubUtils.GitHubRelease latestRelease = quickWeatherRepo.getLatestRelease();

                                    if (currentVersion.equals(latestRelease.tag_name)) {
                                        snackbarHelper.notifyNoNewVersion();
                                    } else {
                                        snackbarHelper.notifyNewVersion(latestRelease);
                                    }
                                }, (e) -> snackbarHelper.logError(getString(R.string.text_error_new_version), e));
                    } else if (itemId == R.id.menu_whats_new) {
                        Promise.create(quickWeatherRepo)
                                .then((repo) -> {
                                    final String version = ApkUtils.getReleaseVersion(MainActivity.this).split("-")[0];
                                    final String releaseNotes = repo.getRelease(version).body;

                                    runOnUiThread(() -> dialogHelper.showReleaseNotes(version, releaseNotes));
                                }, (e) -> snackbarHelper.notifyError(getString(R.string.text_error_getting_release), e));
                    } else if (itemId == R.id.menu_report_a_bug) {
                        CustomTabs.getInstance(MainActivity.this)
                                .launch(MainActivity.this, Uri.parse(quickWeatherRepo.getNewIssueUrl(null, null)));
                    } else if (itemId == R.id.menu_translation) {
                        dialogHelper.showWeblateTranslation();
                    }
                } else {
                    SubMenu subMenu = navigationView.getMenu().getItem(0).getSubMenu();

                    if (subMenu != null) {
                        subMenu.findItem(itemId).setChecked(true);
                        Promise.create((a) -> {
                            WeatherDatabase.getInstance(MainActivity.this).locationDao().setDefaultLocation(itemId);
                            getWeather();
                        });
                    }
                }

                drawerLayout.closeDrawer(GravityCompat.START);

                return true;
            }
        });

        drawerLayout.addDrawerListener(drawerToggle);
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                drawerBackPressedCallback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                drawerBackPressedCallback.setEnabled(false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    private void addAccessibilityLabelToNavMenuItems() {
        ((RecyclerView) navigationView.getChildAt(0)).addItemDecoration(new RecyclerView.ItemDecoration() {
            final String SETTINGS = getString(R.string.text_settings);
            final String CHECK_FOR_UPDATES = getString(R.string.text_check_for_updates);
            final String WHATS_NEW = getString(R.string.text_whats_new);
            final String REPORT_A_BUG = getString(R.string.text_report_a_bug);
            final String TRANSLATION = getString(R.string.dialog_translation_title);
            final String LOCATIONS = getString(R.string.text_locations);

            //TODO should we use custom view instead?
            //Hackermans
            @Override
            public void getItemOffsets(@NonNull Rect outRect,
                                       @NonNull View view,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);

                if (view instanceof ViewGroup) {
                    View childView = ((ViewGroup) view).getChildAt(0);

                    if (childView instanceof AppCompatCheckedTextView) {
                        String textViewText = ((AppCompatCheckedTextView) childView).getText().toString();
                        String clickLabel;

                        if (SETTINGS.equals(textViewText)) {
                            clickLabel = getString(R.string.format_label_open, SETTINGS);
                        } else if (CHECK_FOR_UPDATES.equals(textViewText)) {
                            clickLabel = CHECK_FOR_UPDATES;
                        } else if (WHATS_NEW.equals(textViewText)) {
                            clickLabel = getString(R.string.format_label_open, WHATS_NEW);
                        } else if (REPORT_A_BUG.equals(textViewText)) {
                            clickLabel = REPORT_A_BUG;
                        } else if (TRANSLATION.equals(textViewText)) {
                            clickLabel = getString(R.string.format_label_open, TRANSLATION);
                        } else {
                            clickLabel = getString(R.string.label_location_choose_action);
                        }

                        ViewUtils.setAccessibilityInfo(view, clickLabel, null);
                    }
                } else {
                    if (view instanceof MaterialTextView) {
                        MaterialTextView labelTextView = (MaterialTextView) view;

                        if (labelTextView.getText().equals(LOCATIONS)) {
                            ViewUtils.setDrawable(labelTextView,
                                    R.drawable.ic_edit_white_24dp,
                                    ContextCompat.getColor(getApplicationContext(), R.color.color_accent_text),
                                    ViewUtils.FLAG_END);

                            labelTextView.setOnClickListener(v -> ContextCompat.startActivity(MainActivity.this,
                                    new Intent(MainActivity.this, SettingsActivity.class)
                                            .putExtra(SettingsActivity.EXTRA_GOTOPAGE, 2),
                                    ActivityOptions.makeCustomAnimation(MainActivity.this, R.anim.slide_left_in, R.anim.slide_right_out).toBundle()));

                            ViewUtils.setAccessibilityInfo(view, getString(R.string.format_label_open, LOCATIONS), null);
                        }
                    }
                }
            }
        });
    }
}