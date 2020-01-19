package com.ppvod;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONObject;

public class Uploader {

    private HttpUtil mUtil;
    private String remoteUrl;
    private String apikey;
    private static final int MAX_RETRY = 5;

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String sha1(InputStream in) throws NoSuchAlgorithmException, IOException {
        MessageDigest md5 = MessageDigest.getInstance("SHA-1");
        byte[] buf = new byte[8 * 1024];
        while (true) {
            int r = in.read(buf);
            if (r < 0)
                break;
            md5.update(buf, 0, r);
        }
        byte[] m = md5.digest();// 加密
        return bytesToHex(m);
    }

    public static String sha1(String content) throws NoSuchAlgorithmException, IOException {
        MessageDigest md5 = MessageDigest.getInstance("SHA-1");
        md5.update(content.getBytes());
        byte[] m = md5.digest();// 加密
        return bytesToHex(m);
    }

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

    // JobFuture 内部实现
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
                long startPos = curIdx * chunkSize;
                if (startPos > file.length())
                    break;
                // int endPos = Math.min((curIdx + 1) * chunkSize, (int) file.length());
                long length = chunkSize;
                if (startPos + length > file.length())
                    length = file.length() - startPos;

                try {
                    FileInputStream fin = new FileInputStream(file);
                    SizeLimitInputStream bin = new SizeLimitInputStream(fin, length);
                    bin.skip(startPos);
                    sha1 += sha1(bin);

                    bin.close();
                    fin.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                curIdx++;
            }
            try {
                sha1 = sha1(sha1);
            } catch (Exception e) {
                ;//
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
                // e1.printStackTrace();
                mStatus = STATUS_ERROR;
                return;
            }

            // 上传
            curIdx = 0;
            int chunkRetry = 0;
            while (!Thread.currentThread().isInterrupted()) {

                long startPos = curIdx * chunkSize;
                if (startPos > file.length())
                    break;
                long length = chunkSize;

                if (startPos + length > file.length())
                    length = file.length() - startPos;

                try {
                    root = mUtil.requestForJSON(remoteUrl + "?status=chunkCheck&size=" + length
                            + "&chunkIndex=" + curIdx + "&name=" + sha1);

                    code = root.getInt("ifExist");
                    System.out.println("code is :" + code);
                    if (code == 1) {
                        Log.info("分片" + curIdx + "已经存在,跳过!");
                        curIdx++;
                        continue;
                    }


                    FileInputStream fin = new FileInputStream(file);
                    // SizeLimitInputStream bin = new SizeLimitInputStream(fin, length);
                    fin.skip(startPos);                    
                    JSONObject obj = new JSONObject();
                    obj.put("uniqueFileName", sha1);
                    obj.put("chunk", curIdx + "");
                    obj.put("userId", apikey);
                    Log.info("准备上传:" + length);
                    mUtil.postInputStream(fin, (int)length, remoteUrl, obj, file.getName());
                    // bin.close();
                    fin.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    chunkRetry++;
                    if (chunkRetry >= MAX_RETRY) {
                        Log.debug("分片失败次数已超过最大重试数,上传失败!" + chunkRetry);
                        mStatus = STATUS_ERROR;
                        break;
                    } else {
                        try {
                            Log.debug("分片上传失败,准备重试" + chunkRetry);
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            break;
                        }
                        continue;
                    }
                }
                // 复位重试次数
                chunkRetry = 0;
                curIdx++;
            }

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("被取消!");
                mStatus = STATUS_ERROR;
                return;
            }

            String uniqueFileName = sha1;
            String md5 = uniqueFileName;
            String basename = file.getName().substring(0, file.getName().lastIndexOf("."));
            String extname = file.getName().substring(file.getName().lastIndexOf(".") + 1);
            String result = "";
            try {
                int chunks = (int)(file.length() / chunkSize);
                if(file.length() % chunkSize >0)
                    chunks = chunks +1;
                System.out.println("准备合并分片");
                result = mUtil.request(remoteUrl + "?status=chunksMerge&chunks=" + chunks  + "&md5=" + md5
                        + "&ext=" + extname + "&uniqueFileName=" + uniqueFileName + "&fileoldname=" + basename);
                System.out.println("合并分片完成" + result);
                mStatus = STATUS_FINISH_GENERAL;
            } catch (Exception e) {
                System.out.println("合并分片失败!" + result);
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