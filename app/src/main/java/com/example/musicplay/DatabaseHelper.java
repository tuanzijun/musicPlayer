package com.example.musicplay;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String sql= "create table login("
            +"id integer primary key autoincrement,"
            +"title String,"
            +"artist String,"
            +"url String)";
    private Context mcontext;
    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context,name,factory,version);
        mcontext = context;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sql);
        Toast.makeText(mcontext,"Create succeeded",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists ACCOUNT_PASSWORD");
        onCreate(db);
    }
}
