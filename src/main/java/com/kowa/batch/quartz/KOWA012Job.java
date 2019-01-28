package com.kowa.batch.quartz;

import common.Constants;
import common.DBUtil;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class KOWA012Job extends JobWrapper  {


    /**
     * テーブル「通報メールキューT」
     *
     * @return
     */
    public static List<Map<String, String>> searchDate(Statement stmt) {

        List<Map<String, String>> list = null;
        try {
            String sql = "";
            sql = " SELECT *  ";
            sql += " FROM 通報メールキューT  ";
            sql += " WHERE 送信フラグ IN ('0', '2')  ";

            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData md = rs.getMetaData();
            int columnCount = md.getColumnCount();
            list = new ArrayList<Map<String, String>>();
            Map<String, String> rowData = new HashMap<String, String>();
            while (rs.next()) {
                rowData = new HashMap<String, String>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    if (rs.getObject(i) != null) {
                        rowData.put(md.getColumnName(i), String.valueOf(rs.getObject(i)));
                    } else {
                        rowData.put(md.getColumnName(i), "");
                    }
                }
                list.add(rowData);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return list;
    }

    public static void UpdateSetting(Statement stmt, String inData) {

        String sql = "";
        try {
            sql = " UPDATE [通報メールキューT] SET ";
            sql += " [送信フラグ] = '1' ";
            sql += " WHERE ";
            sql += " [QueueNO] IN (" + inData + ")";

            stmt.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void email(Statement stmt, String addr, String name, String text, String No) {

        Boolean successFlag = false;
        try {
            // JavaMail メールの送信
            Properties properties = new Properties();
            // メールの送信
            properties.put("mail.transport.protocol", "smtp");
            // 検証を必要とする
            properties.put("mail.smtp.auth", "true");
            // 設置debug送信の過程
            properties.put("mail.smtp.debug", "true");

            // for gmail
            properties.put("mail.smtp.port", "587");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.connectiontimeout", "10000");
            properties.put("mail.smtp.timeout", "10000");

            Session session = Session.getInstance(properties);
            // メール情報
            Message messgae = new MimeMessage(session);
            messgae.setSubject(name);
            messgae.setSentDate(new Date());
            messgae.setFrom(new InternetAddress(Constants.POST_MAIL_ADDR));
            messgae.setText(text);
            Transport tran = session.getTransport();
            tran.connect(Constants.MAIL_SMTP, Constants.MAIL_USER,
                    Constants.MAIL_PASSWORD);
            String[] addrList = addr.split(",");
            Address[] addrAddress = new Address[addrList.length];
            for (int i = 0; i < addrList.length; i++) {
                addrAddress[i] = new InternetAddress(addrList[i]);
            }
            tran.sendMessage(messgae, addrAddress);
            tran.close();
            successFlag = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (successFlag) {
                delete(stmt, No);
            } else {
                update(stmt, No);
            }
        }
    }

    public static void delete(Statement stmt, String No) {

        String sql = "";
        try {
            sql = " DELETE FROM [通報メールキューT] WHERE [QueueNO] = " + No;

            stmt.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void update(Statement stmt, String No) {

        String sql = "";
        try {
            sql = " UPDATE [通報メールキューT] SET ";
            sql += " [送信フラグ] = '2' ";
            sql += " WHERE ";
            sql += " [QueueNO] = " + No;

            stmt.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {

        Connection conn = null;
        Statement stmt = null;

        try {
            conn = DBUtil.getConnection();

            stmt = conn.createStatement();

            List<Map<String, String>> list = searchDate(stmt);
            String updateRecord = "";

            for (int i = 0; i < list.size(); i++) {
                Map<String, String> map1 = list.get(i);
                if ("".equals(updateRecord)) {
                    updateRecord = map1.get("QueueNO");
                } else {
                    updateRecord = updateRecord + "," + map1.get("QueueNO");
                }
            }

            // １：送信中に変更する
            if (!"".equals(updateRecord)) {
                UpdateSetting(stmt, updateRecord);

                for (int i = 0; i < list.size(); i++) {
                    Map<String, String> map2 = list.get(i);
                    if (!"".equals(map2.get("受信者メール")) && !"".equals(map2.get("件名")) && !"".equals(map2.get("本文"))) {
                        email(stmt, map2.get("受信者メール"), map2.get("件名"), map2.get("本文"), map2.get("QueueNO"));
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}