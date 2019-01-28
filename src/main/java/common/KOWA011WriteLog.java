package common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * LOG
 */
public class KOWA011WriteLog {

	private BufferedWriter successf1output = null;
	private BufferedWriter faildf1output = null;

	private BufferedWriter successf2output = null;
	private BufferedWriter faildf2output = null;

	private BufferedWriter successf3output = null;
	private BufferedWriter faildf3output = null;

	private BufferedWriter successf4output = null;
	private BufferedWriter faildf4output = null;

	public KOWA011WriteLog() {
		String SuccessfileName = "success-" + CommonUtil.getCurrentDateTime().substring(0, 10).replace("/", "") + ".log";
		String FaildfileName = "faild-" + CommonUtil.getCurrentDateTime().substring(0, 10).replace("/", "") + ".log";
		try {
			File Successf1 = new File(Constants.KOWA011_LOG_PATH + "KOWA\\" + SuccessfileName);
			File faildf1 = new File(Constants.KOWA011_LOG_PATH + "KOWA\\" + FaildfileName);
			File Successf1Path = new File(Constants.KOWA011_LOG_PATH + "KOWA");
			File faildf1Path = new File(Constants.KOWA011_LOG_PATH + "KOWA");

			File Successf2 = new File(Constants.KOWA011_LOG_PATH + "OSAKA\\" + SuccessfileName);
			File faildf2 = new File(Constants.KOWA011_LOG_PATH + "OSAKA\\" + FaildfileName);
			File Successf2Path = new File(Constants.KOWA011_LOG_PATH + "OSAKA");
			File faildf2Path = new File(Constants.KOWA011_LOG_PATH + "OSAKA");

			File Successf3 = new File(Constants.KOWA011_LOG_PATH + "NS\\" + SuccessfileName);
			File faildf3 = new File(Constants.KOWA011_LOG_PATH + "NS\\" + FaildfileName);
			File Successf3Path = new File(Constants.KOWA011_LOG_PATH + "NS");
			File faildf3Path = new File(Constants.KOWA011_LOG_PATH + "NS");

			File Successf4 = new File(Constants.KOWA011_LOG_PATH + "NS通販\\" + SuccessfileName);
			File faildf4 = new File(Constants.KOWA011_LOG_PATH + "NS通販\\" + FaildfileName);
			File Successf4Path = new File(Constants.KOWA011_LOG_PATH + "NS通販");
			File faildf4Path = new File(Constants.KOWA011_LOG_PATH + "NS通販");

			Successf1Path.mkdirs();
			if (!Successf1.exists()) {
				Successf1.createNewFile();
			}
			successf1output = new BufferedWriter(new FileWriter(Successf1, true));
			faildf1Path.mkdirs();
			if (!faildf1.exists()) {
				faildf1.createNewFile();
			}
			faildf1output = new BufferedWriter(new FileWriter(faildf1, true));

			Successf2Path.mkdirs();
			if (!Successf2.exists()) {
				Successf2.createNewFile();
			}
			successf2output = new BufferedWriter(new FileWriter(Successf2, true));
			faildf2Path.mkdirs();
			if (!faildf2.exists()) {
				faildf2.createNewFile();
			}
			faildf2output = new BufferedWriter(new FileWriter(faildf2, true));

			Successf3Path.mkdirs();
			if (!Successf3.exists()) {
				Successf3.createNewFile();
			}
			successf3output = new BufferedWriter(new FileWriter(Successf3, true));
			faildf3Path.mkdirs();
			if (!faildf3.exists()) {
				faildf3.createNewFile();
			}
			faildf3output = new BufferedWriter(new FileWriter(faildf3, true));

			Successf4Path.mkdirs();
			if (!Successf4.exists()) {
				Successf4.createNewFile();
			}
			successf4output = new BufferedWriter(new FileWriter(Successf4, true));
			faildf4Path.mkdirs();
			if (!faildf4.exists()) {
				faildf4.createNewFile();
			}
			faildf4output = new BufferedWriter(new FileWriter(faildf4, true));

		} catch (IOException e) {
			closeLog();
		}
	}

	/**
	 * ログをログファイルに出力
	 *
	 * @return
	 */
    public void writeLog(String content, Boolean flag, String kbn) {

        try {
        	if (flag) {
        		if ("1".equals(kbn)) {
        			successf1output.append(content + "\n");
        			successf1output.flush();
        		} else if ("2".equals(kbn)) {
        			successf2output.append(content + "\n");
        			successf2output.flush();
        		} else if ("3".equals(kbn)) {
        			successf3output.append(content + "\n");
        			successf3output.flush();
        		} else if ("4".equals(kbn)) {
        			successf4output.append(content + "\n");
        			successf4output.flush();
        		}
        	} else {
        		if ("1".equals(kbn)) {
        			faildf1output.append(content + "\n");
        			faildf1output.flush();
        		} else if ("2".equals(kbn)) {
        			faildf2output.append(content + "\n");
        			faildf2output.flush();
        		} else if ("3".equals(kbn)) {
        			faildf3output.append(content + "\n");
        			faildf3output.flush();
        		} else if ("4".equals(kbn)) {
        			faildf4output.append(content + "\n");
        			faildf4output.flush();
        		}
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	/**
	 * ログファイルを閉じる
	 *
	 * @return
	 */
	public void closeLog() {

		try {
			successf1output.close();
			faildf1output.close();

			successf2output.close();
			faildf2output.close();

			successf3output.close();
			faildf3output.close();

			successf4output.close();
			faildf4output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}