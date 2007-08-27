package com.occamlab.te.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.File;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import com.occamlab.te.TECore;
import com.occamlab.te.util.Utils;
import com.occamlab.te.util.IOUtils;

/**
 * An HTTP servlet that parses the incoming request (response) and sends an HTTP
 * status code 204 response to the client.  The original request (response) is then
 * saved for further processing by the TEAM engine.
 *
 * @author jparrpearson
 */
public class CallbackHandlerServlet extends HttpServlet {

	private Logger logger;
	private Map<String, String> xpointerParams;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		this.logger = Logger.getLogger(this.getClass().getName());

		// Get all XPointer expressions from config (web.xml)
		Enumeration paramNames = config.getInitParameterNames();
		xpointerParams = new HashMap();
		while (paramNames.hasMoreElements()) {
			String name = (String) paramNames.nextElement();
			String value = config.getInitParameter(name);
			xpointerParams.put(name, value);
		}

	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.info("Callback request recieved.");

		// 1) Process the request (response from a previous request)
		try {
			// Only process actual responses, length of 1 or more bytes (-1 is unknown)
			int length = request.getContentLength();
			if (length > 0) {
				// Parse the response
				InputStream is = request.getInputStream();
				byte[] respBytes = IOUtils.inputStreamToBytes(is);

				// Construct the HttpResponse (HttpBasicResponse) to send to parsers
				// TODO: Get actual status from servlet request, don't assumed HTTP/1.1, 200 OK
				HttpVersion version = new HttpVersion(1,1);
				BasicStatusLine statusLine = new BasicStatusLine(version, 200, "OK");
				BasicHttpResponse resp = new BasicHttpResponse(statusLine);
				// Set headers
				Enumeration headerNames = request.getHeaderNames();
				for(; headerNames.hasMoreElements(); ) {
					String name = (String) headerNames.nextElement();
					String value = request.getHeader(name);
					if (name == null) continue;
					resp.addHeader(name, value);
				}
				// Set XML body
				HttpEntity entity = new ByteArrayEntity(respBytes);
				resp.setEntity(entity);

				// TODO: Get the correlation id from the response (xpath)
				/*
				ByteArrayInputStream baip = new ByteArrayInputStream(respBytes);
				dbf = DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				dbf.setFeature("http://apache.org/xml/features/xinclude/fixup-base-uris", false);
				db = dbf.newDocumentBuilder();
				Document doc = db.parse(baip);
				// See if the document element matches any of the given Pointers and call it
				doc.getDocumentElement();
				*/
				String reqId = "";

				// Save the response to file, for use by the engine
				String hash = Utils.generateMD5(reqId);
				String path = System.getProperty("java.io.tmpdir") + "/async/" + hash;
				new File(path).mkdirs();
				File file = new File(path, "BasicHttpResponse.dat");
				IOUtils.writeObjectToFile(resp, file);
			}
		} catch (Exception e){
			System.out.println("Error reading XML response: "+e.getMessage());
		}
		// 2) Send a simple acknowledgement response, status code 204 (No Content)
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);

		// Stop the server when we get a response
		logger.info("Callback response, status code 204, sent.");
	}

}