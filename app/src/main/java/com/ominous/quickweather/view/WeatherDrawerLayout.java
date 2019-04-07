package com.ominous.quickweather.view;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.SubMenu;

import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.SettingsActivity;
import com.ominous.quickweather.util.WeatherPreferences;

import java.lang.ref.WeakReference;

public class WeatherDrawerLayout extends DrawerLayout implements NavigationView.OnNavigationItemSelectedListener {
    private WeakReference<AppCompatActivity> parentActivity;
    private SubMenu locationSubMenu;
    private OnDefaultLocationSelectedListener onDefaultLocationSelectedListener;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    public WeatherDrawerLayout(Context context) {
        this(context, null, 0);
    }

    public WeatherDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeatherDrawerLayout(final Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialize(AppCompatActivity activity, Toolbar toolbar, OnDefaultLocationSelectedListener onDefaultLocationSelectedListener) {
        this.parentActivity = new WeakReference<>(activity);
        this.onDefaultLocationSelectedListener = onDefaultLocationSelectedListener;

        actionBarDrawerToggle = new ActionBarDrawerToggle(
                activity,
                this,
                toolbar,
                R.string.drawer_opened,
                R.string.drawer_closed);

        actionBarDrawerToggle.syncState();
        this.addDrawerListener(actionBarDrawerToggle);

        NavigationView navigationView = this.findViewById(R.id.navigationView);

        locationSubMenu = navigationView.getMenu().addSubMenu(activity.getString(R.string.text_locations));

        for (WeatherPreferences.WeatherLocation weatherLocation : WeatherPreferences.getLocations()) {
            locationSubMenu.add(0, 0, 0, weatherLocation.location).setCheckable(true);
        }

        locationSubMenu.setGroupCheckable(0, true, true);

        updateMenuItemIndicators(locationSubMenu, WeatherPreferences.getDefaultLocation());

        navigationView.getMenu().addSubMenu(activity.getString(R.string.text_settings)).add(activity.getString(R.string.text_settings)).setIcon(R.drawable.ic_settings_white_24dp).setChecked(true);

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.invalidate();
    }

    public void setSpinnerColor(int color) {
        actionBarDrawerToggle.getDrawerArrowDrawable().setColor(color);
    }

    private void updateMenuItemIndicators(SubMenu menu, String selectedLocation) {
        MenuItem item;

        for (int i = 0, l = menu.size(); i < l; i++) {
            item = menu.getItem(i);

            item.setIcon(selectedLocation.equals(item.getTitle().toString()) ? R.drawable.ic_gps_fixed_white_24dp : R.drawable.ic_gps_not_fixed_white_24dp);
            item.setChecked(selectedLocation.equals(item.getTitle().toString()));
        }

        WeatherPreferences.setDefaultLocation(selectedLocation);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        WeatherDrawerLayout.this.closeDrawer(Gravity.START);

        String menuItemSelected = menuItem.getTitle().toString();

        if (menuItemSelected.equals(getContext().getString(R.string.text_settings))) {

            ContextCompat.startActivity(parentActivity.get(),new Intent(getContext(), SettingsActivity.class).putExtra(SettingsActivity.EXTRA_SKIP_WELCOME, true),null);
            //parentActivity.get().startActivity(new Intent(getContext(), SettingsActivity.class).putExtra(SettingsActivity.EXTRA_SKIP_WELCOME, true));
            parentActivity.get().overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
        } else {
            updateMenuItemIndicators(locationSubMenu, menuItemSelected);

            onDefaultLocationSelectedListener.onDefaultLocationSelected(menuItemSelected);
        }

        return true;
    }

    public interface OnDefaultLocationSelectedListener {
        void onDefaultLocationSelected(String location);
    }
}
