package controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
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
			updateListeners(errMsg);
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
		for(Element chapter : chapters){
			Element link = chapter.getElementsByAttribute("href").first();
			String linkHref = link.absUrl("href");
			String chapterName = link.text();
			processChapterToPages(linkHref, chapterName, outputDirWithTitle);
		}
	}

	private void processChapterToPages(String linkHref, String chapterName, String outputDir) throws IOException {
		
		Document chapterDoc = getConnection(linkHref).get();
		Elements images = chapterDoc.getElementsByClass("img-responsive scroll-down");
		String msg = String.format("Processing Chapter: %s", chapterName);
		log.debug(msg);
		updateListeners(msg);
		int pageNum=0;
		for(Element image : images) {
			String imgLink = image.absUrl("src");
			saveImage(imgLink, chapterName, ++pageNum, linkHref, outputDir);
		}
	}

	private void saveImage(String link, String chapterName, int pageNum, String referrer, String outputDir) throws IOException {
		String msg = String.format("Processing image number %d", pageNum);
		log.debug(msg);
		updateListeners(msg);
		String folderPath = createFolder(outputDir, chapterName);
		String outFilePath = folderPath + File.separator + pageNum + ".jpg";
		File outputImage = new File(outFilePath);
		if(outputImage.exists()) {
			msg = String.format("Ouput image %s already exists, skipping...", outputImage);
			log.warn(msg);
			updateListeners(msg);
			return;
		}

		try (FileOutputStream out = (new FileOutputStream(new File(outFilePath)));){
			Response resultImageResponse = getConnection(link).execute();
			// resultImageResponse.body() is where the image's contents are.
			out.write(resultImageResponse.bodyAsBytes());
		} catch (Exception e) {
			msg = String.format("Error processing page %d with url %s", pageNum, link);
			log.error(msg, e);
			updateListeners(msg);
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
	
	private void updateListeners(String msg) {
		for(IProcessListener listener : listeners) {
			listener.updateMessage(msg);
		}
	}
}
