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

package com.ominous.quickweather.data;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {WeatherDatabase.WeatherNotification.class}, version = 1, exportSchema = false)
public abstract class WeatherDatabase extends RoomDatabase
{
    private static WeatherDatabase instance = null;

    abstract public WeatherNotificationDao notificationDao();

    @Dao
    public interface WeatherNotificationDao {
        @Insert
        void insert(WeatherNotification weatherNotification);

        @Query("SELECT * FROM WeatherNotification WHERE hashCode = :hashCode LIMIT 1")
        WeatherNotification findByHashCode(int hashCode);

        @Query("DELETE FROM WeatherNotification WHERE expires <= :currentTime / 1000")
        void deleteExpired(long currentTime);
    }

    @Entity
    public static class WeatherNotification {
        @PrimaryKey
        public final int hashCode;

        @ColumnInfo(name = "uri")
        public final String uri;

        @ColumnInfo(name = "expires")
        public final long expires;

        public WeatherNotification(int hashCode, String uri, long expires) {
            this.hashCode = hashCode;
            this.uri = uri;
            this.expires = expires;
        }
    }

    public static WeatherDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room
                    .databaseBuilder(context.getApplicationContext(), WeatherDatabase.class, "QuickWeather")
                    .allowMainThreadQueries() //not recommended, but tiny table
                    .build();
        }

        return instance;
    }
}
