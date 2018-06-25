package textanim;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class CustomDecimalFormat {

	private static DecimalFormat df1;
	private static DecimalFormat df2;

	static {
		Locale locale  = new Locale("en", "US");
		df1 = (DecimalFormat)NumberFormat.getNumberInstance(locale);
		df1.applyPattern("##0.0");
		df2 = (DecimalFormat)NumberFormat.getNumberInstance(locale);
		df2.applyPattern("##0.00");
	}

	public static String format(double number, int decimalPlaces) {
		switch(decimalPlaces) {
		case 1: return df1.format(number);
		case 2: return df2.format(number);
		default: throw new RuntimeException(decimalPlaces + " not supported yet");
		}
	}
}
