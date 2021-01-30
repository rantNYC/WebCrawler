package model;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ModelBuilder {
	
	private final Gson jsonModel = new Gson();
	private final AppFolders appFolders = new AppFolders();
	//private final InputStream userFile = this.getClass().getClassLoader().getResourceAsStream("user.json");
	
	public ModelBuilder() throws IOException {
		appFolders.createAppFolder();
		appFolders.createAppUserFile();
	}
	
	public Model readModelFromJSON() throws IOException {
		Model userModel = null;
		try (BufferedReader br = new BufferedReader(new FileReader(appFolders.getAppUserFile()))){
			userModel = jsonModel.fromJson(br, Model.class);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return userModel;
	}
	
	public void writeModelToJSON(final Model userModel) throws IOException {
		GsonBuilder builder = new GsonBuilder(); 
	    builder.setPrettyPrinting(); 
		Gson jsonModel = builder.create();
		String jsonString = jsonModel.toJson(userModel);
		
		try {
			FileUtils.writeStringToFile(appFolders.getAppUserFile(), jsonString, "UTF-8", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
