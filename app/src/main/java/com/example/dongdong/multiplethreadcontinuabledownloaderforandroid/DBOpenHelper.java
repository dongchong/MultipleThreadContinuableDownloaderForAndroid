package com.example.dongdong.multiplethreadcontinuabledownloaderforandroid;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

/**
 * Created by dongdong on 2015/6/1.
 * SQLite管理器，实现创建数据库和表，版本变化时更新数据库表的操作
 * @author 董崇
 */
public class DBOpenHelper extends SQLiteOpenHelper {
    private static final String DBNAME = "eric.db";
    private static final int VERSION = 1;
    public DBOpenHelper(Context context){
        super(context,DBNAME,null, VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS filedownlog (id integer primary key " +
                    "autoincrement,downpath varchar(100),threadid INTEGER,downlength INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS filedownlog");//删除数据表filedownlog
        onCreate(db);//创建新的数据表
    }
}
