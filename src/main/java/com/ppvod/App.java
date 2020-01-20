package com.ppvod;



/**
 * Hello world!
 *
 */
public class App {
    private static Log logger = Log.getLogger("App");
    public static void main(String[] args) throws Exception {
        // String file = "d:\\wfsroot\\somebody.mp4";
        String file = "d:\\wfsroot\\xs.mp4";
        // 标准上传
        // Uploader up = new Uploader("http://222.186.50.235:2000/uploads/", "v1", 20 * 1024 * 1024);
        Uploader up = new Uploader("http://localhost:2100/uploads", "abcde", 20 * 1024 * 1024);
        JobFuture future = up.upload(file);
        while(future.getState() == JobFuture.STATUS_RUNNING){
            logger.debug("进度:" + future.progress()); 
            Thread.sleep(500);
        }

    }
}
