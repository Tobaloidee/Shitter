package org.nuclearfog.twidda.window;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.MessageAdapter;
import org.nuclearfog.twidda.adapter.MessageAdapter.OnItemSelected;
import org.nuclearfog.twidda.backend.MessageLoader;
import org.nuclearfog.twidda.backend.items.Message;
import org.nuclearfog.twidda.database.GlobalSettings;

import static android.os.AsyncTask.Status.RUNNING;
import static org.nuclearfog.twidda.backend.MessageLoader.Mode.DELETE;
import static org.nuclearfog.twidda.backend.MessageLoader.Mode.LOAD;

/**
 * Direct Message page
 *
 * @see MessageLoader
 */
public class DirectMessage extends AppCompatActivity implements OnRefreshListener, OnItemSelected {

    private MessageLoader mLoader;
    private MessageAdapter mAdapter;
    private SwipeRefreshLayout messageRefresh;
    private GlobalSettings settings;
    private RecyclerView dmList;


    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_dm);

        Toolbar tool = findViewById(R.id.dm_toolbar);
        setSupportActionBar(tool);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.directmessage);
        messageRefresh = findViewById(R.id.dm_reload);
        dmList = findViewById(R.id.messagelist);
        View root = findViewById(R.id.dm_layout);

        settings = GlobalSettings.getInstance(this);
        root.setBackgroundColor(settings.getBackgroundColor());
        messageRefresh.setProgressBackgroundColorSchemeColor(settings.getHighlightColor());

        dmList.setLayoutManager(new LinearLayoutManager(this));
        dmList.setHasFixedSize(true);
        messageRefresh.setOnRefreshListener(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mLoader == null) {
            mAdapter = new MessageAdapter(this);
            mAdapter.setColor(settings.getFontColor(), settings.getHighlightColor());
            mAdapter.setImageLoad(settings.getImageLoad());
            dmList.setAdapter(mAdapter);
            mLoader = new MessageLoader(this, LOAD);
            mLoader.execute();
        }
    }


    @Override
    protected void onStop() {
        if (mLoader != null && mLoader.getStatus() == RUNNING) {
            mLoader.cancel(true);
        }
        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.message, m);
        return super.onCreateOptionsMenu(m);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mLoader != null && mLoader.getStatus() != RUNNING) {
            switch (item.getItemId()) {
                case R.id.message:
                    Intent sendDm = new Intent(this, MessagePopup.class);
                    startActivity(sendDm);
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onAnswer(int position) {
        if (mAdapter != null && !messageRefresh.isRefreshing()) {
            Message message = mAdapter.getData(position);
            Intent sendDm = new Intent(this, MessagePopup.class);
            sendDm.putExtra("username", message.getSender().getScreenname());
            startActivity(sendDm);
        }
    }


    @Override
    public void onDelete(int position) {
        if (mAdapter != null && !messageRefresh.isRefreshing()) {
            Message message = mAdapter.getData(position);
            final long messageId = message.getId();
            new Builder(this).setMessage(R.string.confirm_delete_dm)
                    .setNegativeButton(R.string.no_confirm, null)
                    .setPositiveButton(R.string.yes_confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mLoader = new MessageLoader(DirectMessage.this, DELETE);
                            mLoader.execute(messageId);
                        }
                    }).show();
        }
    }


    @Override
    public void onProfileClick(int index) {
        if (mAdapter != null && !messageRefresh.isRefreshing()) {
            Message message = mAdapter.getData(index);
            long userId = message.getSender().getId();
            String username = message.getSender().getScreenname();
            Intent user = new Intent(this, UserProfile.class);
            user.putExtra("userID", userId);
            user.putExtra("username", username);
            startActivity(user);
        }
    }


    @Override
    public void onClick(String tag) {
        Intent intent = new Intent(this, SearchPage.class);
        intent.putExtra("search", tag);
        startActivity(intent);
    }


    @Override
    public void onRefresh() {
        mLoader = new MessageLoader(this, LOAD);
        mLoader.execute();
    }
}