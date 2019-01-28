package common;

import common.Constants.ExcelType;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReadExcelUtil {

	/**
	 * readExcel
	 *
	 * @param strFile
	 * @return
	 */
	public static Map<String, List<List<String>>> readExcel(String strFile, int intRow, String strType) {

		Map<String, List<List<String>>> dataMap = new HashMap<String, List<List<String>>>();
		List<String> dataLst = null;
		List<List<String>> meisaiLst = new ArrayList<List<String>>();
		List<List<String>> tempLst = new ArrayList<List<String>>();
		List<String> errLst = new ArrayList<String>();
		String strFileName = strFile;
		try {
			InputStream is = new FileInputStream(strFileName);
			XSSFWorkbook wb = new XSSFWorkbook(is);
			XSSFSheet st = wb.getSheetAt(0);
			if (st.getLastRowNum() == 0) {
				errLst.add("該当ファイルが空白ため、取込を中止しました。");
				tempLst.add(errLst);
				dataMap.put("msg", tempLst);
				return dataMap;
			}

			for (int rowNum = intRow; rowNum <= st.getLastRowNum(); rowNum++) {
				XSSFRow xssfRow = st.getRow(rowNum);
				if (xssfRow != null) {
					dataLst = new ArrayList<String>();
					tempLst = new ArrayList<List<String>>();
					if (ExcelType.EXCEL_TYPE_A.equals(strType) && rowNum == intRow) {
						dataLst.add(getValue(xssfRow.getCell(0)));
						tempLst.add(dataLst);
						dataMap.put("simekiri", tempLst);
					} else {
						for (int colNum = 0; colNum < xssfRow.getLastCellNum(); colNum++) {
							switch (xssfRow.getCell(colNum).getCellType()) {
								case HSSFCell.CELL_TYPE_FORMULA:
									dataLst.add(String.valueOf(xssfRow.getCell(colNum).getNumericCellValue()));
									break;
								default :
									dataLst.add(getValue(xssfRow.getCell(colNum)));
							}
						}
						if ((ExcelType.EXCEL_TYPE_A.equals(strType) && rowNum == intRow + 1) ||
							(!ExcelType.EXCEL_TYPE_A.equals(strType) && rowNum == intRow)) {
							tempLst.add(dataLst);
							dataMap.put("title", tempLst);
						} else {
							meisaiLst.add(dataLst);
						}
					}
				}
			}
			dataMap.put("detail", meisaiLst);
			dataMap.get("title").get(0);
		} catch (Exception e) {
			tempLst = new ArrayList<List<String>>();
			errLst.add("取込ファイルを読み込む時、エラーが発生しました。");
			tempLst.add(errLst);
			dataMap.put("msg", tempLst);
		}
		return dataMap;
	}

	private static String getValue(XSSFCell cell) throws Exception {

		String value = "";
		if (cell != null) {
			if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				value = String.valueOf(cell.getNumericCellValue());
				 if (HSSFDateUtil.isCellDateFormatted(cell)) {
					 double d = cell.getNumericCellValue();
					 Date date = HSSFDateUtil.getJavaDate(d);
					 SimpleDateFormat sFormat=new SimpleDateFormat("yyyy/MM/dd");
					 value = sFormat.format(date);
				 }

			     if (value.indexOf(".") != -1 && value.indexOf("E") != -1) {
			    	 DecimalFormat df = new DecimalFormat();
			    	 value = df.parse(value).toString();
			     }

			     if (value.endsWith(".0")) {
			    	 int size = value.length();
			    	 value = value.substring(0, size - 2);
			     }
			} else {
				value = String.valueOf(cell.getStringCellValue());
			}
		} else {
			value = "";
		}
	     return value;
	}
}
