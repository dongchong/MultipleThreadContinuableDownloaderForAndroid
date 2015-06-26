package com.example.dongdong.multiplethreadcontinuabledownloaderforandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;

/**
 * Created by dongdong on 2015/6/15.
 */
public class Music_Video extends Activity {
    private ImageView click;
    private Spinner spinner;
    private EditText title_EditText;
    private String dianbo_title;    //点播文件名的名称
    private String spinner_type;    //点击spinner后具体的类型（音乐文件、视频文件、图片文件）
    private MediaPlayer music_player;   //用MediaPlayer来加载声音文件
    private VideoView videoView;    //视频视图
    private MediaController mediaController;    //音乐或视频的播放控制器
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_video);
        click = (ImageView)findViewById(R.id.click);
        music_player = new MediaPlayer();
        mediaController = new MediaController(Music_Video.this);
        click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //此处是显示一个自定义的View显示框
                final TableLayout dianboForm = (TableLayout)getLayoutInflater().inflate(R.layout.dianbo,null);    //加载点播的界面布局文件
                title_EditText = (EditText)dianboForm.findViewById(R.id.title_EditText);
                dianbo_title = title_EditText.getText().toString().trim();
                spinner = (Spinner)dianboForm.findViewById(R.id.spinner);
                String[] types = getResources().getStringArray(R.array.type_array); //建立数据源
                ArrayAdapter<String> _adapter = new ArrayAdapter<String>(Music_Video.this,android.R.layout.simple_spinner_item,types);  //建立Adapter并且绑定数据源
                spinner.setAdapter(_adapter);   //绑定Adapter到Spinner控件
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        spinner_type = parent.getItemAtPosition(position).toString();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                final AlertDialog.Builder builder = new AlertDialog.Builder(Music_Video.this);
                    builder.setTitle("点播对话框（本地文件）")
                    //显示dianboForm的View
                    .setView(dianboForm)
                    .setPositiveButton("选择",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (spinner_type){
                                case "音乐文件":
//                                    Intent intent1 = new Intent();
//                                    intent1.setClass(Music_Video.this,DianboMusic.class);
//                                    startActivity(intent1);
                                    break;
                                case "视频文件":
                                    File video = new File("/mnt/sdcard/Download/"+dianbo_title);
                                    if(video.exists()){
                                        Intent intent = new Intent();
                                        intent.setClass(Music_Video.this,DianboVideo.class);
                                        startActivity(intent);
                                    }else {
                                        Toast.makeText(getApplicationContext(),"您要播放的mp4文件不存在",Toast.LENGTH_LONG).show();
                                    }
                                    break;
                                case "图片文件":
                                    break;
                            }
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //什么也不做，点击取消会自动退出AlertDialog
                            //Toast.makeText(getApplicationContext(),which+"",Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        }
                    })
                    //创建显示的对话框
                    .create()
                    .show();
            }
        });
    }
}
