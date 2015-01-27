package com.snappit;

import java.net.URL;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gdata.client.Query;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.extensions.Email;

public class MainActivity extends ActionBarActivity {
	final String TAG = getClass().getName();

	private Dialog auth_dialog;
	private ListView list;
	private ProgressDialog pDialog;

	private String auth_token = "";
	private int pagingIndex = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		list = (ListView) findViewById(R.id.list);
		launchAuthDialog();
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void launchAuthDialog() {
		final Context context = this;
		auth_dialog = new Dialog(context);
		auth_dialog.setTitle(getString(R.string.app_name));
		auth_dialog.setCancelable(true);
		auth_dialog.setContentView(R.layout.auth_dialog);

		auth_dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});

		WebView web = (WebView) auth_dialog.findViewById(R.id.webv);
		web.getSettings().setJavaScriptEnabled(true);
		web.loadUrl(GoogleConstants.OAUTH_URL + "?redirect_uri=" + GoogleConstants.REDIRECT_URI 
				+ "&response_type=code&client_id=" + GoogleConstants.CLIENT_ID + "&scope=" 
				+ GoogleConstants.OAUTH_SCOPE);
		web.setWebViewClient(new WebViewClient() {
			boolean authComplete = false;

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				if (url.contains("?code=") && authComplete != true) {

					Uri uri = Uri.parse(url);
					String authCode = uri.getQueryParameter("code");
					authComplete = true;
					auth_dialog.dismiss();

					pDialog = new ProgressDialog(context);
					pDialog.setMessage("Contacting Google ...");
					pDialog.setIndeterminate(false);
					pDialog.setCancelable(true);
					pDialog.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							finish();
						}
					});
					pDialog.show();

					new GoogleAuthToken(context).execute(authCode);

				} else if (url.contains("error=access_denied")) {
					Log.i("", "ACCESS_DENIED_HERE");
					authComplete = true;
					auth_dialog.dismiss();
				}
			}
		});
		auth_dialog.show();
	}

	private class GoogleAuthToken extends AsyncTask<String, String, JSONObject> {
		private Context context;

		public GoogleAuthToken(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected JSONObject doInBackground(String... args) {
			String authCode = args[0];
			GetAccessToken jParser = new GetAccessToken();
			JSONObject json = jParser.gettoken(GoogleConstants.TOKEN_URL, authCode, GoogleConstants.CLIENT_ID, GoogleConstants.CLIENT_SECRET, GoogleConstants.REDIRECT_URI, GoogleConstants.GRANT_TYPE);
			return json;
		}

		@Override
		protected void onPostExecute(JSONObject json) {
			pDialog.dismiss();
			if (json != null) {
				try {
					auth_token = json.getString("access_token");
					String expire = json.getString("expires_in");
					String refresh = json.getString("refresh_token");

					pDialog = new ProgressDialog(context);
					pDialog.setMessage("Authenticated. Getting Google Contacts ...");
					pDialog.setIndeterminate(false);
					pDialog.setCancelable(true);
					pDialog.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							finish();
						}
					});
					pDialog.show();
					new GetGoogleContacts(context).execute(auth_token);

				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class GetGoogleContacts extends AsyncTask<String, String, List<ContactEntry>> {
		private Context context;

		public GetGoogleContacts(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

		}

		@Override
		protected List<ContactEntry> doInBackground(String... args) {
			String accessToken = args[0];
			ContactsService contactsService = new ContactsService(GoogleConstants.APP);
			contactsService.setHeader("Authorization", "Bearer " + accessToken);
			contactsService.setHeader("GData-Version", "3.0");
			List<ContactEntry> contactEntries = null;
			try {
				URL feedUrl = new URL(GoogleConstants.CONTACTS_URL);
				Query myQuery = new Query(feedUrl);
				myQuery.setStartIndex(pagingIndex);
				myQuery.setMaxResults(GoogleConstants.MAX_NB_CONTACTS);
				
				ContactFeed resultFeed = contactsService.getFeed(myQuery, ContactFeed.class);
				contactEntries = resultFeed.getEntries();
			} catch (Exception e) {
				pDialog.dismiss();
				//Toast.makeText(context, "Failed to get Contacts", Toast.LENGTH_SHORT).show();
			}
			return contactEntries;
		}

		@Override
		protected void onPostExecute(List<ContactEntry> googleContacts) {
			if (null != googleContacts && googleContacts.size() > 0) {
				//List<Contact> contacts = new ArrayList<Contact>();
				Log.e(TAG,"googleContacts:"+googleContacts.size());

				for (ContactEntry contactEntry : googleContacts) {
					String name = "";
					String email = "";

					if (contactEntry.hasName()) {
						com.google.gdata.data.extensions.Name tmpName = contactEntry.getName();
						if (tmpName.hasFullName()) {
							name = tmpName.getFullName().getValue();
							//Log.e(TAG,"name:"+name);
						} else {
							if (tmpName.hasGivenName()) {
								name = tmpName.getGivenName().getValue();
								if (tmpName.getGivenName().hasYomi()) {
									name += " (" + tmpName.getGivenName().getYomi() + ")";
								}
								if (tmpName.hasFamilyName()) {
									name += tmpName.getFamilyName().getValue();
									if (tmpName.getFamilyName().hasYomi()) {
										name += " (" + tmpName.getFamilyName().getYomi() + ")";
									}
								}
							}
						}
					}
					List<com.google.gdata.data.extensions.Email> emails = contactEntry.getEmailAddresses();

					if (null != emails && emails.size() > 0) {
						Email tempEmail = (Email) emails.get(0);
						email = tempEmail.getAddress();
						Log.e(TAG,"email:"+email);
					}
				}

				if(googleContacts.size() < GoogleConstants.MAX_NB_CONTACTS){
					pDialog.dismiss();
				} else {
					pagingIndex = pagingIndex+GoogleConstants.MAX_NB_CONTACTS;
					new GetGoogleContacts(context).execute(auth_token);
				}
			} else {
				Log.e(TAG, "No Contact Found.");
				Toast.makeText(context, "No Contact Found.", Toast.LENGTH_SHORT).show();
			}
		}
	}
}