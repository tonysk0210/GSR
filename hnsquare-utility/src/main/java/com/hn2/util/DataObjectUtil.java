package com.hn2.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 資料物件工具 (不適用有static之物件)
 * @author Shawn
 *
 */
public class  DataObjectUtil {

	private static final String C_GETTER = "get";
	private static final String C_SETTER = "set";

	public enum PipeMergeRule {
		OVERRIDE_ALL , SKIP_NULL_VALUE
	}

	/**
	 * 用字串欄名取得資料
	 * @param <T>      更新後的物件
	 * @param dataObj  要被更新的物件
	 * @param colName  哪個欄位要被填值
	 * @return 取出的欄位值
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getItem(T dataObj , String colName ) {
		Class<?> fromClazz 	= dataObj.getClass();
		colName     		= colName.toLowerCase();
		String methodStr    = C_GETTER+colName;
		String methodName   = getMethodName(fromClazz, methodStr  );
		try {
			Method m1 		= fromClazz.getDeclaredMethod(methodName);
			Object retValue	= m1.invoke(dataObj);

			return (T) retValue;
		}catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException  e ) {
			throw new RuntimeException(String.format("無法完成物件取值!! %s.%s" , fromClazz.getName(), methodName) );
		}
	}

	/**
	 * 使用字串欄名方式, 做setter動作
	 * @param <T> 回傳 field 原本型態
	 * @param dataObj 被存取的物件
	 * @param colName 預計填值的櫚名
	 * @param newValue 變更後的值
	 * @return 被變更後的物件
	 *
	 */
	public static <T> T setItem(T dataObj  , String colName  , Object newValue) {
		Class<?> fromClazz 	= dataObj.getClass();
		colName     		= colName.toLowerCase();
		String methodStr    = C_SETTER+colName;
		String methodName   = getMethodName(fromClazz, methodStr  );
		String colDataType  = getFieldType(fromClazz , colName) ;
		try {
			Class<?> colTypeClazz = getFieldType(colDataType);

			Method m1 = fromClazz.getDeclaredMethod(methodName , colTypeClazz);
			m1.invoke(dataObj, newValue);
			return dataObj;
		}catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e ) {
			throw new RuntimeException(String.format("無法完成物件填值!! %s.%s" , fromClazz.getName() , methodName ) );
		}

	}

	public static <T> List<T> transpose(Class<T> clasz , List<Map<String,Object>> datas) throws NoSuchMethodException {
		List<T> rtnDatas = new ArrayList<>();
		for (Map<String,Object> data :datas){
			T rtn ;
			try{
				rtn = (T) clasz.getDeclaredConstructor().newInstance();
				for (Map.Entry<String,Object> entry : data.entrySet()) {
					rtn = setItem(rtn, entry.getKey(),entry.getValue());
				}
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			rtnDatas.add(rtn);
		}
		return  rtnDatas;
	}
	private static String getMethodName(Class<?> clazz, String methodStr) {

		for (Method m : clazz.getDeclaredMethods()) {
			if ( m.getName().toLowerCase().equals(methodStr)) {
				if ("private".equals(Modifier.toString(m.getModifiers()))) {
					throw new RuntimeException("未見對應的函式 " + methodStr);
				}
				return m.getName();
			}
		}
		throw new RuntimeException("未見對應的函式 " + methodStr);

	}

	private static Class<?> getFieldType(String typeStr) throws ClassNotFoundException {
		if (typeStr.startsWith("class ")) {
			return Class.forName(typeStr.replace("class ", ""));
		}
		switch (typeStr) {
			case "int":
				return int.class;
			case "long":
				return long.class;
 			case "boolean":
				return boolean.class;
			case "float":
				return float.class;
			case "double":
				return double.class;
 			default:
				throw new RuntimeException("尚未被定義的資料型態, 請通知元件管理者處理"+ typeStr);
		}
	}
	private static String getFieldType(Class<?> clazz, String colName) {
		String dataType   = "";
	 	Field field  = findUnderlying(clazz ,  colName);
		if (field == null ) {
			throw new RuntimeException("未見欄位 " + colName);
		}
		return field.getType().toString();
	}

	public static Field findUnderlying(Class<?> clazz, String colName) {
		Class<?> current = clazz;
		do {
			try {
				for (Field f : current.getDeclaredFields()) {
					if (f.getName().toLowerCase().equals(colName)) {
						return f;
					}
				}
				return current.getDeclaredField(colName);
			} catch(Exception e) {}
		} while((current = current.getSuperclass()) != null);
		return null;
	}
	/**
	 * 將 [valueObj] 內的 field 合併到 [resultObj]物件內
	 * 注意事項:
	 *  1.物件的field型態為要物件格式。(現不接受boolen /int /log...)
	 *  2.兩者物件可以不用相同, 只要field名稱、型態相同 且有get&set Method就可以處理
	 * @param <S> 新值物件型別
	 * @param <T> 結果物件型別
	 * @param valueObj  新值物件
	 * @param resultObj 結果物件(以此物件為基底)
	 * @param pipeMergeRule 覆寫規則
	 * @return 異動後的結果物
	 */
	public static <S,T> T pipeValues(S valueObj , T resultObj  , PipeMergeRule pipeMergeRule ) {
		Class<?> valueClazz  = valueObj.getClass();
		for (Field f : valueClazz.getDeclaredFields()) {
			try {
				Object colValue = getItem(valueObj , f.getName());
				if (colValue == null && pipeMergeRule.equals(PipeMergeRule.SKIP_NULL_VALUE)) {
					continue;
				}
				resultObj  = setItem(resultObj, f.getName() , colValue );
			} catch (RuntimeException e) {
				// 吃掉對應不到的部分
			}
		}
		return resultObj;
	}

}
