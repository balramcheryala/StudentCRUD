package org.bridgelabz.controllers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bridgelabz.connection.FacebookConnection;
import org.bridgelabz.connection.GitHubConnection;
import org.bridgelabz.connection.GoogleConnection;
import org.bridgelabz.connection.LinkedInConnection;
import org.bridgelabz.dao.ClientCredentialsDao;
import org.bridgelabz.graph.FacebookGraph;
import org.bridgelabz.graph.GitHubGraph;
import org.bridgelabz.graph.GoogleGraph;
import org.bridgelabz.graph.LinkedInGraph;
import org.bridgelabz.model.FacebookDetails;
import org.bridgelabz.model.GitHubDetails;
import org.bridgelabz.model.LinkedInDetails;
import org.bridgelabz.model.TwitterDetails;
import org.bridgelabz.properties.ConnectionProperties;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

/*Class ClientDetailsBean.
 *created: Aug 18, 2016 11:33AM
 *Created By: Balram
 */

@Controller("social")
public class SocialController {
	Session session;

	ConnectionProperties CP = new ConnectionProperties();

	@Autowired
	public SessionFactory sessionFactory;

	@Autowired
	ClientCredentialsDao dao;

	private String code = "";
	JSONObject jsonObj;

	// Facebook AccessToken
	String facebookAccessToken;

	// Twitter AccessToken
	Twitter twitter;
	
