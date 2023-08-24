package com.hn2.util;

import org.apache.commons.lang3.StringUtils;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.chrono.MinguoDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hn2.util.DateUtil.DateFormat.yyy年M月d日;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;

/**
 * 日期 utility
 *
 * @author hsien
 */
public class DateUtil {

    private DateUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 西元年轉民國年
     *
     * @param year 西元年 ex: 2022
     * @return 民國年 ex: 111
     */
    public static String year2RocYear(String year) {
        return String.valueOf(Integer.parseInt(year) - 1911);
    }

    public static String rocYear2Year(String rocYear) {
        return String.valueOf(Integer.parseInt(rocYear) + 1911);
    }

    /**
     * 西元日期轉民國年字串
     *
     * @param sqldate    Date
     * @param dateFormat
     * @return String
     */
    public static String date2Roc(Date sqldate, DateFormat dateFormat) {
        if (null == sqldate) {
            return null;
        }
        java.util.Date date = new java.util.Date(sqldate.getTime());
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        String datestr = sf.format(date);
        LocalDate localDate = LocalDate.parse(datestr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        return MinguoDate.from(localDate).format(DateTimeFormatter.ofPattern(dateFormat.getFormat()));
    }

    public static String date2Roc(java.util.Date date, DateFormat dateFormat) {
        if (null == date) {
            return null;
        }
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        String datestr = sf.format(date);
        LocalDate localDate = LocalDate.parse(datestr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        return MinguoDate.from(localDate).format(DateTimeFormatter.ofPattern(dateFormat.getFormat()));
    }

    /**
     * 毫秒轉民國年字串
     *
     * @param time       long
     * @param dateFormat
     * @return String
     */
    public static String time2Roc(long time, DateFormat dateFormat) {
        if (time == 0L) {
            return null;
        }
        java.util.Date date = new java.util.Date(time);
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd");
        String datestr = sf.format(date);
        LocalDate localDate = LocalDate.parse(datestr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        return MinguoDate.from(localDate).format(DateTimeFormatter.ofPattern(dateFormat.getFormat()));
    }

    /**
     * 民國日期字串轉西元日期
     *
     * @param twDate
     * @return Date
     */
    public static java.util.Date roc2Date(String twDate) {
        if (null == twDate || "".equals(twDate) || twDate.length() < 7) {
            return null;
        }

        int y = Integer.parseInt(twDate.substring(0, 3));
        int m = Integer.parseInt(twDate.substring(3, 5));
        int d = Integer.parseInt(twDate.substring(5, 7));

        return Date.from(LocalDate.from(MinguoDate.of(y, m, d)).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**100-10-10 -> 2011-10-10*/
    public static String roc2Date2(String twDate) {
        if (null == twDate || "".equals(twDate) || twDate.length() < 9) {
            return null;
        }

        int y = Integer.parseInt(twDate.substring(0, 3));
        Integer yCE=y+1911;
        String md=twDate.substring(3, 9);
        String CE=yCE+md;

        return CE;
    }
    /**
     * 日期加 N 天
     *
     * @param localDate 日期
     * @param diff      加減數
     * @return 日期
     */
    public static Date plusDate(LocalDate localDate, String type, int diff) {
        Date date = null;
        if ("d".equals(type)) {
            date = Date.valueOf(localDate.plusDays(diff));
        } else if ("m".equals(type)) {
            date = Date.valueOf(localDate.plusMonths(diff));
        }
        return date;
    }

    /**
     * 日期加 N 月
     *
     * @param localDate 日期
     * @param diff      加減數
     * @return 日期
     */
    public static String plusDate(LocalDate localDate, String type, int diff, DateFormat format) {
        Date date = plusDate(localDate, type, diff);
        return date2Roc(date, format);
    }

    /**
     * 回傳結果說明如下: <br>
     * < 0 : date1 < date2 <br>
     * = 0 : date1 = date2 <br>
     * > 0 : date1 > date2
     *
     * @param date1
     * @param date2
     * @return
     */
    public static int compareTwoDate(String date1, String date2) {
        LocalDate _date1 = getDate(date1);
        LocalDate _date2 = getDate(date2);
        assert _date1 != null;
        return _date1.compareTo(_date2);
    }

    /**
     * 可傳入 yyyymm, yyyymmdd, yyymm, yyymmdd, 無日期會自動補01 , 其餘的date格式或發生錯誤則回傳 NULL
     *
     * @param date
     * @return LocalDateTime
     */
    public static LocalDate getDate(String date) {

        // ex: 201501  or 10401  補足01   這邊不判斷是否為數值
        if (date.length() == 6 || date.length() == 5) {
            date += "01";
        }

        if (date.length() != 7 && date.length() != 8) {
            return null;
        }

        int year = 1911;
        int rocYear;
        int month = 1;
        int day = 1;

        if (date.length() == 7) {
            rocYear = Integer.parseInt(date.substring(0, 3));
            month = Integer.parseInt(date.substring(3, 5));
            day = Integer.parseInt(date.substring(5, 7));
            year = 1911 + rocYear;
        } else if (date.length() == 8) {
            year = Integer.parseInt(date.substring(0, 4));
            month = Integer.parseInt(date.substring(4, 6));
            day = Integer.parseInt(date.substring(6, 8));
        }

        return LocalDate.of(year, month, day);
    }

    /**
     * 若傳入日期包含day(dd), 會以day的差距來判斷是否已滿一個月
     *
     * @param date1       : 可傳入 yyyymm, yyyymmdd, yyymm, yyymmdd, 無日期會自動補01 , 其餘的date格式或發生錯誤則回傳 -1
     * @param date2       : 可傳入 yyyymm, yyyymmdd, yyymm, yyymmdd, 無日期會自動補01 , 其餘的date格式或發生錯誤則回傳 -1
     * @param betweenType : d:日 m:月 y:年
     * @return 其餘的date格式或發生錯誤則回傳-1, 第一個日期大於第二個日期則回傳-2
     */
    public static int getBetweenDates(Object date1, Object date2, String betweenType) {
        return getBetweenDates(String.valueOf(date1), String.valueOf(date2), betweenType);
    }

    /**
     * 若傳入日期包含day(dd), 會以day的差距來判斷是否已滿一個月
     *
     * @param date1       : 可傳入 yyyymm, yyyymmdd, yyymm, yyymmdd, 無日期會自動補01 , 其餘的date格式或發生錯誤則回傳 -1
     * @param date2       : 可傳入 yyyymm, yyyymmdd, yyymm, yyymmdd, 無日期會自動補01 , 其餘的date格式或發生錯誤則回傳 -1
     * @param betweenType : d:日 m:月 y:年
     * @return 其餘的date格式或發生錯誤則回傳-1, 第一個日期大於第二個日期則回傳-2
     */
    public static int getBetweenDates(String date1, String date2, String betweenType) {
        LocalDate dt1 = getDate(date1);
        LocalDate dt2 = getDate(date2);
        if (dt1 == null || dt2 == null) {
            return -1;
        }

        if (dt1.compareTo(dt2) > 0) {
            return -2;
        }

        Period period = Period.between(dt1, dt2);
        int resultInt = 0;
        if ("d".equals(betweenType)) {
            resultInt = Math.abs(period.getDays());
        } else if ("m".equals(betweenType)) {
            resultInt = Math.abs(period.getMonths());
        } else if ("y".equals(betweenType)) {
            resultInt = Math.abs(period.getYears());
        }
        return resultInt;
    }

    /**
     * 月份最後一天
     *
     * @param year
     * @param month
     * @return
     */
    public static java.util.Date getEndDate(String year, String month) {
        Integer yy = 0;
        Integer mm = 0;
        Calendar cal = Calendar.getInstance();

        if (!(null == year && null == month)) {
            if (null != year && null == month) {
                yy = Integer.parseInt(year);
                yy = yy + 1911;
                cal.set(yy, 11, 31, 11, 59, 59);
            } else if (null != year && null != month) {
                yy = Integer.parseInt(year);
                mm = Integer.parseInt(month);
                yy = yy + 1911;
                mm = mm - 1;
                cal.set(Calendar.MONTH, mm);
                int last = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                cal.set(yy, mm, last, 11, 59, 59);
            }
            return cal.getTime();
        } else return null;
    }

    public static String getMonthEndDate(){
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));
        String last =date2Roc(ca.getTime(), yyy年M月d日);
        return  last;
    }

    /**
     * 解析字串格式日期並返回LocalDate格式
     *
     * @param raw ex: 202203, 11103, 2022-01, 111-01, 2022/01/01, 111/01/02 ...
     * @return LocalDate
     */
    public static LocalDate parseStr(String raw) {
        String date = raw;

        Pattern pattern = Pattern.compile("(/|-)");
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            String delimiter = matcher.group();
            String[] inputDate = raw.split(delimiter);
            if (inputDate.length == 3) {
                date = inputDate[0] + StringUtils.leftPad(inputDate[1], 2, "0") + StringUtils.leftPad(inputDate[2], 2, "0");
            } else if (inputDate.length == 2){
                date = inputDate[0] + StringUtils.leftPad(inputDate[1], 2, "0") + "01";
            } else {
                throw new IllegalArgumentException(String.format("輸入日期不合法: %s", raw));
            }
        }

        int year;
        int month;
        int day = 1;

        switch (date.length()) {
            case 7:
                day = Integer.parseInt(date.substring(5, 7));
            case 5:
                year = 1911 + Integer.parseInt(date.substring(0, 3));
                month = Integer.parseInt(date.substring(3, 5));
                break;

            case 8:
                day = Integer.parseInt(date.substring(6, 8));
            case 6:
                year = Integer.parseInt(date.substring(0, 4));
                month = Integer.parseInt(date.substring(4, 6));
                break;

            default:
                throw new IllegalArgumentException(String.format("無法解析日期，輸入格式錯誤， 輸入日期: %s", raw));
        }

        return LocalDate.of(year, month, day);
    }

    /**
     * LocalDate轉成民國日期
     *
     * @param localDate  要轉換的日期
     * @param dateFormat 轉換格式 {@link  DateFormat}
     * @return 民國日期，根據Dateformat決定格式 ex: yyyMMdd -> 1110203
     */
    public static String date2Roc(LocalDate localDate, DateFormat dateFormat) {
        return date2Roc(localDate2Date(localDate), dateFormat);
    }


    /**
     * 目標日期是否為來源日期的下個月
     *
     * @param source 來源日期
     * @param target 目標日期
     * @return true 若目標日期月份 - 來源日期月份差值為1
     */
    public static Boolean isNextMonth(LocalDate source, LocalDate target) {
        return isAfterMonth(source, target, 1);
    }

    /**
     * 比較目標日期與來源日期月份差值是否為指定數
     *
     * @param source 來源日期
     * @param target 目標日期
     * @param gap    差值
     * @return true 若目標日期月份 - 來源日期月份差值為指定差值gap
     */
    public static Boolean isAfterMonth(LocalDate source, LocalDate target, Integer gap) {
        if (source.getYear() > target.getYear()) {
            return false;
        }


        if (!source.getMonth().plus(gap).equals(target.getMonth())) {
            return false;
        }

        return true;
    }

    public static java.util.Date localDate2Date(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 日歷轉 LocalDate
     *
     * @param calendar 日歷
     * @return LocalDate
     */
    public static LocalDate calendar2LocalDate(Calendar calendar) {
        return LocalDate.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
    }

    /**
     * @see DateUtil#year2Month(Integer)
     */
    public static Integer year2Month(String year) {
        return year2Month(Integer.parseInt(year));
    }

    /**
     * 年份轉成月數
     *
     * @return 月數
     */
    public static Integer year2Month(Integer year) {
        return year * 12;
    }

    /**
     * String日期轉成民國日期
     *
     * @param value String日期 ex: "2022-11-01"
     * @return 民國日期 ex: 1110203
     */
    public static String date2Roc(String value) {
        LocalDate parse = LocalDate.parse(value);
        return date2Roc(parse, DateFormat.yyyMMdd);
    }

    /**
     * LocalDate轉成民國日期
     *
     * @param value      String日期 ex: "2022-11-01"
     * @param dateFormat 轉換格式 {@link  DateFormat}
     * @return 民國日期，根據Dateformat決定格式 ex: yyyMMdd -> 1110203
     */
    public static String date2Roc(String value, DateFormat dateFormat) {
        LocalDate parse = LocalDate.parse(value);
        return date2Roc(parse, dateFormat);
    }

    /**
     * 取得現在民國年月日日期
     *
     * @return 民國年月日 ex: 1110302
     */
    public static String getNowRocDate() {
        return date2Roc(LocalDate.now(), DateFormat.yyyMMdd);
    }

    /**
     * 取得月數
     *
     * @param localDate - 要取得的日期
     * @return 月數
     */
    public static Integer localDate2MonthValue(LocalDate localDate) {
        return localDate.getYear() * 12 + localDate.getMonthValue();
    }


    /**
     * 取得兩個日期的月份差值
     *
     * @param from 起始日期
     * @param end  結束日期
     * @return 月份差值
     */
    public static Integer betweenMonthsValue(LocalDate from, LocalDate end) {
        return Period.between(from, end).getMonths();
    }

    /**
     * 取得需要的民國格式的現在時間
     *
     * @param format {@link DateFormat}
     * @return 民國格式的現在時間
     */
    public static String getNowRocDate(DateFormat format) {
        return date2Roc(LocalDate.now(), format);
    }

    public static LocalDate date2LocalDate(java.util.Date enterDate) {
        return Instant.ofEpochMilli(enterDate.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public enum DateFormat {
        yyy年M月d日("yyy年M月d日"), yyyMMdd("yyyMMdd"), yyyMMdd_slash("yyy/MM/dd"), yyyMM("yyyMM"), yyy("yyy");

        private String format;

        DateFormat(String format) {
            this.format = format;
        }

        public String getFormat() {
            return this.format;
        }
    }

    public static LocalDate getLastDayOfMonth(LocalDate date) {
        return date.with(lastDayOfMonth());
    }

    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        return date.with(firstDayOfMonth());
    }

    public static java.sql.Date localDate2SQLDate(LocalDate localDate) {
        return new Date(Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()).getTime());
    }
}
