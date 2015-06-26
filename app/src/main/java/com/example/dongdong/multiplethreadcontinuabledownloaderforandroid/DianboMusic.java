package com.example.dongdong.multiplethreadcontinuabledownloaderforandroid;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.MediaController;

import java.io.IOException;

/**
 * Created by dongdong on 2015/6/17.
 */
public class DianboMusic extends Activity{
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dianbo_music);
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource("mnt/sdcard/Music/music.mp3");
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
