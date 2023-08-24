package com.hn2.util;

/**
 * 全域系統共用參數
 *
 * @author hsien
 */
public class Common {
  public static final String SYSTEM_ERROR = "系統發生錯誤";
  public static final String NO_DATA = "查無資料";
  public static final String INVALID_TOKEN = "無效的TOKEN";
  /**
   * 向左補特定字元
   * @param str 原本的字串
   * @param length 填補後的總長度
   * @param padChar 要填補的字元
   * @return 填補後的字串
   */
  public static String leftPadding(String str, int length, char padChar) {
    if(str == null) {
      str = "";
    }
    if (str.length() > length) {
      return str;
    }
    String pattern = "%" + length + "s";
    return String.format(pattern, str).replace(' ', padChar);
  }

  /**
   * 向右補特定字元
   * @param str 原本的字串
   * @param length 填補後的總長度
   * @param padChar 要填補的字元
   * @return 填補後的字串
   */
  public static String rightPadding(String str, int length, char padChar) {
    if(str == null) {
      str = "";
    }
    if (str.length() > length) {
      return str;
    }
    String pattern = "%-" + length + "s";
    return String.format(pattern, str).replace(' ', padChar);
  }
}
