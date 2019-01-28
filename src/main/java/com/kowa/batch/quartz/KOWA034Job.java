package com.kowa.batch.quartz;

import common.*;
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
import java.util.*;

public class KOWA034Job extends JobWrapper  {


	public static Boolean tyumonGouseiCheckFlag = false;
	public static String tyumonGouseiMsg = "";
	public static Connection conn = null;
	public static Statement stmt = null;
	public static Statement updStmt = null;
	public static WriteLog writeLog = new WriteLog("STOCK");
	public static WriteErrCSV writeErrCSV = new WriteErrCSV("STOCK");
	public static String apiName = "在庫情報取得WebAPI";
	public static String programId = "KOWA034";
	public static boolean flag = true;
	public static String urlFlag = "";
	public static String[] errString = new String[3];

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 在庫伝票情報
		String urlTyumon="";
		String item_code = "";
		String md5password = "";
		int count = 0;
		File file = null;
		//データベース情報取得--商品コード　と　在庫数
		try {
			try {
                conn = DBUtil.getConnection();
				updStmt = conn.createStatement();
			} catch (Exception e) {
    			CommonErrEvent.commonErrEvent("", programId, "0", "", apiName, "", null, updStmt, writeLog, writeErrCSV, null);
    			flag = false;
    			return;
            }

			file = new File(Constants.CSV_ROOT_PATH + "STOCK\\err.csv");
			if (file.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				 String line = null;
				 while((line = reader.readLine()) != null){
					 String item[] = line.split(",");
					 urlTyumon = item[1].replace("\"","");
					 count = Integer.valueOf(item[2].replace("\"",""));

					 errString[0] = "1";
					 errString[1] = urlTyumon;
					 errString[2] = String.valueOf(count + 1);
					 if (count <= 2) {
						 executeBatch(urlTyumon);
					 }
				 }
				 reader.close();
			}
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(selectzaiko());
			if (rs.getRow() == 0) {
				flag = false;
			}
			while (rs.next()) {
				item_code = rs.getString(1);
				// [WEB-API]md5
				md5password = toMD5("account=1131&item_code=" + item_code + "KOWA");

				// 在庫伝票情報URL
				urlTyumon = "https://crossmall.jp/webapi2/get_stock?account=1131&item_code="+ item_code +"&signing="+ md5password;
				//データベース情報取得--商品コード　と　在庫数
				errString[0] = "1";
				errString[1] = urlTyumon;
				errString[2] = String.valueOf(0);
				executeBatch(urlTyumon);
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
				if (updStmt != null) {
					updStmt.close();
				}
				writeLog.closeLog();
				if (flag) {
					file.delete();
					writeErrCSV.reNameCsv("STOCK");
        		} else {
        			writeErrCSV.deleteCsv("STOCK");
        		}
			} catch (Exception e) {}
		}
	}

	/**
	 * @param args
	 */
	public static void executeBatch(String urlTyumon) {
		// 在庫伝票情報
		String jsonTyumon = "";
		String tyumonGouseiSuccessFlag = "";
		String tyumonGouseiErrMsg = "";
		String tyumonGouseiResponse = "";
		List<Map<String, Object>> listTyumon = null;
//		//TODO SLEEP
//		Thread.sleep(50);
		// 在庫伝票情報URLのxmlを取得
		jsonTyumon = loadJSON(urlTyumon);
		listTyumon = readStringXmlOutTyumonGousei(jsonTyumon);

		tyumonGouseiSuccessFlag = listTyumon.get(0).get("GetStatus").toString();
		tyumonGouseiErrMsg = listTyumon.get(0).get("Message").toString();

		//Success と errorの判断
		if ("success".equals(tyumonGouseiSuccessFlag)) {
			// レスポンス内容
			tyumonGouseiResponse = listTyumon.get(1).get("item_code") +"," +listTyumon.get(1).get("stock");

			// CHECK商品コード  と  在庫数
			tyumonGouseiCheckFlag = false;
			chkXmlData(listTyumon.get(1));

			if (tyumonGouseiCheckFlag) {
				if ("".equals(urlFlag)) {
					CommonErrEvent.commonErrEvent(urlTyumon, programId, "2", tyumonGouseiMsg, apiName, tyumonGouseiResponse, errString, updStmt, writeLog, writeErrCSV, true);
					urlFlag = urlTyumon;
				} else if (!urlFlag.contains(urlTyumon)) {
					CommonErrEvent.commonErrEvent(urlTyumon, programId, "2", tyumonGouseiMsg, apiName, tyumonGouseiResponse, errString, updStmt, writeLog, writeErrCSV, true);
					urlFlag = urlFlag + urlTyumon;
				} else {
					//CommonErrEvent.commonErrEvent(urlTyumon, programId, "2", tyumonGouseiMsg, apiName, tyumonGouseiResponse, errString, updStmt, writeLog, writeErrCSV, false);
				}

				return;
			}
			// テーブル「在庫情報取得API」に更新/登録処理
			tyumonGouseiErrMsg = saveTyumonGousei(listTyumon.get(1));
			if ("".equals(tyumonGouseiErrMsg)) {
				writeLog.writeLog(CommonUtil.getCurrentDateTime() + "," + apiName + "," + urlTyumon + ",success", true);
			} else {
				if ("".equals(urlFlag)) {
					CommonErrEvent.commonErrEvent(urlTyumon, programId, "1", tyumonGouseiErrMsg, apiName, tyumonGouseiResponse, errString, updStmt, writeLog, writeErrCSV, true);
					urlFlag = urlTyumon;
				} else if (!urlFlag.contains(urlTyumon)) {
					CommonErrEvent.commonErrEvent(urlTyumon, programId, "1", tyumonGouseiErrMsg, apiName, tyumonGouseiResponse, errString, updStmt, writeLog, writeErrCSV, true);
					urlFlag = urlFlag + urlTyumon;
				} else {
					//CommonErrEvent.commonErrEvent(urlTyumon, programId, "1", tyumonGouseiErrMsg, apiName, tyumonGouseiResponse, errString, updStmt, writeLog, writeErrCSV, false);
				}

			}
		} else if (!"商品コードが見つかりません。".equals(tyumonGouseiErrMsg)) {
			if ("".equals(urlFlag)) {
				CommonErrEvent.commonErrEvent(urlTyumon, programId, "2", tyumonGouseiErrMsg, apiName, tyumonGouseiResponse, errString, updStmt, writeLog, writeErrCSV, true);
				urlFlag = urlTyumon;
			} else if (!urlFlag.contains(urlTyumon)) {
				CommonErrEvent.commonErrEvent(urlTyumon, programId, "2", tyumonGouseiErrMsg, apiName, tyumonGouseiResponse, errString, updStmt, writeLog, writeErrCSV, true);
				urlFlag = urlFlag + urlTyumon;
			} else {
				//CommonErrEvent.commonErrEvent(urlTyumon, programId, "2", "商品コードが見つかりません。", apiName, tyumonGouseiResponse, errString, updStmt, writeLog, writeErrCSV, false);
			}

		}
	}

	/**
	 * XML取得
	 *
	 * @return
	 */
	public static String loadJSON(String url) {
		StringBuilder json = new StringBuilder();
		try{
			URL requestUrl = new URL(url);
			URLConnection yc = requestUrl.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
			String inputLine = null;
			while((inputLine = in.readLine()) != null) {
				json.append(inputLine);
			}
			in.close();
		}catch(MalformedURLException e){
		}catch(IOException e){
		}
		return json.toString();
	}

	/**
	 * md5パスワード情報取得
	 *
	 * @return
	 */
	public static String toMD5(String plainText){
		StringBuffer buf = new StringBuffer("");
		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(plainText.getBytes());
			byte b[] = md.digest();
			int i;
			for(int offset = 0; offset<b.length; offset++) {
				i = b[offset];
				if(i < 0)
					i += 256;
				if(i < 16)
					buf.append("0");
					buf.append(Integer.toHexString(i));
			}

		}catch(Exception e){
			e.printStackTrace();
		}
		return buf.toString();
	}

	/**
	 * 在庫数情報取得
	 *
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static List<Map<String, Object>> readStringXmlOutTyumonGousei(String xml){
		Map<String, Object> map = new HashMap<String, Object>();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Document doc = null;
		try{
			doc = DocumentHelper.parseText(xml);
			Element rootElt = doc.getRootElement();
			Iterator iter = rootElt.elementIterator("ResultSet");
			while (iter.hasNext()) {
				Element recordEle = (Element)iter.next();
				Iterator iters = recordEle.elementIterator("ResultStatus");
				while(iters.hasNext()){
					Element itemEle=(Element)iters.next();
					String GetStatus=itemEle.elementTextTrim("GetStatus");
					String Message=itemEle.elementTextTrim("Message");
					map.put("GetStatus",GetStatus);
					map.put("Message",Message);
					list.add(map);
				}
				Iterator iterss = recordEle.elementIterator("Result");
				while (iterss.hasNext()) {
					Element itermEle = (Element)iterss.next();
					map = new HashMap<String, Object>();
					//商品コード
					String item_code = itermEle.elementTextTrim("item_cd");
					//在庫数量
					String stock1 = itermEle.elementTextTrim("stock");
					Long stock = Long.parseLong(stock1);
					map.put("item_code",item_code);
					map.put("stock",stock);
					list.add(map);
				}
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * テーブル「商品マスクMT」に更新/登録処理
	 *
	 * @return
	 * @throws SQLException
	 */
    public static String saveTyumonGousei(Map<String, Object> dataMap) {
		String item_code = "";
		Long stock = 0l;
		item_code = String.valueOf(dataMap.get("item_code"));
		//在庫数非空判断
		if (!"".equals(CommonUtil.nullToBlank(dataMap.get("stock")))) {
			stock = CommonUtil.cvtToLong((dataMap.get("stock")));
		} else {
			stock = null;
		}
		try {
			updStmt.execute(updatezaiko(item_code, stock));
			return "";
		} catch (SQLException e) {
			return e.getMessage();
		}
    }
	/**
	 * テーブル「商品マスクMT」に更新/検索チャック処理
	 *
	 * @return
	 */
	public static void chkXmlData(Map<String, Object> map) {
		//商品コード判断
		String value_item_code = CommonUtil.nullToBlank(map.get("item_code"));
		String msg ="";
		msg = CheckUtil.chkNull(value_item_code, "商品コード", "1");
		if (!"".equals(msg)) {
			tyumonGouseiMsg = "<在庫情報取得API>商品コードが取得できません。";
			tyumonGouseiCheckFlag = true;
		}

		//在庫数判断
		String value_stock = CommonUtil.nullToBlank(map.get("stock"));
		msg = CheckUtil.chkNull(value_stock, "CROSSMALL在庫", "1");
		if(!"".equals(msg)) {
			if("".equals(tyumonGouseiMsg)) {
				tyumonGouseiMsg = "<在庫情報取得API>在庫数が取得できません。";
			} else {
				tyumonGouseiMsg = tyumonGouseiMsg + "," + "<在庫情報取得API>在庫数が取得できません。";
			}
			tyumonGouseiCheckFlag = true;
		}
	}
	/**
	 * テーブル「商品マスクMT」に更新/検索商品コード処理
	 *
	 * @return
	 */
	public static String selectzaiko() {

		StringBuffer sb = new StringBuffer();
		sb.append(" SELECT ");
		sb.append(" 商品コード ");
		sb.append(" FROM ");
		sb.append(" 商品マスタMT ");

		return sb.toString();
	}

	/**
	 * テーブル「商品マスクMT」に更新/更新在庫数処理
	 *
	 * @return
	 */
	public static String updatezaiko(String item_code,long zaikoSu) {

		StringBuffer sb=new StringBuffer();
		sb.append(" UPDATE ");
		sb.append(" 商品マスタMT SET ");
		sb.append("  CROSSMAll在庫=" + zaikoSu);
		sb.append(" ,更新ユーザ= 'BATCH' ");
		sb.append(" ,更新プログラム='" + programId + "'");
		sb.append(" ,更新日時='" + CommonUtil.getCurrentDateTime()  + "'");
		sb.append(" WHERE ");
		sb.append(" 商品コード='" + item_code + "'");

		return sb.toString();
	}
}
