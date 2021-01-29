package controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Engine {

	public static final int CONNECTION_TIMEOUT = 30000;
	
	private Document doc;
	private String urlDoc;
	private String outputFolder;
//	private Controller fxmlController;
	
	private List<ChapterLink> chapterAndLink;

//	private Map<String, List<String>> chapterToPages;

	public Engine(String url, String outpuFolder) throws IOException {
		this.urlDoc = url;
		this.doc = Jsoup.connect(url).timeout(CONNECTION_TIMEOUT)
				.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36")
				.get();
		this.outputFolder = outpuFolder;
	}

	public List<ChapterLink> getChapterToLink() {
		return List.copyOf(chapterAndLink);
	}

	public void crawlWebsite() throws IOException {
		chapterAndLink = new ArrayList<ChapterLink>();
		Element chapters = doc.getElementsByClass("chapter-list").first();
		if(chapters == null) throw new NullPointerException("Couldn't find chapters in the URL: " + urlDoc);
		Elements rows = chapters.getElementsByClass("row");
		for(Element row : rows) {
			Element link = row.getElementsByAttribute("href").first();
			String linkHref = link.attr("href");
			String chapterName = link.attr("title");
			chapterAndLink.add(new ChapterLink(chapterName, linkHref));
		}
	}

	public List<String> processChapterToPages(String linkHref, String chapterName) throws IOException {
//		chapterToPages = new ConcurrentHashMap<String, List<String>>();
		Document chapterDoc = Jsoup.connect(linkHref).timeout(CONNECTION_TIMEOUT).get();
		Element images = chapterDoc.getElementById("vungdoc");
		Elements imageLinks = images.getElementsByTag("img");

		List<String> pagesToLink = new ArrayList<String>();
		for(Element imageLink : imageLinks) {	
			String link = imageLink.attr("src");
//			String title = imageLink.attr("title");
			pagesToLink.add(link);
//			saveImage(link, title, chapterName, ++pageNum, linkHref);
		}

		return pagesToLink;
	}

	public void saveImage(String link, String chapterName, int pageNum, String referrer) throws IOException {

		Response resultImageResponse = Jsoup.connect(link)
										    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36")
										    .referrer(referrer)
										    .cookie("Cookie", "__cfduid=dad866def94d4f40df98e1a44c77d13251593818265")
										    .ignoreContentType(true)
										    .timeout(CONNECTION_TIMEOUT).execute();


		String folderPath = createFolder(chapterName);
		//int indexOf = title.lastIndexOf("-"); //Implies no title will have '-' besides the URL name
		//String newTitle = title.substring(0, indexOf);
		String outFilePath = folderPath + File.separator + pageNum + ".jpg";
		File outputImage = new File(outFilePath);
		if(outputImage.exists()) {
//			if(this.fxmlController != null) this.fxmlController.updateStatus("Skiping image because it already exists: " + outFilePath);
			return;
		}

		FileOutputStream out = null;
		try {
		//output here
		out = (new FileOutputStream(new File(outFilePath)));
		out.write(resultImageResponse.bodyAsBytes());  // resultImageResponse.body() is where the image's contents are.
		} finally {
			if(out != null) out.close();
		}
		
//		if(this.fxmlController != null) this.fxmlController.updateStatus("File Copied: " + outFilePath);

	}

	private String createFolder(String chapterName) throws IOException {
		String newChapterName = chapterName.replaceAll("\"[\\\\~#%&*{}/:<>?|\\\"-]\"", "");
		String folderName = this.outputFolder + File.separator + newChapterName.replaceAll("[<>:\"'\\/\\|?*]", "");
		Path output = Paths.get(folderName);
		if(!Files.exists(output)) {
//			if(this.fxmlController != null) this.fxmlController.updateStatus("Folder Created: " + folderName);
			Files.createDirectories(output);
		}

		return folderName;
	}
	
	public int getTotalChapters() {
		return chapterAndLink == null ? 0 : chapterAndLink.size();
	}
	
	public class ChapterLink{
		private String chapterName;
		private String chapterLink;
		
		public ChapterLink(String chapterName, String chapterLink) {
			this.chapterName = chapterName;
			this.chapterLink = chapterLink;
		}

		public String getChapterLink() {
			return chapterLink;
		}

		public String getChapterName() {
			return chapterName;
		}
	}
}
