package common;


import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class CheckUtil {

	/**
	 * 必須チェック
	 *
	 * @param obj
	 * @param itemName
	 * @param type (1:String, 2:Integer/Long, 3:ComboBox)
	 * @return
	 */
	public static String chkNull(Object obj, String itemName, String type) {

		boolean flag = false;

		// 文字列の場合
		if ("1".equals(type)) {
			if (obj == null || "".equals(obj.toString().trim())) {
				flag = true;
			}

		// 数字の場合
		} else if ("2".equals(type)) {
			if (obj == null) {
				flag = true;
			}

		// ComboBox
		} else {
			if ((Long) obj == 1l) {
				flag = true;
			}
		}

		if (flag) {
			return "err";
		}
		return "";
	}

	/**
	 * 半角数字のチェック
	 *
	 * @param
	 * @return
	 */
	public static String chkNumberHalf(String str, String itemName) {

		if (!StringUtils.isEmpty(str) || str.length() > 0) {
			for (int i = 0; i < str.length(); i++) {
				char a = str.charAt(i);
				if (a < '0' || a > '9') {
					return "false";
				}
			}
		}
		return "";
	}

	/**
	 * 半角小数のチェック
	 *
	 * @param str
	 * @param length
	 * @param itemName
	 * @return
	 */
	public static String chkFloatHalf(String str, int length, String itemName) {
		if (!StringUtils.isEmpty(str)) {
			if (!chkHalf(str)) {
				return "false";
			}

			Pattern pattern = Pattern.compile("^(([1-9]{1}\\d*)|([0]{1}))(\\.(\\d){0," + length + "})?$");
			if (!pattern.matcher(str).matches()) {
				return "false";
			}
		}

		return "";
	}

	/**
	 * 半角文字列のチェック
	 *
	 * @param str
	 * @return
	 */
	public static boolean chkHalf(String str) {
		if (!StringUtils.isEmpty(str)) {
			if (str.length() != str.getBytes().length) {
				return false;
			}
		}
		return true;
	}
}
