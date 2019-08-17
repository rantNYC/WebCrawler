package controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Engine {

	private Document doc;
	private String outputFolder;
	private Controller fxmlController;

	public Engine(String url, String outpuFolder) {
		try {
			this.doc =  Jsoup.connect(url).get();
			
		} catch (IOException e) {
			// TODO: Debug
			System.out.println(String.format("Error connection the URL: %s", url));
			e.printStackTrace();
		}
		this.outputFolder = outpuFolder;
	}

	public Controller getFxmlController() {
		return fxmlController;
	}

	public void setFxmlController(Controller fxmlController) {
		this.fxmlController = fxmlController;
	}
	
	public void crawlWebsite() {
		Element chapters = doc.getElementsByClass("chapter-list").first();
		Elements rows = chapters.getElementsByClass("row");
		int totalChapters = rows.size();
		float growRate = (float) (1.0 / totalChapters);
		int currentChapter = 0;
		for(Element row : rows) {
			Element link = row.getElementsByAttribute("href").first();
			String linkHref = link.attr("href");
			String chapterName = link.attr("title");
			processLinkToChapter(linkHref, chapterName);
			if(this.fxmlController != null) this.fxmlController.updateProgressBar(++currentChapter * growRate);
		}
	}

	private void processLinkToChapter(String linkHref, String chapterName) {
		try {
			Document chapterDoc = Jsoup.connect(linkHref).get();
			
			Element images = chapterDoc.getElementById("vungdoc");
			
			Elements imageLinks = images.getElementsByTag("img");
			
			int pageNum = 0;
			
			for(Element imageLink : imageLinks) {	
				String link = imageLink.attr("src");
				String title = imageLink.attr("title");
				
				saveImage(link, title, chapterName, ++pageNum);
			}
			
		} catch (IOException e) {
			System.out.println(String.format("Error connection the URL: %s", linkHref));
			// TODO: Debug
			e.printStackTrace();
		}
		
	}

	private void saveImage(String link, String title, String chapterName, int pageNum) {
		try {
		    URL url = new URL(link);
		    
		    String folderPath = createFolder(chapterName);
		    //int indexOf = title.lastIndexOf("-"); //Implies no title will have '-' besides the URL name
		    //String newTitle = title.substring(0, indexOf);
		    String outFilePath = folderPath + File.separator + pageNum + ".jpg";
		    File outputImage = new File(outFilePath);
		    if(outputImage.exists()) {
		    	if(this.fxmlController != null) this.fxmlController.updateStatus("Skiping image because it already exists: " + outFilePath);
		    	return;
		    }
		    
		    FileUtils.copyURLToFile(url, outputImage);
		    if(this.fxmlController != null) this.fxmlController.updateStatus("File Copied: " + outFilePath);
		    
		} catch (IOException e) {
			System.out.println(String.format("Error getting the chapter image: %s", link));
			// TODO: Debug
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.out.println(String.format("Error getting the chapter image: %s", link));
			// TODO: Debug
			e.printStackTrace();
		}
		
	}

	private String createFolder(String chapterName) throws IOException {
		String newChapterName = chapterName.replaceAll("\\.\\.\\.", "");
		String folderName = this.outputFolder + File.separator + newChapterName.replaceAll("[<>:\"'\\/\\|?*]", "");
		Path output = Paths.get(folderName);
		if(!Files.exists(output)) {
			if(this.fxmlController != null) this.fxmlController.updateStatus("Folder Created: " + folderName);
			Files.createDirectory(output);
		}
		
		return folderName;
	}
}
