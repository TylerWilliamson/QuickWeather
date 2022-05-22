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

package com.ominous.quickweather.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;

import com.google.android.material.navigation.NavigationView;
import com.ominous.quickweather.R;
import com.ominous.quickweather.data.WeatherDatabase;
import com.ominous.tylerutils.util.ColorUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class WeatherNavigationView extends NavigationView implements NavigationView.OnNavigationItemSelectedListener {
    private final static int MENU_SETTINGS_ID = -1, MENU_WHATS_NEW = -2, MENU_CHECK_UPDATES = -3,
            MENU_REPORT_BUG = -4;
    private SubMenu locationSubMenu;
    private OnDefaultLocationSelectedListener onDefaultLocationSelectedListener = null;

    public WeatherNavigationView(@NonNull Context context) {
        this(context, null, 0);
    }

    public WeatherNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeatherNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setNavigationItemSelectedListener(this);

        addRippleBackgroundToMenuItems(context);
    }

    public void initialize(OnDefaultLocationSelectedListener onDefaultLocationSelectedListener) {
        this.onDefaultLocationSelectedListener = onDefaultLocationSelectedListener;
    }

    public void updateLocations(List<WeatherDatabase.WeatherLocation> weatherLocations) {
        Menu menu = getMenu();

        menu.clear();

        locationSubMenu = menu.addSubMenu(R.string.text_locations);
        int selectedId = 0;

        for (WeatherDatabase.WeatherLocation weatherLocation : weatherLocations) {
            locationSubMenu.add(0, weatherLocation.id, 0, weatherLocation.name).setCheckable(true);

            if (weatherLocation.isSelected) {
                selectedId = weatherLocation.id;
            }
        }

        locationSubMenu.setGroupCheckable(0, true, true);
        SubMenu settingsSubMenu = menu.addSubMenu(R.string.text_settings);

        settingsSubMenu.add(0, MENU_SETTINGS_ID, 0, getContext().getString(R.string.text_settings))
                .setIcon(R.drawable.ic_settings_white_24dp).setChecked(true);
        settingsSubMenu.add(0, MENU_CHECK_UPDATES, 0, getContext().getString(R.string.text_check_for_updates))
                .setIcon(R.drawable.ic_download_white_24dp).setChecked(true);
        settingsSubMenu.add(0, MENU_WHATS_NEW, 0, getContext().getString(R.string.text_whats_new))
                .setIcon(R.drawable.ic_star_white_24dp).setChecked(true);
        settingsSubMenu.add(0, MENU_REPORT_BUG, 0, getContext().getString(R.string.text_report_a_bug))
                .setIcon(R.drawable.ic_bug_report_white_24dp).setChecked(true);

        updateMenuItemIndicators(selectedId);
    }

    private void updateMenuItemIndicators(int selectedLocationId) {
        MenuItem item;

        for (int i = 0, l = locationSubMenu.size(); i < l; i++) {
            item = locationSubMenu.getItem(i);

            item.setIcon(item.getItemId() == selectedLocationId ? R.drawable.ic_gps_fixed_white_24dp : R.drawable.ic_gps_not_fixed_white_24dp)
                    .setChecked(item.getItemId() == selectedLocationId);
        }
    }

    private void addRippleBackgroundToMenuItems(Context context) {
        ((RecyclerView) this.getChildAt(0)).addItemDecoration(new RecyclerView.ItemDecoration() {
            final int colorStart = ContextCompat.getColor(context, R.color.card_background);
            final int colorEnd = ContextCompat.getColor(context, R.color.card_background_pressed);

            //Hackermans
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);

                final ValueAnimator pressedAnimation = ValueAnimator.ofInt(0, 200)
                        .setDuration(200);

                pressedAnimation.addUpdateListener(animation ->
                        view.setBackgroundColor(
                                ColorUtils.blendColors(colorStart, colorEnd,
                                        animation.getAnimatedFraction() * 100)));

                //The work is purely visual
                //noinspection "ClickableViewAccessibility"
                if (view.hasOnClickListeners()) {
                    view.setOnTouchListener((v, event) -> {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                pressedAnimation.start();
                                break;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                pressedAnimation.reverse();
                        }

                        return false;
                    });
                }
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        if (onDefaultLocationSelectedListener != null) {
            switch (menuItem.getItemId()) {
                case MENU_SETTINGS_ID:
                    onDefaultLocationSelectedListener.onNavigationItemSelected(NavigationKind.SETTINGS, 0);
                    break;
                case MENU_WHATS_NEW:
                    onDefaultLocationSelectedListener.onNavigationItemSelected(NavigationKind.WHATS_NEW, 0);
                    break;
                case MENU_CHECK_UPDATES:
                    onDefaultLocationSelectedListener.onNavigationItemSelected(NavigationKind.CHECK_UPDATES, 0);
                    break;
                case MENU_REPORT_BUG:
                    onDefaultLocationSelectedListener.onNavigationItemSelected(NavigationKind.REPORT_BUG, 0);
                    break;
                default:
                    updateMenuItemIndicators(menuItem.getItemId());
                    onDefaultLocationSelectedListener.onNavigationItemSelected(NavigationKind.LOCATION, menuItem.getItemId());
                    break;
            }
        }

        return true;
    }

    public enum NavigationKind {
        LOCATION,
        SETTINGS,
        WHATS_NEW,
        CHECK_UPDATES,
        REPORT_BUG
    }

    public interface OnDefaultLocationSelectedListener {
        void onNavigationItemSelected(NavigationKind kind, int id);
    }
}
