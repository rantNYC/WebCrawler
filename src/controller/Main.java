package controller;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		// TODO: Make dynamic
		final String webPage = "https://www.readm.org/manga/16103";
		final String outputFolder = "D:\\MangaDownload\\One Punch Man";
		
		
		final long startTime = System.currentTimeMillis();

		WebCrawler site  = new WebCrawler(webPage, outputFolder);
		site.crawlWebsite();
		
		final long endTime = System.currentTimeMillis();

		System.out.println("Total execution time: " + (endTime - startTime)/60000);
	}

}
