package common;

import java.io.*;

/**
 * CSV
 */
public class WriteErrCSV {

	private String double_quotation_marks = "\"";

	private String comma_marks = ",";

	private BufferedWriter csvFileOutputStream = null;

	private File csvFile = null;

	public WriteErrCSV(String folder) {
	   	String outPutPath = Constants.CSV_ROOT_PATH + folder + "\\";
    	String fileName = "newerr.csv";
		try {
			File file = new File(outPutPath);
			file.mkdirs();
			// ファイル設定
			csvFile = new File(outPutPath + "\\" + fileName);
			csvFileOutputStream = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(csvFile), "UTF-8"), 1024);

		} catch (IOException e) {
			reNameCsv(folder);
		}
	}

    /**
	 * エラーログをCSVァイルに出力
	 *
	 * @return
	 */
    public void writeCsv(String[] errData) {

		try {
			// ","を取り込み
			for (int j = 0; j < errData.length; j++) {
				csvFileOutputStream.append(double_quotation_marks + CommonUtil.nullToBlank(errData[j])
						+ double_quotation_marks);
				if (j + 1 < errData.length) {
					csvFileOutputStream.append(comma_marks);
				} else if (j + 1 == errData.length) {
					csvFileOutputStream.append("\n");
				}
			}
			csvFileOutputStream.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	/**
	 * Csvファイルを閉じる
	 *
	 * @return
	 */
	public void reNameCsv(String folder) {
		try {
			csvFileOutputStream.close();
			csvFile.renameTo(new File(Constants.CSV_ROOT_PATH + folder + "\\err.csv"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Csvファイルを閉じる
	 *
	 * @return
	 */
	public void deleteCsv(String folder) {
		try {
			csvFileOutputStream.close();
			csvFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}