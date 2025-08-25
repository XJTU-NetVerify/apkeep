package apkeep.utils;

public class Logger {
	static boolean listinfo = false;
	static boolean isdebug = false;
	public static void logInfo(String msg) {
		if(listinfo) System.out.println(msg);
	}
	public static void logDebugInfo(String msg) {
		if(isdebug) System.out.println(msg);
	}
//	public static void print(String msg) {
//		System.out.println(msg);
//	}
}
