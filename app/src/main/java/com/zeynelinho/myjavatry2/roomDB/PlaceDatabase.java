package com.zeynelinho.myjavatry2.roomDB;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.zeynelinho.myjavatry2.model.Place;

@Database(entities = {Place.class}, version = 1)
public abstract class PlaceDatabase extends RoomDatabase {

    public abstract PlaceDao placeDao();

}
