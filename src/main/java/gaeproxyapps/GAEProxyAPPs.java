package gaeproxyapps;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class GAEProxyAPPs extends HttpServlet {

	private static final long serialVersionUID = -4685945835054414900L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		proxy(req, resp);
	}

	private void proxy(HttpServletRequest req, HttpServletResponse resp) throws MalformedURLException, IOException, ProtocolException {
		String requestURI = req.getRequestURI();

		HttpURLConnection connection = openConnection(requestURI);

		copyHeaders(resp, connection);

		copyBody(resp, connection);

		ignoreGoogleAppsWarning(resp, connection);
	}

	private void ignoreGoogleAppsWarning(HttpServletResponse resp, HttpURLConnection connection) {
		if (!isHtmlContent(connection)) {
			return;
		}

		try {
			PrintWriter pw = new PrintWriter(resp.getOutputStream());
			pw.print("<style type=\"text/css\"> .warning-panel {display: none;} </style>");
		} catch (IOException e) {
			
			throw new RuntimeException(e);
		}
	}

	private boolean isHtmlContent(HttpURLConnection connection) {
		for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {

			if (!entry.getKey().equalsIgnoreCase(("content-type"))) {
				continue;
			}

			for (String value : entry.getValue()) {
				if (value.toLowerCase().contains("text/html")) {
					return true;
				}
			}
		}
		return false;
	}

	private void copyBody(HttpServletResponse resp, HttpURLConnection connection) throws IOException {
		IOUtils.copy(connection.getInputStream(), resp.getOutputStream());
	}

	private HttpURLConnection openConnection(String requestURI) throws MalformedURLException, IOException, ProtocolException {
		URL url = new URL("https://script.google.com" + requestURI);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setRequestMethod("GET");
		return connection;
	}

	private void copyHeaders(HttpServletResponse resp, HttpURLConnection connection) {
		for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {

			if (entry.getKey().equalsIgnoreCase(("x-frame-options"))) {
				continue;
			}

			for (String value : entry.getValue()) {
				resp.addHeader(entry.getKey(), value);
			}
		}
	}
}