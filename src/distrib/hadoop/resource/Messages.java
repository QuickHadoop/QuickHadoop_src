package distrib.hadoop.resource;

import java.util.Locale;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "distrib.hadoop.resource.messages"; //$NON-NLS-1$

	private static ResourceBundle rb = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
	
	private Messages() {
	}

	public static void setLocale(Locale locale) {
		try {
			rb = ResourceBundle.getBundle(BUNDLE_NAME, locale);
		} catch (Exception e) {
			rb = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
		}
	}

	/**
	 * 根据Key值获取字符串
	 * @param key
	 * @return
	 */
	public static String getString(String key) {
		if(key == null){
			return "Message Err";
		}
		try{
			return rb.getString(key);
		}catch(Exception e){
			e.printStackTrace();
			return "Message Err";
		}
	}
}
