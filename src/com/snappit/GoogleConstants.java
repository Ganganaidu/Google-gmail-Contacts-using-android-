package com.snappit;

public class GoogleConstants {

	// Use your own client id
	public static String CLIENT_ID = "246295869728-9b302keva0nrc0gull0nr67f1b3qofag.apps.googleusercontent.com";

	// Use your own client secret
	public static String CLIENT_SECRET = "AwD-uh4XdGQEj105sFFxuxK5";

	public static String REDIRECT_URI = "http://localhost";
	public static String GRANT_TYPE = "authorization_code";
	public static String TOKEN_URL = "https://accounts.google.com/o/oauth2/token";
	public static String OAUTH_URL = "https://accounts.google.com/o/oauth2/auth";
	public static String OAUTH_SCOPE = "https://www.googleapis.com/auth/contacts.readonly";

	public static final String CONTACTS_URL = "https://www.google.com/m8/feeds/contacts/default/full";
	public static final int MAX_NB_CONTACTS = 50;
	public static final String APP = "com.snappit";

}
