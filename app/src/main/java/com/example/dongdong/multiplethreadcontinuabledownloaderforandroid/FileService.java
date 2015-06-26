package com.example.dongdong.multiplethreadcontinuabledownloaderforandroid;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dongdong on 2015/6/2.
 * 业务的Bean，实现对数据库的操作
 * @author 董崇
 */
public class FileService {
    /**
     *业务bean，实现对数据库的操作
     */
    private DBOpenHelper openHelper;
    public FileService(Context context){
        openHelper = new DBOpenHelper(context);
    }
    /**
    * 获取特定的URL的每条线程已经下载的文件长度
    */
    public Map<Integer, Integer> getData(String path){
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select threadid,downlength from filedownlog where downpath=?",new String[]{path});//根据下载的路径查询所有线程下载数据，返回的Cursor指向第一行
        Map<Integer,Integer> data = new HashMap<Integer, Integer>();//用一个哈希表存放每条线程下载的文件长度
        while(cursor.moveToNext()){
            data.put(cursor.getInt(0),cursor.getInt(1));    //将threadid和downlength存入到data表中
            //data.put(cursor.getInt(cursor.getColumnIndexOrThrow("threadid")),cursor.getInt(cursor.getColumnIndexOrThrow("downlength")));
        }
        cursor.close();
        db.close();
        return data;
    }
    /**
    *保存每条线程已经下载的文件长度
    * path为下载的路径
    * map 现在的id和已经下载的长度的集合
    */
    public void save(String path,Map<Integer,Integer> map){
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.beginTransaction();//开始事物，因为此处要插入多批数据
        try {
            for (Map.Entry<Integer,Integer> entry : map.entrySet()){
                //插入特定下载的路径，特定的ID和下载的数据长度
                db.execSQL("insert into filedownlog(downpath,threadid,downlength) values(?,?,?)",new Object[]{path,entry.getKey(),entry.getValue()});
            }
            db.setTransactionSuccessful();
        }finally {
            db.endTransaction();
        }
        db.close();
    }
    /**
     * 更新每条线程已经下载的文件长度
     */
    public void update(String path,int threadid,int pos){
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("update filedownlog set downlength=? where downpath=? and threadid=?",new Object[]{pos,path,threadid});//更新特定下载路径，特定线程，已经下载的文件长度
        db.close();//关闭数据库，释放相关资源
    }
    /**
    * 当文件下载完成后，删除对应的下载记录
    */
    public void delete(String path){
        SQLiteDatabase db = openHelper.getWritableDatabase();//获取可写入数据的句柄
        db.execSQL("delete from filedownlog where downpath=?",new Object[]{path});//删除特定下载路径的所有线程记录
        db.close();
    }
}