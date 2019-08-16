package model;

import java.util.HashSet;

public class Model {
	
	private String urlPath;
	private String outputFolder;
	private boolean isEmpty;
	private HashSet<String> visitedSites;
	
	public Model(String urlPath, String outputFolder) {
		this.urlPath = urlPath;
		this.outputFolder = outputFolder;
		if(this.urlPath == "" || this.outputFolder == "") {
			this.isEmpty = true;
		}
		this.visitedSites = new HashSet<String>();
	}
	
	public String getUrlPath() {
		return urlPath;
	}
	public void setUrlPath(String urlPath) {
		this.urlPath = urlPath;
	}
	public String getOutputFolder() {
		return outputFolder;
	}
	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public boolean isEmpty() {
		return isEmpty;
	}

	public HashSet<String> getVisitedSites() {
		return visitedSites;
	}

	public void setVisitedSites(HashSet<String> visitedSites) {
		this.visitedSites = visitedSites;
	}

	public void addSite() {
		if(!this.visitedSites.contains(this.urlPath)) {
			this.visitedSites.add(this.urlPath);
		}
	}
}
