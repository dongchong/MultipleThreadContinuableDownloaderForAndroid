package com.example.dongdong.multiplethreadcontinuabledownloaderforandroid;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dongdong on 2015/6/5.
 * @author 董崇
 */
public class FileDownloader {
    private static final String TAG = "FileDownloader"; //设置标签，方便logcat的记录
    private static final int RESPONSEOK = 200;          //设置响应码为200，既访问成功
    private Context context;                            //应用程序的上下文对象
    private FileService fileService;                    //获取本地数据库业务的bean
    private boolean exited;                             //停止下载的标志
    private int downloadedSize = 0;                     //已经下载的文件的长度
    private int fileSize = 0;                           //原始文件的长度
    private DownloadThread[] threads;                   //根据线程数设置本地的线程池
    private File saveFile;                              //数据保存到本地文件
    private Map<Integer,Integer> data = new ConcurrentHashMap<Integer,Integer>();   //缓存各线程下载的长度
    private int block;                                  //每条线程的下载长度
    private String downloadUrl;                         //下载的路径

    /**
     * 获取线程数
     */
    public int getThreadSize(){
        return threads.length;                          //根据数组的长度返回线程数
    }
    /**
     * 退出下载
     */
    public void exit(){
        this.exited = true;                             //退出下载设置exited为true
    }
    public boolean getExited(){
        return this.exited;
    }

    /**
     * 获取文件的大小
     */
    public int getFileSize(){
        return fileSize;
    }

    /**
     * 累计已经下载的大小
     */
    public synchronized void append(int size){
        downloadedSize += size;                     //把实时下载的文件长度加入到总下载长度中来
    }

    /**
     * 更新制定线程最后的下载位置
     * @param threadId 线程ID
     * @param  pos 最后下载的位置
     */
    public synchronized void update(int threadId,int pos){
        this.data.put(threadId,pos);//把制定线程的ID赋予最新的下载长度，以前的值会直接被覆盖掉
        this.fileService.update(this.downloadUrl,threadId,pos);
    }

