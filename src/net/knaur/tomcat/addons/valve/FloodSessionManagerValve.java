package net.knaur.tomcat.addons.valve;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple Valve component to protect against flood of newly created sessions.
 *
 * @author Tomasz Knaur
 * @version 0.1
 *
 * <p>
 * Example config:
 * <pre>
 *   <Valve className="net.knaur.tomcat.addons.valve.FloodSessionManagerValve" maxRequestPerClient="5" cleaningInterval="60" blockedListPurgeToken="123456" />
 * </pre>
 *   Sending purge request with curl: curl -v -X "DELETE" --header "PURGE_TOKEN:A1B2C3" http://localhost:8080/
 * </p>
 
 */

public class FloodSessionManagerValve extends ValveBase {
	
	private static final Log log = LogFactory.getLog(FloodSessionManagerValve.class);
	
	private static final String FAVICON_ICO_URI = "/favicon.ico";
	
	private static long LAST_CLEANING_TIME = 0;
	private static String DELETE_BAN_REQUEST_METHOD = "DELETE";
	private static String PURGE_BAN_LIST_REQUEST_HEDER = "PURGE_TOKEN";
	
	private final Queue<Integer> blockedClients = new ConcurrentLinkedQueue<Integer>();
	private final Map<Integer, Integer> counter = new ConcurrentHashMap<Integer, Integer>();
	
	private int maxRequestPerClient = 3;
	private int cleaningInterval =  10;
	private String keyPrefix = "DEFAULT";
	private String blockedListPurgeToken ="A1B2C3";
	
	public FloodSessionManagerValve() {
		super(true);
	}
	
	@Override
	protected void initInternal() throws LifecycleException {
		super.initInternal();
		LAST_CLEANING_TIME = System.currentTimeMillis();
	}
	
	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		
		if (DELETE_BAN_REQUEST_METHOD.equals(request.getMethod())) {
			response.setContentType("text/html");
			response.setCharacterEncoding("utf-8");
			ServletOutputStream outputStream = response.getOutputStream();
			
			String sentPurgeToken = request.getHeader(PURGE_BAN_LIST_REQUEST_HEDER);
			if (sentPurgeToken != null && getBlockedListPurgeToken().equals(sentPurgeToken)) {
				blockedClients.clear();
				response.setStatus(HttpServletResponse.SC_OK);
				outputStream.print("Removed!");
			} else {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				outputStream.print("Purge failed!");
				
			}
			outputStream.flush();
			outputStream.close();
			return;
		}
		
		if (FAVICON_ICO_URI.equals(request.getRequestURI())) {
			return;
		}
		
		long currentTimestamp = System.currentTimeMillis();
		
		if (currentTimestamp >= LAST_CLEANING_TIME + this.getCleaningInterval() * 1000) {
			if (log.isDebugEnabled()) {
				if (log.isDebugEnabled()) log.debug("Cleaning counter");
				counter.clear();
				LAST_CLEANING_TIME = currentTimestamp;
			}
		}
		
		String clientIp;
		String userAgent = null;
		String clientID;
		
		if (request.getSession(false) == null) {
			clientIp = request.getRemoteAddr();
			Enumeration s = request.getHeaders("user-agent");
			
			if (s.hasMoreElements()) userAgent = (String) s.nextElement();
			
			if (userAgent != null && !s.hasMoreElements()) {
				
				clientID = this.getKeyPrefix() + "__" + clientIp + "___" + userAgent;
				int clientHash =   clientID.hashCode();
				
				if (blockedClients.contains(clientHash)) {
					throw new FloodSessionException();
				}
				
				int i=0;
				if (counter.containsKey(clientHash))
					i = counter.get(clientHash);
				++i;
				
				if (this.getMaxRequestPerClient() <= i) {
					blockedClients.add(clientHash);
					counter.remove(clientHash);
					throw new FloodSessionException();
				}
				counter.put(clientHash, i);
			}
		}
		
		this.getNext().invoke(request,response);
	}
	
	public int getMaxRequestPerClient() {
		return maxRequestPerClient;
	}
	
	public void setMaxRequestPerClient(int maxRequestPerClient) {
		this.maxRequestPerClient = maxRequestPerClient;
	}
	
	public int getCleaningInterval() {
		return cleaningInterval;
	}
	
	public void setCleaningInterval(int cleaningInterval) {
		this.cleaningInterval = cleaningInterval;
	}
	
	public String getKeyPrefix() {
		return keyPrefix;
	}
	
	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}
	
	public String getBlockedListPurgeToken() {
		return blockedListPurgeToken;
	}
	
	public void setBlockedListPurgeToken(String blockedListPurgeToken) {
		this.blockedListPurgeToken = blockedListPurgeToken;
	}
}
