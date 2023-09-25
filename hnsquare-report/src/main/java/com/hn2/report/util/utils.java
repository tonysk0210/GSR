package main.java.com.hn2.report.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class utils {



    /**西洋年轉民國年*/
    public static String getMigo(String CE){
        if(CE==null || "".equals(CE)){
            return null;
        }else{
            String MigoY=CE.substring(0,4);
            Integer Y=Integer.parseInt(MigoY);
            Integer X=Y-1911;
            String MigoM=CE.substring(5,7);
            String MigoD=CE.substring(8,10);
            String MigoDate=X+"/"+MigoM+"/"+MigoD;
            return MigoDate;
        }
    }

    public static String getMigo2(String CE){
        if(CE==null || "".equals(CE)){
            return null;
        }else{
            String MigoY=CE.substring(0,4);
            Integer Y=Integer.parseInt(MigoY);
            Integer X=Y-1911;
            String MigoM=CE.substring(5,7);
            String MigoD=CE.substring(8,10);
            String MigoDate=X+"年"+MigoM+"月"+MigoD+"日";
            return MigoDate;
        }
    }

    /**民國年轉西洋年*/
    public static String getCE(String migo){
        if(migo==null || "".equals(migo)){
            return "";
        }else{
            String ceY=migo.substring(0,3);
            Integer Y=Integer.parseInt(ceY);
            Integer X=Y+1911;
            String ceM=migo.substring(4,6);
            String ceD=migo.substring(7,9);
            String ce=X+"-"+ceM+"-"+ceD;
            return ce;
        }
    }

    /**數字加上千分位 numberFormat*/
    public static String getNumberFormat(Integer num){
        if(num==null){
            return "";
        }
        String nv=num.toString();
        StringBuilder numberFormat=new StringBuilder();
        numberFormat.append(nv);
        int a=nv.length();
        for(int i=a-3;i>0;i-=3){
            numberFormat.insert(i,',');
        }
        return  numberFormat.toString();
    }

    public static String getNumberLongFormat(Long num){
        if(num==null){
            return "";
        }
        String nv=num.toString();
        StringBuilder numberFormat=new StringBuilder();
        numberFormat.append(nv);
        int a=nv.length();
        for(int i=a-3;i>0;i-=3){
            numberFormat.insert(i,',');
        }
        return  numberFormat.toString();
    }

    public static String getBigDecimalFormamt(BigDecimal num){
        num = num.setScale(2, RoundingMode.HALF_UP);
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        return nf.format(num);
    }

    /**月轉年月 monthFormat*/
    public static String getMonthFormat(String monthString) {
        if(monthString==null || Integer.parseInt(monthString)<0){
            return "";
        }
        String aaa="";
        Integer month=Integer.parseInt(monthString);
        if(month==0){
            return aaa;
        }else {
            Integer YY=month/12;
            Integer MM=month%12;
            if(YY>0){
                if(MM==0){
                    aaa = String.format("%s 年 ", YY);
                }else {
                    aaa = String.format("%s 年 %s 月", YY, MM);
                }
                return  aaa;
            }else {
                aaa = String.format(" %s 月",MM);
                return aaa;
            }}}
    public static String getMonthFormatI(Integer monthString) {
        if(monthString==null || monthString<0){
            return "";
        }
        String aaa="";
        Integer month=monthString;
        if(month==0){
            return aaa;
        }else {
            Integer YY=month/12;
            Integer MM=month%12;
            if(YY>0){
                if(MM==0){
                    aaa = String.format("%s 年 ", YY);
                }else {
                    aaa = String.format("%s 年 %s 月", YY, MM);
                }
                return  aaa;
            }else {
                aaa = String.format(" %s 月",MM);
                return aaa;
            }}}

    public static String getUntilDay(String hms){
        String untilDay=hms.substring(0,11);
        return untilDay;
    }

    public static Integer getNumberNotNull(Integer integer){
        if(integer==null){
            return 0;
        }else {
            return integer;
        }
    }

    public static BigDecimal getNumberNotNullB(BigDecimal integer){
        if(integer==null){
            return BigDecimal.ZERO;
        }else {
            return integer;
        }
    }



    public static Long getNumberNotNullL(Long number){
        if(number==null){
            return 0L;
        }else {
            return number;
        }
    }

    public static Double getNumberNotNullD(Double number){
        if(Double.isNaN(number)){
            return 0.0;
        }else {
            return number;
        }
    }

    public static Integer getLastDay(Integer Migoyear,Integer month){
        Integer year=Migoyear+1911;

        Calendar cal = Calendar.getInstance();
        //设置年份
        cal.set(Calendar.YEAR,year);
        //设置月份
        cal.set(Calendar.MONTH, month-1);
        //获取某月最大天数
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        return lastDay;
    }
    /**去掉前面多餘的0*/
    public static String getRemovezeros(String s) {
        if (s!=null){
            String removezeros = s.replaceFirst("^0+(?!$)", "");
            return removezeros;
        }else {
            return null;
        }

    }

    public static String getStringNotNull(String s){
        if (s==null||s.equals("")){
            return "";
        }else {
            return s;
        }
    }


    public static String getAccountSubject123(String accountSubject1,String accountSubject2,String accountSubject3){
        String accountSubject123=null;
        if(accountSubject1!=null&&!"".equals(accountSubject1)){
            accountSubject123=accountSubject1;
        }
        if(accountSubject2!=null&&!"".equals(accountSubject2)){
            accountSubject123+="/";
            accountSubject123+=accountSubject2;
        }
        if(accountSubject3!=null&&!"".equals(accountSubject3)){
            accountSubject123+="/";
            accountSubject123+=accountSubject3;
        }
        return accountSubject123;
    }

    public static String getChineseMonth(String month){
        String chineseMonth = null;
        if("01".equals(month)){
            chineseMonth= "一月";
        }else if("02".equals(month)){
            chineseMonth= "二月";
        }else if("03".equals(month)){
            chineseMonth= "三月";
        }else if("04".equals(month)){
            chineseMonth= "四月";
        }else if("05".equals(month)){
            chineseMonth= "五月";
        }else if("06".equals(month)){
            chineseMonth= "六月";
        }else if("07".equals(month)){
            chineseMonth= "七月";
        }else if("08".equals(month)){
            chineseMonth= "八月";
        }else if("09".equals(month)){
            chineseMonth= "九月";
        }else if("10".equals(month)){
            chineseMonth= "十月";
        }else if("11".equals(month)){
            chineseMonth= "十一月";
        }else if("12".equals(month)){
            chineseMonth= "十二月";
        }
        return chineseMonth;
    }

    public static String getRochhmm(Date now){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        int rocYear = calendar.get(Calendar.YEAR) - 1911;
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日 HH:mm");
        String rocDate = rocYear + "年" + sdf.format(now);
        return rocDate;
    }

    public static String[] stringTransToStingArray(String a){
        if(a!=null&&!a.equals("")){
            return a.split("\\s+");
        }else {
            return null;
        }

    }

    public static String arrayToString(String[] a){
        String b="";
        if(a!=null){
            for (int i=0;i<a.length;i++){
                b+=(a[i]+"");
            }
            return b;
        }else {
            return "";
        }
    }


}
