package com.kowa.batch.quartz;

import common.CommonUtil;
import common.Constants;
import common.DBUtil;
import common.NASUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author kevin
 * ZK1000, ZK2000, ZK3000
 * FBA在庫
 */
public class KOWA043Job extends JobWrapper {

    static Logger logger = Logger.getLogger(KOWA043Job.class);


    static String server = "ftp11.gmoserver.jp";
    static int port = 21;
    static String user = "sd0464952@gmoserver.jp";
    static String pass = "zKRs5K64";




    public static void testFtp(){
        try {

            FTPClient ftpClient = new FTPClient();
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
//            ftpClient.enterLocalPassiveMode();
//            ftpClient.enterLocalActiveMode();
            ftpClient.enterLocalActiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);


            String parentDir = "issoh.co.jp";
            boolean tmp = ftpClient.makeDirectory(parentDir + "/" + "ktmp");


            File file = new File("D:/aa.csv");
            InputStream inputStream = new FileInputStream(file);
            String destFile = parentDir + "/" + file.getName();
            boolean b = ftpClient.storeFile(destFile, inputStream);
            if(b){
                inputStream.close();
            }


            boolean rename = ftpClient.rename(destFile, parentDir + "/ktmp/"+file.getName());

            boolean b1 = ftpClient.deleteFile(destFile);


            FTPFile[] ftpFiles = ftpClient.listFiles();
            for (int i = 0; i < ftpFiles.length; i++) {
                System.out.println(ftpFiles[i].getName());

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws Exception{

//        testFtp();

        NASUtil.getNASFileBySmb();

        logger.debug("start process KOWA043");

        /**
         * zk 在庫
         * issoh 2018-05-24
         */

        handleZkStock();

        /**
         * FBA 在庫
         * issoh 2018-05-24
         */
        handleFBAStock();



    }


    /**
     * ZK1000 ZK2000 ZK3000 在庫処理
     * @param
     */
    static String zkRootPath = Constants.STOCK_SOURCE_PATH + "zk";
    static String zkSuccessPath = zkRootPath + "\\SUCCESS";
    static String zkFailPath = zkRootPath + "\\FAILED";
    static void handleZkStock()  throws Exception{

        File[] fileList = new File(zkRootPath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });


        if (fileList.length == 0){
            logger.debug("handle file not found in path :"+ zkRootPath);
            return;
        }



        Connection conn = DBUtil.getConnection();

        File file = fileList[0];


        List<Object[]> param1 = new ArrayList();
        List<Object[]> param2 = new ArrayList();
        List<Object[]> param3 = new ArrayList();

        String currentDateTime = CommonUtil.getCurrentDateTime();

        Iterable<CSVRecord> records = null;
        try {
            records = CSVFormat.RFC4180.parse(new FileReader(file));

            for (CSVRecord record : records) {
                String columnOne = record.get(0);
                String columnTwo = record.get(1);
                String columnThree = record.get(2);

                String column14 = record.get(14);   // excel 中的 O列
                Object[] rowValues = Arrays.asList(column14, currentDateTime, columnThree).toArray();

                if("1000".equals(columnOne)){
                    param1.add(rowValues);
                }else if("2000".equals(columnOne)){
                    param2.add(rowValues);
                } else if ("3000".equals(columnOne)) {
                    param3.add(rowValues);
                }
            }
        } catch (IOException e) {
            logger.error("read file error", e);
            return;
        }finally {
            try {
                ((CSVParser) records).close();
            } catch (IOException e) {
                logger.error("Failed",e);
            }
        }

        // テーブル[商品マスタMT]　更新
        String updateSql1 = "UPDATE [商品マスタMT] SET [ZK1000] = ? , [更新ユーザ] = 'batch' , [更新プログラム] = 'KOWA011' , [更新日時] = ? WHERE 商品コード = ?";
        String updateSql2 = "UPDATE [商品マスタMT] SET [ZK2000] = ? , [更新ユーザ] = 'batch' , [更新プログラム] = 'KOWA011' , [更新日時] = ? WHERE 商品コード = ?";
        String updateSql3 = "UPDATE [商品マスタMT] SET [ZK3000] = ? , [更新ユーザ] = 'batch' , [更新プログラム] = 'KOWA011' , [更新日時] = ? WHERE 商品コード = ?";

        try{
            conn.setAutoCommit(false);

            QueryRunner runner = new QueryRunner();

            runner.batch(conn, updateSql1,  param1.toArray(new Object[0][0]));
            runner.batch(conn, updateSql2,  param2.toArray(new Object[0][0]));
            runner.batch(conn, updateSql3,  param3.toArray(new Object[0][0]));
            conn.commit();

            CommonUtil.moveToHandledDir(file, zkSuccessPath);

        }catch (SQLException e){
            logger.error(e);
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error(e1);
            }
            CommonUtil.moveToHandledDir(file, zkFailPath);
        }finally {
            DbUtils.closeQuietly(conn);
        }
    }



    /**
     * FBA在庫
     */
    static String fbaRootPath = Constants.STOCK_SOURCE_PATH + "FBA在庫";
    static String fbaSuccessPath = fbaRootPath + "\\SUCCESS";
    static String fbaFailPath = fbaRootPath + "\\FAILED";

    static void handleFBAStock() throws Exception{

        List<Object[]> param1 = new ArrayList();

        File[] fileList = new File(fbaRootPath).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });

        if (fileList.length == 0){
            return;
        }
        File processFile = fileList[0];
        String currentDateTime = CommonUtil.getCurrentDateTime();

        Iterable<CSVRecord> records = null;
        try {
            records = CSVFormat.EXCEL.withHeader().parse(new FileReader(processFile));
            for (CSVRecord record : records) {

                String SKU = record.get("SKU");

                SKU = SKU.replace("=", "").replace("\"", "");
                String number = record.get("number");

                Object[] rowValues = Arrays.asList(number, currentDateTime, SKU).toArray();
                param1.add(rowValues);
            }
        } catch (IOException e) {
            logger.error("read file error", e);
        }finally {
            try {
                ((CSVParser) records).close();
            } catch (IOException e1) {
                logger.error("Failed", e1);
            }
        }

        if(param1.size() == 0){
            return;
        }

        // テーブル[商品マスタMT]　更新
        String updateSql1 = "UPDATE [商品マスタMT] SET [FBA在庫] = ? , [更新ユーザ] = 'batch' , [更新プログラム] = 'KOWA011' , [更新日時] = ? WHERE 商品コード = ?";

        Connection conn = DBUtil.getConnection();

        try{
            conn.setAutoCommit(false);

            QueryRunner runner = new QueryRunner();

            runner.batch(conn, updateSql1,  param1.toArray(new Object[0][0]));
            conn.commit();

            CommonUtil.moveToHandledDir(processFile, fbaSuccessPath);

        }catch (SQLException e){

            logger.error(e);
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error(e1);
            }
            CommonUtil.moveToHandledDir(processFile, fbaFailPath);
        }finally {
            DbUtils.closeQuietly(conn);
        }
    }

}
