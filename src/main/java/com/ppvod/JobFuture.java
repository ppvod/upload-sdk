package com.ppvod;

public interface JobFuture {
    static final int STATUS_ERROR = -1;
    static final int STATUS_RUNNING = 1;
    static final int STATUS_FINISH_GENERAL = 10; // 正常上传

    static final int STATUS_FINISH_BIU = 15; // 秒传

    // 取消下载任务
    public void cancel();

    public int getStatue();

}