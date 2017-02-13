package net.knaur.tomcat.addons.manager;

import org.apache.catalina.Engine;
import org.apache.catalina.session.StandardManager;

/**
 * Standard tomcat session manager with bonus.
 * 
 * @author Tomasz Knaur
 * @version 1
 * 
 * <p>
 * example configuration:
 * file: conf/context.xml
 * <pre>
 * 	<Manager className="net.knaur.tomcat.addons.manager.SessionManager" sessionIdPrefix="PRE_1" />
 * </pre>
 * </p>
 *
 */
public class SessionManager extends StandardManager {
	/** Prefix to generated session Id */
	private String sessionIdPrefix;
	
	public void setSessionIdPrefix(String sessionIdPrefix) {
		this.sessionIdPrefix = sessionIdPrefix;
	}
	
	@Override
	protected String generateSessionId() {
		Engine engine = getEngine();
		String sessionId = super.generateSessionId();

		return sessionIdPrefix.concat(sessionId);
	}
	
	
	
}
