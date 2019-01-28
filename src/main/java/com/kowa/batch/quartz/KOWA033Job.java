package com.kowa.batch.quartz;


import common.*;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class KOWA033Job extends JobWrapper {


    /**
     * @param args
     */

    public static WriteLog writeLog = new WriteLog("ORDER");
    public static WriteErrCSV writeErrCSV = new WriteErrCSV("ORDER");
    public static String[] errString = new String[3];
    public static boolean flag = true;
    public static Calendar nowDate1 = Calendar.getInstance();
    public static Calendar nowDate2 = Calendar.getInstance();
    public static Calendar nowDate3 = Calendar.getInstance();
    public static Calendar nowDate4 = Calendar.getInstance();
    public static String nowDate = CommonUtil.getCurrentDateTime();
    public static String urlFlag1 = "";
    public static String urlFlag2 = "";
    public static String urlFlag3 = "";
    public static String[] shCodeArray;
    public static String shCode = "";
    public static HashMap<String, HashMap<String,String>> orderList = new HashMap<>();

    public static void main(String[] args) {
        Connection conn = null;
        Statement stmt = null;
        File file = null;
        try {
            try {
                conn = DBUtil.getConnection();
                stmt = conn.createStatement();
            } catch (Exception e) {
                CommonErrEvent.commonErrEvent("", "kowa033", "0", "", "注文伝票情報取得WebAPI", "", null, stmt, writeLog, writeErrCSV, null);
                flag = false;
                return;
            }

            //rebuildErrorOrderDetail(conn);

            file = new File(Constants.CSV_ROOT_PATH + "ORDER\\err.csv");
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = null;
                while((line = reader.readLine()) != null){
                    String item[] = line.split(",");
                    String kbn = item[0].replace("\"", "");
                    String url = item[1].replace("\"", "");
                    int count = Integer.valueOf(item[2].replace("\"", ""));
                    if ("1".equals(kbn)) {
                        errString[0] = "1";
                        errString[1] = url;
                        errString[2] = String.valueOf(count + 1);
                        if (count <= 2) {
                            loopFirst(url, stmt);
                        }
                    } else if ("2".equals(kbn)) {
                        errString[0] = "2";
                        errString[1] = url;
                        errString[2] = String.valueOf(count + 1);
                        if (count <= 2) {
                            loopSecond(url, stmt);
                        }
                    } else if ("3".equals(kbn)) {
                        errString[0] = "3";
                        errString[1] = url;
                        errString[2] = String.valueOf(count + 1);
                        if (count <= 2) {
                            loopThird(url, stmt);
                        }
                    }
                }
                reader.close();
            }

            //String updated_at_fr = "2017-08-08+12:30:00";
            // 作成日時(Fr)
            String updated_at_fr = CommonUtil.getBatachFromDateTime(-7);
            // 作成日時(To)
            String updated_at_to = CommonUtil.getBatachToDateTime();
            System.out.println(updated_at_fr);System.out.println(updated_at_to);
            String tyumonMd5 = toMD5("account=1131&updated_at_fr=" + updated_at_fr + "&updated_at_to=" + updated_at_to + "KOWA");
            // 注文伝票情報URL
            String urlTyumon = "https://crossmall.jp/webapi2/get_order?account=1131&updated_at_fr=" + updated_at_fr + "&updated_at_to=" + updated_at_to + "&signing="+ tyumonMd5;
            System.out.println(urlTyumon);
            errString[0] = "1";
            errString[1] = urlTyumon;
            errString[2] = String.valueOf(0);

            loopFirst(urlTyumon, stmt);

            // 売上数更新
            String time = nowDate.substring(11, 19);
            if (time.compareTo("03:00:00") >=0 && time.compareTo("03:07:00") <=0) {
                deleteUriageMT(stmt);
                insertAllUriageMT(stmt);
            } else {
                // 該当商品コード毎
                if (!"".equals(shCode)) {
                    shCodeArray = shCode.split(",");
                    for (int i = 0; i < shCodeArray.length; i ++) {
                        if (!"".equals(shCodeArray[i])) {
                            deleteUriageMTByShCode(shCodeArray[i], stmt);
                            insertUriageMTByShCode(shCodeArray[i], stmt);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                writeLog.closeLog();
                if (flag) {
                    file.delete();
                    writeErrCSV.reNameCsv("ORDER");
                } else {
                    writeErrCSV.deleteCsv("ORDER");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void loopFirst(String urlTyumon, Statement stmt) {

        // 注文伝票情報
        String jsonTyumon = "";
        String orderNumber = "";
        String tyumonSuccessFlag = "";
        String tyumonErrMsg = "";
        String tyumonResponse = "";
        Boolean tymonCheckFlag = false;
        // 注文詳細情報
        String urlTyumonMeisai = "";
        String tyumonMeisaiMd5 = "";

        try {
            // 注文伝票情報取得
            jsonTyumon = loadJSON(urlTyumon);
            if ("".equals(jsonTyumon)) {
                if ("".equals(urlFlag1)) {
                    CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "3", "", "注文伝票情報取得WebAPI", "", errString, stmt, writeLog, writeErrCSV, true);
                    urlFlag1 = urlTyumon;
                } else if (!urlFlag1.contains(urlTyumon)) {
                    CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "3", "", "注文伝票情報取得WebAPI", "", errString, stmt, writeLog, writeErrCSV, true);
                    urlFlag1 = urlFlag1 + urlTyumon;
                } else {
                    CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "3", "", "注文伝票情報取得WebAPI", "", errString, stmt, writeLog, writeErrCSV, false);
                }
                return;
            }
            List<Map<String, String>> listTyumon = readStringXmlOutTyumon(jsonTyumon);
            Map<String, String> mapTyumon = new HashMap<String, String>();
            for (int j = 0; j < listTyumon.size(); j++) {
                mapTyumon = listTyumon.get(j);
                Iterator itersTyumon = mapTyumon.keySet().iterator();
                String tyumonMsg = "";

                tyumonSuccessFlag = mapTyumon.get("GetStatus").toString();
                tyumonErrMsg = mapTyumon.get("Message").toString();

                if ("success".equals(tyumonSuccessFlag)) {

                    orderNumber = mapTyumon.get("order_number").toString();

                    // レスポンス内容
                    tyumonResponse = mapTyumon.get("order_number") + "," + mapTyumon.get("order_date") + "," + mapTyumon.get("shop_code") + "," + mapTyumon.get("shop_name") + "," + mapTyumon.get("order_code") + "," + mapTyumon.get("client_section_name1") + "," +
                            mapTyumon.get("client_section_name2") + "," + mapTyumon.get("client_name") + "," + mapTyumon.get("client_kana") + "," + mapTyumon.get("client_zip") + "," + mapTyumon.get("client_address1") + "," + mapTyumon.get("client_address2") + "," + mapTyumon.get("client_tel") + "," +
                            mapTyumon.get("client_mail") + "," + mapTyumon.get("terminal_type") + "," + mapTyumon.get("ship_section_name1") + "," + mapTyumon.get("ship_section_name2") + "," + mapTyumon.get("ship_name") + "," + mapTyumon.get("ship_kana") + "," + mapTyumon.get("ship_zip") + "," +
                            mapTyumon.get("ship_address1") + "," + mapTyumon.get("ship_address2") + "," + mapTyumon.get("ship_tel") + "," + mapTyumon.get("delivery_number") + "," + mapTyumon.get("delivery_type_code") + "," + mapTyumon.get("delivery_type_name") + "," + mapTyumon.get("delivery_code") + "," +
                            mapTyumon.get("delivery_name") + "," + mapTyumon.get("delivery_req_date") + "," + mapTyumon.get("delivery_time_code") + "," + mapTyumon.get("delivery_time_name") + "," + mapTyumon.get("delivery_date") + "," + mapTyumon.get("payment_code") + "," + mapTyumon.get("payment_name") + "," +
                            mapTyumon.get("order_option1") + "," + mapTyumon.get("order_option2") + "," + mapTyumon.get("order_option3") + "," + mapTyumon.get("order_option4") + "," + mapTyumon.get("order_option5") + "," + mapTyumon.get("order_option6") + "," + mapTyumon.get("order_memo") + "," +
                            mapTyumon.get("comment") + "," + mapTyumon.get("subtotal_price") + "," + mapTyumon.get("tax_price") + "," + mapTyumon.get("carriage_price") + "," + mapTyumon.get("cash_on_delivery") + "," + mapTyumon.get("option1_fee") + "," + mapTyumon.get("option2_fee") + "," +
                            mapTyumon.get("point") + "," + mapTyumon.get("coupon") + "," + mapTyumon.get("total_price") + "," + mapTyumon.get("phase_name") + mapTyumon.get("check_mark1") + "," + mapTyumon.get("check_mark2") + mapTyumon.get("check_mark3") + "," +
                            mapTyumon.get("cancel_flag") + "," + mapTyumon.get("bundle_flag") + "," + mapTyumon.get("bundle_ahead_number") + "," + mapTyumon.get("created_at") + "," + mapTyumon.get("updated_at");

                    // 管理番号必須チェック
                    if (checkNull(mapTyumon.get("order_number").toString())) {
                        tyumonMsg = "<注文伝票情報取得API>管理番号が取得できません";
                        tymonCheckFlag = true;
                    }
                    // 注文日時形式チェック
                    if (checkDate(mapTyumon.get("order_date").toString())) {
                        if ("".equals(tyumonMsg)) {
                            tyumonMsg = "<注文伝票情報取得API>注文日時の形式不正です";
                        } else {
                            tyumonMsg = tyumonMsg + "," + "<注文伝票情報取得API>注文日時の形式不正です";
                        }

                        tymonCheckFlag = true;
                    }
                    // 注文番号必須チェック
                    if (checkNull(mapTyumon.get("order_code").toString())) {

                        if ("".equals(tyumonMsg)) {
                            tyumonMsg = "<注文伝票情報取得API>注文番号が取得できません";
                        } else {
                            tyumonMsg = tyumonMsg + "," + "<注文伝票情報取得API>注文番号が取得できません";
                        }

                        tymonCheckFlag = true;
                    }

                    if (tymonCheckFlag) {
                        if ("".equals(urlFlag1)) {
                            CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "2", tyumonMsg, "注文伝票情報取得WebAPI", tyumonResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag1 = urlTyumon;
                        } else if (!urlFlag1.contains(urlTyumon)) {
                            CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "2", tyumonMsg, "注文伝票情報取得WebAPI", tyumonResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag1 = urlFlag1 + urlTyumon;
                        } else {
                            CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "2", tyumonMsg, "注文伝票情報取得WebAPI", tyumonResponse, errString, stmt, writeLog, writeErrCSV, false);
                        }
                        continue;
                    }

                    // テーブル「注文MT」に更新/登録処理
                    boolean isExistsOrder = isExistsOrder(mapTyumon, stmt);
                    tyumonMsg = saveTyumon(isExistsOrder, mapTyumon, stmt);

                    if ("".equals(tyumonMsg)) {
                        writeLog.writeLog(CommonUtil.getCurrentDateTime() + "," + "注文伝票情報取得WebAPI" + "," + urlTyumon + ",success", true);
                    } else {
                        if ("".equals(urlFlag1)) {
                            CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "1", tyumonMsg, "注文伝票情報取得WebAPI", tyumonResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag1 = urlTyumon;
                        } else if (!urlFlag1.contains(urlTyumon)) {
                            CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "1", tyumonMsg, "注文伝票情報取得WebAPI", tyumonResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag1 = urlFlag1 + urlTyumon;
                        } else {
                            CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "1", tyumonMsg, "注文伝票情報取得WebAPI", tyumonResponse, errString, stmt, writeLog, writeErrCSV, false);
                        }
                    }

                    tyumonMeisaiMd5 = toMD5("account=1131&order_number=" + orderNumber + "KOWA");

                    // 注文詳細情報URL
                    urlTyumonMeisai = "https://crossmall.jp/webapi2/get_order_detail?account=1131&order_number=" + orderNumber + "&signing=" + tyumonMeisaiMd5;
                    errString[0] = "2";
                    errString[1] = urlTyumonMeisai;
                    errString[2] = String.valueOf(0);

                    loopSecond(urlTyumonMeisai, stmt);

                } else {
                    if ("".equals(urlFlag1)) {
                        CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "2", tyumonErrMsg, "注文伝票情報取得WebAPI", tyumonResponse, errString, stmt, writeLog, writeErrCSV, true);
                        urlFlag1 = urlTyumon;
                    } else if (!urlFlag1.contains(urlTyumon)) {
                        CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "2", tyumonErrMsg, "注文伝票情報取得WebAPI", tyumonResponse, errString, stmt, writeLog, writeErrCSV, true);
                        urlFlag1 = urlFlag1 + urlTyumon;
                    } else {
                        CommonErrEvent.commonErrEvent(urlTyumon, "kowa033", "2", tyumonErrMsg, "注文伝票情報取得WebAPI", tyumonResponse, errString, stmt, writeLog, writeErrCSV, false);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void loopSecond(String urlTyumonMeisai, Statement stmt) {

        // 注文詳細情報
        String orderNumber = "";
        String jsonTyumonMeisai = "";
        String lineNo = "";
        String componentFlag = "";
        String tyumonMeisaiSuccessFlag = "";
        String tyumonMeisaiErrMsg = "";
        String tyumonMeisaiResponse = "";
        Boolean tyumonMeisaiCheckFlag = false;
        // 注文構成品情報
        String urlTyumonGousei = "";
        String tyumonGouseiMd5 = "";

        try {
            // 注文詳細情報取得
            jsonTyumonMeisai = loadJSON(urlTyumonMeisai);
            if ("".equals(jsonTyumonMeisai)) {
                if ("".equals(urlFlag2)) {
                    CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "3", "", "注文詳細情報取得WebAPI", "", errString, stmt, writeLog, writeErrCSV, true);
                    urlFlag2 = urlTyumonMeisai;
                } else if (!urlFlag2.contains(urlTyumonMeisai)) {
                    CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "3", "", "注文詳細情報取得WebAPI", "", errString, stmt, writeLog, writeErrCSV, true);
                    urlFlag2 = urlFlag2 + urlTyumonMeisai;
                } else {
                    CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "3", "", "注文詳細情報取得WebAPI", "", errString, stmt, writeLog, writeErrCSV, false);
                }
                return;
            }

            List<Map<String, String>> listTyumonMeisai = readStringXmlOutTyumonMeisai(jsonTyumonMeisai);
            Map<String, String> mapTyumonMeisai = new HashMap<String, String>();
            for (int i = 0; i < listTyumonMeisai.size(); i ++) {
                mapTyumonMeisai = listTyumonMeisai.get(i);
                String tyumonMeisaiMsg = "";

                tyumonMeisaiSuccessFlag = mapTyumonMeisai.get("GetStatus").toString();
                tyumonMeisaiErrMsg = mapTyumonMeisai.get("Message").toString();

                if ("success".equals(tyumonMeisaiSuccessFlag)) {

                    orderNumber = mapTyumonMeisai.get("order_number").toString();
                    lineNo = mapTyumonMeisai.get("line_no").toString();
                    componentFlag = mapTyumonMeisai.get("component_flag").toString();

                    if ("false".equals(CheckUtil.chkNumberHalf(mapTyumonMeisai.get("item_code").toString(), ""))) {
                        continue;
                    }

                    // レスポンス内容
                    tyumonMeisaiResponse = mapTyumonMeisai.get("order_number") + "," + mapTyumonMeisai.get("line_no") + "," + mapTyumonMeisai.get("item_code") + "," + mapTyumonMeisai.get("item_name") + "," + mapTyumonMeisai.get("attribute1_code") + "," + mapTyumonMeisai.get("attribute1_name") + "," +
                            mapTyumonMeisai.get("attribute2_code") + "," + mapTyumonMeisai.get("attribute2_name") + "," + mapTyumonMeisai.get("item_option1_label") + "," + mapTyumonMeisai.get("item_option1_labe2") + "," + mapTyumonMeisai.get("item_option1_labe3") + "," + mapTyumonMeisai.get("item_option1_labe4") + "," +
                            mapTyumonMeisai.get("item_option1_labe5") + "," + mapTyumonMeisai.get("item_option1_labe6") + "," + mapTyumonMeisai.get("item_option1_labe7") + "," + mapTyumonMeisai.get("item_option1_labe8") + "," + mapTyumonMeisai.get("item_option1_labe9") + "," + mapTyumonMeisai.get("item_option1_label0") + "," +
                            mapTyumonMeisai.get("item_option1_label1") + "," + mapTyumonMeisai.get("item_option1_label2") + "," + mapTyumonMeisai.get("item_option1_label3") + "," + mapTyumonMeisai.get("item_option1_label4") + "," + mapTyumonMeisai.get("item_option1_label5") + "," + mapTyumonMeisai.get("item_option1_label6") + "," +
                            mapTyumonMeisai.get("item_option1_label7") + "," + mapTyumonMeisai.get("item_option1_label8") + "," + mapTyumonMeisai.get("item_option1_label9") + "," + mapTyumonMeisai.get("item_option1_labe20") + "," + mapTyumonMeisai.get("item_option1") + "," + mapTyumonMeisai.get("item_option2") + "," +
                            mapTyumonMeisai.get("item_option3") + "," + mapTyumonMeisai.get("item_option4") + "," + mapTyumonMeisai.get("item_option5") + "," + mapTyumonMeisai.get("item_option6") + "," + mapTyumonMeisai.get("item_option7") + "," + mapTyumonMeisai.get("item_option8") + "," + mapTyumonMeisai.get("item_option9") + "," +
                            mapTyumonMeisai.get("item_option10") + "," + mapTyumonMeisai.get("item_option11") + "," + mapTyumonMeisai.get("item_option12") + "," + mapTyumonMeisai.get("item_option13") + "," + mapTyumonMeisai.get("item_option14") + "," + mapTyumonMeisai.get("item_option15") + "," + mapTyumonMeisai.get("item_option16") + "," +
                            mapTyumonMeisai.get("item_option17") + "," + mapTyumonMeisai.get("item_option18") + "," + mapTyumonMeisai.get("item_option19") + "," + mapTyumonMeisai.get("item_option20") + "," + mapTyumonMeisai.get("amount") + "," + mapTyumonMeisai.get("unit_price") + "," + mapTyumonMeisai.get("amount_price") + "," +
                            mapTyumonMeisai.get("tax_type") + "," + mapTyumonMeisai.get("freight_type") + "," + mapTyumonMeisai.get("free_item_code") + "," + mapTyumonMeisai.get("component_flag") + "," + mapTyumonMeisai.get("tax_rate") + "," + mapTyumonMeisai.get("jan_cd");


                    // 管理番号
                    if (checkNull(mapTyumonMeisai.get("order_number").toString())) {
                        tyumonMeisaiMsg = "<注文詳細情報取得API>管理番号が取得できません";
                        tyumonMeisaiCheckFlag = true;
                    }
                    // 商品コード
                    if (checkNull(mapTyumonMeisai.get("item_code").toString())) {
                        if ("".equals(tyumonMeisaiMsg)) {
                            tyumonMeisaiMsg = "<注文詳細情報取得API>商品コードが取得できません";
                        } else {
                            tyumonMeisaiMsg = tyumonMeisaiMsg + "," + "<注文詳細情報取得API>商品コードが取得できません";
                        }
                        tyumonMeisaiCheckFlag = true;
                    }
                    // 数量
                    if (checkNull(mapTyumonMeisai.get("amount").toString())) {
                        if ("".equals(tyumonMeisaiMsg)) {
                            tyumonMeisaiMsg = "<注文詳細情報取得API>数量が取得できません";
                        } else {
                            tyumonMeisaiMsg = tyumonMeisaiMsg + "," + "<注文詳細情報取得API>数量が取得できません";
                        }
                        tyumonMeisaiCheckFlag = true;
                    }
                    // 単価
                    if (checkNull(mapTyumonMeisai.get("unit_price").toString())) {
                        if ("".equals(tyumonMeisaiMsg)) {
                            tyumonMeisaiMsg = "<注文詳細情報取得API>単価が取得できません";
                        } else {
                            tyumonMeisaiMsg = tyumonMeisaiMsg + "," + "<注文詳細情報取得API>単価が取得できません";
                        }
                        tyumonMeisaiCheckFlag = true;
                    }

                    if (tyumonMeisaiCheckFlag) {
                        if ("".equals(urlFlag2)) {
                            CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "2", tyumonMeisaiMsg, "注文詳細情報取得WebAPI", tyumonMeisaiResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag2 = urlTyumonMeisai;
                        } else if (!urlFlag2.contains(urlTyumonMeisai)) {
                            CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "2", tyumonMeisaiMsg, "注文詳細情報取得WebAPI", tyumonMeisaiResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag2 = urlFlag2 + urlTyumonMeisai;
                        } else {
                            CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "2", tyumonMeisaiMsg, "注文詳細情報取得WebAPI", tyumonMeisaiResponse, errString, stmt, writeLog, writeErrCSV, false);
                        }
                        continue;
                    }

                    // テーブル「注文詳細T」に登録処理/「注文詳細オプションT」に登録処理
                    tyumonMeisaiMsg = saveTyumonMeisai(mapTyumonMeisai, stmt);

                    if ("".equals(tyumonMeisaiMsg)) {
                        writeLog.writeLog(CommonUtil.getCurrentDateTime() + "," + "注文詳細情報取得WebAPI" + "," + urlTyumonMeisai + ",success", true);
                    } else {
                        if ("".equals(urlFlag2)) {
                            CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "1", tyumonMeisaiMsg, "注文詳細情報取得取得WebAPI", tyumonMeisaiResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag2 = urlTyumonMeisai;
                        } else if (!urlFlag2.contains(urlTyumonMeisai)) {
                            CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "1", tyumonMeisaiMsg, "注文詳細情報取得取得WebAPI", tyumonMeisaiResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag2 = urlFlag2 + urlTyumonMeisai;
                        } else {
                            CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "1", tyumonMeisaiMsg, "注文詳細情報取得取得WebAPI", tyumonMeisaiResponse, errString, stmt, writeLog, writeErrCSV, false);
                        }
                    }

                    if ("1".equals(componentFlag)) {

                        tyumonGouseiMd5 = toMD5("account=1131&order_number=" + orderNumber + "line_no=" + lineNo + "KOWA");

                        // 注文構成品情報
                        urlTyumonGousei = "https://crossmall.jp/webapi2/get_order_component?account=1131&order_number=" + orderNumber + "line_no=" + lineNo + "&signing=" + tyumonGouseiMd5;

                        errString[0] = "3";
                        errString[1] = urlTyumonGousei;
                        errString[2] = String.valueOf(0);

                        loopThird(urlTyumonGousei, stmt);
                    }
                } else {
                    if ("".equals(urlFlag2)) {
                        CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "2", tyumonMeisaiErrMsg, "注文詳細情報取得WebAPI", tyumonMeisaiResponse, errString, stmt, writeLog, writeErrCSV, true);
                        urlFlag2 = urlTyumonMeisai;
                    } else if (!urlFlag2.contains(urlTyumonMeisai)) {
                        CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "2", tyumonMeisaiErrMsg, "注文詳細情報取得WebAPI", tyumonMeisaiResponse, errString, stmt, writeLog, writeErrCSV, true);
                        urlFlag2 = urlFlag2 + urlTyumonMeisai;
                    } else {
                        CommonErrEvent.commonErrEvent(urlTyumonMeisai, "kowa033", "2", tyumonMeisaiErrMsg, "注文詳細情報取得WebAPI", tyumonMeisaiResponse, errString, stmt, writeLog, writeErrCSV, false);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loopThird(String urlTyumonGousei, Statement stmt) {

        // 注文構成品情報
        String jsonTyumonGousei = "";
        String tyumonGouseiSuccessFlag = "";
        String tyumonGouseiErrMsg = "";
        String tyumonGouseiResponse = "";
        Boolean tyumonGouseiCheckFlag = false;

        try {
            // 注文構成品情報取得
            jsonTyumonGousei = loadJSON(urlTyumonGousei);
            if ("".equals(jsonTyumonGousei)) {
                if ("".equals(urlFlag3)) {
                    CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "3", "", "注文構成品情報取得WebAPI", "", errString, stmt, writeLog, writeErrCSV, true);
                    urlFlag3 = urlTyumonGousei;
                } else if (!urlFlag3.contains(urlTyumonGousei)) {
                    CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "3", "", "注文構成品情報取得WebAPI", "", errString, stmt, writeLog, writeErrCSV, true);
                    urlFlag3 = urlFlag3 + urlTyumonGousei;
                } else {
                    CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "3", "", "注文構成品情報取得WebAPI", "", errString, stmt, writeLog, writeErrCSV, false);
                }
                return;
            }

            List<Map<String, String>> listTyumonGousei = readStringXmlOutTyumonGousei(jsonTyumonGousei);
            Map<String, String> mapTyumonGousei = new HashMap<String, String>();
            for (int k = 0; k < listTyumonGousei.size(); k++) {
                mapTyumonGousei = listTyumonGousei.get(k);
                String tyumonGouseiMsg = "";

                tyumonGouseiSuccessFlag = mapTyumonGousei.get("GetStatus").toString();
                tyumonGouseiErrMsg = mapTyumonGousei.get("Message").toString();

                if ("success".equals(tyumonGouseiSuccessFlag)) {

                    if ("false".equals(CheckUtil.chkNumberHalf(mapTyumonGousei.get("item_code").toString(), ""))) {
                        continue;
                    }

                    // レスポンス内容
                    tyumonGouseiResponse = mapTyumonGousei.get("order_number") + "," + mapTyumonGousei.get("line_no") + "," + mapTyumonGousei.get("set_item_code") + "," + mapTyumonGousei.get("item_code") + "," + mapTyumonGousei.get("item_name") + "," + mapTyumonGousei.get("attribute1_code") + "," +
                            mapTyumonGousei.get("attribute1_name") + "," + mapTyumonGousei.get("attribute2_code") + "," + mapTyumonGousei.get("attribute2_name") + "," + mapTyumonGousei.get("component_count") + "," + mapTyumonGousei.get("jan_cd");

                    // 管理番号
                    if (checkNull(mapTyumonGousei.get("order_number").toString())) {
                        tyumonGouseiMsg = "<注文構成品情報取得API>管理番号が取得できません";
                        tyumonGouseiCheckFlag = true;
                    }
                    // 商品コード
                    if (checkNull(mapTyumonGousei.get("item_code").toString())) {
                        if ("".equals(tyumonGouseiMsg)) {
                            tyumonGouseiMsg = "<注文構成品情報取得API>商品コードが取得できません";
                        } else {
                            tyumonGouseiMsg = tyumonGouseiMsg + "," + "<注文構成品情報取得API>商品コードが取得できません";
                        }
                        tyumonGouseiCheckFlag = true;
                    }

                    if (tyumonGouseiCheckFlag) {
                        if ("".equals(urlFlag3)) {
                            CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "2", tyumonGouseiMsg, "注文構成品情報取得WebAPI", tyumonGouseiResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag3 = urlTyumonGousei;
                        } else if (!urlFlag3.contains(urlTyumonGousei)) {
                            CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "2", tyumonGouseiMsg, "注文構成品情報取得WebAPI", tyumonGouseiResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag3 = urlFlag3 + urlTyumonGousei;
                        } else {
                            CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "2", tyumonGouseiMsg, "注文構成品情報取得WebAPI", tyumonGouseiResponse, errString, stmt, writeLog, writeErrCSV, false);
                        }
                        continue;
                    }

                    // テーブル「注文構成品情報T」に更新/登録処理
                    tyumonGouseiMsg = saveTyumonGousei(mapTyumonGousei, stmt);

                    if ("".equals(tyumonGouseiMsg)) {
                        writeLog.writeLog(CommonUtil.getCurrentDateTime() + "," + "注文構成品情報取得WebAPI" + "," + urlTyumonGousei + ",success", true);
                    } else {
                        if ("".equals(urlFlag3)) {
                            CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "1", tyumonGouseiMsg, "注文構成品情報取得WebAPI", tyumonGouseiResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag3 = urlTyumonGousei;
                        } else if (!urlFlag3.contains(urlTyumonGousei)) {
                            CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "1", tyumonGouseiMsg, "注文構成品情報取得WebAPI", tyumonGouseiResponse, errString, stmt, writeLog, writeErrCSV, true);
                            urlFlag3 = urlFlag3 + urlTyumonGousei;
                        } else {
                            CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "1", tyumonGouseiMsg, "注文構成品情報取得WebAPI", tyumonGouseiResponse, errString, stmt, writeLog, writeErrCSV, false);
                        }
                    }

                } else {
                    if ("".equals(urlFlag3)) {
                        CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "2", tyumonGouseiErrMsg, "注文構成品情報取得WebAPI", tyumonGouseiResponse, errString, stmt, writeLog, writeErrCSV, true);
                        urlFlag3 = urlTyumonGousei;
                    } else if (!urlFlag3.contains(urlTyumonGousei)) {
                        CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "2", tyumonGouseiErrMsg, "注文構成品情報取得WebAPI", tyumonGouseiResponse, errString, stmt, writeLog, writeErrCSV, true);
                        urlFlag3 = urlFlag3 + urlTyumonGousei;
                    } else {
                        CommonErrEvent.commonErrEvent(urlTyumonGousei, "kowa033", "2", tyumonGouseiErrMsg, "注文構成品情報取得WebAPI", tyumonGouseiResponse, errString, stmt, writeLog, writeErrCSV, false);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * XML取得
     *
     * @return
     */
    public static String loadJSON (String url) {
        StringBuilder json = new StringBuilder();
        try {
            URL oracle = new URL(url);
            URLConnection yc = oracle.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    yc.getInputStream()));
            String inputLine = null;
            while ( (inputLine = in.readLine()) != null) {
                json.append(inputLine);
            }
            in.close();
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return json.toString();
    }

    /**
     * md5パスワード情報取得
     *
     * @return
     */
    public static String toMD5(String plainText){

        StringBuffer buf= new StringBuffer("");
        try{
            MessageDigest md= MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte b[]=md.digest();
            int i;
            for(int offset=0;offset<b.length;offset++){
                i=b[offset];
                if(i<0)
                    i+=256;
                if(i<16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return buf.toString();

    }

    /**
     * 注文伝票情報取得
     *
     * @return
     */
    public static List<Map<String, String>> readStringXmlOutTyumon(String xml) {
        Map<String, String> map = new HashMap<String, String>();
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        Document doc = null;
        String GetStatus = "";
        String Message = "";
        try {
            doc = DocumentHelper.parseText(xml);

            Element rootElt = doc.getRootElement();

            Iterator iter = rootElt.elementIterator("ResultSet");

            while (iter.hasNext()) {

                Element recordEle = (Element) iter.next();

                Iterator iters = recordEle.elementIterator("ResultStatus");
                while (iters.hasNext()) {
                    Element itemEle1 = (Element) iters.next();
                    GetStatus = itemEle1.elementTextTrim("GetStatus");
                    Message = itemEle1.elementTextTrim("Message");
                    map.put("GetStatus", GetStatus);
                    map.put("Message", Message);

                    if ("error".equals(GetStatus)) {
                        list.add(map);
                        return list;
                    }
                }

                Iterator iterss = recordEle.elementIterator("Result");
                while (iterss.hasNext()) {

                    Element itemEle = (Element) iterss.next();
                    map = new HashMap<String, String>();

                    // 管理番号
                    String order_number = itemEle.elementTextTrim("order_number");
                    // 注文日時
                    String order_date = itemEle.elementTextTrim("order_date");
                    // 店舗コード
                    String shop_code = itemEle.elementTextTrim("shop_code");
                    // 店舗名
                    String shop_name = itemEle.elementTextTrim("shop_name");
                    // 注文番号
                    String order_code = itemEle.elementTextTrim("order_code");
                    // 注文者会社名
                    String client_section_name1 = itemEle.elementTextTrim("client_section_name1");
                    // 注文者部署名
                    String client_section_name2 = itemEle.elementTextTrim("client_section_name2");
                    // 注文者氏名
                    String client_name = itemEle.elementTextTrim("client_name");
                    // 注文者カナ
                    String client_kana = itemEle.elementTextTrim("client_kana");
                    // 注文者郵便番号
                    String client_zip = itemEle.elementTextTrim("client_zip");
                    // 注文者住所１
                    String client_address1 = itemEle.elementTextTrim("client_address1");
                    // 注文者住所２
                    String client_address2 = itemEle.elementTextTrim("client_address2");
                    // 注文者ＴＥＬ
                    String client_tel = itemEle.elementTextTrim("client_tel");
                    // 注文者メール
                    String client_mail = itemEle.elementTextTrim("client_mail");
                    // 端末識別
                    String terminal_type = itemEle.elementTextTrim("terminal_type");
                    // 届け先会社名
                    String ship_section_name1 = itemEle.elementTextTrim("ship_section_name1");
                    // 届け先部署名
                    String ship_section_name2 = itemEle.elementTextTrim("ship_section_name2");
                    // 届け先氏名
                    String ship_name = itemEle.elementTextTrim("ship_name");
                    // 届け先カナ
                    String ship_kana = itemEle.elementTextTrim("ship_kana");
                    // 届け先郵便番号
                    String ship_zip = itemEle.elementTextTrim("ship_zip");
                    // 届け先住所１
                    String ship_address1 = itemEle.elementTextTrim("ship_address1");
                    // 届け先住所２
                    String ship_address2 = itemEle.elementTextTrim("ship_address2");
                    // 届け先ＴＥＬ
                    String ship_tel = itemEle.elementTextTrim("ship_tel");
                    // 配送番号
                    String delivery_number = itemEle.elementTextTrim("delivery_number");
                    // 配送方法コード
                    String delivery_type_code = itemEle.elementTextTrim("delivery_type_code");
                    // 配送方法名
                    String delivery_type_name = itemEle.elementTextTrim("delivery_type_name");
                    // 配送便コード
                    String delivery_code = itemEle.elementTextTrim("delivery_code");
                    // 配送便名
                    String delivery_name = itemEle.elementTextTrim("delivery_name");
                    // 配送希望日
                    String delivery_req_date = itemEle.elementTextTrim("delivery_req_date");
                    // 配送時間帯コード
                    String delivery_time_code = itemEle.elementTextTrim("delivery_time_code");
                    // 配送時間帯名
                    String delivery_time_name = itemEle.elementTextTrim("delivery_time_name");
                    // 発送日
                    String delivery_date = itemEle.elementTextTrim("delivery_date");
                    // 支払方法コード
                    String payment_code = itemEle.elementTextTrim("payment_code");
                    // 支払方法名
                    String payment_name = itemEle.elementTextTrim("payment_name");
                    // 取引オプション１
                    String order_option1 = itemEle.elementTextTrim("order_option1");
                    // 取引オプション２
                    String order_option2 = itemEle.elementTextTrim("order_option2");
                    // 取引オプション３
                    String order_option3 = itemEle.elementTextTrim("order_option3");
                    // 取引オプション４
                    String order_option4 = itemEle.elementTextTrim("order_option4");
                    // 取引オプション５
                    String order_option5 = itemEle.elementTextTrim("order_option5");
                    // 取引オプション６
                    String order_option6 = itemEle.elementTextTrim("order_option6");
                    // 備考
                    String order_memo = itemEle.elementTextTrim("order_memo");
                    // コメント
                    String comment = itemEle.elementTextTrim("comment");
                    // 小計
                    String subtotal_price = itemEle.elementTextTrim("subtotal_price");
                    // 消費税
                    String tax_price = itemEle.elementTextTrim("tax_price");
                    // 送料
                    String carriage_price = itemEle.elementTextTrim("carriage_price");
                    // 代引料
                    String cash_on_delivery = itemEle.elementTextTrim("cash_on_delivery");
                    // 手数料１
                    String option1_fee = itemEle.elementTextTrim("option1_fee");
                    // 手数料２
                    String option2_fee = itemEle.elementTextTrim("option2_fee");
                    // ポイント
                    String point = itemEle.elementTextTrim("point");
                    // クーポン
                    String coupon = itemEle.elementTextTrim("coupon");
                    // 合計
                    String total_price = itemEle.elementTextTrim("total_price");
                    // 注文処理フェーズ
                    String phase_name = itemEle.elementTextTrim("phase_name");
                    // チェックマーク１
                    String check_mark1 = itemEle.elementTextTrim("check_mark1");
                    // チェックマーク２
                    String check_mark2 = itemEle.elementTextTrim("check_mark2");
                    // チェックマーク３
                    String check_mark3 = itemEle.elementTextTrim("check_mark3");
                    // キャンセルフラグ
                    String cancel_flag = itemEle.elementTextTrim("cancel_flag");
                    // 同梱処理フラグ
                    String bundle_flag = itemEle.elementTextTrim("bundle_flag");
                    // 同梱先管理番号
                    String bundle_ahead_number = itemEle.elementTextTrim("bundle_ahead_number");
                    // 作成日時
                    String created_at = itemEle.elementTextTrim("created_at");
                    // 更新日時
                    String updated_at = itemEle.elementTextTrim("updated_at");

                    map.put("GetStatus", GetStatus);
                    map.put("Message", Message);
                    map.put("order_number", order_number);
                    map.put("order_date", order_date);
                    map.put("shop_code", shop_code);
                    map.put("shop_name", shop_name);
                    map.put("order_code", order_code);
                    map.put("client_section_name1", client_section_name1);
                    map.put("client_section_name2", client_section_name2);
                    map.put("client_name", client_name);
                    map.put("client_kana", client_kana);
                    map.put("client_zip", client_zip);
                    map.put("client_address1", client_address1);
                    map.put("client_address2", client_address2);
                    map.put("client_tel", client_tel);
                    map.put("client_mail", client_mail);
                    map.put("terminal_type", terminal_type);
                    map.put("ship_section_name1", ship_section_name1);
                    map.put("ship_section_name2", ship_section_name2);
                    map.put("ship_name", ship_name);
                    map.put("ship_kana", ship_kana);
                    map.put("ship_zip", ship_zip);
                    map.put("ship_address1", ship_address1);
                    map.put("ship_address2", ship_address2);
                    map.put("ship_tel", ship_tel);
                    map.put("delivery_number", delivery_number);
                    map.put("delivery_type_code", delivery_type_code);
                    map.put("delivery_type_name", delivery_type_name);
                    map.put("delivery_code", delivery_code);
                    map.put("delivery_name", delivery_name);
                    map.put("delivery_req_date", delivery_req_date);
                    map.put("delivery_time_code", delivery_time_code);
                    map.put("delivery_time_name", delivery_time_name);
                    map.put("delivery_date", delivery_date);
                    map.put("payment_code", payment_code);
                    map.put("payment_name", payment_name);
                    map.put("order_option1", order_option1);
                    map.put("order_option2", order_option2);
                    map.put("order_option3", order_option3);
                    map.put("order_option4", order_option4);
                    map.put("order_option5", order_option5);
                    map.put("order_option6", order_option6);
                    map.put("order_memo", order_memo);
                    map.put("comment", comment);
                    map.put("subtotal_price", subtotal_price);
                    map.put("tax_price", tax_price);
                    map.put("carriage_price", carriage_price);
                    map.put("cash_on_delivery", cash_on_delivery);
                    map.put("option1_fee", option1_fee);
                    map.put("option2_fee", option2_fee);
                    map.put("point", point);
                    map.put("coupon", coupon);
                    map.put("total_price", total_price);
                    map.put("phase_name", phase_name);
                    map.put("check_mark1", check_mark1);
                    map.put("check_mark2", check_mark2);
                    map.put("check_mark3", check_mark3);
                    map.put("cancel_flag", cancel_flag);
                    map.put("bundle_flag", bundle_flag);
                    map.put("bundle_ahead_number", bundle_ahead_number);
                    map.put("created_at", created_at);
                    map.put("updated_at", updated_at);

                    list.add(map);
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return list;
    }
    /**
     * 注文詳細情報取得
     *
     * @return
     */
    public static List<Map<String, String>> readStringXmlOutTyumonMeisai(String xml) {
        Map<String, String> map = new HashMap<String, String>();
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        Document doc = null;
        String GetStatus = "";
        String Message = "";
        try {
            doc = DocumentHelper.parseText(xml);

            Element rootElt = doc.getRootElement();

            Iterator iter = rootElt.elementIterator("ResultSet");

            while (iter.hasNext()) {

                Element recordEle = (Element) iter.next();

                Iterator iters = recordEle.elementIterator("ResultStatus");
                while (iters.hasNext()) {
                    Element itemEle1 = (Element) iters.next();
                    GetStatus = itemEle1.elementTextTrim("GetStatus");
                    Message = itemEle1.elementTextTrim("Message");
                    map.put("GetStatus", GetStatus);
                    map.put("Message", Message);

                    if ("error".equals(GetStatus)) {
                        list.add(map);
                        return list;
                    }
                }

                Iterator iterss = recordEle.elementIterator("Result");
                while (iterss.hasNext()) {

                    Element itemEle = (Element) iterss.next();
                    map = new HashMap<String, String>();

                    // 管理番号
                    String order_number = itemEle.elementTextTrim("order_number");
                    // 行№
                    String line_no = itemEle.elementTextTrim("line_no");
                    // 商品コード
                    String item_code = itemEle.elementTextTrim("item_code");
                    // 商品名
                    String item_name = itemEle.elementTextTrim("item_name");
                    // 属性１コード
                    String attribute1_code = itemEle.elementTextTrim("attribute1_code");
                    // 属性１名
                    String attribute1_name = itemEle.elementTextTrim("attribute1_name");
                    // 属性２コード
                    String attribute2_code = itemEle.elementTextTrim("attribute2_code");
                    // 属性２名
                    String attribute2_name = itemEle.elementTextTrim("attribute2_name");
                    // 選択肢ラベル1
                    String item_option1_label = itemEle.elementTextTrim("item_option1_label");
                    // 選択肢ラベル2
                    String item_option2_label = itemEle.elementTextTrim("item_option2_label");
                    // 選択肢ラベル3
                    String item_option3_label = itemEle.elementTextTrim("item_option3_label");
                    // 選択肢ラベル4
                    String item_option4_label = itemEle.elementTextTrim("item_option4_label");
                    // 選択肢ラベル5
                    String item_option5_label = itemEle.elementTextTrim("item_option5_label");
                    // 選択肢ラベル6
                    String item_option6_label = itemEle.elementTextTrim("item_option6_label");
                    // 選択肢ラベル7
                    String item_option7_label = itemEle.elementTextTrim("item_option7_label");
                    // 選択肢ラベル8
                    String item_option8_label = itemEle.elementTextTrim("item_option8_label");
                    // 選択肢ラベル9
                    String item_option9_label = itemEle.elementTextTrim("item_option9_label");
                    // 選択肢ラベル10
                    String item_option10_label = itemEle.elementTextTrim("item_option10_label");
                    // 選択肢ラベル11
                    String item_option11_label = itemEle.elementTextTrim("item_option11_label");
                    // 選択肢ラベル12
                    String item_option12_label = itemEle.elementTextTrim("item_option12_label");
                    // 選択肢ラベル13
                    String item_option13_label = itemEle.elementTextTrim("item_option13_label");
                    // 選択肢ラベル14
                    String item_option14_label = itemEle.elementTextTrim("item_option14_label");
                    // 選択肢ラベル15
                    String item_option15_label = itemEle.elementTextTrim("item_option15_label");
                    // 選択肢ラベル16
                    String item_option16_label = itemEle.elementTextTrim("item_option16_label");
                    // 選択肢ラベル17
                    String item_option17_label = itemEle.elementTextTrim("item_option17_label");
                    // 選択肢ラベル18
                    String item_option18_label = itemEle.elementTextTrim("item_option18_label");
                    // 選択肢ラベル19
                    String item_option19_label = itemEle.elementTextTrim("item_option19_label");
                    // 選択肢ラベル20
                    String item_option20_label = itemEle.elementTextTrim("item_option20_label");
                    // 選択肢1
                    String item_option1 = itemEle.elementTextTrim("item_option1");
                    // 選択肢2
                    String item_option2 = itemEle.elementTextTrim("item_option2");
                    // 選択肢3
                    String item_option3 = itemEle.elementTextTrim("item_option3");
                    // 選択肢4
                    String item_option4 = itemEle.elementTextTrim("item_option4");
                    // 選択肢5
                    String item_option5 = itemEle.elementTextTrim("item_option5");
                    // 選択肢6
                    String item_option6 = itemEle.elementTextTrim("item_option6");
                    // 選択肢7
                    String item_option7 = itemEle.elementTextTrim("item_option7");
                    // 選択肢8
                    String item_option8 = itemEle.elementTextTrim("item_option8");
                    // 選択肢9
                    String item_option9 = itemEle.elementTextTrim("item_option9");
                    // 選択肢10
                    String item_option10 = itemEle.elementTextTrim("item_option10");
                    // 選択肢11
                    String item_option11 = itemEle.elementTextTrim("item_option11");
                    // 選択肢12
                    String item_option12 = itemEle.elementTextTrim("item_option12");
                    // 選択肢13
                    String item_option13 = itemEle.elementTextTrim("item_option13");
                    // 選択肢14
                    String item_option14 = itemEle.elementTextTrim("item_option14");
                    // 選択肢15
                    String item_option15 = itemEle.elementTextTrim("item_option15");
                    // 選択肢16
                    String item_option16 = itemEle.elementTextTrim("item_option16");
                    // 選択肢17
                    String item_option17 = itemEle.elementTextTrim("item_option17");
                    // 選択肢18
                    String item_option18 = itemEle.elementTextTrim("item_option18");
                    // 選択肢19
                    String item_option19 = itemEle.elementTextTrim("item_option19");
                    // 選択肢20
                    String item_option20 = itemEle.elementTextTrim("item_option20");
                    // 数量
                    String amount = itemEle.elementTextTrim("amount");
                    // 単価
                    String unit_price = itemEle.elementTextTrim("unit_price");
                    // 金額
                    String amount_price = itemEle.elementTextTrim("amount_price");
                    // 税区分
                    String tax_type = itemEle.elementTextTrim("tax_type");
                    // 送料区分
                    String freight_type = itemEle.elementTextTrim("freight_type");
                    // 諸口商品コード
                    String free_item_code = itemEle.elementTextTrim("free_item_code");
                    // セット品フラグ
                    String component_flag = itemEle.elementTextTrim("component_flag");
                    // 消費税率
                    String tax_rate = itemEle.elementTextTrim("tax_rate");
                    // JANコード
                    String jan_cd = itemEle.elementTextTrim("jan_cd");

                    map.put("GetStatus", GetStatus);
                    map.put("Message", Message);
                    map.put("order_number", order_number);
                    map.put("line_no", line_no);
                    map.put("item_code", item_code);
                    map.put("item_name", item_name);
                    map.put("attribute1_code", attribute1_code);
                    map.put("attribute1_name", attribute1_name);
                    map.put("attribute2_code", attribute2_code);
                    map.put("attribute2_name", attribute2_name);
                    map.put("item_option1_label", item_option1_label);
                    map.put("item_option2_label", item_option2_label);
                    map.put("item_option3_label", item_option3_label);
                    map.put("item_option4_label", item_option4_label);
                    map.put("item_option5_label", item_option5_label);
                    map.put("item_option6_label", item_option6_label);
                    map.put("item_option7_label", item_option7_label);
                    map.put("item_option8_label", item_option8_label);
                    map.put("item_option9_label", item_option9_label);
                    map.put("item_option10_label", item_option10_label);
                    map.put("item_option11_label", item_option11_label);
                    map.put("item_option12_label", item_option12_label);
                    map.put("item_option13_label", item_option13_label);
                    map.put("item_option14_label", item_option14_label);
                    map.put("item_option15_label", item_option15_label);
                    map.put("item_option16_label", item_option16_label);
                    map.put("item_option17_label", item_option17_label);
                    map.put("item_option18_label", item_option18_label);
                    map.put("item_option19_label", item_option19_label);
                    map.put("item_option20_label", item_option20_label);
                    map.put("item_option1", item_option1);
                    map.put("item_option2", item_option2);
                    map.put("item_option3", item_option3);
                    map.put("item_option4", item_option4);
                    map.put("item_option5", item_option5);
                    map.put("item_option6", item_option6);
                    map.put("item_option7", item_option7);
                    map.put("item_option8", item_option8);
                    map.put("item_option9", item_option9);
                    map.put("item_option10", item_option10);
                    map.put("item_option11", item_option11);
                    map.put("item_option12", item_option12);
                    map.put("item_option13", item_option13);
                    map.put("item_option14", item_option14);
                    map.put("item_option15", item_option15);
                    map.put("item_option16", item_option16);
                    map.put("item_option17", item_option17);
                    map.put("item_option18", item_option18);
                    map.put("item_option19", item_option19);
                    map.put("item_option20", item_option20);
                    map.put("amount", amount);
                    map.put("unit_price", unit_price);
                    map.put("amount_price", amount_price);
                    map.put("tax_type", tax_type);
                    map.put("freight_type", freight_type);
                    map.put("free_item_code", free_item_code);
                    map.put("component_flag", component_flag);
                    map.put("tax_rate", tax_rate);
                    map.put("jan_cd", jan_cd);

                    list.add(map);
                }
            }

        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return list;
    }
    /**
     * 注文構成品情報取得
     *
     * @return
     */
    public static List<Map<String, String>> readStringXmlOutTyumonGousei(String xml) {

        Map<String, String> map = new HashMap<String, String>();
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        Document doc = null;
        String GetStatus = "";
        String Message = "";
        try {
            doc = DocumentHelper.parseText(xml);

            Element rootElt = doc.getRootElement();

            Iterator iter = rootElt.elementIterator("ResultSet");

            while (iter.hasNext()) {
                Element recordEle = (Element) iter.next();
                Iterator iters = recordEle.elementIterator("ResultStatus");
                while (iters.hasNext()) {
                    Element itemEle1 = (Element) iters.next();
                    GetStatus = itemEle1.elementTextTrim("GetStatus");
                    Message = itemEle1.elementTextTrim("Message");
                    map.put("GetStatus", GetStatus);
                    map.put("Message", Message);

                    if ("error".equals(GetStatus)) {
                        list.add(map);
                        return list;
                    }
                }

                Iterator iterss = recordEle.elementIterator("Result");
                while (iterss.hasNext()) {

                    Element itemEle = (Element) iterss.next();
                    map = new HashMap<String, String>();

                    // 管理番号
                    String order_number = itemEle.elementTextTrim("order_number");
                    // 行№
                    String line_no = itemEle.elementTextTrim("line_no");
                    // セット商品コード
                    String set_item_code = itemEle.elementTextTrim("set_item_code");
                    // 商品コード
                    String item_code = itemEle.elementTextTrim("item_code");
                    // 商品名
                    String item_name = itemEle.elementTextTrim("item_name");
                    // 属性１コード
                    String attribute1_code = itemEle.elementTextTrim("attribute1_code");
                    // 属性１名
                    String attribute1_name = itemEle.elementTextTrim("attribute1_name");
                    // 属性２コード
                    String attribute2_code = itemEle.elementTextTrim("attribute2_code");
                    // 属性２名
                    String attribute2_name = itemEle.elementTextTrim("attribute2_name");
                    // 員数
                    String component_count = itemEle.elementTextTrim("component_count");
                    // JANコード
                    String jan_cd = itemEle.elementTextTrim("jan_cd");

                    map.put("GetStatus", GetStatus);
                    map.put("Message", Message);
                    map.put("order_number", order_number);
                    map.put("line_no", line_no);
                    map.put("set_item_code", set_item_code);
                    map.put("item_code", item_code);
                    map.put("item_name", item_name);
                    map.put("attribute1_code", attribute1_code);
                    map.put("attribute1_name", attribute1_name);
                    map.put("attribute2_code", attribute2_code);
                    map.put("attribute2_name", attribute2_name);
                    map.put("component_count", component_count);
                    map.put("jan_cd", jan_cd);

                    list.add(map);
                }
            }

        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 注文情報有無
     *
     * @return 有:true/無:false
     */
    public static boolean isExistsOrder(Map<String, String> map, Statement stmt) {

        ResultSet rs;
        boolean result = false;
        try {
            rs = stmt.executeQuery("SELECT * FROM 注文MT WHERE 管理番号 = " + map.get("order_number") + "AND 取込先 = '1'");
            while (rs.next()) {
                result = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * テーブル「注文MT」に更新/登録処理
     *
     * @return
     */
    public static String saveTyumon(boolean dataFlg, Map<String, String> map, Statement stmt) {

        try {
            String sql = "";
            if (dataFlg) {
                sql = updateTyumon(map);
                stmt.execute(sql);
            } else {
                sql = insertTyumon(map);
                stmt.execute(sql);
            }

            return "";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 注文詳細情報有無
     *
     * @return 有:true/無:false
     */
    public static boolean isExistsOrderDetail(Map<String, String> map, Statement stmt) {

        ResultSet rs;
        boolean result = false;
        try {
            rs = stmt.executeQuery("SELECT * FROM [注文詳細T] WHERE [管理番号] = '" + map.get("order_number") + "' AND [行No] = '" + map.get("line_no") + "'");
            while (rs.next()) {
                result = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 注文詳細情報有無
     *
     * @return 有:true/無:false
     */
    public static HashMap<String, String> getOrderDetails(Map<String, String> map, Statement stmt) {

        HashMap<String, String> list = new HashMap<String, String>();
        String orderNumber = map.get("order_number");
        try {
            if (orderList.containsKey(orderNumber)) {
                list = orderList.get(orderNumber);
            } else {
                ResultSet rs = stmt.executeQuery("SELECT * FROM [注文詳細T] WHERE [管理番号] = '" + orderNumber + "'");
                while (rs.next()) {
                    String orderDetailNo = rs.getString("注文詳細NO");
                    String itemCode = rs.getString("商品コード");
                    list.put(itemCode, orderDetailNo);
                }
                orderList.put(orderNumber, list);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 注文No取得
     *
     * @return
     */
    public static String getOrderNo(Map<String, String> map, Statement stmt) {
        String orderNo = "";
        try {
            ResultSet rs = stmt.executeQuery("SELECT [注文NO] FROM [注文MT] WHERE [管理番号] = '" + map.get("order_number") + "' AND [取込先] = '1'");
            while (rs.next()) {
                orderNo = rs.getString("注文NO");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orderNo;
    }

    /**
     * テーブル「注文詳細T」に登録処理
     *
     * @return
     */
    public static String saveTyumonMeisai(Map<String, String> map, Statement stmt) {

        Connection connSecond = null;
        boolean bFlg = false;
        try {
            connSecond = stmt.getConnection();
            connSecond.setAutoCommit(false);

            //HashMap<String, String> orderDetails = getOrderDetails(map, stmt);
            //同一商品コードが複数行存在する場合あるため、重複チェックは元の行Noで行う
            // TODO:
            boolean isExists = isExistsOrderDetail(map, stmt);

            String sql = "";
            String tyumonSyosaiNo = "";

            // 更新
            if (isExists) {
                String tyumonNo = getOrderNo(map, stmt);
                sql = updateTyumonSyosai(map, tyumonNo);
                stmt.execute(sql);
                // 「注文詳細T」に登録
            } else {
                String tyumonNo = getOrderNo(map, stmt);
                sql = insertTyumonSyosai(map, tyumonNo);
                stmt.execute(sql);

                if (!shCode.contains(map.get("item_code"))) {
                    shCode = shCode + "," + map.get("item_code");
                }

                ResultSet rs2 = stmt.executeQuery("SELECT [注文詳細NO] FROM [注文詳細T] WHERE [管理番号] = '" + map.get("order_number") + "' AND [行No] = '" + map.get("line_no") + "'");
                while (rs2.next()) {
                    tyumonSyosaiNo = rs2.getString("注文詳細NO");
                    break;
                }

                // 「注文詳細オプションT」に登録処理
                for (int i = 1; i <= 20; i++ ) {
                    if (map.get("item_option" + String.valueOf(i) + "_label").length() > 0) {
                        sql = "";
                        sql = insertTyumonSyosaiOption(map, String.valueOf(i), tyumonSyosaiNo);
                        stmt.execute(sql);
                    }
                }

                // 「注文詳細関連T」に登録処理
                String exceptionmsg = saveTyumonMeisaiKanren(map, tyumonSyosaiNo, stmt);
                if (!"".equals(exceptionmsg)) {
                    bFlg = true;
                    return exceptionmsg;
                }
            }
            return "";
        } catch (Exception e) {
            bFlg = true;
            return e.getMessage();
        } finally {
            if (connSecond != null) {
                try {
                    if (bFlg) {
                        connSecond.rollback();
                    } else {
                        connSecond.commit();
                    }
                } catch (Exception e) {
                    return e.getMessage();
                } finally {
                    try {
                        connSecond.setAutoCommit(true);
                    } catch (Exception e) {
                        return e.getMessage();
                    }
                }
            }
        }
    }

    /**
     * 「注文詳細T」重複データ調整
     *
     * @return
     */
    public static void rebuildErrorOrderDetail(Connection conn) {
        // 重複有データ取得
        String sql = "SELECT count(注文詳細NO), [管理番号], [商品コード] FROM [注文詳細T] ";
        sql += " group by 管理番号,商品コード HAVING count(注文詳細NO) > 1";
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            Statement stmt = conn.createStatement();
            Statement stmt2 = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                HashMap<String, String> row = new HashMap<>();
                row.put("order_number", rs.getString("管理番号"));
                row.put("item_code", rs.getString("商品コード"));
                list.add(row);
            }

            for(int i=0; i<list.size(); i++){
                HashMap<String, String> row = list.get(i);
                String orderNumber = row.get("order_number");
                String itemCode = row.get("item_code");

                // 関連データ削除
                ResultSet rs2 = stmt2.executeQuery("SELECT [注文詳細NO] FROM [注文詳細T] WHERE [管理番号] = '" + orderNumber+ "' AND [商品コード] = '" + itemCode + "'");
                while (rs2.next()) {
                    String orderDetailNo = rs2.getString("注文詳細NO");
                    stmt.execute("DELETE FROM [注文詳細関連T] WHERE [注文詳細NO]='"+ orderDetailNo + "'" );
                }

                // 重複データ削除
                stmt.execute("DELETE FROM [注文詳細T] WHERE 管理番号='" + orderNumber +"' AND 商品コード='" + itemCode + "'");

                // 注文詳細情報再取得
                String tyumonMeisaiMd5 = toMD5("account=1131&order_number=" + orderNumber + "KOWA");
                String urlTyumonMeisai = "https://crossmall.jp/webapi2/get_order_detail?account=1131&order_number=" + orderNumber + "&signing=" + tyumonMeisaiMd5;
                loopSecond(urlTyumonMeisai, stmt);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * 「注文詳細関連T」に登録処理
     *
     * @return
     */
    public static String saveTyumonMeisaiKanren(Map<String, String> map, String tyumonSyosaiNo, Statement stmt) {

        try {

            String sql1 = "";
            sql1 = insertTyumonSyosaiKanren(map, "1", tyumonSyosaiNo);
            stmt.execute(sql1);

            String sql2 = "";
            sql2 = insertTyumonSyosaiKanren(map, "2", tyumonSyosaiNo);
            stmt.execute(sql2);

            String sql3 = "";
            sql3 = insertTyumonSyosaiKanren(map, "3", tyumonSyosaiNo);
            stmt.execute(sql3);

            String sql4 = "";
            sql4 = insertTyumonSyosaiKanren(map, "4", tyumonSyosaiNo);
            stmt.execute(sql4);

            return "";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * テーブル「注文構成品情報T」に登録処理
     *
     * @return
     */
    public static String saveTyumonGousei(Map<String, String> map, Statement stmt) {

        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM 注文構成品情報T WHERE 管理番号 = '" + map.get("order_number") + "' AND [行№] = '" + map.get("line_no") + "' AND セット商品コード = '" + map.get("set_item_code") + "' AND 商品コード = '" + map.get("item_code") + "'");
            ResultSetMetaData md = rs.getMetaData();
            String sql = "";
            int columnCount = md.getColumnCount();
            List list = new ArrayList();
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    list.add(rs.getObject(i));
                }
            }
            if (list.size() == 0) {
                sql = insertTyumonKouseihinJyoho(map);
                stmt.execute(sql);
            }

            return "";
        } catch (Exception e) {
            return e.getMessage();
        }
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
                SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                format.setLenient(false);
                format.parse(str);
            } catch (ParseException e) {
                flag = true;
            }
        }

        return flag;
    }

    /**
     * テーブル「注文MT」に更新処理
     *
     * @return
     */
    public static String updateTyumon(Map<String, String> map) {
        String sql = "";
        sql = " UPDATE 注文MT SET";
        sql += "  管理番号 = '" + map.get("order_number") + "'";
        sql += " ,注文日時 = '" + map.get("order_date") + "'";
        sql += " ,店舗コード = '" + map.get("shop_code") + "'";
        sql += " ,店舗名 = '" + CommonUtil.escapeSql(map.get("shop_name")) + "'";
        sql += " ,注文番号 = '" + CommonUtil.escapeSql(map.get("order_code")) + "'";
        sql += " ,注文者会社名 = '" + CommonUtil.escapeSql(map.get("client_section_name1")) + "'";
        sql += " ,注文者部署名 = '" + CommonUtil.escapeSql(map.get("client_section_name2")) + "'";
        sql += " ,注文者氏名 = '" + CommonUtil.escapeSql(map.get("client_name")) + "'";
        sql += " ,注文者カナ = '" + CommonUtil.escapeSql(map.get("client_kana")) + "'";
        sql += " ,注文者郵便番号 = '" + CommonUtil.escapeSql(map.get("client_zip")) + "'";
        sql += " ,注文者住所１ = '" + CommonUtil.escapeSql(map.get("client_address1")) + "'";
        sql += " ,注文者住所２ = '" + CommonUtil.escapeSql(map.get("client_address2")) + "'";
        sql += " ,注文者ＴＥＬ = '" + CommonUtil.escapeSql(map.get("client_tel")) + "'";
        sql += " ,注文者メール = '" + CommonUtil.escapeSql(map.get("client_mail")) + "'";

        if (!"".equals(map.get("terminal_type"))) {
            sql += " ,端末識別 = " + map.get("terminal_type");
        } else {
            sql += " ,端末識別 = null";
        }

        sql += " ,届け先会社名 = '" + CommonUtil.escapeSql(map.get("ship_section_name1")) + "'";
        sql += " ,届け先部署名 = '" + CommonUtil.escapeSql(map.get("ship_section_name2")) + "'";
        sql += " ,届け先氏名 = '" + CommonUtil.escapeSql(map.get("ship_name")) + "'";
        sql += " ,届け先カナ = '" + CommonUtil.escapeSql(map.get("ship_kana")) + "'";
        sql += " ,届け先郵便番号 = '" + CommonUtil.escapeSql(map.get("ship_zip")) + "'";
        sql += " ,届け先住所１ = '" + CommonUtil.escapeSql(map.get("ship_address1")) + "'";
        sql += " ,届け先住所２ = '" + CommonUtil.escapeSql(map.get("ship_address2")) + "'";
        sql += " ,届け先ＴＥＬ = '" + CommonUtil.escapeSql(map.get("ship_tel")) + "'";
        sql += " ,配送番号 = '" + CommonUtil.escapeSql(map.get("delivery_number")) + "'";
        sql += " ,配送方法コード = '" + CommonUtil.escapeSql(map.get("delivery_type_code")) + "'";
        sql += " ,配送方法名 = '" + CommonUtil.escapeSql(map.get("delivery_type_name")) + "'";
        sql += " ,配送便コード = '" + CommonUtil.escapeSql(map.get("delivery_code")) + "'";
        sql += " ,配送便名 = '" + CommonUtil.escapeSql(map.get("delivery_name")) + "'";
        sql += " ,配送希望日 = '" + CommonUtil.escapeSql(map.get("delivery_req_date")) + "'";
        sql += " ,配送時間帯コード = '" + CommonUtil.escapeSql(map.get("delivery_time_code")) + "'";
        sql += " ,配送時間帯名 = '" + CommonUtil.escapeSql(map.get("delivery_time_name")) + "'";
        sql += " ,発送日 = '" + CommonUtil.escapeSql(map.get("delivery_date")) + "'";
        sql += " ,支払方法コード = '" + CommonUtil.escapeSql(map.get("payment_code")) + "'";
        sql += " ,支払方法名 = '" + CommonUtil.escapeSql(map.get("payment_name")) + "'";
        sql += " ,取引オプション１ = '" + CommonUtil.escapeSql(map.get("order_option1")) + "'";
        sql += " ,取引オプション２ = '" + CommonUtil.escapeSql(map.get("order_option2")) + "'";
        sql += " ,取引オプション３ = '" + CommonUtil.escapeSql(map.get("order_option3")) + "'";
        sql += " ,取引オプション４ = '" + CommonUtil.escapeSql(map.get("order_option4")) + "'";
        sql += " ,取引オプション５ = '" + CommonUtil.escapeSql(map.get("order_option5")) + "'";
        sql += " ,取引オプション６ = '" + CommonUtil.escapeSql(map.get("order_option6")) + "'";
        sql += " ,備考 = '" + CommonUtil.escapeSql(map.get("order_memo")) + "'";
        sql += " ,コメント = '" + CommonUtil.escapeSql(map.get("comment")) + "'";
        if (!"".equals(map.get("subtotal_price"))) {
            sql += " ,小計 = " + map.get("subtotal_price");
        } else {
            sql += " ,小計 = null";
        }
        if (!"".equals(map.get("tax_price"))) {
            sql += " ,消費税 = " + map.get("tax_price");
        } else {
            sql += " ,消費税 = null";
        }
        if (!"".equals(map.get("carriage_price"))) {
            sql += " ,送料 = " + map.get("carriage_price");
        } else {
            sql += " ,送料 = null";
        }
        if (!"".equals(map.get("cash_on_delivery"))) {
            sql += " ,代引料 = " + map.get("cash_on_delivery");
        } else {
            sql += " ,代引料 = null";
        }
        if (!"".equals(map.get("option1_fee"))) {
            sql += " ,手数料１ = " + map.get("option1_fee");
        } else {
            sql += " ,手数料１ = null";
        }
        if (!"".equals(map.get("option2_fee"))) {
            sql += " ,手数料２ = " + map.get("option2_fee");
        } else {
            sql += " ,手数料２ = null";
        }
        if (!"".equals(map.get("point"))) {
            sql += " ,ポイント = " + map.get("point");
        } else {
            sql += " ,ポイント = null";
        }
        if (!"".equals(map.get("coupon"))) {
            sql += " ,クーポン = " + map.get("coupon");
        } else {
            sql += " ,クーポン = null";
        }
        if (!"".equals(map.get("total_price"))) {
            sql += " ,合計 = " + map.get("total_price");
        } else {
            sql += " ,合計 = null";
        }
        sql += " ,注文処理フェーズ = '" + CommonUtil.escapeSql(map.get("phase_name")) + "'";
        if (!"".equals(map.get("check_mark1"))) {
            sql += " ,チェックマーク１ = " + CommonUtil.escapeSql(map.get("check_mark1"));
        } else {
            sql += " ,チェックマーク１ = null";
        }
        if (!"".equals(map.get("check_mark2"))) {
            sql += " ,チェックマーク２ = " + CommonUtil.escapeSql(map.get("check_mark2"));
        } else {
            sql += " ,チェックマーク２ = null";
        }
        if (!"".equals(map.get("check_mark3"))) {
            sql += " ,チェックマーク３ = " + CommonUtil.escapeSql(map.get("check_mark3"));
        } else {
            sql += " ,チェックマーク３ = null";
        }
        if (!"".equals(map.get("cancel_flag"))) {
            sql += " ,キャンセルフラグ = " + CommonUtil.escapeSql(map.get("cancel_flag"));
        } else {
            sql += " ,キャンセルフラグ = null";
        }
        if (!"".equals(map.get("bundle_flag"))) {
            sql += " ,同梱処理フラグ = " + map.get("bundle_flag");
        } else {
            sql += " ,同梱処理フラグ = null";
        }
        sql += " ,同梱先管理番号 = '" + map.get("bundle_ahead_number") + "'";
        sql += " ,注文作成日時 = '" + map.get("created_at") + "'";
        sql += " ,注文更新日時 = '" + map.get("updated_at") + "'";
        sql += " ,更新ユーザ = 'batch'";
        sql += " ,更新プログラム = 'kowa033'";
        sql += " ,更新日時 = '" + CommonUtil.getCurrentDateTime() + "'";
        sql += " WHERE 管理番号 = '" + map.get("order_number") + "'";
        sql += " AND 取込先 = '1'";

        return sql;
    }

    /**
     * テーブル「注文MT」に登録処理
     *
     * @return
     */
    public static String insertTyumon(Map<String, String> map) {
        String sql = "";

        sql = " INSERT INTO 注文MT ( ";
        sql += " 管理番号 ";
        sql += " ,注文日時 ";
        sql += " ,店舗コード ";
        sql += " ,店舗名 ";
        sql += " ,注文番号 ";
        sql += " ,注文者会社名 ";
        sql += " ,注文者部署名 ";
        sql += " ,注文者氏名 ";
        sql += " ,注文者カナ ";
        sql += " ,注文者郵便番号 ";
        sql += " ,注文者住所１ ";
        sql += " ,注文者住所２ ";
        sql += " ,注文者ＴＥＬ ";
        sql += " ,注文者メール ";
        sql += " ,端末識別 ";
        sql += " ,届け先会社名 ";
        sql += " ,届け先部署名 ";
        sql += " ,届け先氏名 ";
        sql += " ,届け先カナ ";
        sql += " ,届け先郵便番号 ";
        sql += " ,届け先住所１ ";
        sql += " ,届け先住所２ ";
        sql += " ,届け先ＴＥＬ ";
        sql += " ,配送番号 ";
        sql += " ,配送方法コード ";
        sql += " ,配送方法名 ";
        sql += " ,配送便コード ";
        sql += " ,配送便名 ";
        sql += " ,配送希望日 ";
        sql += " ,配送時間帯コード ";
        sql += " ,配送時間帯名 ";
        sql += " ,発送日 ";
        sql += " ,支払方法コード ";
        sql += " ,支払方法名 ";
        sql += " ,取引オプション１ ";
        sql += " ,取引オプション２ ";
        sql += " ,取引オプション３ ";
        sql += " ,取引オプション４ ";
        sql += " ,取引オプション５ ";
        sql += " ,取引オプション６ ";
        sql += " ,備考 ";
        sql += " ,コメント ";
        sql += " ,小計 ";
        sql += " ,消費税 ";
        sql += " ,送料 ";
        sql += " ,代引料 ";
        sql += " ,手数料１ ";
        sql += " ,手数料２ ";
        sql += " ,ポイント ";
        sql += " ,クーポン ";
        sql += " ,合計 ";
        sql += " ,注文処理フェーズ ";
        sql += " ,チェックマーク１ ";
        sql += " ,チェックマーク２ ";
        sql += " ,チェックマーク３ ";
        sql += " ,キャンセルフラグ ";
        sql += " ,同梱処理フラグ ";
        sql += " ,同梱先管理番号 ";
        sql += " ,注文作成日時 ";
        sql += " ,注文更新日時 ";
        sql += " ,取込先 ";
        sql += " ,登録ユーザ ";
        sql += " ,登録プログラム ";
        sql += " ,登録日時 ";
        sql += " ,更新ユーザ ";
        sql += " ,更新プログラム ";
        sql += " ,更新日時 ";
        sql += " ) VALUES ( ";
        sql += "'" + CommonUtil.escapeSql(map.get("order_number")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("order_date")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("shop_code")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("shop_name")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("order_code")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("client_section_name1")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("client_section_name2")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("client_name")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("client_kana")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("client_zip")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("client_address1")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("client_address2")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("client_tel")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("client_mail")) + "'";
        if (!"".equals(CommonUtil.escapeSql(map.get("terminal_type")))) {
            sql += "," + CommonUtil.escapeSql(map.get("terminal_type"));
        } else {
            sql += "," + null;
        }
        sql += ",'" + CommonUtil.escapeSql(map.get("ship_section_name1")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("ship_section_name2")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("ship_name")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("ship_kana")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("ship_zip")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("ship_address1")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("ship_address2")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("ship_tel")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("delivery_number")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("delivery_type_code")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("delivery_type_name")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("delivery_code")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("delivery_name")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("delivery_req_date")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("delivery_time_code")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("delivery_time_name")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("delivery_date")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("payment_code")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("payment_name")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("order_option1")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("order_option2")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("order_option3")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("order_option4")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("order_option5")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("order_option6")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("order_memo")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("comment")) + "'";
        if (!"".equals(CommonUtil.escapeSql(map.get("subtotal_price")))) {
            sql += "," + CommonUtil.escapeSql(map.get("subtotal_price"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("tax_price"))) {
            sql += "," + map.get("tax_price");
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("carriage_price"))) {
            sql += "," + map.get("carriage_price");
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("cash_on_delivery"))) {
            sql += "," + CommonUtil.escapeSql(map.get("cash_on_delivery"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("option1_fee"))) {
            sql += "," + CommonUtil.escapeSql(map.get("option1_fee"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("option2_fee"))) {
            sql += "," + CommonUtil.escapeSql(map.get("option2_fee"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("point"))) {
            sql += "," + CommonUtil.escapeSql(map.get("point"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("coupon"))) {
            sql += "," + CommonUtil.escapeSql(map.get("coupon"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("total_price"))) {
            sql += "," + map.get("total_price");
        } else {
            sql += "," + null;
        }
        sql += " ,'" + CommonUtil.escapeSql(map.get("phase_name")) + "'";
        if (!"".equals(map.get("check_mark1"))) {
            sql += "," + CommonUtil.escapeSql(map.get("check_mark1"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("check_mark2"))) {
            sql += "," + CommonUtil.escapeSql(map.get("check_mark2"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("check_mark3"))) {
            sql += "," + CommonUtil.escapeSql(map.get("check_mark3"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("cancel_flag"))) {
            sql += "," + CommonUtil.escapeSql(map.get("cancel_flag"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("bundle_flag"))) {
            sql += "," + map.get("bundle_flag");
        } else {
            sql += "," + null;
        }
        sql += ",'" + CommonUtil.escapeSql(map.get("bundle_ahead_number")) + "'";
        sql += ",'" + map.get("created_at") + "'";
        sql += ",'" + map.get("updated_at") + "'";
        sql += ", '1'";
        sql += ", 'batch'";
        sql += ", 'kowa033'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += ", 'batch'";
        sql += ", 'kowa033'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " )";

        return sql;
    }

    /**
     * テーブル「注文詳細T」に登録処理
     *
     * @return
     */
    public static String insertTyumonSyosai(Map<String, String> map, String tyumonNo) {
        String sql = "";

        sql = " INSERT INTO 注文詳細T ( ";
        sql += " 注文NO ";
        sql += " ,管理番号 ";
        sql += " ,行No ";
        sql += " ,商品コード ";
        sql += " ,商品名 ";
        sql += " ,属性１コード ";
        sql += " ,属性１名 ";
        sql += " ,属性２コード ";
        sql += " ,属性２名 ";
        sql += " ,数量 ";
        sql += " ,単価 ";
        sql += " ,金額 ";
        sql += " ,税区分 ";
        sql += " ,送料区分 ";
        sql += " ,諸口商品コード ";
        sql += " ,セット品フラグ ";
        sql += " ,消費税率 ";
        sql += " ,JANコード ";
        sql += " ,登録ユーザ ";
        sql += " ,登録プログラム ";
        sql += " ,登録日時 ";
        sql += " ,更新ユーザ ";
        sql += " ,更新プログラム ";
        sql += " ,更新日時 ";
        sql += " ) VALUES ( ";
        sql += "'" + tyumonNo + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("order_number")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("line_no")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("item_code")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("item_name")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("attribute1_code")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("attribute1_name")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("attribute2_code")) + "'";
        sql += ",'" + CommonUtil.escapeSql(map.get("attribute2_name")) + "'";
        if (!"".equals(map.get("amount"))) {
            sql += "," + Long.valueOf(map.get("amount"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("unit_price"))) {
            sql += "," + Long.valueOf(map.get("unit_price"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("amount_price"))) {
            sql += "," + Long.valueOf(map.get("amount_price"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("tax_type"))) {
            sql += "," + Long.valueOf(map.get("tax_type"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("freight_type"))) {
            sql += "," + Long.valueOf(map.get("freight_type"));
        } else {
            sql += "," + null;
        }
        sql += ",'" + map.get("free_item_code") + "'";
        if (!"".equals(map.get("component_flag"))) {
            sql += "," + Long.valueOf(map.get("component_flag"));
        } else {
            sql += "," + null;
        }
        if (!"".equals(map.get("tax_rate"))) {
            sql += "," + Long.valueOf(map.get("tax_rate"));
        } else {
            sql += "," + null;
        }
        sql += ",'" + map.get("jan_cd") + "'";
        sql += ",'batch'";
        sql += ",'kowa033'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += ",'batch'";
        sql += ",'kowa033'";
        sql += ",'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " )";

        return sql;
    }

    /**
     * テーブル「注文詳細T」に更新処理
     *
     * @return
     */
    public static String updateTyumonSyosai(Map<String, String> map, String tyumonNo) {
        String sql = "";

        sql = " UPDATE 注文詳細T SET ";
        sql += " 注文NO = '" + tyumonNo + "'";
        sql += " ,行No = '" + map.get("line_no") + "'";
        if (!"".equals(map.get("amount"))) {
            sql += " ,数量 = " + Long.valueOf(map.get("amount"));
        }
        if (!"".equals(map.get("unit_price"))) {
            sql += " ,単価 = " + Long.valueOf(map.get("unit_price"));
        }
        if (!"".equals(map.get("amount_price"))) {
            sql += " ,金額 = " + Long.valueOf(map.get("amount_price"));
        }

        sql += " ,更新日時 = '" + CommonUtil.getCurrentDateTime() + "'";
        sql += "  WHERE  ";
        sql += "  管理番号 = '" + map.get("order_number") + "'";
        sql += "  AND 商品コード = '" + map.get("item_code") + "'";

        return sql;
    }

    /**
     * テーブル「注文詳細オプションT」に登録処理
     *
     * @return
     */
    public static String insertTyumonSyosaiOption(Map<String, String> map, String num, String tyumonSyosaiNo) {
        String sql = "";

        sql = " INSERT INTO 注文詳細オプションT ( ";
        sql += " 注文詳細NO ";
        sql += " ,選択肢ラベル ";
        sql += " ,選択肢 ";
        sql += " ,選択肢NO ";
        sql += " ,登録ユーザ ";
        sql += " ,登録プログラム ";
        sql += " ,登録日時 ";
        sql += " ,更新ユーザ ";
        sql += " ,更新プログラム ";
        sql += " ,更新日時 ";
        sql += " ) VALUES ( ";
        sql += tyumonSyosaiNo;
        sql += " ,'" + CommonUtil.escapeSql(map.get("item_option" + num + "_label")) + "'";
        sql += " ,'" + CommonUtil.escapeSql(map.get("item_option" + num)) + "'";
        sql += " ," + null;
        sql += " ,'batch'";
        sql += " ,'kowa033'";
        sql += " ,'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " ,'batch'";
        sql += " ,'kowa033'";
        sql += " ,'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " )";

        return sql;
    }

    /**
     * テーブル「注文詳細関連T」に登録処理
     *
     * @return
     */
    public static String insertTyumonSyosaiKanren(Map<String, String> map, String kbn, String tyumonSyosaiNo) {
        String sql = "";

        sql = " INSERT INTO 注文詳細関連T ( ";
        sql += " 注文詳細NO ";
        sql += " ,商品コード ";
        sql += " ,平均単価 ";
        sql += " ,掛率 ";
        sql += " ,在庫種別 ";
        sql += " ,登録ユーザ ";
        sql += " ,登録プログラム ";
        sql += " ,登録日時 ";
        sql += " ,更新ユーザ ";
        sql += " ,更新プログラム ";
        sql += " ,更新日時 ";
        sql += " ) SELECT  ";
        sql += tyumonSyosaiNo;
        sql += " ,'" + map.get("item_code") + "'";
        sql += " ,平均単価 ";
        sql += " ,掛率 ";
        sql += " ," + kbn;
        sql += " ,'batch'";
        sql += " ,'kowa033'";
        sql += " ,'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " ,'batch'";
        sql += " ,'kowa033'";
        sql += " ,'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " FROM ";
        sql += " 商品在庫関連T ";
        sql += " WHERE ";
        sql += " 商品コード = '" + map.get("item_code") + "'";
        sql += " AND 在庫種別 = '" + kbn + "'";

        return sql;
    }

    /**
     * テーブル「注文構成品情報T」に登録処理
     *
     * @return
     */
    public static String insertTyumonKouseihinJyoho(Map<String, String> map) {
        String sql = "";

        sql = " INSERT INTO 注文構成品情報T ( ";
        sql += " 管理番号 ";
        sql += " ,[行№] ";
        sql += " ,セット商品コード ";
        sql += " ,商品コード ";
        sql += " ,商品名 ";
        sql += " ,属性１コード ";
        sql += " ,属性１名 ";
        sql += " ,属性２コード ";
        sql += " ,属性２名 ";
        sql += " ,員数 ";
        sql += " ,JANコード ";
        sql += " ,登録ユーザ ";
        sql += " ,登録プログラム ";
        sql += " ,登録日時 ";
        sql += " ,更新ユーザ ";
        sql += " ,更新プログラム ";
        sql += " ,更新日時 ";
        sql += " ) VALUES ( ";
        sql += "'" + map.get("order_number") + "'";
        sql += " ,'" + map.get("line_no") + "'";
        sql += " ,'" + map.get("set_item_code") + "'";
        sql += " ,'" + map.get("item_code") + "'";
        sql += " ,'" + map.get("item_name") + "'";
        sql += " ,'" + map.get("attribute1_code") + "'";
        sql += " ,'" + CommonUtil.escapeSql(map.get("attribute1_name")) + "'";
        sql += " ,'" + map.get("attribute2_code") + "'";
        sql += " ,'" + CommonUtil.escapeSql(map.get("attribute2_name")) + "'";
        if (!"".equals(map.get("component_count"))) {
            sql += " ," + Long.valueOf(map.get("component_count"));
        } else {
            sql += " ," + null;
        }
        sql += " ,'" + map.get("jan_cd") + "'";
        sql += " ,'batch'";
        sql += " ,'kowa033'";
        sql += " ,'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " ,'batch'";
        sql += " ,'kowa033'";
        sql += " ,'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " ) ";

        return sql;
    }

    public static void deleteUriageMT(Statement stmt) {

        try {
            String sql = "";
            sql = " DELETE 売上MT WHERE 店舗 IN ('1', '2', '3', '4', '5', '9', '10') ";

            stmt.execute(sql);

        } catch (Exception e) {
        }
    }

    public static void deleteUriageMTByShCode(String shCode, Statement stmt) {

        try {
            String sql = "";
            sql = " DELETE 売上MT WHERE 商品コード = '" + shCode + "'";

            stmt.execute(sql);

        } catch (Exception e) {
        }
    }

    public static void insertAllUriageMT(Statement stmt) {

        try {
            String sql = "";
            sql = " INSERT INTO [売上MT] ( ";
            sql += " [商品コード] ";
            sql += " ,[店舗] ";
            sql += " ,[範囲] ";
            sql += " ,[売数] ";
            sql += " ,[登録ユーザ] ";
            sql += " ,[登録プログラム] ";
            sql += " ,[登録日時] ";
            sql += " ,[更新ユーザ] ";
            sql += " ,[更新プログラム] ";
            sql += " ,[更新日時] ";
            sql += " ) ";
            sql += "  SELECT ALL_COUNT.商品コード  ";
            sql += "        ,ALL_COUNT.店舗コード  ";
            sql += "        ,ALL_COUNT.範囲  ";
            sql += "        ,ALL_COUNT.数量  ";
            sql += "        ,'batch'";
            sql += "        ,'kowa033'";
            sql += "        ,'" + CommonUtil.getCurrentDateTime() + "'";
            sql += "        ,'batch'";
            sql += "        ,'kowa033'";
            sql += "        ,'" + CommonUtil.getCurrentDateTime() + "'";
            sql += "  FROM (  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,CASE 注文MT.店舗コード  ";
            sql += "         WHEN '8' THEN '1' ";
            sql += "         WHEN '1' THEN '2' ";
            sql += "         WHEN '18' THEN '3' ";
            sql += "         WHEN '3' THEN '4' ";
            sql += "         WHEN '98' THEN '5' ";
            sql += "         WHEN '6' THEN '10' ";
            sql += "         END AS 店舗コード ";
            sql += "        ,'1' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-1, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "          ,注文MT.店舗コード  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,CASE 注文MT.店舗コード  ";
            sql += "         WHEN '8' THEN '1' ";
            sql += "         WHEN '1' THEN '2' ";
            sql += "         WHEN '18' THEN '3' ";
            sql += "         WHEN '3' THEN '4' ";
            sql += "         WHEN '98' THEN '5' ";
            sql += "         WHEN '6' THEN '10' ";
            sql += "         END AS 店舗コード ";
            sql += "        ,'2' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-3, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "          ,注文MT.店舗コード  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,CASE 注文MT.店舗コード  ";
            sql += "         WHEN '8' THEN '1' ";
            sql += "         WHEN '1' THEN '2' ";
            sql += "         WHEN '18' THEN '3' ";
            sql += "         WHEN '3' THEN '4' ";
            sql += "         WHEN '98' THEN '5' ";
            sql += "         WHEN '6' THEN '10' ";
            sql += "         END AS 店舗コード ";
            sql += "        ,'3' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-7, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "          ,注文MT.店舗コード  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,CASE 注文MT.店舗コード  ";
            sql += "         WHEN '8' THEN '1' ";
            sql += "         WHEN '1' THEN '2' ";
            sql += "         WHEN '18' THEN '3' ";
            sql += "         WHEN '3' THEN '4' ";
            sql += "         WHEN '98' THEN '5' ";
            sql += "         WHEN '6' THEN '10' ";
            sql += "         END AS 店舗コード ";
            sql += "        ,'4' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-14, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "          ,注文MT.店舗コード) ALL_COUNT  ";
            sql += "  ORDER BY ALL_COUNT.商品コード  ";
            sql += "          ,ALL_COUNT.取込先  ";
            sql += "          ,ALL_COUNT.店舗コード  ";
            sql += "          ,ALL_COUNT.範囲  ";

            stmt.execute(sql);

            sql = "";
            sql = " INSERT INTO [売上MT] ( ";
            sql += " [商品コード] ";
            sql += " ,[店舗] ";
            sql += " ,[範囲] ";
            sql += " ,[売数] ";
            sql += " ,[登録ユーザ] ";
            sql += " ,[登録プログラム] ";
            sql += " ,[登録日時] ";
            sql += " ,[更新ユーザ] ";
            sql += " ,[更新プログラム] ";
            sql += " ,[更新日時] ";
            sql += " ) ";
            sql += "  SELECT ALL_COUNT.商品コード  ";
            sql += "        ,'9' AS 店舗コード  ";
            sql += "        ,ALL_COUNT.範囲  ";
            sql += "        ,ALL_COUNT.数量  ";
            sql += "        ,'batch'";
            sql += "        ,'kowa033'";
            sql += "        ,'" + CommonUtil.getCurrentDateTime() + "'";
            sql += "        ,'batch'";
            sql += "        ,'kowa033'";
            sql += "        ,'" + CommonUtil.getCurrentDateTime() + "'";
            sql += "  FROM (  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,'1' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day, -1, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,'2' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-3, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,'3' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-7, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,'4' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-14, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先) ALL_COUNT  ";
            sql += "  ORDER BY ALL_COUNT.商品コード  ";
            sql += "          ,ALL_COUNT.取込先  ";
            sql += "          ,ALL_COUNT.範囲  ";

            stmt.execute(sql);

        } catch (Exception e) {
        }
    }

    public static void insertUriageMTByShCode(String shCode, Statement stmt) {

        try {
            String sql = "";
            sql = " INSERT INTO [売上MT] ( ";
            sql += " [商品コード] ";
            sql += " ,[店舗] ";
            sql += " ,[範囲] ";
            sql += " ,[売数] ";
            sql += " ,[登録ユーザ] ";
            sql += " ,[登録プログラム] ";
            sql += " ,[登録日時] ";
            sql += " ,[更新ユーザ] ";
            sql += " ,[更新プログラム] ";
            sql += " ,[更新日時] ";
            sql += " ) ";
            sql += "  SELECT ALL_COUNT.商品コード  ";
            sql += "        ,ALL_COUNT.店舗コード  ";
            sql += "        ,ALL_COUNT.範囲  ";
            sql += "        ,ALL_COUNT.数量  ";
            sql += "        ,'batch'";
            sql += "        ,'kowa033'";
            sql += "        ,'" + CommonUtil.getCurrentDateTime() + "'";
            sql += "        ,'batch'";
            sql += "        ,'kowa033'";
            sql += "        ,'" + CommonUtil.getCurrentDateTime() + "'";
            sql += "  FROM (  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,CASE 注文MT.店舗コード  ";
            sql += "         WHEN '8' THEN '1' ";
            sql += "         WHEN '1' THEN '2' ";
            sql += "         WHEN '18' THEN '3' ";
            sql += "         WHEN '3' THEN '4' ";
            sql += "         WHEN '98' THEN '5' ";
            sql += "         WHEN '6' THEN '10' ";
            sql += "         END AS 店舗コード ";
            sql += "        ,'1' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-1, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  AND   注文詳細T.商品コード = '" + shCode + "'";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "          ,注文MT.店舗コード  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,CASE 注文MT.店舗コード  ";
            sql += "         WHEN '8' THEN '1' ";
            sql += "         WHEN '1' THEN '2' ";
            sql += "         WHEN '18' THEN '3' ";
            sql += "         WHEN '3' THEN '4' ";
            sql += "         WHEN '98' THEN '5' ";
            sql += "         WHEN '6' THEN '10' ";
            sql += "         END AS 店舗コード ";
            sql += "        ,'2' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-3, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  AND   注文詳細T.商品コード = '" + shCode + "'";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "          ,注文MT.店舗コード  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,CASE 注文MT.店舗コード  ";
            sql += "         WHEN '8' THEN '1' ";
            sql += "         WHEN '1' THEN '2' ";
            sql += "         WHEN '18' THEN '3' ";
            sql += "         WHEN '3' THEN '4' ";
            sql += "         WHEN '98' THEN '5' ";
            sql += "         WHEN '6' THEN '10' ";
            sql += "         END AS 店舗コード ";
            sql += "        ,'3' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-7, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  AND   注文詳細T.商品コード = '" + shCode + "'";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "          ,注文MT.店舗コード  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,CASE 注文MT.店舗コード  ";
            sql += "         WHEN '8' THEN '1' ";
            sql += "         WHEN '1' THEN '2' ";
            sql += "         WHEN '18' THEN '3' ";
            sql += "         WHEN '3' THEN '4' ";
            sql += "         WHEN '98' THEN '5' ";
            sql += "         WHEN '6' THEN '10' ";
            sql += "         END AS 店舗コード ";
            sql += "        ,'4' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-14, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  AND   注文詳細T.商品コード = '" + shCode + "'";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "          ,注文MT.店舗コード) ALL_COUNT  ";
            sql += "  ORDER BY ALL_COUNT.商品コード  ";
            sql += "          ,ALL_COUNT.取込先  ";
            sql += "          ,ALL_COUNT.店舗コード  ";
            sql += "          ,ALL_COUNT.範囲  ";

            stmt.execute(sql);

            sql = "";
            sql = " INSERT INTO [売上MT] ( ";
            sql += " [商品コード] ";
            sql += " ,[店舗] ";
            sql += " ,[範囲] ";
            sql += " ,[売数] ";
            sql += " ,[登録ユーザ] ";
            sql += " ,[登録プログラム] ";
            sql += " ,[登録日時] ";
            sql += " ,[更新ユーザ] ";
            sql += " ,[更新プログラム] ";
            sql += " ,[更新日時] ";
            sql += " ) ";
            sql += "  SELECT ALL_COUNT.商品コード  ";
            sql += "        ,'9' AS 店舗コード  ";
            sql += "        ,ALL_COUNT.範囲  ";
            sql += "        ,ALL_COUNT.数量  ";
            sql += "        ,'batch'";
            sql += "        ,'kowa033'";
            sql += "        ,'" + CommonUtil.getCurrentDateTime() + "'";
            sql += "        ,'batch'";
            sql += "        ,'kowa033'";
            sql += "        ,'" + CommonUtil.getCurrentDateTime() + "'";
            sql += "  FROM (  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,'1' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day, -1, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  AND   注文詳細T.商品コード = '" + shCode + "'";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,'2' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-3, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  AND   注文詳細T.商品コード = '" + shCode + "'";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,'3' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-7, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  AND   注文詳細T.商品コード = '" + shCode + "'";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先  ";
            sql += "  UNION ALL  ";
            sql += "  SELECT 注文詳細T.商品コード  ";
            sql += "        ,注文MT.取込先  ";
            sql += "        ,'4' AS 範囲  ";
            sql += "        ,SUM(ISNULL(注文詳細T.数量, 0)) AS 数量  ";
            sql += "  FROM  注文MT INNER JOIN 注文詳細T ON 注文MT.注文NO = 注文詳細T.注文NO  ";
            sql += "  WHERE 注文MT.注文日時 >= dateadd(day,-14, '" + nowDate + "')  ";
            sql += "  AND   注文MT.キャンセルフラグ = 0  ";
            sql += "  AND   注文MT.取込先 = '1' ";
            sql += "  AND   注文詳細T.商品コード = '" + shCode + "'";
            sql += "  GROUP BY 注文詳細T.商品コード  ";
            sql += "          ,注文MT.取込先) ALL_COUNT  ";
            sql += "  ORDER BY ALL_COUNT.商品コード  ";
            sql += "          ,ALL_COUNT.取込先  ";
            sql += "          ,ALL_COUNT.範囲  ";

            stmt.execute(sql);

        } catch (Exception e) {
        }
    }
}