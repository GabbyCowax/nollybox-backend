package com.example.nollybox;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DownloadedMovie.class, FavoriteMovie.class, Movie.class, RecentMovie.class}, version = 7)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract MovieDao movieDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "nollybox_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}