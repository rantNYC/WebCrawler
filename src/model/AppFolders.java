package model;

import java.io.File;
import java.io.IOException;

public class AppFolders {

	private final String userFolder = System.getProperty("user.home");
	private final String defaultUserFile = "user.json";
	
	public AppFolders() {}
	
	public File getAppFolder() {
		return new File(userFolder, "MangaCrawler");
	}
	
	public File getAppUserFile() {
		return new File(getAppFolder(), defaultUserFile);
	}
	
	public void createAppFolder() {
		if (!getAppFolder().exists()) {
			getAppFolder().mkdir();
		}
	}
	
	public void createAppUserFile() throws IOException {
		if(!getAppUserFile().exists()) {
			getAppUserFile().createNewFile();
		}
	}
}
