package com.kowa.batch.quartz;

import common.CommonUtil;
import common.Constants;
import common.DBUtil;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * PriceSearch file parse
 * @kevin 2018-05-24
 */
public class KOWA042Job extends JobWrapper{

    static Logger logger = Logger.getLogger(KOWA042Job.class);


    public static void main(String[] args) {

        try {
            handlePriceSearch();
        } catch (Exception e) {
            logger.error("failed", e);
        }
    }


    static void handlePriceSearch() throws SQLException {

        logger.debug("start kowa042...");
        String rootPath = Constants.STOCK_SOURCE_PATH + "PriceSearch";
        String successPath = rootPath + "\\SUCCESS";
        String failPath = rootPath + "\\FAILED";
        String currentDateTime = CommonUtil.getCurrentDateTime();

        File[] fileList = new File(rootPath).listFiles(pathname -> pathname.isFile());
        if (fileList.length == 0){
            return;
        }

        File processFile = fileList[0];

        InputStream is = null;
        XSSFWorkbook wb = null;
        List<Object[]> param = new LinkedList<>();
        try {
            is = new FileInputStream(processFile);
            wb = new XSSFWorkbook(is);

            XSSFSheet sheet = wb.getSheetAt(0);

            // zero based
            int startRow = 8;
            int lastRowNum = sheet.getLastRowNum();

//            Set<String> sets = new HashSet();

            for (int i = startRow; i <= lastRowNum; i++) {

                XSSFRow row = sheet.getRow(i);
                String stringCellValue = row.getCell(4).getStringCellValue();

                param.add(Arrays.asList("1", currentDateTime, stringCellValue).toArray());
            }
        } catch (IOException e) {
            logger.error("Failed", e);
            CommonUtil.moveToHandledDir(processFile, failPath);
            return;
        }

        if(param.size() == 0){
            return;
        }
        Connection conn = DBUtil.getConnection();

        // テーブル[商品マスタMT]　更新
        String updateSql1 = "UPDATE [商品マスタMT] SET [PS登録] = ? , [更新ユーザ] = 'batch' , [更新プログラム] = 'KOWA042' , [更新日時] = ? WHERE 商品コード = ?";
        try{
            conn.setAutoCommit(false);

            // set all product pricesearch to unchecked state
            QueryRunner runner = new QueryRunner();
            runner.update(conn, "UPDATE [商品マスタMT] SET [PS登録] = '0'");

            List<List<Object[]>> partition = ListUtils.partition(param, 100);

            for (int i = 0; i < partition.size(); i++) {
                List<Object[]> objects = partition.get(i);
                int[] batch = runner.batch(conn, updateSql1, param.toArray(new Object[0][0]));
            }
//            partition.forEach(item->{
//                try {
//
//                } catch (SQLException e) {
//                    e.printStackTrace();
//                    logger.error("", e);
//                }
//            });
            conn.commit();

            CommonUtil.moveToHandledDir(processFile, successPath);
        }catch (SQLException e){
            logger.error("database update error , move file to failed directory ");
            CommonUtil.moveToHandledDir(processFile, failPath);
            logger.error("", e);
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error("",e1);
            }
        }finally {
            DbUtils.closeQuietly(conn);
        }

    }



}
