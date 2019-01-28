package common;

import java.util.Locale;

public class SystemConfig {

    /** アプリケーション設定のホームディレクトリ */
    public static String APP_CONFIG_HOME;

    /** テンプレートファイルのディレクトリ */
    public static String APP_TEMPLATE_DIR;

    /** アプリケーション名 */
    public static String APP_NAME;
    /** ディフォルト言語 */
    public static Locale LANGUAGE;

    public static String testA;
    public static String testB;
    public static String testC;

    public static class Option {
        public static int MAX_UPLOAD_SIZE = 50 * 1000;
    }

    public static class Mail {
        public static String SERVER;
        public static int PORT;
        public static String USER;
        public static String PASSWORD;
    }

}
