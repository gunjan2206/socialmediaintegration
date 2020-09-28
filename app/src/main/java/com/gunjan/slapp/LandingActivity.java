package com.gunjan.slapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.twitter.sdk.android.core.TwitterCore;


public class LandingActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = LandingActivity.class.getName();
    TextView welcomeTxt, Email, logout;
    String socialId, userName, userEmail, SocialType, profileImageUrl;
    FirebaseAuth mAuth;
    GoogleSignInClient mGoogleSignInClient;
    ImageView profileImage;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        Intent intent = getIntent();
        userName = intent.getStringExtra("userName");
        userEmail = intent.getStringExtra("userEmail");
        socialId = intent.getStringExtra("SocialId");
        SocialType = intent.getStringExtra("SocialType");
        profileImageUrl = intent.getStringExtra("userProfile");

        welcomeTxt = findViewById(R.id.welcomeTxt);
        Email = findViewById(R.id.Email);
        logout = findViewById(R.id.logout);
        profileImage = findViewById(R.id.profileImage);

        welcomeTxt.setText("Welcome " + userName);
        Email.setText(userEmail);

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)      //Then we need a GoogleSignInOptions object
                .requestIdToken(getString(R.string.default_web_client_id))          //And we need to build it as below
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        logout.setOnClickListener(this);

        try {
            Glide.with(this).load(profileImageUrl.replace("~", "").replace("..", ""))
                    .thumbnail(0.5f)
                    .into(profileImage);
        } catch (Exception e) {
            Log.d(TAG, "Error : " + e.toString());
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.logout:

                if (Utilities.isConnected(getApplicationContext())) {
                    AlertDialog.Builder alBuilder = new AlertDialog.Builder(this);
                    alBuilder.setMessage("Are ypu sure want to logout");
                    alBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if (SocialType.equalsIgnoreCase("gPlus")) {
                                //Toast.makeText(MainActivity.this, "Google plus SignOut", Toast.LENGTH_SHORT).show();
                                mAuth.signOut();
                                mGoogleSignInClient.signOut().addOnCompleteListener(LandingActivity.this,
                                        new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Intent intent = new Intent(LandingActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        });

                            } else if (SocialType.equalsIgnoreCase("twitter")) {
                                //Toast.makeText(MainActivity.this, "Google plus SignOut", Toast.LENGTH_SHORT).show();
                                mAuth.signOut();
                                TwitterCore.getInstance().getSessionManager().clearActiveSession();
                                mGoogleSignInClient.signOut().addOnCompleteListener(LandingActivity.this,
                                        new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Intent intent = new Intent(LandingActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        });

                            } else {
                                LoginManager.getInstance().logOut();

                                Intent intent = new Intent(LandingActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
                    alBuilder.setNegativeButton("Cancel", null);
                    AlertDialog alertDialog = alBuilder.create();
                    alertDialog.show();

                } else {
                    Toast.makeText(this, "Check your internet connection!", Toast.LENGTH_SHORT).show();
                }
        }
    }
}
