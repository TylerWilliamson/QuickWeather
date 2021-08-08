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

package com.ominous.quickweather.view;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import com.google.android.material.navigation.NavigationView;
import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.SettingsActivity;
import com.ominous.quickweather.util.WeatherPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class WeatherNavigationView extends NavigationView implements NavigationView.OnNavigationItemSelectedListener {
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

        updateLocations();

        //TODO ripple backgrounds
        //setItemBackground(new RippleDrawable(ColorStateList.valueOf(0xFFFF0000),null,navigationView.getItemBackground()));
    }

    public void initialize(OnDefaultLocationSelectedListener onDefaultLocationSelectedListener) {
        this.onDefaultLocationSelectedListener = onDefaultLocationSelectedListener;
    }

    public void updateLocations() {
        Menu menu = getMenu();

        menu.clear();

        locationSubMenu = menu.addSubMenu(getContext().getString(R.string.text_locations));

        for (WeatherPreferences.WeatherLocation weatherLocation : WeatherPreferences.getLocations()) {
            locationSubMenu.add(0, 0, 0, weatherLocation.location).setCheckable(true);
        }

        locationSubMenu.setGroupCheckable(0, true, true);

        menu.addSubMenu(getContext().getString(R.string.text_settings)).add(getContext().getString(R.string.text_settings)).setIcon(R.drawable.ic_settings_white_24dp).setChecked(true);

        updateMenuItemIndicators(WeatherPreferences.getDefaultLocation());
    }

    //TODO move away from the location name and instead use an index
    private void updateMenuItemIndicators(String selectedLocation) {
        MenuItem item;

        for (int i = 0, l = locationSubMenu.size(); i < l; i++) {
            item = locationSubMenu.getItem(i);

            item.setIcon(selectedLocation.equals(item.getTitle().toString()) ? R.drawable.ic_gps_fixed_white_24dp : R.drawable.ic_gps_not_fixed_white_24dp);
            item.setChecked(selectedLocation.equals(item.getTitle().toString()));
        }
    }

    //TODO: move non-UI logic to MainActivity
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        /*RippleDrawable rippleDrawable = (RippleDrawable) navigationView.getItemBackground();
        rippleDrawable.setState(new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled});*/

        String menuItemSelected = menuItem.getTitle().toString();

        if (menuItemSelected.equals(getContext().getString(R.string.text_settings))) {
            ContextCompat.startActivity(getContext(), new Intent(getContext(), SettingsActivity.class).putExtra(SettingsActivity.EXTRA_SKIP_WELCOME, true), ActivityOptions.makeCustomAnimation(getContext(), R.anim.slide_left_in, R.anim.slide_right_out).toBundle());
        } else {
            updateMenuItemIndicators(menuItemSelected);

            if (onDefaultLocationSelectedListener != null) {
                onDefaultLocationSelectedListener.onDefaultLocationSelected(menuItemSelected);
            }
        }

        return true;
    }

    public interface OnDefaultLocationSelectedListener {
        void onDefaultLocationSelected(String location);
    }
}
