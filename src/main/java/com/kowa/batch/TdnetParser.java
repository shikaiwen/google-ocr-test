package com.kowa.batch;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TdnetParser {


    public static void main(String[] args) throws Exception{
        getSourceHtml();
    }

    public static String getSourceHtml() throws Exception{



        byte[] buffer = new byte[1024];

        String url = "https://www.release.tdnet.info/onsf/TDJFSearch/TDJFSearch";

        Map<String, String> params = new HashMap<>();
        params.put("t0","20181110");
        params.put("t1", "20181210");
        params.put("m", "0");
        params.put("q", "決算短信");

        Document doc = Jsoup.connect(url).data(params).post();

        Elements trList = doc.select("#contentwrapper table tr");


        for (Element element : trList) {

            // skip header and non xblr tr
            Element xbrlFileUrl = element.selectFirst(".xbrl a");
            if(xbrlFileUrl == null){
                continue;
            }

            String code = element.selectFirst(".code").text();
            String time = element.selectFirst(".time").text();
            String companyname = element.selectFirst(".companyname").text();


            String linkUrl = xbrlFileUrl.attr("href");

            String downloadUrl = "https://www.release.tdnet.info/" + linkUrl;

            ByteArrayOutputStream baos = HttpHelper.getFileContent(downloadUrl);

            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()));

            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null){

                System.out.println(zipEntry.getName());

                String entryName = zipEntry.getName();
                if(entryName.contains("ixbrl.htm") && entryName.contains("Summary")){

                    ByteArrayOutputStream htmlByteArray = new ByteArrayOutputStream();
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        htmlByteArray.write(buffer, 0, len);
                    }
//                    System.out.println(htmlByteArray.toString("UTF8"));
                    IOUtils.write(htmlByteArray.toByteArray(),new FileOutputStream("D:\\tmp\\xblf\\"+ (code + "-" + companyname)+".html"));
                    break;
                }
                zipEntry = zis.getNextEntry();
            }

            System.out.println(downloadUrl);
        }

        return null;
    }


}
