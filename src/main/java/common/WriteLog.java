package common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * LOG
 */
public class WriteLog {

	private BufferedWriter success_output = null;

	private BufferedWriter faild_output = null;

	public WriteLog(String folder) {
		String fileName = CommonUtil.getCurrentDateTime().substring(0, 10).replace("/", "") + ".log";
		try {
			File success_LogFilePath = new File(Constants.LOG_ROOT_PATH + folder);
			File faild_LogFilePath = new File(Constants.LOG_ROOT_PATH + folder);
			File success_LogFile = new File(Constants.LOG_ROOT_PATH + folder + "\\success-" + fileName);
			File faild_LogFile = new File(Constants.LOG_ROOT_PATH + folder + "\\faild-" + fileName);
			success_LogFilePath.mkdirs();
			if (!success_LogFile.exists()) {
				success_LogFile.createNewFile();
			}
			success_output = new BufferedWriter(new FileWriter(success_LogFile, true));
			faild_LogFilePath.mkdirs();
			if (!faild_LogFile.exists()) {
				faild_LogFile.createNewFile();
			}
			faild_output = new BufferedWriter(new FileWriter(faild_LogFile, true));
		} catch (IOException e) {
			closeLog();
		}
	}

	/**
	 * ログをログファイルに出力
	 *
	 * @return
	 */
    public void writeLog(String content, Boolean flag) {

        try {
        	if (flag) {
            	success_output.append(content + "\n");
            	success_output.flush();
        	} else {
            	faild_output.append(content + "\n");
            	faild_output.flush();
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
			success_output.close();
			faild_output.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}