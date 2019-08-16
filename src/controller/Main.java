package controller;

public class Main {

	public static void main(String[] args) {
		// TODO: Make dynamic
		final String webPage = "https://manganelo.com/manga/baki_dou";
		final String outputFolder = "D:\\Downloads\\Manga\\Baki Dou";
		
		
		final long startTime = System.currentTimeMillis();

		Engine site  = new Engine(webPage, outputFolder, null);
		site.crawlWebsite();
		
		final long endTime = System.currentTimeMillis();

		System.out.println("Total execution time: " + (endTime - startTime)/60000);
	}

}
