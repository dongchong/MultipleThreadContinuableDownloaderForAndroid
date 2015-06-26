package com.example.dongdong.multiplethreadcontinuabledownloaderforandroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dongdong on 2015/6/15.
 */
public class SelectFile extends Activity{
    private TextView path;
    private ListView list;
    private Button parent;
    File currentParent; //记录当前的父文件夹
    File[] currentFiles;    //记录当前路径下所有文件的文件数组
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selectfile);
        path = (TextView)findViewById(R.id.path);
        list = (ListView)findViewById(R.id.list);
        parent = (Button)findViewById(R.id.parent);
        File root = new File("/mnt/sdcard/");
        if (root.exists()){ //判断sd卡是否存在
            currentParent = root;
            currentFiles = root.listFiles();   //获取到root根目录下的文件数组
            showByListView(currentFiles);   //将文件数组填充到ListView中去
        }
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { //为ListView中的每一个item添加点击事件
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentFiles[position].isFile()) return;    //如果用户点击了具体的文件，此处不处理（因为文件类型太多）
                File[] tmp = currentFiles[position].listFiles();    //获取点击后文件下的所有文件
                if (tmp == null || tmp.length == 0){
                    Toast.makeText(getApplication(),"该路径不可访问或路径下没有文件",Toast.LENGTH_LONG).show();
                } else {
                    currentParent = currentFiles[position];
                    currentFiles = tmp;
                    showByListView(currentFiles);
                }
            }
        });
        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    if (!currentParent.getCanonicalPath().equals("/")){   //如果父目录不是root的最终根目录
                        currentParent = currentParent.getParentFile();
                        currentFiles = currentParent.listFiles();
                        showByListView(currentFiles);
                    }
                }catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 将currentFiles的目录下的文件显示到ListView上去
     * @param files
     */
    private void showByListView(File[] files){
        List<Map<String,Object>> listItems = new ArrayList<Map<String,Object>>();
        for (int i = 0; i < files.length; i++){
            Map<String,Object> listItem = new HashMap<String,Object>();
            if (files[i].isDirectory()){    //如果是文件夹，用folder图片，如果是文件，用file图片显示
                listItem.put("icon",R.drawable.folder);
            } else {
                listItem.put("icon",R.drawable.file);
            }
            listItem.put("fileName",files[i].getName());
            listItems.add(listItem);
        }
        SimpleAdapter simpleAdapter = new SimpleAdapter(this,listItems,R.layout.line,new String[]{"icon","fileName"},new int[]{R.id.icon,R.id.file_name});
        list.setAdapter(simpleAdapter); //为ListView设置adapter
        try{
            path.setText("当前路径为："+currentParent.getCanonicalPath());
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
