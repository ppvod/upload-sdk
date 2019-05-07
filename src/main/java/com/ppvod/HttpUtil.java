package com.ppvod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.json.JSONObject;
import org.json.JSONTokener;

public class HttpUtil {
    private static final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
    static {

        // 设置最大连接数
        connManager.setMaxTotal(5);
        // 设置每个连接的路由数
        connManager.setDefaultMaxPerRoute(20);

    }

    public void postJson(JSONObject logObj, String url) {
        try {

            CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(connManager).build();

            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/json");

            // 解决中文乱码问题
            StringEntity stringEntity = new StringEntity(logObj.toString(), "UTF-8");
            // stringEntity.setContentEncoding("UTF-8");
            // stringEntity.setContentType("application/json");

            httpPost.setEntity(stringEntity);
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {//
                    byte[] a = new byte[1024];
                    int r = response.getEntity().getContent().read(a);
                    return null;
                }
            };
            httpclient.execute(httpPost, responseHandler);

        } catch (Exception e) {
            // System.out.println(e);
        } finally {

        }
    }

    public String request(String url) throws ClientProtocolException, IOException {
        String result = "";

        CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(connManager).build();

        HttpGet httpGet = new HttpGet(url);

        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {//
                byte[] a = new byte[1024];
                int r = response.getEntity().getContent().read(a);
                return new String(a, 0, r);
            }
        };
        Log.info("请求" + url);
        result = httpclient.execute(httpGet, responseHandler);

        return result;
    }

    public JSONObject requestForJSON(String url) throws ClientProtocolException, IOException {
        String r = request(url);
        if (r == null)
            return null;
        JSONTokener tokener = new JSONTokener(r);
        return new JSONObject(tokener);
    }

    public String postInputStream(InputStream in, String url, JSONObject fields, String filename)
            throws ClientProtocolException, IOException {
        String result = "";

        CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(connManager).build();

        HttpPost httpPost = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        Iterator<String> itera = fields.keySet().iterator();
        while (itera.hasNext()) {
            String key = itera.next();
            builder.addTextBody(key, fields.getString(key));
        }
        builder.addBinaryBody("filename", in, ContentType.create("multipart/form-data"), filename);

        HttpEntity entity = builder.build();
        httpPost.setEntity(entity);
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {//
                byte[] a = new byte[1024];
                int r = response.getEntity().getContent().read(a);
                return new String(a, 0, r);
            }
        };
        result = httpclient.execute(httpPost, responseHandler);
        Log.debug("返回结果" + result);

        return result;
    }

}