package common;

public class Constants {

	/**
	 * DBのPATH
	 */
//	public final static String DB_URL = "jdbc:sqlserver://ISSOH998;instanceName=SQLEXPRESS;DatabaseName=KOWA";
	public final static String DB_URL = "jdbc:sqlserver://AONSERVER-2;instanceName=AONINSTANCE;DatabaseName=KOWA";

	/**
	 * DBのUSER_NAME
	 */
	public final static String USER_NAME = "sa";

	/**
	 * DBのPASSWORD
	 */
	public final static String PASSWORD = "sysad";

	public final static String ROOT_DIR = "D:\\KOWABATCH\\";

	/**
	 * CSV ROOTPATH
	 */
	public final static String CSV_ROOT_PATH = ROOT_DIR + "kowa\\import\\source\\CROSSMALL\\";

	/**
	 * LOG ROOTPATH
	 */
	public final static String LOG_ROOT_PATH = ROOT_DIR + "kowa\\import\\log\\CROSSMALL\\";

	public final static String LOG_ROOT_PATH_MAIL = ROOT_DIR + "kowa\\import\\log\\";

	public static class ExcelType {
		public static final String EXCEL_TYPE_A = "A";
		public static final String EXCEL_TYPE_H = "H";
		public static final String EXCEL_TYPE = "M";
	}

	/**
	 * 送信
	 */
	public final static String POST_MAIL_ADDR = "issohcojp@gmail.com";

	/**
	 * SMTP
	 */
	public final static String MAIL_SMTP = "smtp.gmail.com";

	/**
	 * SMTP
	 */
	public final static String MAIL_USER = "issohcojp@gmail.com";

	/**
	 * SMTP
	 */
	public final static String MAIL_PASSWORD = "issoh.co.jp";

	/**
	 * 件名
	 */
	public final static String MAIL_NAME = "テスト件名";

	/**
	 * 本文
	 */
	public final static String MAIL_FORM_HEAD = "xx様\nいつもお世話になっております。凰和商事のｘｘです。";

	/**
	 * 本文
	 */
	public final static String MAIL_FORM_FOOT = "以上です、どうぞ宜しくお願いします。";


	public final static String STOCK_SOURCE_PATH = ROOT_DIR + "kowa\\import\\source\\stock\\";

	public final static String STOCK_EXEC_PATH = ROOT_DIR + "kowa\\import\\exec\\stock\\";

	public final static String KOWA011_LOG_PATH = ROOT_DIR + "kowa\\import\\log\\stock\\";



}

