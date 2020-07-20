package com.ominous.quickweather.view;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.ominous.quickweather.R;
import com.ominous.quickweather.activity.SettingsActivity;
import com.ominous.quickweather.util.WeatherPreferences;

import java.lang.ref.WeakReference;

public class WeatherDrawerLayout extends DrawerLayout implements NavigationView.OnNavigationItemSelectedListener {
    private WeakReference<AppCompatActivity> parentActivity;
    private SubMenu locationSubMenu;
    private OnDefaultLocationSelectedListener onDefaultLocationSelectedListener;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;

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

        navigationView = this.findViewById(R.id.navigationView);
        //TODO ripple backgrounds
        //navigationView.setItemBackground(new RippleDrawable(ColorStateList.valueOf(0xFFFF0000),null,navigationView.getItemBackground()));

        updateLocations(activity);

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.invalidate();
    }

    public void setSpinnerColor(int color) {
        actionBarDrawerToggle.getDrawerArrowDrawable().setColor(color);
    }

    public void updateLocations(Context context) {
        Menu menu = navigationView.getMenu();

        menu.clear();

        locationSubMenu = menu.addSubMenu(context.getString(R.string.text_locations));

        for (WeatherPreferences.WeatherLocation weatherLocation : WeatherPreferences.getLocations()) {
            locationSubMenu.add(0, 0, 0, weatherLocation.location).setCheckable(true);
        }

        locationSubMenu.setGroupCheckable(0, true, true);

        menu.addSubMenu(context.getString(R.string.text_settings)).add(context.getString(R.string.text_settings)).setIcon(R.drawable.ic_settings_white_24dp).setChecked(true);

        updateMenuItemIndicators(WeatherPreferences.getDefaultLocation());
    }

    private void updateMenuItemIndicators(String selectedLocation) {
        MenuItem item;

        for (int i = 0, l = locationSubMenu.size(); i < l; i++) {
            item = locationSubMenu.getItem(i);

            item.setIcon(selectedLocation.equals(item.getTitle().toString()) ? R.drawable.ic_gps_fixed_white_24dp : R.drawable.ic_gps_not_fixed_white_24dp);
            item.setChecked(selectedLocation.equals(item.getTitle().toString()));
        }

        WeatherPreferences.setDefaultLocation(selectedLocation);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        WeatherDrawerLayout.this.closeDrawer(GravityCompat.START);

        /*RippleDrawable rippleDrawable = (RippleDrawable) navigationView.getItemBackground();
        rippleDrawable.setState(new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled});*/

        String menuItemSelected = menuItem.getTitle().toString();

        if (menuItemSelected.equals(getContext().getString(R.string.text_settings))) {
            ContextCompat.startActivity(parentActivity.get(),new Intent(getContext(), SettingsActivity.class).putExtra(SettingsActivity.EXTRA_SKIP_WELCOME, true),null);
            parentActivity.get().overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
        } else {
            updateMenuItemIndicators(menuItemSelected);

            onDefaultLocationSelectedListener.onDefaultLocationSelected(menuItemSelected);
        }

        return true;
    }

    public interface OnDefaultLocationSelectedListener {
        void onDefaultLocationSelected(String location);
    }
}
