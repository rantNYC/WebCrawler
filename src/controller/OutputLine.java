package controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class OutputLine {

    private ByteArrayOutputStream newConsole;
    
    public OutputLine() {
    	newConsole = new ByteArrayOutputStream();
    }
	
	public void setConsoleOut() {
		System.setOut(new PrintStream(newConsole));
	}
	
	public String getConsoleString() {
		return newConsole.toString();
	}
}
