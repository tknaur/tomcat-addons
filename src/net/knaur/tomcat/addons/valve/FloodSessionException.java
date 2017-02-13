package net.knaur.tomcat.addons.valve;

import javax.servlet.ServletException;

public class FloodSessionException extends ServletException {

	private final static String message = "Your request is banned permanently, because flood. You can remove ban sending DELETE request with valid token. ";
	
	
	public FloodSessionException() {
		super(message);
	}
	
}
