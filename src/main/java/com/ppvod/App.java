package com.ppvod;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) throws Exception {

        // 标准上传
        Uploader up = new Uploader("http://localhost:2100/uploads/", "v1", 20 * 1024 * 1024);
        JobFuture future = up.upload("d:\\wfsroot\\somebody.mp4");
        Thread.sleep(3000);
        System.out.println(future.getStatue());

        // 秒传
        future = up.upload("d:\\wfsroot\\somebody.mp4");
        Thread.sleep(3000);
        System.out.println(future.getStatue());
    }
}
