package com.example.dongdong.multiplethreadcontinuabledownloaderforandroid;

import android.util.Log;

import org.apache.http.params.HttpParams;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by dongdong on 2015/6/14.
 * 下载线程，根据具体的下载地址、数据保存到的文件、下载块的大小、已经下载的数据大小等信息进行下载
 * @author 董崇
 */
public class DownloadThread extends Thread{
    private static final String TAG = "DownloadThread"; //定义TAG，方便LogCat的打印输出
    private File saveFile;                              //下载的数据保存到的文件
    private URL downUrl;                                //下载的URL
    private int block;                                  //每条线程的下载长度
    private int threadId = -1;                          //初始化线程ID的设置
    private int downloadedLength;                       //该线程已经下载的数据长度
    private boolean finished = false;                   //线程是否下载完成的标志
    private FileDownloader downloader;                  //文件下载器
    public DownloadThread(FileDownloader downloader,URL downUrl,File saveFile,int block,int downloadedLength,int threadId) {
        this.downloader = downloader;
        this.downUrl = downUrl;
        this.saveFile = saveFile;
        this.downloadedLength = downloadedLength;
        this.block = block;
        this.threadId = threadId;
    }
    public void run(){
        if (downloadedLength < block){  //为下载完成
            try {
                HttpURLConnection http = (HttpURLConnection) downUrl.openConnection();  //开启HttpURLConnection连接
                http.setConnectTimeout(5*1000); //连接超时时间为5秒
                http.setRequestMethod("GET");   //设置请求方法为get
                http.setRequestProperty("Accept","image/gif,image/jpeg,image/pjpeg,image/pjpeg,application/x-shockwave-flash," +
                        "application/xaml+xml,application/vnd.ms-xpsdocument,application/x-ms-xbap,application/x-ms-application," +
                        "application/vnd.ms-excel,application/vnd.ms-powerpoint,application/msword,*/*");   //设置客户端可以接受的返回数据类型
                http.setRequestProperty("Accept-Lauguage","zh-CN"); //设置客户端使用的语言为中文
                http.setRequestProperty("Referer",downUrl.toString());  //设置请求的来源，便于对访问来源进行统计
                http.setRequestProperty("Charset","UTF-8");         //设置通信的编码为UTF-8
                int startPos = block * (threadId - 1) + downloadedLength;   //开始位置
                int endPos = block * threadId - 1;                          //结束位置
                http.setRequestProperty("Range","bytes=" + startPos + "-"+ endPos); //设置获取实体数据的范围，如果超过了实体数据的大小会自动返回实际实体数据的大小
                http.setRequestProperty("User-Agent","Mozilla/4.0(compatible;MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; " +
                        ".NET CLR 2.0.50727; .NET CLR 3.5.30729)"); //客户端用户的代理
                http.setRequestProperty("Connection","Keep-Alive"); //使用长连接
                InputStream inputStream = http.getInputStream();    //获取远程连接的输入流
                byte[] buffer = new byte[1024];         //设置本地的数据缓存大小为1MB
                int offset = 0;                         //设置每次读取的数据量
                print("Thread " + this.threadId +" starts to download from position "+ startPos);       //打印该线程开始下载的位置
                RandomAccessFile threadFile = new RandomAccessFile(this.saveFile,"rwd");
                threadFile.seek(startPos);  //文件指针指向开始下载的位置
                while(!downloader.getExited() && (offset = inputStream.read(buffer, 0, 1024)) != -1){   //但用户没有要求停止下载，同时没有到达请求数据的末尾时会一直循环读取数据
                    threadFile.write(buffer, 0, offset);    //直接把数据写入文件中
                    downloadedLength += offset;             //把新下载的已经写入到文件中的数据加入到下载的长度中
                    downloader.update(this.threadId,downloadedLength);//把该线程已经下载的数据长度更新到数据库和内存哈希表中
                    downloader.append(offset);              //把新下载的数据长度加入到已经下载的数据总长度中
                }
                threadFile.close();     //关闭random access 文件流
                inputStream.close();    //关闭输入流
                if (downloader.getExited()){    //下载暂停
                    print("Thread "+this.threadId + " has been paused");
                } else {
                    print("Thread "+this.threadId + " download finish");
                }
                this.finished = true;       //设置完成标志为true，无论是下载完成还是用户主动中断下载
            } catch (Exception e){
                this.downloadedLength = -1; //设置该线程已经下载的长度为-1
                print("Thread "+this.threadId +" :"+e); //打印异常信息
            }
        }
    }
    /**
     * 打印信息
     * @param msg 信息
     */
    private static void print(String msg){
        Log.i(TAG,msg);
    }
    /**
     * 下载是否完成
     */
    public boolean isFinished(){
        return finished;
    }
    /**
     * 已经下载的内容大小
     * @return 如果返回值为-1表示下载失败
     */
    public long getDownloadedLength(){
        return downloadedLength;
    }
}
