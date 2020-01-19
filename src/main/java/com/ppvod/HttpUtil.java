package com.ppvod;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class HttpUtil {
    private static final String TAG = "httputil";
    private OkHttpClient client;

    public HttpUtil() {
        this.client = new OkHttpClient();
    }

    public String request(String surl) throws IOException {
        URL url = new URL(surl);
        String result = "";
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            byte[] data = new byte[1024];
            int r = in.read(data);
            result = new String(data, 0, r, "utf-8");
        } finally {
            urlConnection.disconnect();
        }

        return result;
    }

    public JSONObject requestForJSON(String url) throws IOException {
        String r = request(url);
        if (r == null)
            return null;
        JSONTokener tokener = new JSONTokener(r);
        JSONObject result = null;
        try {
            result = new JSONObject(tokener);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;

    }

    public String postInputStream(final InputStream in, final int length, String url, JSONObject fields,
                                  String filename) throws IOException {
        System.out.println("上传数据" + length + ":" + "" + url + ":" + filename);
        RequestBody mbody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return null;
            }

            @Override
            public long contentLength() {
                return length;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source = null;
                try {
                    source = Okio.source(in);
                    // sink.writeAll(source);
                    sink.write(source, length);
                } catch (Exception e) {
                    if (source != null) {
                        try {
                            source.close();
                        } catch (RuntimeException rethrown) {
                            throw rethrown;
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        };


        MultipartBuilder builder = new MultipartBuilder();
        builder.type(MultipartBuilder.FORM);

        Iterator<String> itera = fields.keys();
        while (itera.hasNext()) {
            String key = itera.next();
            try {
                System.out.println(key +  fields.getString(key));
                builder.addFormDataPart(key, fields.getString(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        builder.addFormDataPart("filename", filename, mbody);

        RequestBody body = builder.build();
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = this.client.newCall(request).execute();
        String result = response.body().string();
        Log.debug("postInputStream返回结果:" + result);

        return result;
    }

}