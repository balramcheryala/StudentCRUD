package org.bridgelabz.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.bridgelabz.controllers.Dashboard;
import org.bridgelabz.dao.ClientCredentialsDao;
import org.bridgelabz.properties.ConnectionProperties;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;

/*Connection name: FacebookConnection
 *created: Aug 18, 2016 11:33AM
 *Created By: Balram
 */



public class FacebookConnection {

	ConnectionProperties CP = new ConnectionProperties();

	Session session;

	static String accessToken = "";

	@Autowired
	public SessionFactory sessionFactory;
	
	@Autowired
	ClientCredentialsDao dao;
	
	// Invoking a authentication url
	public String getAuthUrl(String app_id) {
		String fbLoginUrl = "";
		try {
			System.out.println("Creating a connection TO faceBook");
			fbLoginUrl = "http://www.facebook.com/dialog/oauth?" + "client_id=" + app_id + "&redirect_uri="
					+ URLEncoder.encode(CP.FB_REDIRECT_URI, "UTF-8") + "&scope=email,user_posts,publish_actions";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return fbLoginUrl;
	}

	// creating a connection to grapghurl
	public String getGraphUrl(String code) {
		String fbGraphUrl = "";
		try {
			fbGraphUrl = "https://graph.facebook.com/oauth/access_token?" + "client_id="
					+ dao.credentials(Dashboard.globalname, "FACEBOOK").get(0) + "&redirect_uri="
					+ URLEncoder.encode(CP.FB_REDIRECT_URI, "UTF-8") + "&client_secret="
					+ dao.credentials(Dashboard.globalname, "FACEBOOK").get(1) + "&code=" + code;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return fbGraphUrl;
	}

	// getting accesstoken
	public String getAccessToken(String code) {
		System.out.println("getAccessToken method"+code);
		if ("".equals(accessToken)) {
			URL fbGraphURL;
			try {
				fbGraphURL = new URL(getGraphUrl(code));
			} catch (MalformedURLException e) {
				e.printStackTrace();
				throw new RuntimeException("Invalid code received " + e);
			}
			URLConnection FacebookConnection;
			StringBuffer b = null;
			try {
				// openingConnection of fbgraph url
				FacebookConnection = fbGraphURL.openConnection();
				BufferedReader in;
				in = new BufferedReader(new InputStreamReader(FacebookConnection.getInputStream()));
				String inputLine;
				b = new StringBuffer();
				// reading a accesstoken from jsp page
				while ((inputLine = in.readLine()) != null)
					b.append(inputLine + "\n");
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Unable to connect with Facebook " + e);
			}

			accessToken = b.toString();
			// Validating a AccessToken Got from the APiProvider
			if (accessToken.startsWith("{")) {
				throw new RuntimeException("ERROR: Access Token Invalid: " + accessToken);
			}
		}
		return accessToken;
	}

}
