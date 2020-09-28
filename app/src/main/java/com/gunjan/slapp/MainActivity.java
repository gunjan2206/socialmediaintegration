package com.gunjan.slapp;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getName();
    private static final String EMAIL = "email";
    private static final String PUBLIC_PROFILE = "public_profile";
    private static final int RC_SIGN_IN = 234;
    CallbackManager callbackManager;
    GoogleSignInClient mGoogleSignInClient;
    SignInButton signInButton;
    FirebaseAuth mAuth;
    private LoginButton loginButton;
    private TwitterLoginButton nLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        //Configure Twitter SDK
        TwitterAuthConfig authConfig = new TwitterAuthConfig(
                getString(R.string.twitter_consumer_key),
                getString(R.string.twitter_consumer_secret));

        TwitterConfig twitterConfig = new TwitterConfig.Builder(this)
                .twitterAuthConfig(authConfig)
                .build();

        Twitter.initialize(twitterConfig);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        signInButton = findViewById(R.id.sign_in_button);
        //signInButton.setSize(SignInButton.SIZE_STANDARD);

        PrintHashKeyFB();
        callbackManager = CallbackManager.Factory.create();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.sign_in_button).setOnClickListener(this);

        loginButton = findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList(PUBLIC_PROFILE, EMAIL));
        // If you are using in a fragment, call loginButton.setFragment(this);

        // Callback registration
        loginButton.registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        getUserDetails(loginResult);
                    }

                    @Override
                    public void onCancel() {
                        // App code
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.e(TAG, "FB_test" + error.toString());
                    }
                });

        nLoginButton = findViewById(R.id.buttononTwitterLogin);

        nLoginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                Log.e(TAG, "twitterlogin:success" + result);
                handleTwitterSession(result.data);
            }

            @Override
            public void failure(TwitterException exception) {

                Log.w(TAG, "failure: ", exception);

            }
        });
    }

    private void handleTwitterSession(TwitterSession session) {
        Log.e(TAG, "handleTwitterSession: " + session);

        AuthCredential credential = TwitterAuthProvider.getCredential(
                session.getAuthToken().token,
                session.getAuthToken().secret);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //Sign in Success,update UI with signed-in users's information
                            Log.e(TAG, "signInWithCredential:success ");
                            FirebaseUser user = mAuth.getCurrentUser();
                            startActivity(new Intent(MainActivity.this, LandingActivity.class)
                                    .putExtra("userName", user.getDisplayName())
                                    .putExtra("userEmail", user.getEmail())
                                    .putExtra("SocialId", user.getUid())
                                    .putExtra("SocialType", "twitter")
                                    .putExtra("userProfile", user.getPhotoUrl().toString()));
                            finish();
                        } else {
                            //If signin fails , display a message to thr user.
                            Log.w(TAG, "signInWithCredential:failure ", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void getUserDetails(LoginResult loginResult) {
        GraphRequest data_request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject json_object, GraphResponse response) {

                Log.e(TAG, "facebook_data: " + json_object.toString());

                try {
                    String userIdFb = json_object.get("id").toString();
                    String userNameFb = json_object.get("name").toString();
                    String userEmail = json_object.get("email").toString();
                    String userProfile = json_object.getJSONObject("picture").getJSONObject("data").get("url").toString();

                    Toast.makeText(MainActivity.this, "id:" + userIdFb + "\n" + "name:" + userNameFb, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, LandingActivity.class)
                            .putExtra("userName", userNameFb)
                            .putExtra("userEmail", userEmail)
                            .putExtra("SocialId", userIdFb)
                            .putExtra("SocialType", "fb")
                            .putExtra("userProfile", userProfile));
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "fb: " + e.toString());
                }
            }
        });

        Bundle permission_param = new Bundle();
        permission_param.putString("fields", "id,name,email,picture.width(120).height(120)");
        data_request.setParameters(permission_param);
        data_request.executeAsync();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void PrintHashKeyFB() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.gunjan.slapp",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.e(TAG, "KeyHash: " + Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            // ...
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Log.e("globtier", "two");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);//Getting the GoogleSignIn Task
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);//Google Sign In was successful, authenticate with Firebase
                firebaseAuthWithGoogle(account);//authenticating with firebase
            } catch (ApiException e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Google: " + e.getMessage());
            }
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
        nLoginButton.onActivityResult(requestCode, resultCode, data);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.e(TAG, "firebaseAuthWithGoogle:" + account.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null); //getting the auth credential
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.e(TAG, "signInWithCredential:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    String userIdGPlus = user.getUid();
                    String userNameGPlus = user.getDisplayName();
                    String userEmailGPlus = user.getEmail();
                    String userProfileUrl = String.valueOf(user.getPhotoUrl());

                    Log.e(TAG, "googleplus : " + user.getUid() + "__" + user.getProviderId() + "__" + user.isAnonymous() + "__" + user.getDisplayName() + "__" + user.getPhotoUrl() + "__" + user.getEmail() + "__" + user.getPhoneNumber());
                    Toast.makeText(MainActivity.this, "id:" + userIdGPlus + "\n" + "name:" + userNameGPlus, Toast.LENGTH_SHORT).show();
                    try {
                        Toast.makeText(MainActivity.this, "Success Google", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, LandingActivity.class)
                                .putExtra("userName", userNameGPlus)
                                .putExtra("userEmail", userEmailGPlus)
                                .putExtra("SocialId", userIdGPlus)
                                .putExtra("SocialType", "gPlus")
                                .putExtra("userProfile", userProfileUrl));
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.getException());
                    Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }


}