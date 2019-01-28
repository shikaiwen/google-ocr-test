package com.kowa.batch.quartz;

import common.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 商品マスター取込(在庫)
 * @return
 */
public class KOWA011Job extends JobWrapper {



    private static FileOutputStream fos = null;
    private static FileInputStream fis = null;
    public static KOWA011WriteLog writeLog = new KOWA011WriteLog();


    /**
     * 在庫ファイルを探す
     *
     * @return
     */
    public static List<File> searchFile(File folder) {
        // 在庫ファイルを取得
        File[] subFolders = folder.listFiles();

        List<File> result = new ArrayList<File>();

        if (subFolders != null) {
            for (int i = 0; i < subFolders.length; i++) {
                if (subFolders[i].isFile()) {
                    result.add(subFolders[i]);
                }
            }
        }
        File files[] = new File[result.size()];
        result.toArray(files);
        return result;
    }
    /**
     * コピー
     *
     * @return
     */
    private static File copy(String src, String des) {
        File file1 = new File(src);
        File[] fs = file1.listFiles();
        File file2 = new File(des);
        File file3 = null;
        String datatime = "_" + CommonUtil.getCurrentDateTime().replace("/", "").replace(" ", "").replace(":", "") + "10";
        if(!file2.exists()){
            file2.mkdirs();
        }
        for (File f : fs) {
            if(f.isFile()){
                fileCopy(f.getPath(), des+"\\"+f.getName().replace(".xlsx", "") + datatime + ".xlsx");
                file3 = new File(des+"\\"+ f.getName().replace(".xlsx", "") + datatime + ".xlsx");
            }
        }
        return file3;
    }
    /**
     * ファイルコピー
     *
     * @return
     */
    private static void fileCopy(String src, String des) {
        try {
            fis =new FileInputStream(src);
            fos =new FileOutputStream(des);
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = fis.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     *
     * @return
     */
    public static String fileCheck(List<File> result) {

        List<List<String>> dataLst = null;
        List<String> list = null;
        String failMsg = "";
        for (int i = 0; i < result.size(); i++) {
            File file = result.get(i);
            //ファイルデータ取得
            Map<String, List<List<String>>> dataMap = null;
            dataMap = ReadExcelUtil.readExcel(file.getAbsolutePath(), 1, "3");
            dataLst = dataMap.get("detail");
            for (int k = 0; k < dataLst.size(); k++) {
                list = dataLst.get(k);
                // 商品コード
                // 必須チェック
                if (!"".equals(CheckUtil.chkNull(list.get(0), "商品コード", "1"))) {
                    failMsg = failMsg + String.valueOf(k + 1)
                            + "行目に商品コードは未入力です。\n";
                }
                // 半角数値
                if (!"".equals(CheckUtil.chkNumberHalf(list.get(0), "商品コード"))) {
                    failMsg = failMsg + String.valueOf(k + 1)
                            + "行目に商品コードは半角数値ではありません。\n";
                }
                // 桁数
                if (list.get(0).length() > 15) {
                    failMsg = failMsg + String.valueOf(k + 1)
                            + "行目に商品コードは15桁以下にしてください。\n";
                }
                // 平均単価
                // 半角数値
                if (!"".equals(CheckUtil.chkFloatHalf(
                        list.get(7).replace("￥", "").replace(",", ""), 2, "平均単価"))) {
                    failMsg = failMsg + String.valueOf(k + 1)
                            + "行目に平均単価は半角数値ではありません。\n";
                }
                // 掛率
                // 半角数値
                if (!"".equals(CheckUtil.chkFloatHalf(list.get(9), 2, "掛率"))) {
                    failMsg = failMsg + String.valueOf(k + 1)
                            + "行目に掛率は半角数値ではありません。\n";
                }
            }
        }

        return failMsg;
    }

    /**
     * 取込エラーメッセージ
     *
     * @return
     */
    public static String failMsg(List<File> result, File failFile) {

        List<List<String>> dataLst = null;
        List<String> list = null;
        String failMsg = "";
        String date = CommonUtil.getCurrentDateTime();
        String syouhinCode = "";
        for (int i = 0; i < result.size(); i++) {
            File file = result.get(i);
            //ファイルデータ取得
            Map<String, List<List<String>>> dataMap = null;
            dataMap = ReadExcelUtil.readExcel(file.getAbsolutePath(), 1, "3");
            dataLst = dataMap.get("detail");
            for (int k = 0; k < dataLst.size(); k++) {
                list = dataLst.get(k);
                syouhinCode = list.get(0);
                // 商品コード
                // 必須チェック
                if (!"".equals(CheckUtil.chkNull(list.get(0), "商品コード", "1"))) {
                    failMsg = failMsg + String.valueOf(k + 1) + "行目に商品コードは未入力です。\n";
                }
                // 半角数値
                if (!"".equals(CheckUtil.chkNumberHalf(list.get(0), "商品コード"))) {
                    failMsg = failMsg + String.valueOf(k + 1) + "行目に商品コードは半角数値ではありません。\n";
                }
                // 桁数
                if (list.get(0).length() > 15) {
                    failMsg = failMsg + String.valueOf(k + 1) + "行目に商品コードは15桁以下にしてください。\n";
                }

                // 平均単価
                // 半角数値
                if (!"".equals(CheckUtil.chkFloatHalf(list.get(7), 2, "平均単価"))) {
                    failMsg = failMsg + String.valueOf(k + 1) + "行目に平均単価は半角数値ではありません。\n";
                }

                // 掛率
                // 半角数値
                if (!"".equals(CheckUtil.chkFloatHalf(
                        list.get(9).replace("￥", "").replace(",", ""), 2, "掛率"))) {
                    failMsg = failMsg + String.valueOf(k + 1) + "行目に掛率は半角数値ではありません。\n";
                }
            }
            failMsg = date + "," + failFile.getName() + "," + String.valueOf(i + 1) + syouhinCode + "," + failMsg;
            file.delete();
        }

        return failMsg;
    }

    /**
     * 取込成功メッセージ
     *
     * @return
     */
    public static String successMsg(List<File> result, File successFile) {

        List<List<String>> dataLst = null;
        List<String> list = null;
        String successMsg = "";
        String syouhinCode = "";
        String date = CommonUtil.getCurrentDateTime();
        for (int i = 0; i < result.size(); i++) {
            File file = result.get(i);
            //ファイルデータ取得
            Map<String, List<List<String>>> dataMap = null;
            dataMap = ReadExcelUtil.readExcel(file.getAbsolutePath(), 1, "3");
            dataLst = dataMap.get("detail");
            for (int k = 0; k < dataLst.size(); k++) {
                list = dataLst.get(k);
                syouhinCode = list.get(0);

                if (!"".equals(successMsg)) {
                    successMsg = successMsg + date + "," + successFile.getName() + "," + String.valueOf(k + 1) + "," + syouhinCode + ", success \n";
                } else {
                    successMsg = date + "," + successFile.getName() + "," + String.valueOf(k + 1) + "," + syouhinCode + ", success \n";
                }
            }
            file.delete();
        }

        return successMsg;
    }

    /**
     * テーブル「通報メールキューT」に登録
     *
     * @return
     */
    public static void mailSave(File kowaFailFile, String fileName, String failMsg, Statement stmt) {

        // 受信者メール
        String sendUser = "";
        // 本文
        String text = "";
        String sql1 = "";
        String sql2 = "";

        try {
            sql1 = " SELECT ";
            sql1 += "    B.[タイプ], ";
            sql1 += "    LEFT (USERLIST, LEN(USERLIST) -1) AS [受信者メール] ";
            sql1 += "    FROM (  ";
            sql1 += "  SELECT [タイプ] ";
            sql1 += "        ,( ";
            sql1 += "  SELECT [受信者メール]+',' ";
            sql1 += "    FROM (SELECT   [通報メールMT].[タイプ] ";
            sql1 += "           ,[通報メールMT].[受信者メール] AS [受信者メール] ";
            sql1 += "    FROM [通報メールMT] ) C ";
            sql1 += "   WHERE C.[タイプ] = A.[タイプ] ";
            sql1 += "     FOR XML PATH(''))AS USERLIST ";
            sql1 += "    FROM (SELECT   [通報メールMT].[タイプ] ";
            sql1 += "           ,[通報メールMT].[受信者メール] AS [受信者メール] ";
            sql1 += "    FROM [通報メールMT] ) A ";
            sql1 += "   GROUP BY [タイプ]) B ";
            sql1 += "   WHERE [タイプ] = '0' ";

            ResultSet rs = stmt.executeQuery(sql1);
            while (rs.next()) {
                sendUser = rs.getString(2);
            }

            // 件名
            fileName = fileName + "取込エラー発生";
            // 本文
            text = "対象ファイル：" + kowaFailFile.getAbsolutePath() + "," + failMsg;

            sql2 = "";
            sql2 += " INSERT INTO 通報メールキューT ( ";
            sql2 += " 受信者メール ";
            sql2 += " ,件名 ";
            sql2 += " ,本文 ";
            sql2 += " ,送信フラグ ";
            sql2 += " ,登録ユーザ ";
            sql2 += " ,登録プログラム ";
            sql2 += " ,登録日時 ";
            sql2 += " ,更新ユーザ ";
            sql2 += " ,更新プログラム ";
            sql2 += " ,更新日時 ";
            sql2 += " ) VALUES ( ";
            sql2 += "'" + sendUser + "'";
            sql2 += " ,'" + fileName + "'";
            sql2 += " ,'" + text + "'";
            sql2 += " ," + "0";
            sql2 += " ,'batch'";
            sql2 += " ,'KOWA011'";
            sql2 += " ,'" + CommonUtil.getCurrentDateTime()  + "'";
            sql2 += " ,'batch'";
            sql2 += " ,'KOWA011'";
            sql2 += " ,'" + CommonUtil.getCurrentDateTime()  + "'";
            sql2 += " ) ";

            stmt.execute(sql2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ＤＢに登録/更新する
     *
     * @return
     */
    public static void save(File file, Statement stmt, String kbn) {

        Map<String, List<List<String>>> map = ReadExcelUtil.readExcel(file.getAbsolutePath(), 1, "");
        List<List<String>> detail = null;
        List<String> list = null;
        // 商品コード
        String syouhinnCode = "";
        // 在庫数
        BigDecimal zaikusu = new BigDecimal(0);
        // 平均単価
        BigDecimal tanka = new BigDecimal(0);
        // 掛率
        BigDecimal karitu = new BigDecimal(0);

        try {
            if (map != null) {
                detail = map.get("detail");
                if (detail != null && detail.size() > 0) {
                    for (int j = 0; j < detail.size(); j++) {
                        try {
                            list = detail.get(j);
                            if (list != null && list.size() > 0) {
                                syouhinnCode = list.get(0);
                                zaikusu = new BigDecimal(list.get(6));
                                tanka = new BigDecimal(list.get(7)).setScale(0, RoundingMode.HALF_UP);
                                karitu = new BigDecimal(list.get(9)).setScale(2, RoundingMode.HALF_UP);

                                ResultSet rs = stmt.executeQuery("SELECT * FROM [商品マスタMT] WHERE [商品コード] = '" + syouhinnCode + "'");
                                ResultSetMetaData md = rs.getMetaData();
                                String sql = "";
                                int columnCount = md.getColumnCount();
                                List listSql = new ArrayList();
                                while (rs.next()) {
                                    for (int i = 1; i <= columnCount; i++) {
                                        listSql.add(rs.getObject(i));
                                    }
                                }
                                if (listSql.size() != 0) {
                                    ResultSet rs1 = stmt.executeQuery("SELECT * FROM [商品在庫関連T] WHERE [商品コード] = '" + syouhinnCode + "' AND [在庫種別] = '" + kbn + "'");
                                    ResultSetMetaData md1 = rs1.getMetaData();
                                    int columnCount1 = md1.getColumnCount();
                                    List listSql1 = new ArrayList();
                                    while (rs1.next()) {
                                        for (int i = 1; i <= columnCount1; i++) {
                                            listSql1.add(rs1.getObject(i));
                                        }
                                    }

                                    // kowa在庫の場合
                                    if ("1".equals(kbn)) {
                                        sql = "UPDATE [商品マスタMT] SET [KOWA在庫] = '" + zaikusu + "' ,[更新ユーザ] = 'batch' ,[更新プログラム] = 'KOWA011' ,[更新日時] = '" + CommonUtil.getCurrentDateTime() + "' WHERE 商品コード = '" + syouhinnCode + "'";
                                        stmt.execute(sql);

                                        if (listSql1.size() == 0) {
                                            sql = insertSql(syouhinnCode, tanka, karitu, kbn);

                                            stmt.execute(sql);
                                        } else {
                                            sql = updateSql(syouhinnCode, tanka, karitu, kbn);

                                            stmt.execute(sql);
                                        }
                                        // 大阪支社在庫の場合
                                    } else if ("2".equals(kbn)) {
                                        sql = "UPDATE [商品マスタMT] SET [大阪支社在庫] = '" + zaikusu + "' ,[更新ユーザ] = 'batch' ,[更新プログラム] = 'KOWA011' ,[更新日時] = '" + CommonUtil.getCurrentDateTime() + "' WHERE 商品コード = '" + syouhinnCode + "'";
                                        stmt.execute(sql);

                                        if (listSql1.size() == 0) {
                                            sql = insertSql(syouhinnCode, tanka, karitu, kbn);

                                            stmt.execute(sql);
                                        } else {
                                            sql = updateSql(syouhinnCode, tanka, karitu, kbn);

                                            stmt.execute(sql);
                                        }
                                        // NS在庫
                                    } else if ("3".equals(kbn)) {
                                        sql = "UPDATE [商品マスタMT] SET [NS在庫] = '" + zaikusu + "' ,[更新ユーザ] = 'batch' ,[更新プログラム] = 'KOWA011' ,[更新日時] = '" + CommonUtil.getCurrentDateTime() + "' WHERE 商品コード = '" + syouhinnCode + "'";
                                        stmt.execute(sql);

                                        if (listSql1.size() == 0) {
                                            sql = insertSql(syouhinnCode, tanka, karitu, kbn);

                                            stmt.execute(sql);
                                        } else {
                                            sql = updateSql(syouhinnCode, tanka, karitu, kbn);

                                            stmt.execute(sql);
                                        }
                                        // NS通販在庫の場合
                                    } else if ("4".equals(kbn)) {
                                        sql = "UPDATE [商品マスタMT] SET [NS通販在庫] = '" + zaikusu + "' ,[更新ユーザ] = 'batch' ,[更新プログラム] = 'KOWA011' ,[更新日時] = '" + CommonUtil.getCurrentDateTime() + "' WHERE 商品コード = '" + syouhinnCode + "'";
                                        stmt.execute(sql);

                                        if (listSql1.size() == 0) {
                                            sql = insertSql(syouhinnCode, tanka, karitu, kbn);

                                            stmt.execute(sql);
                                        } else {
                                            sql = updateSql(syouhinnCode, tanka, karitu, kbn);

                                            stmt.execute(sql);
                                        }
                                    }
                                }
                            }

                        } catch (Exception e) {
                            if ("1".equals(kbn)) {
                                // ログファイルに出力
                                writeLog.writeLog(e.getMessage(), false, "1");
                                // テーブル「通報メールキューT」に登録
                                mailSave(file, "KOWA在庫", e.getMessage(), stmt);
                            } else if ("2".equals(kbn)) {
                                // ログファイルに出力
                                writeLog.writeLog(e.getMessage(), false, "2");
                                // テーブル「通報メールキューT」に登録
                                mailSave(file, "大阪支社在庫", e.getMessage(), stmt);
                            } else if ("3".equals(kbn)) {
                                // ログファイルに出力
                                writeLog.writeLog(e.getMessage(), false, "3");
                                // テーブル「通報メールキューT」に登録
                                mailSave(file, "NS在庫", e.getMessage(), stmt);
                            } else if ("4".equals(kbn)) {
                                // ログファイルに出力
                                writeLog.writeLog(e.getMessage(), false, "4");
                                // テーブル「通報メールキューT」に登録
                                mailSave(file, "NS通販在庫", e.getMessage(), stmt);
                            }
                            continue;
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public static String insertSql(String syouhinnCode, BigDecimal tanka, BigDecimal karitu, String kbn) {
        String sql = "";
        sql = "INSERT INTO [商品在庫関連T] (";
        sql += " 商品コード";
        sql += " ,平均単価";
        sql += " ,掛率";
        sql += " ,在庫種別";
        sql += " ,登録ユーザ ";
        sql += " ,登録プログラム ";
        sql += " ,登録日時 ";
        sql += " ,更新ユーザ ";
        sql += " ,更新プログラム ";
        sql += " ,更新日時 ";
        sql += " ) VALUES ( ";
        sql += "'" + syouhinnCode + "'";
        sql += " ," + tanka;
        sql += " ," + karitu;
        sql += " ,'" + kbn + "'";
        sql += " ,'batch'";
        sql += " ,'KOWA011'";
        sql += " ,'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " ,'batch'";
        sql += " ,'KOWA011'";
        sql += " ,'" + CommonUtil.getCurrentDateTime() + "'";
        sql += " ) ";

        return sql;
    }


    public static String updateSql(String syouhinnCode, BigDecimal tanka, BigDecimal karitu, String kbn) {
        String sql = "";
        sql = "UPDATE [商品在庫関連T] SET ";
        sql += " [商品コード] = '" + syouhinnCode + "'";
        sql += " ,[平均単価] = " + tanka;
        sql += " ,[掛率] = " + karitu;
        sql += " ,[在庫種別] = '" + kbn + "'";
        sql += " ,[更新ユーザ] = 'batch'";
        sql += " ,[更新プログラム] = 'KOWA011'";
        sql += " ,[更新日時] = '" + CommonUtil.getCurrentDateTime() + "'";
        sql += " WHERE ";
        sql += "     [商品コード] = '" + syouhinnCode + "'" ;
        sql += " AND [在庫種別] = '" + kbn + "'";

        return sql;
    }

    static Connection conn = null;

    static Logger logger = Logger.getLogger(KOWA011Job.class);

    public static void main(String[] args) {


        // kowa在庫
        String downKowaPath = Constants.STOCK_SOURCE_PATH + "kowa";
        String saveKowaSuccessPath = Constants.STOCK_EXEC_PATH + "kowa\\SUCCESS";
        String saveKowaFailPath = Constants.STOCK_EXEC_PATH + "kowa\\FAILD";
        // 大阪支社在庫
        String downOsakaPath = Constants.STOCK_SOURCE_PATH + "osaka";
        String saveOsakaSuccessPath = Constants.STOCK_EXEC_PATH + "osaka\\SUCCESS";
        String saveOsakaFailPath = Constants.STOCK_EXEC_PATH + "osaka\\FAILD";
        // NS在庫
        String downNsPath = Constants.STOCK_SOURCE_PATH + "ns";
        String saveNsSuccessPath = Constants.STOCK_EXEC_PATH + "ns\\SUCCESS";
        String saveNsFailPath = Constants.STOCK_EXEC_PATH + "ns\\FAILD";
        // NS通販在庫
        String downNstuuhanPath = Constants.STOCK_SOURCE_PATH + "ns通販";
        String saveNstuuhanSuccessPath = Constants.STOCK_EXEC_PATH + "ns通販\\SUCCESS";
        String saveNstuuhanFailPath = Constants.STOCK_EXEC_PATH + "ns通販\\FAILD";




        // kowa在庫ファイル
        File downKowaFolder = new File(downKowaPath);
        // 大阪支社在庫ファイル
        File downOsakaFolder = new File(downOsakaPath);
        // NS在庫ファイル
        File downNsFolder = new File(downNsPath);
        // NS通販在庫ファイル
        File downNstuuhanFolder = new File(downNstuuhanPath);

        // zk在庫ファイル



        Statement stmt = null;

        try {

            conn = DBUtil.getConnection();

            stmt = conn.createStatement();

            // パスが存在チェック
            if (!downKowaFolder.exists() && !downOsakaFolder.exists() &&
                    !downNsFolder.exists() && !downNstuuhanFolder.exists()) {
                System.out.println(downKowaFolder.getAbsolutePath() + "パスが存在しません");
                System.out.println(downOsakaFolder.getAbsolutePath() + "パスが存在しません");
                System.out.println(downNsFolder.getAbsolutePath() + "パスが存在しません");
                System.out.println(downNstuuhanFolder.getAbsolutePath() + "パスが存在しません");
                return;
            }






            // kowa在庫ファイルを取込
            List<File> downKowaResult = searchFile(downKowaFolder);
            if (downKowaResult.size() > 0) {
                String kowaFailMsg = fileCheck(downKowaResult);
                String kowaSuccessMsg = "";
                // kowa在庫共通エラー処理
                if ("".equals(kowaFailMsg)) {
                    // 対象ファイルを取込
                    File kowaSuccessFile = copy(downKowaPath, saveKowaSuccessPath);
                    // 取込成功ログ
                    kowaSuccessMsg = successMsg(downKowaResult, kowaSuccessFile);
                    // ログファイルに出力
                    writeLog.writeLog(kowaSuccessMsg, true, "1");
                    // ＤＢに登録/更新
                    save(kowaSuccessFile, stmt, "1");
                } else {
                    // 対象ファイルを取込
                    File kowaFailFile = copy(downKowaPath, saveKowaFailPath);
                    // 取込エラーログ
                    kowaFailMsg = failMsg(downKowaResult, kowaFailFile);
                    // ログファイルに出力
                    writeLog.writeLog(kowaFailMsg, false, "1");
                    // テーブル「通報メールキューT」に登録
                    mailSave(kowaFailFile, "kowa在庫", kowaFailMsg, stmt);
                }
            }
            // 大阪支社在庫ファイルを取込
            List<File> downOsakaResult = searchFile(downOsakaFolder);
            if (downOsakaResult.size() > 0) {
                String osakaFailMsg = fileCheck(downOsakaResult);
                String osakaSuccessMsg = "";
                // 大阪支社在庫共通エラー処理
                if ("".equals(osakaFailMsg)) {
                    // 対象ファイルを取込
                    File osakaSuccessFile = copy(downOsakaPath, saveOsakaSuccessPath);
                    // 取込成功ログ
                    osakaSuccessMsg = successMsg(downOsakaResult, osakaSuccessFile);
                    // ログファイルに出力
                    writeLog.writeLog(osakaSuccessMsg, true, "2");
                    // ＤＢに登録/更新
                    save(osakaSuccessFile, stmt, "2");
                } else {
                    // 対象ファイルを取込
                    File osakaFailFile = copy(downOsakaPath, saveOsakaFailPath);
                    // 取込エラーログ
                    osakaFailMsg = failMsg(downOsakaResult, osakaFailFile);
                    // ログファイルに出力
                    writeLog.writeLog(osakaFailMsg, false, "2");
                    // テーブル「通報メールキューT」に登録
                    mailSave(osakaFailFile, "大阪支社在庫", osakaFailMsg, stmt);
                }
            }
            // NS在庫ファイルを取込
            List<File> downNsResult = searchFile(downNsFolder);
            if (downNsResult.size() > 0) {
                String nsFailMsg = fileCheck(downNsResult);
                String nsSuccessMsg = "";
                // 大阪支社在庫共通エラー処理
                if ("".equals(nsFailMsg)) {
                    // 対象ファイルを取込
                    File nsSuccessFile = copy(downNsPath, saveNsSuccessPath);
                    // 取込成功ログ
                    nsSuccessMsg = successMsg(downNsResult, nsSuccessFile);
                    // ログファイルに出力
                    writeLog.writeLog(nsSuccessMsg, true, "3");
                    // ＤＢに登録/更新
                    save(nsSuccessFile, stmt, "3");
                } else {
                    // 対象ファイルを取込
                    File nsFailFile = copy(downNsPath, saveNsFailPath);
                    // 取込エラーログ
                    nsFailMsg = failMsg(downNsResult, nsFailFile);
                    // ログファイルに出力
                    writeLog.writeLog(nsFailMsg, false, "3");
                    // テーブル「通報メールキューT」に登録
                    mailSave(nsFailFile, "NS在庫", nsFailMsg, stmt);
                }
            }
            // NS通販在庫ファイルを取込
            List<File> downNstuuhanResult = searchFile(downNstuuhanFolder);
            if (downNstuuhanResult.size() > 0) {
                String nstuuhanFailMsg = fileCheck(downNstuuhanResult);
                String nstuuhanSuccessMsg = "";
                // 大阪支社在庫共通エラー処理
                if ("".equals(nstuuhanFailMsg)) {
                    // 対象ファイルを取込
                    File nstuuhanSuccessFile = copy(downNstuuhanPath, saveNstuuhanSuccessPath);
                    // 取込成功ログ
                    nstuuhanSuccessMsg = successMsg(downNstuuhanResult, nstuuhanSuccessFile);
                    // ログファイルに出力
                    writeLog.writeLog(nstuuhanSuccessMsg, true, "4");
                    // ＤＢに登録/更新
                    save(nstuuhanSuccessFile, stmt, "4");
                } else {
                    // 対象ファイルを取込
                    File nstuuhanFailFile = copy(downNstuuhanPath, saveNstuuhanFailPath);
                    // 取込エラーログ
                    nstuuhanFailMsg = failMsg(downNstuuhanResult, nstuuhanFailFile);
                    // ログファイルに出力
                    writeLog.writeLog(nstuuhanFailMsg, false, "4");
                    // テーブル「通報メールキューT」に登録
                    mailSave(nstuuhanFailFile, "NS通販在庫", nstuuhanFailMsg, stmt);
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }







}