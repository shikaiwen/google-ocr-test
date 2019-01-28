package common;

import java.sql.ResultSet;
import java.sql.Statement;

public class CommonErrEvent04 {

    /**
	 * エラー共通処理
	 *
	 * @return
	 */
    public static void CommonErrEvent04(String programId, String kbn, String msg, Statement stmt, WriteLog04 writeLog, String kenmei, String mailText) {
    	// 受信者メール
    	String sendUser = "";
    	// 件名
    	String fileName = "";
    	// フォーマット
    	String content = "";
    	String text = "";

    	String sql1 = "";
    	String sql2 = "";

    	try {
        	if ("0".equals(kbn) || "1".equals(kbn)) {
        		fileName = msg;
        		content = CommonUtil.getCurrentDateTime() + "," + msg;
        		text = CommonUtil.getCurrentDateTime() + "," + msg;
        	} else {
        		fileName = kenmei;
        		content = msg;
        		text = mailText;
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
                    sql2 += " ,'" + text.replace("'", "''") + "'";
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
            writeLog.writeLog04(content, false);

    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
}
