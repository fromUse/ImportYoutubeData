package com.example.chenq.oauthdemo;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.PlaylistListResponse;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 10000;
    private GoogleApiClient mGoogleApiClient;
    private static final String[] SCOPES = { YouTubeScopes.YOUTUBE_READONLY };
    private GoogleAccountCredential mCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
//                .requestScopes(new Scope("https://www.googleapis.com/auth/youtube.readonly"))
//                .requestIdToken("298180841000-heskfbha623p3g8doepc38s63nkv01e6.apps.googleusercontent.com")
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()){
                GoogleSignInAccount account = result.getSignInAccount();
                String text =  "name : "  + account.getDisplayName()
                        + "\nid : " + account.getId()
                        + "\ntoken : " + account.getIdToken()
                        + "\nemail : " + account.getEmail()
                        + "\nServerAuthCode : " + account.getServerAuthCode()
                        + "\nPhotoUrl : " + account.getPhotoUrl()
                        + "\nGrantedScopes : " + account.getGrantedScopes();
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                TextView textView = (TextView) findViewById(R.id.text);
                textView.setText(text);
                Task task = new Task(account.getEmail());
                task.start();
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this,"error : " +  connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    public void sign(View view){
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void out(View view){
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallbacks<Status>() {
                @Override
                public void onSuccess(@NonNull Status status) {
                    Toast.makeText(MainActivity.this, "sign out success", Toast.LENGTH_SHORT).show();
                    TextView textView = (TextView) findViewById(R.id.text);
                    textView.setText("");
                }

                @Override
                public void onFailure(@NonNull Status status) {

                }
            });
        }
    }

    private class Task extends Thread{
        private String name;

        public Task(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            try {
                String accountName = null;

                AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
                Account[] list = manager.getAccounts();
                for (Account account : list) {
                    if (account.type.equalsIgnoreCase("com.google")) {
                        accountName = account.name;
                        mCredential.setSelectedAccountName(accountName);
                        Log.e("name ",accountName );
                        break;
                    }
                }
              mCredential.setSelectedAccountName(name);
              HttpTransport transport = AndroidHttp.newCompatibleTransport();
              JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
              YouTube mService = new com.google.api.services.youtube.YouTube.Builder(
                        transport, jsonFactory, mCredential)
                        .setApplicationName("YouTube Data API Android Quickstart")
                        .build();
                PlaylistListResponse listResponse = mService.playlists().list("snippet,contentDetails").setMine(true).execute();

                Log.d("aaaa", "run: " + listResponse.getItems().size());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("AUTH","aaaa",e);
            }
        }
    }
}
