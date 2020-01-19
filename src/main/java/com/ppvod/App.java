package com.ppvod;



/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws Exception {
        // String file = "d:\\wfsroot\\69jOG1RR.mkv";
        String file = "d:\\wfsroot\\somebody.mp4";
        // 标准上传
        // Uploader up = new Uploader("http://222.186.50.235:2000/uploads/", "v1", 20 * 1024 * 1024);
        Uploader up = new Uploader("http://iamluodong.vicp.net:2000/uploads/", "abcde", 20 * 1024 * 1024);
        JobFuture future = up.upload(file);
        Thread.sleep(3000);
        System.out.println(future.getStatue());

    }
}
