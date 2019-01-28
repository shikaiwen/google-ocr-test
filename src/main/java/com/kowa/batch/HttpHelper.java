package com.kowa.batch;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;

public class HttpHelper {

    public static void main(String[] args)throws Exception {
        downloadFile("https://www.nyk.com/release/blank/kt/__icsFiles/afieldfile/2018/10/31/1809J_1.pdf");
    }



    public static ByteArrayOutputStream getFileContent(String url) throws Exception{


        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet(url);

//        String fileName = new File(url).getName();

//        String filePath = "d://" + fileName;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        CloseableHttpResponse response = httpclient.execute(httpget);
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();

                BufferedOutputStream bos = new BufferedOutputStream(baos);
                int inByte;
                while((inByte = instream.read()) != -1){
                    bos.write(inByte);
                }
                instream.close();
                bos.close();
            }
        } finally {
            response.close();
        }

        return baos;
    }


    public static void downloadFile(String url) throws Exception{


        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpget = new HttpGet(url);

        String fileName = new File(url).getName();
        String filePath = "d://" + fileName;

        CloseableHttpResponse response = httpclient.execute(httpget);
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();


                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                int inByte;
                while((inByte = instream.read()) != -1){
                    bos.write(inByte);
                }

                instream.close();
                bos.close();
            }
        } finally {
            response.close();
        }
    }
}
