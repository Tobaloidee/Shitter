package org.nuclearfog.twidda.window;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.Registration;

import static android.os.AsyncTask.Status.RUNNING;

/**
 * Login Page
 *
 * @see Registration
 */
public class LoginPage extends AppCompatActivity implements OnClickListener {

    private Registration register;
    private EditText pin;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_login);
        pin = findViewById(R.id.pin);
        findViewById(R.id.linkButton).setOnClickListener(this);
        findViewById(R.id.get).setOnClickListener(this);
        findViewById(R.id.clipboard).setOnClickListener(this);
    }


    @Override
    protected void onDestroy() {
        if (register != null && register.getStatus() == RUNNING)
            register.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }


    @Override
    public void onClick(View v) {
        if (register != null && register.getStatus() == RUNNING)
            register.cancel(true);

        switch (v.getId()) {
            case R.id.linkButton:
                register = new Registration(this);
                register.execute("");
                break;

            case R.id.get:
                String twitterPin = pin.getText().toString();
                if (!twitterPin.trim().isEmpty()) {
                    register = new Registration(this);
                    register.execute(twitterPin);
                } else {
                    Toast.makeText(this, R.string.enter_pin, Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.clipboard:
                ClipboardManager clip = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (clip != null && clip.getPrimaryClip() != null) {
                    String text = clip.getPrimaryClip().getItemAt(0).getText().toString();
                    if (text.matches("\\d++\n?")) {
                        pin.setText(text);
                        Toast.makeText(this, R.string.pin_added, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.false_format, Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }


    public void connect(String link) {
        ConnectivityManager mConnect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnect != null && mConnect.getActiveNetworkInfo() != null) {
            if (mConnect.getActiveNetworkInfo().isConnected()) {
                Intent browser = new Intent(Intent.ACTION_VIEW);
                browser.setData(Uri.parse(link));
                startActivity(browser);
            }
        } else {
            Toast.makeText(this, R.string.connection_failed, Toast.LENGTH_SHORT).show();
        }
    }
}