package org.nuclearfog.twidda.window;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import org.nuclearfog.tag.Tagger.OnTagClickListener;
import org.nuclearfog.twidda.MainActivity;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.OnItemClickListener;
import org.nuclearfog.twidda.adapter.TimelineAdapter;
import org.nuclearfog.twidda.backend.StatusLoader;
import org.nuclearfog.twidda.backend.items.Tweet;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.AsyncTask.Status.RUNNING;
import static org.nuclearfog.twidda.backend.StatusLoader.Mode.ANS;
import static org.nuclearfog.twidda.backend.StatusLoader.Mode.DELETE;
import static org.nuclearfog.twidda.backend.StatusLoader.Mode.FAVORITE;
import static org.nuclearfog.twidda.backend.StatusLoader.Mode.LOAD;
import static org.nuclearfog.twidda.backend.StatusLoader.Mode.RETWEET;

/**
 * Detailed Tweet Activity
 *
 * @see StatusLoader
 */
public class TweetDetail extends AppCompatActivity implements OnClickListener,
        OnItemClickListener, OnRefreshListener, OnTagClickListener {

    public static final int STAT_CHANGED = 1;
    private static final int TWEET = 2;

    private RecyclerView answer_list;
    private TimelineAdapter answerAdapter;
    private StatusLoader mStat;
    private GlobalSettings settings;
    private SwipeRefreshLayout answerReload;
    private ConnectivityManager mConnect;
    private String username = "";
    private boolean isHome;
    private long tweetID = 0;


    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_tweet);

        Bundle param = getIntent().getExtras();
        Uri link = getIntent().getData();

        if(link != null) {
            getTweet(link.getPath());
        }
        else if (param != null) {
            tweetID = param.getLong("tweetID");
            username = param.getString("username");
        }
        else{
            finish();
        }

        Toolbar tool = findViewById(R.id.tweet_toolbar);
        setSupportActionBar(tool);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        settings = GlobalSettings.getInstance(this);
        mConnect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        View root = findViewById(R.id.tweet_layout);
        View retweet = findViewById(R.id.rt_button_detail);
        View favorite = findViewById(R.id.fav_button_detail);
        View txtRt = findViewById(R.id.no_rt_detail);
        View txtFav = findViewById(R.id.no_fav_detail);
        View answer = findViewById(R.id.answer_button);
        TextView tweetTxt = findViewById(R.id.tweet_detailed);
        answerReload = findViewById(R.id.answer_reload);
        answer_list = findViewById(R.id.answer_list);
        answer_list.setLayoutManager(new LinearLayoutManager(this));
        tweetTxt.setMovementMethod(ScrollingMovementMethod.getInstance());

        root.setBackgroundColor(settings.getBackgroundColor());
        answerReload.setProgressBackgroundColorSchemeColor(settings.getHighlightColor());

        favorite.setOnClickListener(this);
        retweet.setOnClickListener(this);
        answerReload.setOnRefreshListener(this);
        txtFav.setOnClickListener(this);
        txtRt.setOnClickListener(this);
        answer.setOnClickListener(this);
    }


    protected void onStart() {
        super.onStart();
        if (mStat == null) {
            answerAdapter = new TimelineAdapter(this);
            answerAdapter.toggleImage(settings.getImageLoad());
            answerAdapter.setColor(settings.getHighlightColor(), settings.getFontColor());
            answer_list.setAdapter(answerAdapter);
            mStat = new StatusLoader(this, LOAD);
            mStat.execute(tweetID);
        }
    }


    @Override
    protected void onStop() {
        if (mStat != null && mStat.getStatus() == RUNNING)
            mStat.cancel(true);
        super.onStop();
    }


    @Override
    protected void onActivityResult(int reqCode, int returnCode, Intent i) {
        if (reqCode == TWEET && returnCode == STAT_CHANGED) {
            mStat = null;
        }
        super.onActivityResult(reqCode, returnCode, i);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.tweet, m);
        return super.onCreateOptionsMenu(m);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu m) {
        if (isHome)
            m.findItem(R.id.delete_tweet).setVisible(true);
        return super.onPrepareOptionsMenu(m);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mStat != null && mStat.getStatus() != RUNNING) {
            switch (item.getItemId()) {
                case R.id.delete_tweet:
                    Builder deleteDialog = new Builder(this);
                    deleteDialog.setMessage(R.string.delete_tweet);
                    deleteDialog.setPositiveButton(R.string.yes_confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mStat != null && mStat.getStatus() == RUNNING)
                                mStat.cancel(true);
                            mStat = new StatusLoader(TweetDetail.this, DELETE);
                            mStat.execute(tweetID);
                        }
                    });
                    deleteDialog.setNegativeButton(R.string.no_confirm, null);
                    deleteDialog.show();
                    break;

                case R.id.tweet_link:
                    if (mConnect.getActiveNetworkInfo() != null && mConnect.getActiveNetworkInfo().isConnected()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        String tweetLink = "https://twitter.com/" + username.substring(1) + "/status/" + tweetID;
                        intent.setData(Uri.parse(tweetLink));
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, R.string.connection_failed, Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.link_copy:
                    String tweetLink = "https://twitter.com/" + username.substring(1) + "/status/" + tweetID;
                    ClipboardManager clip = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData linkClip = ClipData.newPlainText("tweet link", tweetLink);
                    clip.setPrimaryClip(linkClip);
                    Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View v) {
        if (mStat != null && mStat.getStatus() != RUNNING) {
            switch (v.getId()) {
                case R.id.rt_button_detail:
                    if (mStat != null && mStat.getStatus() == RUNNING)
                        mStat.cancel(true);
                    mStat = new StatusLoader(this, RETWEET);
                    mStat.execute(tweetID);
                    Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show();
                    break;

                case R.id.fav_button_detail:
                    if (mStat != null && mStat.getStatus() == RUNNING)
                        mStat.cancel(true);
                    mStat = new StatusLoader(this, FAVORITE);
                    mStat.execute(tweetID);
                    Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show();
                    break;

                case R.id.no_rt_detail:
                    Intent retweet = new Intent(this, UserDetail.class);
                    retweet.putExtra("ID", tweetID);
                    retweet.putExtra("mode", 2);
                    startActivity(retweet);
                    break;

                case R.id.no_fav_detail:
                    Intent favorit = new Intent(this, UserDetail.class);
                    favorit.putExtra("ID", tweetID);
                    favorit.putExtra("mode", 3);
                    startActivity(favorit);
                    break;

                case R.id.answer_button:
                    Intent tweet = new Intent(this, TweetPopup.class);
                    tweet.putExtra("TweetID", tweetID);
                    tweet.putExtra("Addition", username);
                    startActivityForResult(tweet, TWEET);
                    break;
            }
        }
    }


    @Override
    public void onClick(String text) {
        Intent intent = new Intent(this, SearchPage.class);
        intent.putExtra("search", text);
        startActivity(intent);
    }


    @Override
    public void onItemClick(RecyclerView rv, int position) {
        if (!answerReload.isRefreshing()) {
            Tweet tweet = answerAdapter.getData(position);
            Intent intent = new Intent(this, TweetDetail.class);
            intent.putExtra("tweetID", tweet.getId());
            intent.putExtra("userID", tweet.getUser().getId());
            intent.putExtra("username", tweet.getUser().getScreenname());
            startActivityForResult(intent, TWEET);
        }
    }


    @Override
    public void onRefresh() {
        mStat = new StatusLoader(this, ANS);
        mStat.execute(tweetID);
    }


    public void imageClick(String mediaLinks[]) {
        Intent image = new Intent(this, ImageDetail.class);
        image.putExtra("link", mediaLinks);
        startActivity(image);
    }


    public void enableDelete() {
        isHome = true;
        invalidateOptionsMenu();
    }


    private void getTweet(String link) {
        if (link != null) {
            Pattern linkPattern = Pattern.compile("/@?[\\w_]+/status/\\d{1,20}");
            Matcher linkMatch = linkPattern.matcher(link);
            if (linkMatch.matches()) {
                Pattern idPattern = Pattern.compile("\\d{1,20}");
                Pattern usrPattern = Pattern.compile("/@?[\\w_]+/");

                Matcher matcher = idPattern.matcher(link);
                if (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();
                    tweetID = Long.parseLong(link.substring(start, end));
                }

                matcher = usrPattern.matcher(link);
                if (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();
                    username = link.substring(start + 1, end - 1);
                }
            }
            else {
                Toast.makeText(this,R.string.tweet_not_found,Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }
}