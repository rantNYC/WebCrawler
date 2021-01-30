package controller;

import javafx.concurrent.Task;

public class EngineTask extends Task<Void> {

	private final WebCrawler engine;
	private long startTime;
	
	public EngineTask(String processURL, String outputDir) {
		engine = new WebCrawler(processURL, outputDir);
	}
	
	public void setListener(IProcessListener... listeners) {
		engine.setListeners(listeners);
	}

	@Override
	protected Void call() throws Exception {
		engine.crawlWebsite();
		return null;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public long getStartTime() {
		return startTime;
	}
}
