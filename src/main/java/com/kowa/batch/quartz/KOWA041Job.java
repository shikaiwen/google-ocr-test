package com.kowa.batch.quartz;

import com.sun.mail.pop3.POP3Folder;
import common.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KOWA041Job extends JobWrapper  {

    public static WriteLog04 writeLog = new WriteLog04("Amazon");
    public static Connection conn = null;

    // 確定メール解析用
    public static class Keyword {
        public static class Base {
            public final static  String SUBTOTAL   = "小計";
            public final static  String GRANDTOTAL = "合計";
            public final static  String SHIPPING   = "配送料";
            public final static  String DISCOUNT   = "割引";
            public final static  String PAYMENT    = "支払い方法";
        }
    }


    public static void main(String[] args) throws Exception {
        receive();
    }

    /**
     * 受信
     */
    public static void receive() throws Exception {
        // 接続  サーバー
        //Connection conn = null;
        Statement stmt = null;
        POP3Folder folder = null;
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "pop3");       // 協議
        props.setProperty("mail.pop3.port", "110");             // ポート
        props.setProperty("mail.pop3.host", "211.13.204.15");   // pop3サーバー
        props.setProperty("mail.pop3.filecache.enable", "true");

        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();
        } catch (Exception e) {
            CommonErrEvent04.CommonErrEvent04("kowa041", "0", "接続タイムアウトしました", stmt, writeLog, "", "");
        }

        //  セッション実例 の対象
        Session session = Session.getInstance(props);
        Store store = session.getStore("pop3");
        try {
            store.connect("all@onetime.tokyo", "K5v0jHPN");
        } catch (Exception e) {
            CommonErrEvent04.CommonErrEvent04("kowa041", "1", "メール接続失敗", stmt, writeLog, "", "");
        }

        try {
            // 受信
            folder = (POP3Folder)store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            // メールを解析する
            Message[] messages = folder.getMessages();
            for (int i = 0, count = messages.length; i < count; i++) {
            	parseMessage(stmt, folder, messages[i]);
            	//System.gc();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                //資源を解放する
                folder.close(true);
                store.close();
                if (conn != null) {
                    conn.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                writeLog.closeLog04();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * メールを解析する
     * @param messages メール
     */
    @SuppressWarnings("deprecation")
    public static void parseMessage(Statement stmt, POP3Folder folder, Message message) throws MessagingException, IOException {
        if (message == null) {
            throw new MessagingException("メールは見つからない");
        }
        // 注文確認メール
        String localUid = "";
        String uid = "";
        String title = "";
        String tyuumonbanngou = "";        // 注文番号
        String tyuumonsya = "";            // 注文者
        String tyuumonbi = "";             // 注文日
        String hattyuuMail = "";           // 発注者メール
        String hattyuuhouhou = "";         // 発送方法
        String syoukei = "";               // 小計
        String haisouryou = "";            // 配送料
        String waribiki = "";              // 割引
        String goukei = "";                // 合計
        String siharaihouhou = "";         // 支払い方法
        String hannbaisya = "";            // 販売者
        String asin = "";                  // ASIN
        String syouhinmei = "";            // 商品名
        String suuryou = "";               // 数量
        String tannka = "";                // 単価
        String subtotal = "";              // 小計
        String todokeyoteibiFrom = "";     // 届け予定日From
        String todokeyoteibiTo = "";       // 届け予定日To
        String sousinnsya = "";            // 送信者

        // 発送メール
        String tyuumonbanngou2 = "";       // 注文番号
        String haisougyousya = "";         // 配送業者
        String dannbyouNo = "";            // お問合せ伝票番号
        String todokeyoteibi = "";         // 届け予定日
        String hannbaisya2 = "";           // 販売者
        String syouhinmei2 = "";           // 商品名
        String asin2 = "";                 // ASIN
        String suuryou2 = "";              // 数量

        String sendDatatime = "";          // 受信日時
        String kenmei = "";                // 件名
        String sendName = "";              // 送信者
        String sendCC = "";                // 宛先
        String mailMsg = "";
        String sendYear = "";

        Map<String, String> mapHead = new HashMap<String, String>();
        Map<String, String> mapDetail = new HashMap<String, String>();
        List<Map<String, String>> listDetail = new ArrayList<Map<String, String>>();

        // メールを解析
        MimeMessage msg = null;
        //for (int i = 0, count = messages.length; i < count; i++) {

        try{
            mapHead = null; mapDetail = null; listDetail = null;
            mapHead = new HashMap<String, String>();
            mapDetail = new HashMap<String, String>();
            listDetail = new ArrayList<Map<String, String>>();

            //msg = null;
            msg = (MimeMessage) message;
            // 30日超えたメールを省略する
            int diffDay = CommonUtil.diffWithCurrentDate(msg.getSentDate());
            if (diffDay >= 30) {System.out.println(msg.getSentDate());msg=null;
                return;
            }

            sendDatatime = getSentDate(msg, null);
            kenmei = getSubject(msg);
            sendName = getFrom(msg);
            sendCC = getReceiveAddress(msg, null);
            sendYear = getSentDate(msg, "yyyy");

            // 「UID」を全件取得する
            //localUid = getLocalUid(stmt);
            uid = folder.getUID(message);               // UID
            mailMsg = uid + "," + sendDatatime + "," + kenmei + "," + sendName + "," + sendCC;
System.out.println(mailMsg);
            if (ifUidExists(stmt, uid)) {
            	mailMsg = null;
                return;
            }

            Elements liElements1 = null;
            Elements liElements2 = null;
            Elements liElements3 = null;
            Elements liElements4 = null;
            Elements liElements5 = null;
            Elements liElements6 = null;
            Elements liElements7 = null;

            title = getSubject(msg);                                            // 件名
            sousinnsya = getFrom(msg);                                          // 送信者

            if (title.indexOf("注文の確認") >= 0 && sousinnsya.indexOf("auto-confirm@amazon.co.jp") >= 0) {
                StringBuffer content = new StringBuffer(30);
                getMailTextContent(msg, content);
                Document a = Jsoup.parse(content.toString());

                liElements1 = new Elements();
                liElements2 = new Elements();
                liElements3 = new Elements();
                liElements4 = new Elements();
                liElements5 = new Elements();
                liElements6 = new Elements();
                liElements7 = new Elements();

                liElements1 = a.getElementsByClass("title");
                tyuumonbanngou = liElements1.get(0).text().split("：")[1].trim();                                                                            // 注文番号

                // 重複チェックを行う
                //if (sonnzaiCheck(stmt, tyuumonbanngou)) {
                //    continue;
                //}

                liElements2 = a.getElementsByAttributeValue("id", "summary");
                tyuumonsya = liElements2.select("h3").text().replace("様", "").trim();                                                                 // 注文者

                liElements3 = a.getElementsByAttributeValue("id", "orderDetails");
                tyuumonbi = liElements3.select("span").text().split("：")[1];                                                                          // 注文日

                liElements4 = a.getElementsByAttributeValue("id", "criticalInfo");

                hattyuuMail = getReceiveAddress(msg, null);                                                                                          // 発注者メール

                liElements5 = a.getElementsByAttributeValue("id", "costBreakdown");
                Elements costBreakdown = liElements5.select("tr");
                for(int c=0; c<costBreakdown.size(); c++) {
                    Element trElement = costBreakdown.get(c);
                    Elements tdElements = trElement.select("td");
                    String tdTitle = tdElements.get(0).text().trim();
                    if(tdTitle.indexOf(Keyword.Base.SUBTOTAL) >= 0) {           //小計
                        syoukei = tdElements.get(1).text().replace("￥", "").replace(",", "").trim();
                    } else if(tdTitle.indexOf(Keyword.Base.SHIPPING) >= 0) {    //配送料
                        haisouryou = tdElements.get(1).text().replace("￥", "").replace(",", "").trim();
                    } else if(tdTitle.indexOf(Keyword.Base.DISCOUNT) >= 0) {     //割引
                        waribiki = tdElements.get(1).text().replace("￥", "").replace(",", "").replace("-", "").trim();
                    } else if(tdTitle.indexOf(Keyword.Base.GRANDTOTAL) >= 0) {   // 合計
                        goukei = tdElements.get(1).text().replace("￥", "").replace(",", "").trim();
                    } else if(tdTitle.indexOf(Keyword.Base.PAYMENT) >= 0) {      // 支払方法
                        JSONObject paymentJson = new JSONObject();
                        try {
                            for (int cc=c+1; cc<costBreakdown.size(); cc++) {
                                Element trElementPayment = costBreakdown.get(cc);
                                Elements tdElementsPayment = trElementPayment.select("td");
                                String tdTitlePyment = tdElementsPayment.get(0).text().trim().replace("：", "");
                                if (tdElementsPayment.size() > 1) {
                                    paymentJson.put(tdTitlePyment, tdElementsPayment.get(1).text().replace("￥", "").replace(",", "").trim());
                                }

                                if (tdElementsPayment.get(0).hasClass("end")) {
                                    break;
                                }
                            }
                        } catch (JSONException e) {
                            // TODO 自動生成された catch ブロック
                            e.printStackTrace();
                        }

                        if (paymentJson.length() > 0) {
                            siharaihouhou = paymentJson.toString();
                        }
                    }
                }

                mapHead.put("tyuumonbanngou", tyuumonbanngou);
                mapHead.put("tyuumonsya", tyuumonsya);
                mapHead.put("tyuumonbi", tyuumonbi);
                mapHead.put("hattyuuMail", hattyuuMail);
                mapHead.put("syoukei", syoukei);
                mapHead.put("haisouryou", haisouryou);
                mapHead.put("waribiki", waribiki);
                mapHead.put("goukei", goukei);
                mapHead.put("siharaihouhou", siharaihouhou);

                hattyuuhouhou = "通常配送";
                for (int k = 0; k < liElements4.size(); k++) {
                    Element element1 = liElements4.get(k);
                    Elements liElement4b = element1.select("b");

                    if(liElement4b.size() >= 3) {
                        hattyuuhouhou = liElement4b.get(1).text();                                                                                       // 発送方法
                        String date = liElement4b.get(0).text();
                        if (date.indexOf("-") >= 0) {
                            String[] tempDateFrom = date.split("-")[0].split(",");
                            if (tempDateFrom.length == 2 && checkDate2(tempDateFrom[1])) {
                                todokeyoteibiFrom = sendYear + '/' + tempDateFrom[1].trim();                                   // 届け予定日From
                            }
                            String[] tempDateTo = date.split("-")[1].split(",");
                            if (tempDateTo.length == 2 && checkDate2(tempDateTo[1])) {
                                todokeyoteibiTo = sendYear + '/' + tempDateTo[1].trim();                                       // 届け予定日To
                            }
                        } else {
                            String[] tempDate = date.split(",");
                            if (tempDate.length == 2 && checkDate2(tempDate[1])) {
                                todokeyoteibiFrom = sendYear + '/' + tempDate[1].trim();                                       // 届け予定日From
                                todokeyoteibiTo = todokeyoteibiFrom;                                                           // 届け予定日To
                            }
                        }
                        break;
                    }
                }

                mapHead.put("hattyuuhouhou", hattyuuhouhou);

                if (checkNull(todokeyoteibiFrom)) {
                    todokeyoteibiFrom = todokeyoteibiTo = tyuumonbi;
                }

                liElements6 = a.getElementsByAttributeValue("id", "itemDetails");
                for (int j = 0; j < liElements6.size(); j++) {
                    Element element2 = liElements6.get(j);

                    Elements tdName = element2.select("td.name a");
                    syouhinmei = tdName.get(0).text();                                                                             // 商品名
                    suuryou = tdName.get(0).select("b").text();                                                                    // 数量
                    hannbaisya = element2.select("td.name").text().split("販売：")[1].trim();                                      // 販売者

                    if ("".equals(suuryou)) {
                        suuryou = "1";                                                                                                                   // 数量
                    } else {
                        suuryou = suuryou.replace("点", "").replace("\u00a0", "").trim();                                                                // 数量
                    }

                    tannka = element2.select("td.price strong").text().replace("￥", "").replace(",", "").trim();                                        // 単価
                    subtotal = tannka;
                    if(Integer.valueOf(suuryou) > 1) {
                        subtotal = String.valueOf(Integer.valueOf(tannka) * Integer.valueOf(suuryou));
                    }

                    asin = element2.select("td.name a").get(0).attr("href").toString();
                    int start = asin.indexOf("%2Fdp%2F") + 8;
                    int end = asin.indexOf("%2Fref");
                    asin = asin.substring(start, end);                                                                                           // ASIN

                    mapDetail = new HashMap<String, String>();
                    mapDetail.put("tyuumonbanngou", tyuumonbanngou);
                    mapDetail.put("syouhinmei", syouhinmei);
                    mapDetail.put("hannbaisya", hannbaisya);
                    mapDetail.put("suuryou", suuryou);
                    mapDetail.put("tannka", tannka);
                    mapDetail.put("subtotal", subtotal);
                    mapDetail.put("asin", asin);
                    mapDetail.put("todokeyoteibiFrom", todokeyoteibiFrom);
                    mapDetail.put("todokeyoteibiTo", todokeyoteibiTo);

                    listDetail.add(mapDetail);
                }

                // チェック処理
                if (!"".equals(saveCheck(mapHead, listDetail))) {
                    mailMsg = mailMsg + "," + saveCheck(mapHead, listDetail);
                    CommonErrEvent04.CommonErrEvent04("kowa041", "", mailMsg, stmt, writeLog, kenmei,content.toString());
                    return;
                }
                // 「Amazon発注MT」、「Amazon発注詳細T」の登録処理
                 if (!"".equals(hattyuuSave(stmt, mapHead, listDetail))) {
                    CommonErrEvent04.CommonErrEvent04("kowa041", "", mailMsg, stmt, writeLog, kenmei,content.toString());
                    return;
                }
                // 「AmazonMailMT」テーブルに登録
                saveMailMt(stmt, uid);
                writeLog.writeLog04(mailMsg, true);

            } else if (title.indexOf("発送") >= 0 && (sousinnsya.indexOf("ship-confirm@amazon.co.jp") >= 0 || sousinnsya.indexOf("shipment-tracking@amazon.co.jp") >=0 )) {
                StringBuffer content = new StringBuffer(30);
                getMailTextContent(msg, content);
                Document a = Jsoup.parse(content.toString());

                liElements1 = new Elements();
                liElements2 = new Elements();
                liElements3 = new Elements();
                liElements4 = new Elements();
                liElements5 = new Elements();
                liElements6 = new Elements();

                liElements1 = a.getElementsByClass("title");
                tyuumonbanngou2 = liElements1.get(0).text().split("：")[1];                                                                            // 注文番号
                liElements2 = a.getElementsByAttributeValue("id", "trackingText");
                haisougyousya = liElements2.select("p").text();
                int start1 = haisougyousya.indexOf("お客様の商品は") + 7;
                int end1 = haisougyousya.indexOf("でお届けいたします。");
                haisougyousya = haisougyousya.substring(start1, end1);                                                                                // 配送業者

                liElements3 = a.getElementsByAttributeValue("id", "trackingText");
                dannbyouNo = liElements3.select("p").text();
                int start2 = dannbyouNo.indexOf("お問い合わせ伝票番号は") + 11;
                int end2 = dannbyouNo.indexOf("です。なお、");
                dannbyouNo = dannbyouNo.substring(start2, end2);                                                                                      // お問合せ伝票番号

                // 重複チェックを行う
                //if (hassouSonnzaiCheck(stmt, tyuumonbanngou2, dannbyouNo)) {
                //    continue;
                //}

                liElements4 = a.getElementsByAttributeValue("id", "criticalInfo");
                todokeyoteibi = sendYear + "/" + liElements4.select("strong").get(0).text().split(",")[1].trim();                                                      // 届け予定日

                mapHead.put("tyuumonbanngou2", tyuumonbanngou2);
                mapHead.put("haisougyousya", haisougyousya);
                mapHead.put("dannbyouNo", dannbyouNo);
                mapHead.put("todokeyoteibi", todokeyoteibi);

                liElements5 = a.getElementsByAttributeValue("id", "itemDetails");
                liElements7 = liElements5.select("td.name ul li");
                for (int j = 0; j < liElements7.size(); j++) {
                    Element element = liElements7.get(j);

                    //Element element2 = liElements7.get(l);
                    suuryou2 = element.select("a").select("b").text();
                    syouhinmei2 = element.select("a").text();                                                                                    // 商品名
                    hannbaisya2 = element.html().split("<br>")[1];
                    hannbaisya2 = hannbaisya2.replace("が販売", "").trim();                                                                      // 販売者
                    if ("".equals(suuryou2)) {
                        suuryou2 = "1";                                                                                                          // 数量
                    } else {
                        suuryou2 = suuryou2.replace("点", "").replace("\u00a0", "").trim();                                                      // 数量
                    }

                    asin2 = element.select("a").attr("href").toString();
                    int start3 = asin2.indexOf("%2Fdp%2F") + 8;
                    int end3 = asin2.indexOf("%2Fref%");
                    asin2 = asin2.substring(start3, end3);                                                                                       // ASIN

                    mapDetail = new HashMap<String, String>();
                    mapDetail.put("tyuumonbanngou2", tyuumonbanngou2);
                    mapDetail.put("hannbaisya2", hannbaisya2);
                    mapDetail.put("suuryou2", suuryou2);
                    mapDetail.put("asin2", asin2);
                    mapDetail.put("syouhinmei2", syouhinmei2);

                    listDetail.add(mapDetail);
                }

            // チェック処理
            if (!"".equals(hassousaveCheck(mapHead, listDetail))) {
                mailMsg = mailMsg + "," + hassousaveCheck(mapHead, listDetail);
                CommonErrEvent04.CommonErrEvent04("kowa041", "", mailMsg, stmt, writeLog, kenmei, content.toString());
                return;
            }

            // 「Amazon発送MT」、「Amazon発送詳細T」の登録処理
            if (!"".equals(hassouSave(stmt, mapHead, listDetail))) {
                CommonErrEvent04.CommonErrEvent04("kowa041", "", mailMsg, stmt, writeLog, kenmei, content.toString());
                return;
            }

            // 「Amazon発注MT」、「Amazon発注詳細T」の更新処理
            if (!"".equals(hattyuuUpdate(stmt, mapHead, listDetail))) {
                CommonErrEvent04.CommonErrEvent04("kowa041", "", mailMsg, stmt, writeLog, kenmei, content.toString());
                return;
            }

            // 「AmazonMailMT」テーブルに登録
            saveMailMt(stmt, uid);
            writeLog.writeLog04(mailMsg, true);

            } else {
                // 「AmazonMailMT」テーブルに登録
                saveMailMt(stmt, uid);
            }

        } catch(Exception e){
            writeLog.writeLog04(e.getMessage(),false);
        }

        //}// endforeach
    }

    /**
     * 「UID」を全件取得する
     * @param messages メール
     */
    public static String getLocalUid(Statement stmt) {

        String localUid = "";
        String sql = "";

        try {
            sql = " SELECT STUFF((SELECT ',' + UID FROM AmazonMailMT for xml path('')),1,1,'') UID ";

            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                localUid = rs.getString(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return localUid;
    }

    /**
     * 「UID」を存在チェック
     * @param messages メール
     */
    public static boolean ifUidExists(Statement stmt, String uid) {

    	boolean flag = false;

        try {
        	ResultSet rs = stmt.executeQuery("SELECT UID FROM AmazonMailMT WHERE UID = '" + uid + "'");
        	while (rs.next()) {
                flag = true;
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return flag;
    }

    /**
     * 重複チェックを行う
     * @param
     */
    public static boolean sonnzaiCheck(Statement stmt, String tyuumonbanngou) {

        boolean flag = false;
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM Amazon発注MT WHERE 注文番号 = '" + tyuumonbanngou + "'");
            while (rs.next()) {
                flag = true;
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 重複チェックを行う
     * @param
     */
    public static boolean sonnzaiDetailCheck(Statement stmt, String tyuumonbanngou, String ASIN) {

        boolean flag = false;
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM Amazon発注詳細T WHERE 注文番号 = '" + tyuumonbanngou + "' AND ASIN = '" + ASIN +"'");
            while (rs.next()) {
                flag = true;
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 発送重複チェックを行う
     * @param
     */
    public static boolean hassouSonnzaiCheck(Statement stmt, String tyuumonbanngou, String dannbyouNo) {

        boolean flag = false;
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM Amazon注文発送MT WHERE 注文番号 = '" + tyuumonbanngou + "' AND お問合せ伝票番号 = '" + dannbyouNo + "'");
            while (rs.next()) {
                flag = true;
                break;
            }
        } catch (Exception e) {
        }
        return flag;
    }

    /**
     * 発送重複詳細チェックを行う
     * @param
     */
    public static boolean hassouSonnzaiDetailCheck(Statement stmt, String tyuumonbanngou, String ASIN, String no) {

        boolean flag = false;
        try {
            ResultSet rs = stmt.executeQuery("SELECT 発送NO FROM Amazon注文発送詳細T WHERE 発送NO='"+ no  + "' AND 注文番号 = '" + tyuumonbanngou + "' AND ASIN = '" + ASIN + "'");
            while (rs.next()) {
                flag = true;
                break;
            }
        } catch (Exception e) {
        }
        return flag;
    }

    /**
     * チェックを行う
     * @param
     */
    public static String saveCheck(Map<String, String> map, List<Map<String, String>> list) {

        boolean flag = false;
        String errMsg = "";

        // 注文番号
        if (checkNull(map.get("tyuumonbanngou").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "注文番号がありません";
            }
            flag = true;
        }
        // 注文者
        if (checkNull(map.get("tyuumonsya").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "注文者がありません";
            } else {
                errMsg = errMsg + "," + "注文者がありません";
            }
            flag = true;
        }
        // 注文日
        if (checkNull(map.get("tyuumonbi").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "注文日がありません";
            } else {
                errMsg = errMsg + "," + "注文日がありません";
            }
            flag = true;
        }
        if (checkDate(map.get("tyuumonbi").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "注文日の形式が不正";
            } else {
                errMsg = errMsg + "," + "注文日の形式が不正";
            }
            flag = true;
        }
        // 発注者メール
        if (checkNull(map.get("hattyuuMail").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "発注者メールがありません";
            } else {
                errMsg = errMsg + "," + "発注者メールがありません";
            }
            flag = true;
        }
        if (checkMail(map.get("hattyuuMail").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "発注者メールの形式が不正";
            } else {
                errMsg = errMsg + "," + "発注者メールの形式が不正";
            }
            flag = true;
        }
        // 発送方法
        if (checkNull(map.get("hattyuuhouhou").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "発送方法がありません";
            } else {
                errMsg = errMsg + "," + "発送方法がありません";
            }
            flag = true;
        }
        // 小計
        if (checkNull(map.get("syoukei").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "小計がありません";
            } else {
                errMsg = errMsg + "," + "小計がありません";
            }
            flag = true;
        }
        // 合計
        if (checkNull(map.get("goukei").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "合計がありません";
            } else {
                errMsg = errMsg + "," + "合計がありません";
            }
            flag = true;
        }
        // 支払い方法
        if (checkNull(map.get("siharaihouhou").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "支払い方法がありません";
            } else {
                errMsg = errMsg + "," + "支払い方法がありません";
            }
            flag = true;
        }

        for (int i = 0; i < list.size(); i++) {
            list.get(i);
            // 販売者
            if (checkNull(list.get(i).get("hannbaisya").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "販売者がありません";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "販売者がありません";
                }
                flag = true;
            }
            // ASIN
            if (checkNull(list.get(i).get("asin").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "ASINがありません";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "ASINがありません";
                }
                flag = true;
            }
            if (chkNumEng(list.get(i).get("asin").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "ASINの形式が不正";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "ASINの形式が不正";
                }
                flag = true;
            }
            // 商品名
            if (checkNull(list.get(i).get("syouhinmei").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "商品名がありません";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "商品名がありません";
                }
                flag = true;
            }
            // 数量
            if (checkNull(list.get(i).get("suuryou").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "数量がありません";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "数量がありません";
                }
                flag = true;
            }
            // 単価
            if (checkNull(list.get(i).get("tannka").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "単価がありません";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "単価がありません";
                }
                flag = true;
            }
            // 届け予定日From
            if (checkNull(list.get(i).get("todokeyoteibiFrom").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "届け予定日Fromがありません";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "届け予定日Fromがありません";
                }
                flag = true;
            }
            // 届け予定日To
            if (checkNull(list.get(i).get("todokeyoteibiTo").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "届け予定日Toがありません";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "届け予定日Toがありません";
                }
                flag = true;
            }
        }
        return errMsg;
    }

    /**
     * 発送チェックを行う
     * @param
     */
    public static String hassousaveCheck(Map<String, String> map, List<Map<String, String>> list) {

        boolean flag = false;
        String errMsg = "";

        // 注文番号
        if (checkNull(map.get("tyuumonbanngou2").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "注文番号がありません";
            }
            flag = true;
        }
        // 配送業者
        if (checkNull(map.get("haisougyousya").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "配送業者がありません";
            } else {
                errMsg = errMsg + "," + "配送業者がありません";
            }
            flag = true;
        }
        // お問合せ伝票番号
        if (checkNull(map.get("dannbyouNo").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "お問合せ伝票番号がありません";
            } else {
                errMsg = errMsg + "," + "お問合せ伝票番号がありません";
            }
            flag = true;
        }
        // 届け予定日
        if (checkNull(map.get("todokeyoteibi").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "届け予定日がありません";
            } else {
                errMsg = errMsg + "," + "届け予定日がありません";
            }
            flag = true;
        }
        if (checkDate(map.get("todokeyoteibi").toString())) {
            if ("".equals(errMsg)) {
                errMsg = "届け予定日の形式が不正";
            } else {
                errMsg = errMsg + "," + "届け予定日の形式が不正";
            }
            flag = true;
        }

        for (int i = 0; i < list.size(); i++) {
            list.get(i);
            // 販売者
            if (checkNull(list.get(i).get("hannbaisya2").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "販売者がありません";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "販売者がありません";
                }
                flag = true;
            }
            // ASIN
            if (checkNull(list.get(i).get("asin2").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "ASINがありません";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "ASINがありません";
                }
                flag = true;
            }
            if (chkNumEng(list.get(i).get("asin2").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "ASINの形式が不正";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "ASINの形式が不正";
                }
                flag = true;
            }
            // 数量
            if (checkNull(list.get(i).get("suuryou2").toString())) {
                if ("".equals(errMsg)) {
                    errMsg = String.valueOf(i + 1) + "数量がありません";
                } else {
                    errMsg = errMsg + "," + String.valueOf(i + 1) + "数量がありません";
                }
                flag = true;
            }
        }
        return errMsg;
    }

    /**
     * 「Amazon発注MT」、「Amazon発注詳細T」の登録処理
     *
     * @return
     */
    public static String hattyuuSave(Statement stmt, Map<String, String> map, List<Map<String, String>> list) {

        String sql = "";
        try {
            conn.setAutoCommit(false);

            if (!sonnzaiCheck(stmt, map.get("tyuumonbanngou"))) {
	            sql = insertHattyuuMt(map);
	            stmt.execute(sql);
            }

            for (int i = 0; i < list.size(); i++) {
                Map<String, String> item = list.get(i);
                if (!sonnzaiDetailCheck(stmt, item.get("tyuumonbanngou"), item.get("asin"))) {
	                sql = insertHattyuuDetailMt(item);
	                stmt.execute(sql);
                }
            }

            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                // TODO 自動生成された catch ブロック
                e1.printStackTrace();
            }
            return e.getMessage();
        }
        return "";
    }
    /**
     * 「Amazon発注MT」、「Amazon発注詳細T」の更新処理
     *
     * @return
     */
    public static String hattyuuUpdate(Statement stmt, Map<String, String> map, List<Map<String, String>> list) {

        String sql = "";
        try {
        	conn.setAutoCommit(false);

            sql = updateHattyuuMt(map);
            stmt.execute(sql);

            for (int i = 0; i < list.size(); i++) {
                sql = updateHattyuuDetailMt(list.get(i));
                stmt.execute(sql);
            }
            conn.commit();
        } catch (Exception e) {
        	try {
                conn.rollback();
            } catch (SQLException e1) {
                // TODO 自動生成された catch ブロック
                e1.printStackTrace();
            }
            return e.getMessage();
        }
        return "";
    }
    /**
     * 「Amazon注文発送MT」、「Amazon注文発送詳細T」の登録処理
     *
     * @return
     */
    public static String hassouSave(Statement stmt, Map<String, String> map, List<Map<String, String>> list) {

        String sql = "";
        try {
            conn.setAutoCommit(false);

            if (!hassouSonnzaiCheck(stmt, map.get("tyuumonbanngou2"), map.get("dannbyouNo"))) {
                sql = insertHassouMt(map);
                stmt.execute(sql);
            }

            ResultSet rs = stmt.executeQuery("SELECT 発送NO FROM Amazon注文発送MT WHERE 注文番号 = '" + map.get("tyuumonbanngou2") + "' AND お問合せ伝票番号 = '" + map.get("dannbyouNo") + "'");
            String no = "";
            while (rs.next()) {
                no = String.valueOf(rs.getObject(1));
            }

            for (int i = 0; i < list.size(); i++) {
                Map<String, String> item = list.get(i);
                if (!hassouSonnzaiDetailCheck(stmt, item.get("tyuumonbanngou2"), item.get("asin2"), no)){
                    sql = insertHassouDetailMt(list.get(i), no);
                    stmt.execute(sql);
                }
            }

            conn.commit();
        } catch (Exception e) {
        	try {
                conn.rollback();
            } catch (SQLException e1) {
                // TODO 自動生成された catch ブロック
                e1.printStackTrace();
            }
            return e.getMessage();
        }
        return "";
    }
    /**
     * 「AmazonMailMT」の登録処理
     *
     * @return
     */
    public static String saveMailMt(Statement stmt, String uid) {

        String sql = "";
        try {
            sql = " INSERT INTO AmazonMailMT ( ";
            sql += " UID ";
            sql += " ,登録ユーザ ";
            sql += " ,登録プログラム ";
            sql += " ,登録日時 ";
            sql += " ,更新ユーザ ";
            sql += " ,更新プログラム ";
            sql += " ,更新日時 ";
            sql += " ) VALUES ( ";
            sql += "'" + uid + "'";
            sql += ", 'batch'";
            sql += ", 'kowa041'";
            sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
            sql += ", 'batch'";
            sql += ", 'kowa041'";
            sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
            sql += " )";
            stmt.execute(sql);

        } catch (Exception e) {
            return e.getMessage();
        }
        return "";
    }
    /**
     * 「Amazon発注MT」登録SQL
     *
     * @return
     */
    public static String insertHattyuuMt(Map<String, String> map) {
        String sql = "";

        sql = " INSERT INTO Amazon発注MT ( ";
        sql += " 注文番号 ";
        sql += " ,注文者 ";
        sql += " ,注文日 ";
        sql += " ,発注者メール ";
        sql += " ,発送方法 ";
        sql += " ,小計 ";
        sql += " ,配送料 ";
        sql += " ,割引 ";
        sql += " ,合計 ";
        sql += " ,支払い方法 ";
        sql += " ,ステータス ";
        sql += " ,登録ユーザ ";
        sql += " ,登録プログラム ";
        sql += " ,登録日時 ";
        sql += " ,更新ユーザ ";
        sql += " ,更新プログラム ";
        sql += " ,更新日時 ";
        sql += " ) VALUES ( ";
        sql += "'" + CommonUtil.escapeSql(map.get("tyuumonbanngou")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("tyuumonsya")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("tyuumonbi")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("hattyuuMail")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("hattyuuhouhou")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("syoukei")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("haisouryou")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("waribiki")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("goukei")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("siharaihouhou")) + "'";
        sql += ",'1'";
        sql += ", 'batch'";
        sql += ", 'kowa041'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += ", 'batch'";
        sql += ", 'kowa041'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " )";

        return sql;
    }

    /**
     * 「Amazon発注詳細T」登録SQL
     *
     * @return
     */
    public static String insertHattyuuDetailMt(Map<String, String> map) {
        String sql = "";

        sql = " INSERT INTO Amazon発注詳細T ( ";
        sql += " 注文番号 ";
        sql += " ,販売者 ";
        sql += " ,ASIN ";
        sql += " ,商品名 ";
        sql += " ,数量 ";
        sql += " ,単価 ";
        sql += " ,小計 ";
        sql += " ,届け予定日From ";
        sql += " ,届け予定日To ";
        sql += " ,ステータス ";
        sql += " ,登録ユーザ ";
        sql += " ,登録プログラム ";
        sql += " ,登録日時 ";
        sql += " ,更新ユーザ ";
        sql += " ,更新プログラム ";
        sql += " ,更新日時 ";
        sql += " ) VALUES ( ";
        sql += "'" + CommonUtil.escapeSql(map.get("tyuumonbanngou")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("hannbaisya")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("asin")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("syouhinmei")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("suuryou")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("tannka")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("subtotal")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("todokeyoteibiFrom")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("todokeyoteibiTo")) + "'";
        sql += ",'1'";
        sql += ", 'batch'";
        sql += ", 'kowa041'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += ", 'batch'";
        sql += ", 'kowa041'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " )";

        return sql;
    }

    /**
     * 「Amazon発注MT」更新SQL
     *
     * @return
     */
    public static String updateHattyuuMt(Map<String, String> map) {
        String sql = "";

        sql = " UPDATE Amazon発注MT SET ";
        sql += " ステータス = '2'";
        sql += " ,更新ユーザ = 'batch'"  ;
        sql += " ,更新プログラム = 'kowa041'";
        sql += " ,更新日時 = '"+ CommonUtil.getCurrentDateTime() + "'";
        sql += " WHERE ";
        sql += " 注文番号 = '" + CommonUtil.escapeSql(map.get("tyuumonbanngou2")) + "'";

        return sql;
    }
    /**
     * 「Amazon発注詳細T」更新SQL
     *
     * @return
     */
    public static String updateHattyuuDetailMt(Map<String, String> map) {
        String sql = "";

        sql = " UPDATE Amazon発注詳細T SET ";
        sql += " ステータス = '2' ";
        sql += " ,更新ユーザ = 'batch'"  ;
        sql += " ,更新プログラム = 'kowa041'";
        sql += " ,更新日時 = '"+ CommonUtil.getCurrentDateTime() + "'";
        sql += " WHERE ";
        sql += " 注文番号 = '" + CommonUtil.escapeSql(map.get("tyuumonbanngou2")) + "'";
        sql += " AND 販売者 = '" + CommonUtil.escapeSql(map.get("hannbaisya2")) + "'";
        sql += " AND ASIN = '" + CommonUtil.escapeSql(map.get("asin2")) + "'";

        return sql;
    }

    /**
     * 「Amazon注文発送MT」SQL
     *
     * @return
     */
    public static String insertHassouMt(Map<String, String> map) {
        String sql = "";

        sql = " INSERT INTO Amazon注文発送MT ( ";
        sql += " 注文番号 ";
        sql += " ,配送業者 ";
        sql += " ,お問合せ伝票番号 ";
        sql += " ,届け予定日 ";
        sql += " ,ステータス ";
        sql += " ,登録ユーザ ";
        sql += " ,登録プログラム ";
        sql += " ,登録日時 ";
        sql += " ,更新ユーザ ";
        sql += " ,更新プログラム ";
        sql += " ,更新日時 ";
        sql += " ) VALUES ( ";
        sql += "'" + CommonUtil.escapeSql(map.get("tyuumonbanngou2")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("haisougyousya")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("dannbyouNo")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("todokeyoteibi")) + "'";
        sql += ",'1'";
        sql += ", 'batch'";
        sql += ", 'kowa041'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += ", 'batch'";
        sql += ", 'kowa041'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " )";

        return sql;
    }
    /**
     * 「Amazon発送詳細T」SQL
     *
     * @return
     */
    public static String insertHassouDetailMt(Map<String, String> map, String No) {
        String sql = "";

        sql = " INSERT INTO Amazon注文発送詳細T ( ";
        sql += " 発送NO ";
        sql += " ,注文番号 ";
        sql += " ,販売者 ";
        sql += " ,ASIN ";
        sql += " ,数量 ";
        sql += " ,登録ユーザ ";
        sql += " ,登録プログラム ";
        sql += " ,登録日時 ";
        sql += " ,更新ユーザ ";
        sql += " ,更新プログラム ";
        sql += " ,更新日時 ";
        sql += " ) VALUES ( ";
        sql += "'" + No + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("tyuumonbanngou2")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("hannbaisya2")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("asin2")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("suuryou2")) + "'";
        sql += ", 'batch'";
        sql += ", 'kowa041'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += ", 'batch'";
        sql += ", 'kowa041'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " )";

        return sql;
    }
    /**
     * 必須チェック処理
     *
     * @return
     */
    public static Boolean checkNull(String obj) {

        boolean flag = false;

        // 文字列の場合
        if (obj == null || "".equals(obj.toString().trim())) {
            flag = true;
        }

        return flag;
    }
    /**
     * 形式チェック処理
     *
     * @return
     */
    public static Boolean checkDate(String str) {

        boolean flag = false;

        // 半角文字列のチェック
        if (!StringUtils.isEmpty(str)) {
            if (str.length() != str.getBytes().length) {
                flag = true;
            }

            // 日付チェック
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
                format.setLenient(false);
                format.parse(str);
            } catch (ParseException e) {
                flag = true;
            }
        }

        return flag;
    }
    /**
     * 形式チェック処理
     *
     * @return
     */
    public static Boolean checkDate2(String str) {

        boolean flag = false;

        // 半角文字列のチェック
        if (!StringUtils.isEmpty(str)) {
            if (str.length() != str.getBytes().length) {
                flag = true;
            }

            // 日付チェック
            try {
                SimpleDateFormat format = new SimpleDateFormat("MM/dd");
                format.setLenient(false);
                format.parse(str);
            } catch (ParseException e) {
                flag = true;
            }
        }

        return flag;
    }
    /**
     * メール形式チェック処理
     *
     * @return
     */
    public static Boolean checkMail(String str) {
        Pattern emailPattern = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
        Matcher matcher = emailPattern.matcher(str);
        if(matcher.find()){
            return false;
        }
        return true;
    }
    /**
     * 半角英数字のチェック
     *
     * @param str
     * @return
     */
    public static boolean chkNumEng(String str) {

        boolean chkFlg = false;
        String numEng = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-";
        for (int i = 0; i < str.length(); i++) {
            chkFlg = (numEng.indexOf(str.charAt(i)) < 0);
            if (chkFlg) {
                break;
            }
        }
        return chkFlg;
    }
    /**
     * 件名
     * @param msg
     * @return 件名
     */
    public static String getSubject(MimeMessage msg) throws UnsupportedEncodingException, MessagingException {
        return MimeUtility.decodeText(msg.getSubject());
    }
    /**
     * 送信者
     * @param msg
     * @return 送信者
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    public static String getFrom(MimeMessage msg) throws MessagingException, UnsupportedEncodingException {
        String from = "";
        Address[] froms = msg.getFrom();
        if (froms.length < 1)
            throw new MessagingException("送信者なし!");

        InternetAddress address = (InternetAddress) froms[0];
        String person = address.getPersonal();
        if (person != null) {
            person = MimeUtility.decodeText(person) + " ";
        } else {
            person = "";
        }
        from = person + "<" + address.getAddress() + ">";

        return from;
    }
    /**
     * 発注者メール
     * <p>Message.RecipientType.TO  </p>
     * <p>Message.RecipientType.CC  </p>
     * <p>Message.RecipientType.BCC </p>
     * @param msg
     * @param type
     * @return
     * @throws MessagingException
     */
    public static String getReceiveAddress(MimeMessage msg, Message.RecipientType type) throws MessagingException {
        StringBuffer receiveAddress = new StringBuffer();
        Address[] addresss = null;
        if (type == null) {
            addresss = msg.getAllRecipients();
        } else {
            addresss = msg.getRecipients(type);
        }

        if (addresss == null || addresss.length < 1)
            throw new MessagingException("発注者メールなし!");
        for (Address address : addresss) {
            InternetAddress internetAddress = (InternetAddress)address;
            receiveAddress.append(internetAddress.toUnicodeString()).append(",");
        }

        receiveAddress.deleteCharAt(receiveAddress.length()-1);

        return receiveAddress.toString();
    }
    /**
     * 受信日時
     * @param msg
     * @return yyyy年MM月dd日 E HH:mm
     * @throws MessagingException
     */
    public static String getSentDate(MimeMessage msg, String pattern) throws MessagingException {
        Date receivedDate = msg.getSentDate();
        if (receivedDate == null)
            return "";

        if (pattern == null || "".equals(pattern))
            pattern = "yyyy年MM月dd日 E HH:mm ";

        return new SimpleDateFormat(pattern).format(receivedDate);
    }
    /**
     * 获得メールのテキストの  内容
     * @param part
     * @param content
     * @throws MessagingException
     * @throws IOException
     */
    public static void getMailTextContent(Part part, StringBuffer content) throws MessagingException, IOException {
        boolean isContainTextAttach = part.getContentType().indexOf("name") > 0;
        if (part.isMimeType("text/*") && !isContainTextAttach) {
            content.append(part.getContent().toString());
        } else if (part.isMimeType("message/rfc822")) {
            getMailTextContent((Part)part.getContent(),content);
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            int partCount = multipart.getCount();
            for (int i = 0; i < partCount; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                getMailTextContent(bodyPart,content);
            }
        }
    }
    /**
     * テキストデコード
     * @param encodeText
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String decodeText(String encodeText) throws UnsupportedEncodingException {
        if (encodeText == null || "".equals(encodeText)) {
            return "";
        } else {
            return MimeUtility.decodeText(encodeText);
        }
    }
}
