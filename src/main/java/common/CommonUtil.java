package common;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CommonUtil {

	/**
	 * 現在日付までの日数
	 *
	 * @return
	 */
	public static Integer diffWithCurrentDate(Date fromDate) {
		int days = 0;
		try {
			Date from = new SimpleDateFormat("yyyy/MM/dd").parse(new SimpleDateFormat("yyyy/MM/dd").format(fromDate));
			Date current = new SimpleDateFormat("yyyy/MM/dd").parse(getCurrentDate());
			long milliseconds = current.getTime() - from.getTime();
			days = (int)(milliseconds / (1000 * 60 * 60 * 24));
		} catch (ParseException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return days;
	}

	/**
	 * DBのdatetime類型のフィールドは「yyyy/MM/dd」型に転換する
	 *
	 * @param datetime
	 * @return
	 */
	public static String cvtDateTime(Object datetime) {
		if (datetime == null || String.valueOf(datetime).length() < 10) {
			return "";
		} else {
			return String.valueOf(datetime).substring(0, 10).replace("-", "/");
		}
	}

	/**
	 * DBのdatetime類型のフィールドは「yyyy/MM/dd」型に転換する
	 *
	 * @param datetime
	 * @return
	 */
	public static String cvtDateTime(String datetime) {
		if (datetime == null || datetime.length() < 10) {
			return "";
		} else {
			return datetime.substring(0, 10).replace("-", "/");
		}
	}

	/**
	 * システム日時を取得する
	 *
	 * @return
	 */
	public static String getCurrentDateTime() {
		return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
	}

    public static String getFullCurrentDateTime() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

	/**
	 * システム日時を取得する
	 *
	 * @return
	 */
	public static String getCurrentDate() {
		return new SimpleDateFormat("yyyy/MM/dd").format(new Date());
	}

	/**
	 * システム日時を取得する
	 *
	 * @return
	 */
	public static String getBatachToDateTime() {
		return new SimpleDateFormat("yyyy-MM-dd+HH:mm:ss").format(new Date());
	}

	/**
	 * システム日時を取得する
	 *
	 * @return
	 */
	public static String getBatachFromDateTime(int minute) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.MINUTE, minute);
		return new SimpleDateFormat("yyyy-MM-dd+HH:mm:ss").format(calendar.getTime());
	}

	/**
	 * nullを空文字に転換する
	 *
	 * @param str
	 * @return
	 */
	public static String nullToBlank(Object str) {
		if (str == null) {
			return "";
		} else {
			return str.toString();
		}
	}

	/**
	 * Long類型に転換する
	 *
	 * @param num
	 * @return
	 */
	public static Long cvtToLong(Object obj) {
		if (obj == null) {
			return null;
		} else {
			return Long.valueOf(Math.round(Double.valueOf(obj.toString())));
		}
	}

	/**
	 * sqlConvert
	 *
	 * @param str
	 * @return
	 */
	public static String escapeSql(String str) {

		return str.replaceAll("'", "''");
	}


    /**
     *
     * @param file
     */
    public static void moveToHandledDir(File file, String destPath){

        try {
            File newFile = new File(file.getParent(), CommonUtil.getFullCurrentDateTime() + file.getName());
            // rename current processed file
            Files.move(file.toPath(), newFile.toPath());
            File destDir = new File(destPath);
            FileUtils.moveFileToDirectory(newFile, destDir, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
