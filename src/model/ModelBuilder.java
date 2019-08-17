package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ModelBuilder {
	
	//private final String filename = "user.json";
	private final String defaultUserFile = "src/model/user.json";
	//private final InputStream userFile = this.getClass().getClassLoader().getResourceAsStream("user.json");
	
	public ModelBuilder() throws IOException {
		Path filePath = Path.of(defaultUserFile);
		if(!Files.exists(filePath)) {
			Files.createFile(filePath);
		}
	}
	
	public Model readModelFromJSON() throws IOException {
		Model userModel = null;
		BufferedReader br = null;
		try {
			Gson jsonModel = new Gson();
			br = new BufferedReader(new FileReader(defaultUserFile));
			userModel = jsonModel.fromJson(br, Model.class);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}  finally{
			br.close();
		}
		return userModel;
	}
	
	public void writeModelToJSON(final Model userModel) throws IOException {
		GsonBuilder builder = new GsonBuilder(); 
	    builder.setPrettyPrinting(); 
		Gson jsonModel = builder.create();
		String jsonString = jsonModel.toJson(userModel);
		
		try {
			File file = new File(defaultUserFile);
			FileUtils.writeStringToFile(file, jsonString, "UTF-8", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
