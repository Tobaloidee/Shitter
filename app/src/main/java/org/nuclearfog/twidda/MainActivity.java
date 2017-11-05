package org.nuclearfog.twidda;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.Toast;
import android.content.SharedPreferences;

import org.nuclearfog.twidda.engine.TwitterEngine;

public class MainActivity extends Activity
{
    private Button linkButton, verifierButton, loginButton;
    private EditText pin;
    private Context con;
    private SharedPreferences einstellungen;
    private TabHost tabhost;
    private SwipeRefreshLayout refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        einstellungen = getApplicationContext().getSharedPreferences("settings", 0);
        con = getApplicationContext();
        if( !loggedIn() ) {
            setContentView(R.layout.activity_login);
            pin = (EditText) findViewById(R.id.pin);
            linkButton  = (Button) findViewById(R.id.linkButton);
            verifierButton = (Button) findViewById(R.id.verifier);
            loginButton = (Button) findViewById(R.id.loginButton);
            linkButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View arg0){linkTwitter();}});
            verifierButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View arg0){verifier();}});
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View arg0){login();}});
        } else {
            login();
        }
    }

    private void linkTwitter() {
        RegisterAccount account = new RegisterAccount(con);
        account.execute("");
    }

    private void verifier() {
        String twitterPin = pin.getText().toString();
        if(!twitterPin.trim().isEmpty()) {
            RegisterAccount account = new RegisterAccount(con);
            account.setButton(loginButton,verifierButton);
            account.execute(twitterPin);
        } else {
            Toast.makeText(con,"PIN eingeben!",Toast.LENGTH_LONG).show();
        }
    }

    private void login(){
        setTheme(R.style.AppTheme);
        setContentView(R.layout.main_layout);
        tabhost = (TabHost)findViewById(R.id.tabHost);
        tabhost.setup();

        // Tab #1
        TabSpec tab1 = tabhost.newTabSpec("timeline");
        tab1.setIndicator("Timeline").setContent(R.id.timeline);
        tabhost.addTab(tab1);

        // Tab #2
        TabSpec tab2 = tabhost.newTabSpec("trends");
        tab2.setIndicator("Trend").setContent(R.id.timeline);
        tabhost.addTab(tab2);

        // Tab #3
        TabSpec tab3 = tabhost.newTabSpec("mention");
        tab3.setIndicator("Mention").setContent(R.id.timeline);
        tabhost.addTab(tab3);

        tabhost.setCurrentTab(0);
        tabhost.getCurrentTab();

        setRefreshListener();


        tabhost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                switch(tabId) {
                    case "timeline":


                        break;

                    case "trends":

                        break;

                    case "mention":

                        break;
                }
            }
        });
    }

    private void setRefreshListener() {
        refresh = (SwipeRefreshLayout) findViewById(R.id.refresh_list);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                switch(tabhost.getCurrentTab()) {
                    case(0):
                        ListView timeline = (ListView) findViewById(R.id.timelinelist);
                        TwitterEngine homeView = new TwitterEngine(getApplicationContext(), timeline);
                        homeView.setRefresh(refresh);
                        homeView.execute(0);
                        break;
                    case(1):
                        ListView trends = (ListView) findViewById(R.id.trendlist);
                        TwitterEngine trendView = new TwitterEngine(getApplicationContext(), trends);
                        trendView.execute(1);
                        break;
                    case(2):
                        ListView mentions = (ListView) findViewById(R.id.mentionlist);
                        TwitterEngine mentionView = new TwitterEngine(getApplicationContext(), mentions);
                        mentionView.execute(2);
                        break;
                }
            }
        });
    }

    private boolean loggedIn() {
        return einstellungen.getBoolean("login", false);
    }
}