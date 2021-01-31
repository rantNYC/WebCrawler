package controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WebCrawler implements Runnable{

	private static Logger log = LoggerFactory.getLogger(WebCrawler.class);
	private static final int CONNECTION_TIMEOUT = 30000;

	private final String url;
	private final String outputDir;
	
	private WebCrawlerStats stats = new WebCrawlerStats();
	
	private Set<IProcessListener> listeners = new HashSet<>();
	
	public WebCrawler(String url, String outputDir) {
		this.url = url;
		this.outputDir = outputDir;
	}

	public void setListeners(IProcessListener... listeners) {
		for(IProcessListener listener : listeners) {
			this.listeners.add(listener);
		}
	}
	
	@Override
	public void run() {
		try {
			crawlWebsite();
		} catch (IOException e) {
			String errMsg = "Error processing website " + url;
			log.error(errMsg, e);
			updateListenersMessage(errMsg);
		}
	}
	
	public void crawlWebsite() throws IOException {
		File output = new File(outputDir);
		if(!output.exists())
			throw new IllegalArgumentException("Output directory doesn't exist " + outputDir);
		if(!output.isDirectory())
			throw new IllegalArgumentException("Path is not a directory" + outputDir);
		
		Document doc = getConnection(url).get();
		Elements chapters = doc.getElementsByClass("table-episodes-title");
		if(chapters == null) throw new IllegalArgumentException("Couldn't find chapters in the URL: " + url);
		
		String title = doc.getElementsByTag("title").first().ownText();
		String outputDirWithTitle = outputDir + File.separatorChar + title;
		stats.setTotalNumChapters(chapters.size());
		Map<String, List<String>> chapterByImgUrl = new LinkedHashMap<>();
		for(Element chapter : chapters){
			if(isInterrupted()) 
				return;
			Element link = chapter.getElementsByAttribute("href").first();
			String linkHref = link.absUrl("href");
			String chapterName = link.text();
			chapterByImgUrl.put(chapterName, processChapterToPages(linkHref, chapterName));
		}
		
		for(Map.Entry<String, List<String>> entry : chapterByImgUrl.entrySet()) {
			if(isInterrupted()) 
				return;
			String chptrName = entry.getKey();
			List<String> imgs = entry.getValue();
			int pageNum = 0;
			String msg = String.format("Processing Chapter: %s", chptrName);
			log.debug(msg);
			updateListenersMessage(msg);
			for(String img : imgs) {
				if(isInterrupted()) 
					return;
				saveImage(img, chptrName, ++pageNum, outputDirWithTitle);
				updateListenersProgress(stats.calculateProgress());
			}
		}
	}

	private List<String> processChapterToPages(String linkHref, String chapterName) throws IOException {
		Document chapterDoc = getConnection(linkHref).get();
		Elements images = chapterDoc.getElementsByClass("img-responsive scroll-down");
		List<String> imagesLink = new ArrayList<>();
		stats.addNumImages(images.size());
		for(Element image : images) {
			String imgLink = image.absUrl("src");
			imagesLink.add(imgLink);
		}
		
		return imagesLink;
	}

	private void saveImage(String link, String chapterName, int pageNum, String outputDir) throws IOException {
		String msg = String.format("Processing image number %d", pageNum);
		log.debug(msg);
		updateListenersMessage(msg);
		String folderPath = createFolder(outputDir, chapterName);
		String outFilePath = folderPath + File.separator + pageNum + ".jpg";
		File outputImage = new File(outFilePath);
		if(outputImage.exists()) {
			msg = String.format("Ouput image %s already exists, skipping...", outputImage);
			log.warn(msg);
			updateListenersMessage(msg);
			return;
		}

		try (FileOutputStream out = (new FileOutputStream(new File(outFilePath)));){
			Response resultImageResponse = getConnection(link).execute();
			// resultImageResponse.body() is where the image's contents are.
			out.write(resultImageResponse.bodyAsBytes());
		} catch (Exception e) {
			msg = String.format("Error processing page %d with url %s", pageNum, link);
			log.error(msg, e);
			updateListenersMessage(msg);
		}
	}

	private String createFolder(String outputFolder, String chapterName) throws IOException {
		String newChapterName = chapterName.replaceAll("\"[\\\\~#%&*{}/:<>?|\\\"-]\"", "");
		String folderName = outputFolder + File.separator + newChapterName.replaceAll("[<>:\"'\\/\\|?*]", "");
		Path output = Paths.get(folderName);
		if(!Files.exists(output)) {
			Files.createDirectories(output);
		}

		return folderName;
	}

	private Connection getConnection(String url) {
		return Jsoup.connect(url)
	    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36")
	    .ignoreContentType(true)
	    .timeout(CONNECTION_TIMEOUT);
	}
	
	private void updateListenersMessage(String msg) {
		for(IProcessListener listener : listeners) {
			listener.updateMessage(msg);
		}
	}
	
	private void updateListenersProgress(double progress) {
		for(IProcessListener listener : listeners) {
			listener.updateProgess(progress);
		}
	}
	
	private boolean isInterrupted() {
		return Thread.currentThread().isInterrupted();
	}
	
	private class WebCrawlerStats{
		private int totalNumChapters = 0;
		private int totalNumImages = 0;
		private int iteration = 0;
		
		public WebCrawlerStats() {}

		public void setTotalNumChapters(int totalNumChapters) {
			this.totalNumChapters = totalNumChapters;
		}

		public void addNumImages(int totalNumImages) {
			this.totalNumImages += totalNumImages;
		}

		public double calculateProgress() {
			if(totalNumChapters == 0 || totalNumImages == 0) return 0;
			
			return ++iteration/(double)(totalNumChapters * totalNumImages);
		}
	}
}
