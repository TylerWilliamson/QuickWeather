/*
 *   Copyright 2019 - 2024 Tyler Williamson
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

package com.ominous.quickweather.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.Update;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Calendar;
import java.util.List;

@Database(
        entities = {WeatherDatabase.WeatherNotification.class, WeatherDatabase.WeatherLocation.class, WeatherDatabase.WeatherCard.class},
        version = 3,
        exportSchema = false)
public abstract class WeatherDatabase extends RoomDatabase {
    final static Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `WeatherLocation` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `name` TEXT, `isCurrentLocation` INTEGER NOT NULL, `isSelected` INTEGER NOT NULL, `order` INTEGER NOT NULL)");
        }
    };
    final static Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `WeatherCard` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `activityId` INTEGER NOT NULL, `weatherCardType` TEXT, `order` INTEGER NOT NULL, `enabled` INTEGER NOT NULL)");

            database.execSQL("INSERT INTO `WeatherCard` VALUES " +
                    "(0,0,'CURRENT_MAIN',0,1)," +
                    "(1,0,'ALERT',1,1)," +
                    "(2,0,'GRAPH',2,1)," +
                    "(3,0,'RADAR',3,1)," +
                    "(4,0,'CURRENT_FORECAST',4,1)," +
                    "(5,1,'FORECAST_MAIN',3,1)," +
                    "(6,1,'ALERT',3,1)," +
                    "(7,1,'GRAPH',3,1)," +
                    "(8,1,'FORECAST_DETAIL',3,1)");
        }
    };
    private static WeatherDatabase instance = null;

    public static WeatherDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room
                    .databaseBuilder(context.getApplicationContext(), WeatherDatabase.class, "QuickWeather")
                    //.allowMainThreadQueries() //not recommended
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build();
        }

        return instance;
    }

    public void insertAlert(CurrentWeather.Alert alert) {
        WeatherNotificationDao notifcationDao = this.notificationDao();

        notifcationDao
                .insert(new WeatherDatabase.WeatherNotification(
                        alert.getId(),
                        alert.getUri(),
                        alert.end
                ));

        notifcationDao
                .deleteExpired(Calendar.getInstance().getTimeInMillis());
    }

    abstract public WeatherLocationDao locationDao();

    abstract public WeatherNotificationDao notificationDao();

    abstract public WeatherCardDao cardDao();

    @Dao
    public interface WeatherLocationDao {
        @Insert(onConflict = OnConflictStrategy.IGNORE)
        long insert(WeatherLocation weatherLocation);

        @Update
        void update(WeatherLocation... weatherLocation);

        @Delete
        void delete(WeatherLocation... weatherLocation);

        @Query("SELECT * FROM WeatherLocation ORDER BY `order`")
        List<WeatherLocation> getAllWeatherLocations();

        @Query("SELECT * FROM WeatherLocation ORDER BY `order`")
        LiveData<List<WeatherLocation>> getLiveWeatherLocations();

        @Query("UPDATE WeatherLocation SET isSelected = CASE id WHEN :id THEN 1 ELSE 0 END")
        void setDefaultLocation(int id);

        @Query("SELECT * FROM WeatherLocation WHERE isSelected = 1")
        WeatherLocation getSelected();

        @Query("SELECT COUNT(1) FROM WeatherLocation")
        int getCount();

        @Query("SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END FROM WeatherLocation WHERE isCurrentLocation = 1")
        boolean isCurrentLocationSelected();
    }

    @Dao
    public interface WeatherNotificationDao {
        @Insert(onConflict = OnConflictStrategy.IGNORE)
        void insert(WeatherNotification weatherNotification);

        @Query("SELECT * FROM WeatherNotification WHERE hashCode = :hashCode LIMIT 1")
        WeatherNotification findByHashCode(int hashCode);

        @Query("DELETE FROM WeatherNotification WHERE expires <= :currentTime / 1000")
        void deleteExpired(long currentTime);
    }

    @Dao
    public interface WeatherCardDao {
        @Query("SELECT * FROM WeatherCard WHERE activityId = 0 ORDER BY `order`")
        List<WeatherCard> getCurrentWeatherCards();

        @Query("SELECT * FROM WeatherCard WHERE activityId = 1 ORDER BY `order`")
        List<WeatherCard> getForecastWeatherCards();

        @Query("SELECT weatherCardType FROM WeatherCard WHERE activityId = 0 AND enabled = 1 ORDER BY `order`")
        LiveData<WeatherCardType[]> getEnabledCurrentWeatherCards();

        @Query("SELECT weatherCardType FROM WeatherCard WHERE activityId = 1 AND enabled = 1 ORDER BY `order`")
        LiveData<WeatherCardType[]> getEnabledForecastWeatherCards();

        @Update
        void update(WeatherCard... weatherCards);

        @Query("UPDATE WeatherCard SET enabled = 0 WHERE activityId = 0 AND weatherCardType = 'RADAR'")
        void disableRadar();
    }

    @Entity
    public static class WeatherLocation implements Parcelable {
        public final static Parcelable.Creator<WeatherLocation> CREATOR = new Parcelable.Creator<WeatherLocation>() {
            public WeatherLocation createFromParcel(Parcel in) {
                return new WeatherLocation(in);
            }

            public WeatherLocation[] newArray(int size) {
                return new WeatherLocation[size];
            }
        };
        @PrimaryKey(autoGenerate = true)
        public final int id;
        public final double latitude;
        public final double longitude;
        public final String name;
        public final boolean isCurrentLocation;
        public boolean isSelected;
        public int order;

        @Ignore
        public WeatherLocation(double latitude, double longitude, String name) {
            this.id = 0;
            this.isSelected = false;
            this.order = 0;
            this.latitude = latitude;
            this.longitude = longitude;
            this.name = name;
            this.isCurrentLocation = false;
        }

        public WeatherLocation(int id, double latitude, double longitude, String name, boolean isSelected, boolean isCurrentLocation, int order) {
            this.id = id;
            this.isSelected = isSelected;
            this.order = order;
            this.latitude = latitude;
            this.longitude = longitude;
            this.name = name;
            this.isCurrentLocation = isCurrentLocation;
        }

        WeatherLocation(Parcel in) {
            this.id = in.readInt();
            this.isSelected = in.readInt() == 1;
            this.order = in.readInt();
            this.latitude = in.readDouble();
            this.longitude = in.readDouble();
            this.name = in.readString();
            this.isCurrentLocation = in.readInt() == 1;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(id);
            dest.writeInt(isSelected ? 1 : 0);
            dest.writeInt(order);
            dest.writeDouble(latitude);
            dest.writeDouble(longitude);
            dest.writeString(name);
            dest.writeInt(isCurrentLocation ? 1 : 0);
        }
    }

    @Entity
    public static class WeatherNotification {
        @PrimaryKey
        public final int hashCode;
        public final String uri;
        public final long expires;

        public WeatherNotification(int hashCode, String uri, long expires) {
            this.hashCode = hashCode;
            this.uri = uri;
            this.expires = expires;
        }
    }

    @Entity
    public static class WeatherCard {
        @PrimaryKey(autoGenerate = true)
        public final int id;
        public final int activityId; //0 - current, 1 - forecast
        public final WeatherCardType weatherCardType;
        public int order;
        public boolean enabled;

        public WeatherCard(int id, int activityId, WeatherCardType weatherCardType, int order, boolean enabled) {
            this.id = id;
            this.activityId = activityId;
            this.weatherCardType = weatherCardType;
            this.order = order;
            this.enabled = enabled;
        }
    }
}