	@RequestMapping("/fbcontroller")
	public String facebookrequest() {
		String fbLoginUrl = "";
		try {
			ArrayList<String> arrayList = new ArrayList<>();
			System.out.println("project name:" + Dashboard.globalname);
			arrayList = dao.credentials(Dashboard.globalname, "FACEBOOK");
			System.out.println("app Id:" + arrayList.get(0));
			System.out.println("app password:" + arrayList.get(1));
			fbLoginUrl = "http://www.facebook.com/dialog/oauth?" + "client_id=" + arrayList.get(0) + "&redirect_uri="
					+ URLEncoder.encode(CP.FB_REDIRECT_URI, "UTF-8") + "&scope=email,user_posts,publish_actions";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return "redirect:"+fbLoginUrl;
	}
	
	@RequestMapping("/linkedinrequest")
	public String linkedinrequest() {
		String LoginUrl = "";
		try {
			ArrayList<String> arrayList = new ArrayList<>();
			System.out.println("project name:" + Dashboard.globalname);
			arrayList = dao.credentials(Dashboard.globalname, "LINKEDIN");
			System.out.println("app Id:" + arrayList.get(0));
			System.out.println("app password:" + arrayList.get(1));
			LoginUrl = "https://www.linkedin.com/oauth/v2/authorization?" + "response_type=code&client_id="
					+ dao.credentials(Dashboard.globalname, "LINKEDIN").get(0) + "&redirect_uri="
					+ URLEncoder.encode(CP.LK_REDIRECT_URI, "UTF-8")
					+ "&state=DCEe45A53sdfKef424FWf&scope=r_basicprofile";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return "redirect:"+LoginUrl;
	}

	// Request Mapping For Twitterpost

	@RequestMapping(value = "/twitterpost", method = RequestMethod.POST)
	public ModelAndView playersList(@RequestParam(value = "tweet", required = true) String post)
			throws TwitterException {
		System.out.println(post);
		StatusUpdate status = new StatusUpdate(post);

		twitter.updateStatus(status);
		Map<String, String> map = new HashMap<String, String>();
		map.put("msg", "Successfully posted To Twitter");
		return new ModelAndView("tweetsuccess", map);
	}

	/*
	 * FaceBook Controller
	 */

	@RequestMapping(value = "/fbpost", method = RequestMethod.POST)
	public ModelAndView fbpost(@RequestParam(value = "facebookpost", required = true) String facebookpost)
			throws IOException {
		System.out.println("I am executing");
		HttpsURLConnection connection;
		String fbLoginUrl = "https://graph.facebook.com/106097123166287/feed?message="
				+ URLEncoder.encode(facebookpost, "UTF-8") + "&"
				+ dao.AccessToken(Dashboard.globalname, "FacebookDetails");

		URL url = new URL(fbLoginUrl);
		connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		// connection.connect();
		InputStream response = connection.getInputStream();
		System.out.println("Response " + response);
		Map<String, String> map = new HashMap<String, String>();
		map.put("msg", "Successfully posted To FaceBook");
		return new ModelAndView("DataSaved", map);
	}

	@RequestMapping(value = "/bufferpost", method = RequestMethod.POST)
	public ModelAndView bufferPost(@RequestParam(value = "post", required = true) String post)
			throws TwitterException, IOException {
		System.out.println("bufferpostexecuting");

		HttpsURLConnection connection;
		String fbLoginUrl = "https://graph.facebook.com/106097123166287/feed?message="
				+ URLEncoder.encode(post, "UTF-8") + "&" + dao.AccessToken(Dashboard.globalname, "FacebookDetails");
		URL url = new URL(fbLoginUrl);
		connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		InputStream response = connection.getInputStream();
		System.out.println("Response " + response);

		// TWITTER POST
		StatusUpdate status = new StatusUpdate(post);
		twitter.updateStatus(status);
		Map<String, String> map = new HashMap<String, String>();
		map.put("msg", "Successfully posted To FaceBook And Twitter");
		return new ModelAndView("bufferpost", map);
	}

	@RequestMapping(value = "/fbcomnn", method = RequestMethod.GET)
	public ModelAndView facebook(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// getiing the autherazation code
		code = req.getParameter("code");
		System.out.println("Authorization Code : " + code);
		// validating the autherazation code
		if (code == null || code.equals("")) {
			throw new RuntimeException("ERROR: Didn't get code parameter in callback.");
		}

		// handle response.
		// creating FacebookConnection by calling FacebookConnection class
		FacebookConnection Connection = new FacebookConnection();
		// getting accesstoken here by exchanging the autherization code with
		// provider
		System.out.println("Created the object for FacebookConnection");
		facebookAccessToken = Connection.getAccessToken(code);
		System.out.println("Facebook accessToken : " + facebookAccessToken);
		// paasing the accesstoken to FacebookGraph graph method to access the
		// user
		// details
		FacebookGraph Graph = new FacebookGraph(facebookAccessToken);
		// getting the graph user in json format
		String graph = Graph.getFacebookGraph();
		// passing the graph to get graph data for getting the graph from given
		// json fromat
		Map<String, String> ProfileData = Graph.getGraphData(graph);

		session = sessionFactory.openSession();
		FacebookDetails details = new FacebookDetails();
		// setting AccessToken into Database
		details.setAccessToken(facebookAccessToken);
		details.setFacebookid(ProfileData.get("id"));
		details.setProjectname(Dashboard.globalname);
		details.setBio(ProfileData.get("bio"));
		details.setEmail(ProfileData.get("email"));
		details.setFirst_name(ProfileData.get("first_name"));
		details.setGender(ProfileData.get("gender"));
		details.setLast_name(ProfileData.get("last_name"));
		details.setName(ProfileData.get("name"));
		session.save(details);
		session.close();

		return new ModelAndView("DataSaved");

	}

	/*
	 * 
	 * LinkedIn Controller
	 * 
	 */
	@RequestMapping(value = "/linkedin", method = RequestMethod.GET)
	public ModelAndView LinkedIn(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// getiing the autherazation code
		code = req.getParameter("code");
		System.out.println("Authorization Code : " + code);
		// validating the autherazation code
		if (code == null || code.equals("")) {
			throw new RuntimeException("ERROR: Didn't get code parameter in callback.");
		}
		// creating LinkedInConnection by calling LinkedInConnection class
		LinkedInConnection Connection = new LinkedInConnection();
		// getting accesstoken here by exchanging the autherization code with
		// provider
		String accessToken = Connection.getAccessToken(code);

		try {
			jsonObj = new JSONObject(accessToken);
			System.out.println(jsonObj.get("access_token"));
			// paasing the accesstoken to LinkedInGraph graph method to access
			// the user
			// details
			LinkedInGraph Graph = new LinkedInGraph(jsonObj.get("access_token").toString());
			// getting the graph user in json format
			String graph = Graph.getGraph();
			// passing the graph to get graph data for getting the graph from
			// given
			// json fromat
			Map<String, String> ProfileData = Graph.getGraphData(graph);
			// Parsed Data Storing into the DataBase
			session = sessionFactory.openSession();
			LinkedInDetails details = new LinkedInDetails();
			details.setAccessToken(accessToken);
			details.setLinkedinId(ProfileData.get("id"));
			details.setProjectName(Dashboard.globalname);
			details.setFirstName(ProfileData.get("firstName"));
			details.setHeadline(ProfileData.get("headline"));
			details.setIndustry(ProfileData.get("industry"));
			details.setPictureUrl(ProfileData.get("pictureUrl"));
			details.setPublicProfileUrl(ProfileData.get("publicProfileUrl"));
			session.save(details);
			session.close();

		} catch (JSONException e) {

			e.printStackTrace();
		}
		return new ModelAndView("DataSaved");

	}

	/*
	 * Google Controller
	 */

	@RequestMapping(value = "/google", method = RequestMethod.GET)
	public ModelAndView google(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// getiing the autherazation code
		code = req.getParameter("code");
		System.out.println("Authorization Code : " + code);
		// validating the autherazation code
		if (code == null || code.equals("")) {
			throw new RuntimeException("ERROR: Didn't get code parameter in callback.");
		}
		// creating GoogleConnection by calling GoogleConnection class
		GoogleConnection Connection = new GoogleConnection();
		// getting accesstoken here by exchanging the autherization code with
		// provider
		String accessToken = Connection.getAccessToken(code);
		System.out.println(accessToken);
		// paasing the accesstoken to GoogleGraph graph method to access the
		// user
		// details
		GoogleGraph Graph = new GoogleGraph(accessToken);
		// getting the graph user in json format
		String graph = Graph.getGoogleGraph();
		// passing the graph to get graph data for getting the graph from given
		// json fromat
		Map<String, String> ProfileData = Graph.getGraphData(graph);
		ServletOutputStream out = res.getOutputStream();
		out.println("<h1>BRIDGEMBAAS</h1>");
		out.println("<h2>Google Application Main Menu</h2>");
		out.println("<div>Welcome " + ProfileData.get("fullname"));
		out.println("<div>Your first_name: " + ProfileData.get("first_name"));
		out.println("<div>Your last_name: " + ProfileData.get("last_name"));
		out.println("<div>You are " + ProfileData.get("gender"));
		out.println("<div>Your'e birthday " + ProfileData.get("birthday"));
		out.println("<div>About :" + ProfileData.get("bio"));
		return new ModelAndView("DataSaved");

	}

	/*
	 * GitHub Controller
	 */

	@RequestMapping(value = "/github", method = RequestMethod.GET)
	public ModelAndView GitHub(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		// getiing the autherazation code
		code = req.getParameter("code");

		// validating the autherazation code
		if (code == null || code.equals("")) {
			throw new RuntimeException("ERROR: Didn't get code parameter in callback.");
		}

		// creating github connection by calling github connection class
		GitHubConnection Connection = new GitHubConnection();

		// getting accesstoken here by exchanging the autherization code with
		// provider
		String accessToken = Connection.getAccessToken(code);

		// paasing the accesstoken to github graph method to access the user
		// details
		GitHubGraph Graph = new GitHubGraph(accessToken);

		// getting the graph user in json format
		String graph = Graph.getGitHubGraph();

		// passing the graph to get graph data for getting the graph from given
		// json fromat
		Map<String, String> ProfileData = Graph.getGraphData(graph);

		// getting the parsed user provider id here
		session = sessionFactory.openSession();
		GitHubDetails details = new GitHubDetails();
		details.setAccessToken(accessToken);
		details.setProjectName(Dashboard.globalname);
		details.setGithubId(ProfileData.get("id"));
		details.setLogin(ProfileData.get("login"));
		details.setFollowers_url(ProfileData.get("followers_url"));
		details.setRepos_url(ProfileData.get("repos_url"));
		details.setBio(ProfileData.get("bio"));
		details.setAvatar_url(ProfileData.get("avatar_url"));
		details.setName(ProfileData.get("name"));
		details.setLocation(ProfileData.get("location"));
		details.setCreated_at(ProfileData.get("created_at"));
		details.setUpdated_at(ProfileData.get("updated_at"));
		session.save(details);
		session.close();
		return new ModelAndView("DataSaved");
	}

	/*
	 * Twitter Controller
	 */
	@RequestMapping("/twitter")
	public ModelAndView twitter(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, TwitterException {

		// Get twitter object from session
		twitter = (Twitter) request.getSession().getAttribute("twitter");

		// Get twitter request token object from session
		RequestToken requestToken = (RequestToken) request.getSession().getAttribute("requestToken");
		String verifier = request.getParameter("oauth_verifier");

		// Get twitter access token object by verifying request token
		AccessToken accessToken;
		Map<String, String> map = new HashMap<String, String>();
		map.put("msg", "Successfully Loggedin To Twitter");
		
		try {

			accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

			System.out.println(accessToken);
			request.getSession().removeAttribute("requestToken");

			session = sessionFactory.openSession();
			TwitterDetails details = new TwitterDetails();
			details.setAccessToken(twitter.toString());
			details.setUserId(accessToken.getUserId());
			details.setScreenName(accessToken.getScreenName());
			details.setToken(accessToken.getToken());
			details.setTokenSecret(accessToken.getTokenSecret());
			session.save(details);
			session.close();

		} catch (TwitterException e) {

			e.printStackTrace();
		}
		return new ModelAndView("DataSaved", map);
	}

	// Method For Upload Pic For Twitter

	public void uploadPic(File file, String message) throws Exception {
		try {
			StatusUpdate status = new StatusUpdate(message);
			status.setMedia(file);
			twitter.updateStatus(status);
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

	/*
	 * 
	 * Twitter Sign In Controller
	 */
	@RequestMapping("/signin")
	public void twitterSignin(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// configure twitter api with consumer key and secret key
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(dao.credentials(Dashboard.globalname, "TWITTER").get(0))
				.setOAuthConsumerSecret(dao.credentials(Dashboard.globalname, "TWITTER").get(1));
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();
		request.getSession().setAttribute("twitter", twitter);
		try {

			// setup callback URL
			StringBuffer callbackURL = request.getRequestURL();
			int index = callbackURL.lastIndexOf("/");
			callbackURL.replace(index, callbackURL.length(), "").append("/twitter");

			// get request object and save to session
			RequestToken requestToken = twitter.getOAuthRequestToken(callbackURL.toString());
			System.out.println(requestToken);
			request.getSession().setAttribute("requestToken", requestToken);

			// redirect to twitter authentication URL
			response.sendRedirect(requestToken.getAuthenticationURL());

		} catch (TwitterException e) {
			throw new ServletException(e);
		}

	}
}
