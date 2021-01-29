package controller;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		// TODO: Make dynamic
		final String webPage = "https://manganelo.com/manga/read_one_punch_man_manga_online_free3";
		final String outputFolder = "D:\\Downloads\\Manga\\Baki Dou";
		
		
		final long startTime = System.currentTimeMillis();

		Engine site  = new Engine(webPage, outputFolder);
		site.crawlWebsite();
		
		final long endTime = System.currentTimeMillis();

		System.out.println("Total execution time: " + (endTime - startTime)/60000);
	}

}
