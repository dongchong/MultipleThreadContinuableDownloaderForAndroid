package com.example.dongdong.multiplethreadcontinuabledownloaderforandroid;

import android.app.Activity;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.File;

/**
 * Created by dongdong on 2015/6/17.
 */
public class DianboVideo extends Activity {
    private VideoView videoView;
    private MediaController mediaController;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dianbo_video);
        videoView = (VideoView)findViewById(R.id.videoView);
        mediaController = new MediaController(DianboVideo.this);
        File file = new File("/mnt/sdcard/Download/movie.mp4");
        videoView.setVideoPath(file.getAbsolutePath());
        videoView.setMediaController(mediaController);
        mediaController.setMediaPlayer(videoView);
        videoView.requestFocus();
    }
}
