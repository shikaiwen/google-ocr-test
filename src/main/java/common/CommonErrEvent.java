package common;

import java.sql.ResultSet;
import java.sql.Statement;

public class CommonErrEvent {

    /**
	 * エラー共通処理
	 *
	 * @return
	 */
    public static void commonErrEvent(String url, String programId, String kbn, String msg, String apiName, String response, String[] errString, Statement stmt, WriteLog writeLog, WriteErrCSV writeErrCSV, Boolean csvFlag) {
    	// 受信者メール
    	String sendUser = "";
    	// 本文
    	String text = "";
    	// 件名
    	String fileName = "";
    	// フォーマット
    	String content = "";

    	String sql1 = "";
    	String sql2 = "";

    	try {
    		fileName = apiName + "エラー発生。";
        	content = CommonUtil.getCurrentDateTime() + "," + apiName + "," + url + "," + msg;
        	if (!"".equals(response)) {
        		content = content +  "," + response;
        	}
        	if ("0".equals(kbn)) {
        		content = CommonUtil.getCurrentDateTime() + "," + apiName + "は接続タイムアウトしました。";
        	} else if ("3".equals(kbn)) {
        		content = CommonUtil.getCurrentDateTime() + "," + apiName + "," + url + "," + "リクエストはタイムアウトしました。";
        	}

        	if ("0".equals(kbn)) {
        		text = apiName + "は接続タイムアウトしました。";
        	} else if ("1".equals(kbn)) {
        		text = "DBエラーが発生しました。" + msg.replaceAll("'", "\"");
        	} else if ("2".equals(kbn)) {
        		text = msg;
        	} else if ("3".equals(kbn)) {
        		text = "リクエストはタイムアウトしました。";
        	}

        	if (!"0".equals(kbn)) {
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

                if (!"".equals(sendUser)) {
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
                    sql2 += " ,'" + programId + "'";
                    sql2 += " ,'" + CommonUtil.getCurrentDateTime()  + "'";
                    sql2 += " ,'batch'";
                    sql2 += " ,'" + programId + "'";
                    sql2 += " ,'" + CommonUtil.getCurrentDateTime()  + "'";
                    sql2 += " ) ";

                    stmt.execute(sql2);
                }
        	}

        	// エラーログをログファイルに出力
            writeLog.writeLog(content, false);

        	// エラーcsvファイルを書出す
            if (!"0".equals(kbn) && csvFlag) {
            	writeErrCSV.writeCsv(errString);
            }

    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
}
