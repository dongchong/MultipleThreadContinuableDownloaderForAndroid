package com.example.dongdong.multiplethreadcontinuabledownloaderforandroid;

/**
 * Created by dongdong on 2015/6/14.
 */
public interface DownloadProgressListener {
    /**
     * 下载进度的监听方法，获取和处理下载点的数据大小
     * @param size 数据大小
     * @author 董崇
     */
    public void onDownloadSize(int size);
}
