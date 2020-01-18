package com.ppvod;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;

public class Uploader {

    private HttpUtil mUtil;
    private String remoteUrl;
    private String apikey;
    private static final int MAX_RETRY = 5;

    // 分片尺寸
    private int chunkSize = 1024 * 1024 * 10;

    /**
     * @param remoteUrl 远程服务器url
     * @param key       api秘钥
     * @param chunkSize 分片尺寸
     */
    public Uploader(String remoteUrl, String key, int chunkSize) {
        this.chunkSize = chunkSize;
        this.mUtil = new HttpUtil();
        this.remoteUrl = remoteUrl;
        this.apikey = key;
    }

    public JobFuture upload(String path) {
        InnerFuture f = new InnerFuture(path);
        f.start();
        return f;
    }

    //JobFuture 内部实现
    class InnerFuture implements JobFuture {
        private int mStatus = JobFuture.STATUS_RUNNING;
        private Thread mThrd;
        private String path;

        public void cancel() {
            if (mThrd != null) {
                try {
                    mThrd.interrupt();
                    mThrd = null;
                } catch (Exception e) {

                }
            }
        }

        InnerFuture(String path) {
            this.path = path;
        }

        public void start() {
            mThrd = new Thread(new Runnable() {

                public void run() {
                    upload();
                }
            });
            mThrd.start();
        }

        private void upload() {
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                Log.info("文件不存在或者不是一个标准文件!");
                mStatus = JobFuture.STATUS_ERROR;
                return;
            }

            int curIdx = 0;
            String sha1 = "";
            while (!Thread.currentThread().isInterrupted()) {
                int startPos = curIdx * chunkSize;
                int endPos = Math.min((curIdx + 1) * chunkSize, (int) file.length());

                try {
                    FileInputStream fin = new FileInputStream(file);
                    SizeLimitInputStream bin = new SizeLimitInputStream(fin, endPos - startPos);
                    bin.skip(startPos);
                    sha1 += DigestUtils.sha1Hex(bin);
                    bin.close();
                    fin.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (endPos >= file.length())
                    break;
                curIdx++;
            }
            if (Thread.currentThread().isInterrupted())
                return;
            // 检测指定的md5资源是否存在
            JSONObject root;
            int code = 0;
            try {
                root = mUtil.requestForJSON(remoteUrl + "?status=md5Check&uploadkey=" + apikey + "&md5=" + sha1);
                code = root.getInt("ifExist");
                if (code == 1) {
                    mStatus = JobFuture.STATUS_FINISH_BIU;
                    Log.info("秒传完成" + path);
                    return;
                }
            } catch (Exception e1) {
                e1.printStackTrace();
                mStatus = STATUS_ERROR;
                return;
            }

            // 上传
            curIdx = 0;
            int chunkRetry = 0;
            while (!Thread.currentThread().isInterrupted()) {

                int startPos = curIdx * chunkSize;
                int endPos = Math.min((curIdx + 1) * chunkSize, (int) file.length());

                try {
                    root = mUtil.requestForJSON(remoteUrl + "?status=chunkCheck&uploadkey=" + apikey + "&size="
                            + (endPos - startPos) + "&chunkIndex=" + curIdx);

                    code = root.getInt("ifExist");
                    if (code == 1) {

                        Log.info("分片" + curIdx + "已经存在,跳过!");
                        continue;
                    }

                    FileInputStream fin = new FileInputStream(file);
                    SizeLimitInputStream bin = new SizeLimitInputStream(fin, endPos - startPos);
                    bin.skip(startPos);
                    JSONObject obj = new JSONObject();
                    obj.put("uniqueFileName", sha1);
                    obj.put("chunk", curIdx + "");

                    mUtil.postInputStream(bin, endPos - startPos,remoteUrl,   obj, file.getName());
                    bin.close();
                    fin.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    chunkRetry++;
                    if (chunkRetry >= MAX_RETRY) {
                        mStatus = STATUS_ERROR;
                        break;
                    }
                }
                // 复位重试次数
                chunkRetry = 0;

                if (endPos >= file.length())
                    break;
                curIdx++;
            }
            if (Thread.currentThread().isInterrupted())
                return;

            String uniqueFileName = sha1;
            String md5 = uniqueFileName;
            String basename = file.getName().substring(0, file.getName().lastIndexOf("."));
            String extname = file.getName().substring(file.getName().lastIndexOf(".") + 1);
            try {
                mUtil.request(remoteUrl + "?status=chunksMerge&chunks=" + (curIdx + 1) + "&md5=" + md5 + "&ext="
                        + extname + "&uniqueFileName=" + uniqueFileName + "&fileoldname=" + basename);
                mStatus = STATUS_FINISH_GENERAL;
            } catch (Exception e) {
                e.printStackTrace();
                mStatus = STATUS_ERROR;
            }

        }

        public int getStatue() {
            return mStatus;
        }

    }

    public static void main(String[] args) {
        Uploader up = new Uploader("http://localhost:2100/uploads/", "v1", 20 * 1024 * 1024);
        JobFuture future = up.upload("d:\\wfsroot\\somebody.mp4");

    }
}