    /**
     * 构建文件的下载构造器
     * @param downloadUrl 下载的路径
     * @param fileSaveDir 文件保存的目录
     * @param threadNum 下载的线程数
     */
    public FileDownloader(Context context,String downloadUrl,File fileSaveDir,int threadNum){
        try {
            this.context = context;         //对上下文的内容进行赋值
            this.downloadUrl = downloadUrl; //对下载的路径赋值
            fileService = new FileService(this.context);    //实例化数据操作业务的bean，此处需要使用Context，此处的数据库是应用程序的私有
            URL url = new URL(this.downloadUrl);            //根据下载的路径实例化URL
            if (!fileSaveDir.exists()) fileSaveDir.mkdirs();//如果指定的文件不存在，创建目录，此处可以创建多层目录
            this.threads = new DownloadThread[threadNum];   //根据下载的线程数创建下载线程池
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();  //建立一个远程连接句柄，此时尚未建立真正的连接
            conn.setConnectTimeout(5*1000);                 //设置连接超时的时间为5秒
            conn.setRequestMethod("GET");                   //设置请求方式为get get是向服务器索取数据，post则是想服务器发送数据，相比来说post更加安全
            conn.setRequestProperty("Accept","image/gif,image/jpeg,image/pjpeg,image/pjpeg,application/x-shockwave-flash," +
                    "application/xaml+xml,application/vnd.ms-xpsdocument,application/x-ms-xbap,application/x-ms-application," +
                    "application/vnd.ms-excel,application/vnd.ms-powerpoint,application/msword,*/*");   //设置客户端可接受的媒体类型
            conn.setRequestProperty("Accept-Language","zh-CN"); //设置客户端语言
            conn.setRequestProperty("Referer",downloadUrl);     //设置请求来源的界面，便于服务器进行来源的统计
            conn.setRequestProperty("Charset","UTF-8");         //设置客户端编码
            conn.setRequestProperty("User-Agent","Mozilla/4.0(compatible;MSIE 8.0;Windows NT 5.2;Trident/4.0;.NET CLR 1.1.4322; " +
                    ".NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");   //设置用户代理
            conn.setRequestProperty("Connection","Keep-Alive");
            //设置Connection的样式
            conn.connect(); //和远程资源建立真正的连接，但尚未返回的数据量
            printResponseHeader(conn);  //答应返回的HTTP头字段集合
            if (conn.getResponseCode() == RESPONSEOK) {     //此处的请求会打开返回流并获得返回的状态码，用于检查是否请求成功，当返回码为200时执行下面的代码
                this.fileSize = conn.getContentLength(); //根据响应获取文件大小
                if (this.fileSize <= 0){                //当文件长度小于零时抛出异常说文件size有误
                    throw new RuntimeException("未知的文件大小");
                }
                String filename = getFileName(conn);               //获取文件名称
                this.saveFile = new File(fileSaveDir,filename);     //根据文件目录和文件名保存文件
                Map<Integer,Integer> loaddata = fileService.getData(downloadUrl);   //获取下载的记录
                if (loaddata.size()>0){     //如果存在下载记录
                    for (Map.Entry<Integer,Integer> entry : loaddata.entrySet()) {
                        data.put(entry.getKey(),entry.getValue());  //把各条线程已经下载完的数据长度存放到data中
                    }
                }
                if (this.data.size() == this.threads.length){       //如果已经下载的线程数和现在设置的线程数相同则计算所有线程已经下载的数据总长度
                    for (int i = 0; i < this.threads.length; i++){
                        this.downloadedSize += this.data.get(i+1);
                    }
                    print("已经下载的长度"+this.downloadedSize+"个字节");//打印出已经下载的数据的总和
                }
                this.block = (this.fileSize % this.threads.length) == 0?this.fileSize/this.threads.length:this.fileSize/this.threads.length+1;
            }else {
                print("服务器错误：" + conn.getResponseCode() + conn.getResponseMessage());    //打印错误
                throw new RuntimeException("服务器应答出错");                //抛出运行时异常： 服务器回应错误
            }
        } catch(Exception e){
            print(e.toString());    //打印出错误并显示在控制台上
            throw new RuntimeException("不能连接到该URL地址");    //抛出运行时的连接异常
        }
    }
    /**
     * 获取文件名
     */
    private String getFileName(HttpURLConnection conn) {
        String filename = this.downloadUrl.substring(this.downloadUrl.lastIndexOf('/') + 1);    //从下载路径的字符串中获取文件名称，此处得到的是Music.mp3文件名
        if (filename == null || "".equals(filename.trim())){    //如果获取不到文件的名称
            for (int i = 0;; i++) {
                String mine = conn.getHeaderField(i);   //从返回的流中获取特定的索引头字段值
                if (mine == null) break;                //如果遍历到了返回头末尾处，退出循环
                if ("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase())){     //获取content-disposition返回头字段，里面可能会包含文件名
                    Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());  //使用正则表达式查询文件名
                    if (m.find()) return m.group(1);    //如果有符合正则表达式规则的字符串
                }
            }
            filename = UUID.randomUUID() + ".tmp";  //由网卡上的标识数字（每个网卡上都有唯一的标识号）及CPU时钟的唯一数字生成的一个16字节的二进制数作为文件名
        }
        return filename;
    }
    /**
     * 开始下载文件
     * @param listener 监听下载数量的变化，如果不需要了解实时下载的数量，可以设置为null
     * @return 已下载的文件大小
     * @throws java.lang.Exception
     */
    public int download(DownloadProgressListener listener) throws Exception {
        try {
            RandomAccessFile randOut = new RandomAccessFile(this.saveFile,"rwd");   //此类的实例支持对随机访问文件的读取和写入。随机访问文件的行为类似存储在文件系统中的一个大型 byte 数组。存在指向该隐含数组的光标或索引，称为文件指针；
                                                                                    //"rwd"   打开以便读取和写入，对于 "rw"，还要求对文件内容的每个更新都同步写入到底层存储设备。
            if (this.fileSize > 0) randOut.setLength(this.fileSize);    //设置文件的大小
            randOut.close();            //关闭文件，设置生效
            URL url = new URL(this.downloadUrl);
            if (this.data.size() != this.threads.length){           //如果原来的未曾下载或者原来下载的线程数与现在的线程数不一致
                this.data.clear();          //移除Map中全部的元素，使其制空
                for (int i = 0; i < this.threads.length; i++) {     //遍历线程池
                    this.data.put(i+1,0);   //初始化每条线程已经下载的数据为0
                }
            }
            for (int i = 0; i < this.threads.length; i++) {         //开启线程进行下载
                int downloadedLength = this.data.get(i+1);          //通过特定的线程ID获取该线程已经下载的数据长度
                if (downloadedLength < this.block && this.downloadedSize < this.fileSize){      //判断线程是否已经完成下载，否则继续下载
                    this.threads[i] = new DownloadThread(this,url,this.saveFile,this.block,this.data.get(i+1),i+1); //初始化特定的ID线程
                    this.threads[i].setPriority(7);     //设置优先级为7
                    this.threads[i].start();            //启动线程

                }else {
                    this.threads[i] = null;             //表明该线程已经完成下载任务
                }
            }
            fileService.delete(this.downloadUrl);       //如果存在下载记录，删除它们然后重新添加
            fileService.save(this.downloadUrl,this.data);   //把已经下载的实时数据写入数据库
            boolean notFinished = true;                 //下载未完成
            while(notFinished) {        //循环判断所有线程是否下载完成
                Thread.sleep(900);
                notFinished = false;    //假定全部线程下载完成
                for (int i = 0; i < this.threads.length; i++) {
                    if (this.threads[i] != null && !this.threads[i].isFinished()){  //如果发现线程未完成下载
                        notFinished = true; //设置标志为下载未完成
                        if (this.threads[i].getDownloadedLength() == -1){   //如果下载失败，再重新在已经下载的数据长度的基础上进行下载
                            this.threads[i] = new DownloadThread(this,url,this.saveFile,this.block,this.data.get(i+1),i+1); //重新开辟下载线程
                            this.threads[i].setPriority(7); //设置线程的优先级
                            this.threads[i].start();        //开始下载线程
                        }
                    }
                }
                if(listener != null) listener.onDownloadSize(this.downloadedSize);  //通知目前已经下载完成的数据长度
            }
            if (downloadedSize == this.fileSize) fileService.delete(this.downloadUrl);  //下载完成删除记录
        }catch (Exception e){
            print(e.toString());    //打印错误
            throw new RuntimeException("File downloads error"); //抛出文件下载的异常
        }
        return this.downloadedSize;
    }
    /**
     * 获取HTTP响应头字段
     * @param http HttpURLConnection 对象
     * @return 返回头字段LinkedHashMap
     */
    public static Map<String,String> getHttpResponseHeader(HttpURLConnection http) {
        Map<String,String> header = new LinkedHashMap<String,String>();//使用LinkedHashMap保证写入和遍历的时候的顺序相同，而且允许空值的存在
        for (int i = 0;; i++) {     //此处为无限循环，因为不知道头字段的数量
            String fieldValue = http.getHeaderField(i);//getHeaderField(int n)用于返回第n个头字段的值
            if (fieldValue == null) break;  //如果第i个字段没有值了，则表明头字段的部分已经循环完毕，此处使用了break退出循环
            header.put(http.getHeaderFieldKey(i),fieldValue);   //getHeadFieldKey(int n)用于返回第n个头字段的键
        }
        return header;
    }
    /**
     * 打印HTTP头字段
     * @param http HttpURLConnection对象
     */
    public static void  printResponseHeader(HttpURLConnection http) {
        Map<String,String> header = getHttpResponseHeader(http);    //获取HTTP的响应头字段
        for(Map.Entry<String,String> entry : header.entrySet()){    //foreach循环的方式遍历获取头字段的值，遍历的循环和输入的顺序相同
            String key = entry.getKey() != null ? entry.getKey()+":" : "";  //当有键的时候获取键，没有则返回空
            print("键和值的组合： 键为："+key+" 值为："+ entry.getValue());   //答应键和值的组合

        }
    }
    /**
     * 打印信息
     * @param msg 信息字符串
     */
    private static void print(String msg){
        Log.i(TAG,msg);     //使用LogCat方式打印信息
    }
}
