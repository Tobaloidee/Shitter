package org.nuclearfog.twidda.window;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import static android.content.DialogInterface.*;

import org.nuclearfog.twidda.backend.ShowStatus;
import org.nuclearfog.twidda.database.TweetDatabase;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.viewadapter.TimelineAdapter;

/**
 * Detailed Tweet Window
 * @see ShowStatus
 */
public class TweetDetail extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemClickListener, DialogInterface.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private ListView answer_list;
    private long tweetID;
    private long userID;
    private ShowStatus mStat, mReply;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.tweet_detail);
        tweetID = getIntent().getExtras().getLong("tweetID");
        userID = getIntent().getExtras().getLong("userID");
        SharedPreferences settings = getApplicationContext().getSharedPreferences("settings", 0);
        boolean home = userID == settings.getLong("userID", -1);

        answer_list = (ListView) findViewById(R.id.answer_list);
        Button answer = (Button) findViewById(R.id.answer_button);
        Button retweet = (Button) findViewById(R.id.rt_button_detail);
        Button favorite = (Button) findViewById(R.id.fav_button_detail);
        Button delete = (Button) findViewById(R.id.delete);
        ImageView pb = (ImageView) findViewById(R.id.profileimage_detail);
        SwipeRefreshLayout answerReload = (SwipeRefreshLayout) findViewById(R.id.answer_reload);

        TextView txtRt = (TextView) findViewById(R.id.no_rt_detail);
        TextView txtFav = (TextView) findViewById(R.id.no_fav_detail);
        if(home) {
            delete.setVisibility(View.VISIBLE);
        }

        answer_list.setOnItemClickListener(this);
        favorite.setOnClickListener(this);
        retweet.setOnClickListener(this);
        answerReload.setOnRefreshListener(this);
        answer.setOnClickListener(this);
        txtFav.setOnClickListener(this);
        txtRt.setOnClickListener(this);
        delete.setOnClickListener(this);
        setContent();
    }

    @Override
    protected void onDestroy() {
        mStat.cancel(true);
        mReply.cancel(true);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        Bundle bundle = new Bundle();
        ShowStatus mStat = new ShowStatus(this);
        switch(v.getId()) {
            case R.id.answer_button:
                intent = new Intent(getApplicationContext(), TweetPopup.class);
                bundle.putLong("TweetID", tweetID);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.rt_button_detail:
                mStat.execute(tweetID, ShowStatus.RETWEET);
                break;
            case R.id.fav_button_detail:
                mStat.execute(tweetID, ShowStatus.FAVORITE);
                break;
            case R.id.no_rt_detail:
                intent = new Intent(getApplicationContext(), UserDetail.class);
                bundle.putLong("userID",userID);
                bundle.putLong("tweetID",tweetID);
                bundle.putLong("mode",2L);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.no_fav_detail:
                intent = new Intent(getApplicationContext(), UserDetail.class);
                bundle.putLong("userID",userID);
                bundle.putLong("tweetID",tweetID);
                bundle.putLong("mode",3L);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.delete:
                AlertDialog.Builder alerta = new AlertDialog.Builder(this);
                alerta.setMessage("Tweet löschen?");
                alerta.setPositiveButton(R.string.yes_confirm, this);
                alerta.setNegativeButton(R.string.no_confirm, this);
                alerta.show();
                break;
        }
    }

    @Override
    public void onClick(DialogInterface d, int id) {
        switch(id){
            case BUTTON_NEGATIVE:
                break;
            case BUTTON_POSITIVE:
                new ShowStatus(this).execute(tweetID,ShowStatus.DELETE);
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TimelineAdapter tlAdp = (TimelineAdapter) answer_list.getAdapter();
        TweetDatabase twDB = tlAdp.getData();
        long userID = twDB.getUserID(position);
        long tweetID = twDB.getTweetId(position);
        Intent intent = new Intent(getApplicationContext(), TweetDetail.class);
        Bundle bundle = new Bundle();
        bundle.putLong("userID",userID);
        bundle.putLong("tweetID",tweetID);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public void onRefresh() {
        mReply = new ShowStatus(this);
        mReply.execute(tweetID, ShowStatus.LOAD_REPLY);
    }

    private void setContent() {
        ColorPreferences mColor = ColorPreferences.getInstance(getApplicationContext());
        int backgroundColor = mColor.getColor(ColorPreferences.BACKGROUND);
        int fontColor = mColor.getColor(ColorPreferences.FONT_COLOR);
        CollapsingToolbarLayout cLayout = (CollapsingToolbarLayout) findViewById(R.id.tweet_detail);
        LinearLayout tweetaction = (LinearLayout) findViewById(R.id.tweetbar);
        TextView txtTw = (TextView) findViewById(R.id.tweet_detailed);
        cLayout.setBackgroundColor(backgroundColor);
        tweetaction.setBackgroundColor(backgroundColor);
        answer_list.setBackgroundColor(backgroundColor);
        txtTw.setTextColor(fontColor);

        mStat = new ShowStatus(this);
        mReply = new ShowStatus(this);
        mStat.execute(tweetID, ShowStatus.LOAD_TWEET);
        mReply.execute(tweetID, ShowStatus.LOAD_REPLY);
    }
}