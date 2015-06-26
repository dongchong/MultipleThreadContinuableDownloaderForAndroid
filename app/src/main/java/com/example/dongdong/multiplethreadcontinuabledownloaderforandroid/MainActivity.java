package com.example.dongdong.multiplethreadcontinuabledownloaderforandroid;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.logging.LogRecord;

/**
 * 主界面，主要负责下载界面的显示、与用户的交互、响应用户的事件
 * @author 董崇
 */
public class MainActivity extends Activity {
    private static final int PROCESSING = 1;    //正在下载的实时数据传输 Message的标志
    private static final int FAILURE = -1;      //下载失败时的Message标志
    private EditText pathText;
    private TextView resultView;
    private Button downloadButton;
    private Button stopbutton;
    private ProgressBar progressBar;
    private Handler handler = new UIHandler();
    private final class UIHandler extends Handler {
        /**
         * 系统会自动调用回调的方法，用于处理消息事件
         * Message一般会包含消息的标志和消息的内容、消息的处理器的handler
         */
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PROCESSING:
                    int size = msg.getData().getInt("size");    //从消息中获取已经下载的数据长度
                    progressBar.setProgress(size);  //设置进度条的进度
                    float num = (float)progressBar.getProgress() / (float)progressBar.getMax(); //计算已经下载的百分比
                    int result = (int)(num * 100);
                    resultView.setText(result+" %");
                    if(progressBar.getProgress() == progressBar.getMax()){  //当下载完成时
                        Toast.makeText(getApplicationContext(),R.string.success,Toast.LENGTH_LONG).show();  //提示用户下载完成
                        /*
                            知识点：MainActivity.this和getApplicationContext()的异同
                                两个都是返回context对象
                                getApplicationContext() 返回应用的上下文，生命周期是整个应用，应用摧毁它才摧毁
                                MainActivity.this返回的context 返回当前activity的上下文，属于activity ，activity 摧毁他就摧毁
                         */
                    }
                    break;
                case -1:    //下载失败时
                    Toast.makeText(getApplicationContext(),R.string.error,Toast.LENGTH_LONG).show();    //提示用户下载失败
                    break;
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pathText = (EditText) findViewById(R.id.path);
        resultView = (TextView) findViewById(R.id.resultView);
        downloadButton = (Button) findViewById(R.id.downloadbutton);
        stopbutton = (Button) findViewById(R.id.stopbutton);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String path = pathText.getText().toString();
                Log.i("TAG",path);
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){   //获取SDCard是否存在
//                    File saveDir = Environment.getExternalStorageDirectory();   //获取SDCard的根目录
//                    File saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
//                    File saveDir = getApplicationContext().getExternalCacheDir(Environment.DIRECTORY_MOVIES);
                    File saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);  //获取SDCard目录中的Music文件夹来保存MP3格式的文件
                    download(path,saveDir); //下载文件
                    /*
                        在android4.0过后的安卓版本中，不允许直接在主线程UI中建立网络连接、访问网络的资源，因为建立网络连接、
                        网络通讯是不稳定的，所需要的时间也是不稳定的，直接在主线程中建立网络连接可能阻塞主线程导致应用失去响应
                        所以新版安卓平台是通过新建立一个线程，在新的线程中进行网络连接的建立和网络资源的访问
                     */
                }else { //当SDCard不存在
                    Toast.makeText(getApplicationContext(),R.string.sdcardererror,Toast.LENGTH_LONG).show();//提示用户SDCard不存在
                }
                downloadButton.setEnabled(false);
                stopbutton.setEnabled(true);
            }
        });
        stopbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit(); //停止下载
                downloadButton.setEnabled(true);
                stopbutton.setEnabled(false);
            }
        });
    }
    private DownloadTask task;  //声明下载执行者

    /**
     * 退出下载
     */
    public void exit(){
        if (task != null){
            task.exit();    //如果有下载对象时，退出下载
        }
    }
    /**
     * 下载资源，声明下载执行者并开辟线程开始下载
     * @param path 下载的路径
     * @param  saveDir 保存文件
     */
    private void download(String path,File saveDir){
        task = new DownloadTask(path, saveDir);
        new Thread(task).start();
    }

    /**
     * UI 控制画面的重绘由主线程负责处理
     */
    private final class DownloadTask implements Runnable {
        private String path;    //下载路径
        private File saveDir;   //下载到保存的文件
        private FileDownloader loader;  //文件下载器
        public DownloadTask(String path,File saveDir) {
            this.path = path;
            this.saveDir = saveDir;
        }

        /**
         * 退出下载
         */
        public void exit() {
            if (loader != null) {
                loader.exit();  //如果下载器存在的话则退出下载
            }
        }
        DownloadProgressListener downloadProgressListener = new DownloadProgressListener() {
            /**
             * 下载文件的长度会不断的被传入该回调方法
             * @param size 数据大小
             */
            @Override
            public void onDownloadSize(int size) {
                Message msg = new Message();
                msg.what = PROCESSING;
                msg.getData().putInt("size",size);
                handler.sendMessage(msg);
            }
        };
        @Override
        public void run() {
            try {
                loader = new FileDownloader(getApplicationContext(),path,saveDir,3);    //初始化下载
                progressBar.setMax(loader.getFileSize());   //设置进度条的最大刻度
                loader.download(downloadProgressListener);
            } catch (Exception e) {
                e.printStackTrace();
                handler.sendMessage(handler.obtainMessage(FAILURE));    //下载失败时向消息队列发送消息
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
