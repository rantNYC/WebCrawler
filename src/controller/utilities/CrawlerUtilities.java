package controller.utilities;

public final class CrawlerUtilities {

	private CrawlerUtilities() {}
	
	public static String formatTime(long seconds) {
		int hour = 0, min = 0, sec = 0;
		
		if(seconds >= 60) {
			min = (int) (seconds / 60);
			sec = (int) (seconds % 60);
		}else {
			sec = (int) seconds;
		}
		
		if(min >= 60) {
			hour = min / 60;
			min = min % 60;
		}
		
		return String.format("%d:%02d:%02d", hour, min, sec);
	}
	
	public static boolean isNullOrEmpty(String text) {
		return text == null || text.isEmpty() || text.isBlank();
	}
}
