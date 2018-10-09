package org.nuclearfog.twidda.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import org.nuclearfog.twidda.backend.listitems.Message;
import org.nuclearfog.twidda.backend.listitems.Trend;
import org.nuclearfog.twidda.backend.listitems.Tweet;
import org.nuclearfog.twidda.backend.listitems.TwitterUser;

import java.util.ArrayList;
import java.util.List;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;

public class DatabaseAdapter {

    private final int LIMIT = 50;
    private final int favoritedMask = 1;
    private final int retweetedMask = 1 << 1;
    private final int homeMask = 1 << 2;
    private final int mentionMask = 1 << 3;
    private final int userTweetMask = 1 << 4;
    private final int replyMask = 1 << 5;

    private final int verifiedMask = 1;
    private final int lockedMask = 1 << 1;

    private AppDatabase dataHelper;
    private long homeId;

    public DatabaseAdapter(Context context) {
        dataHelper = AppDatabase.getInstance(context);
        GlobalSettings settings = GlobalSettings.getInstance(context);
        homeId = settings.getUserId();
    }

    /**
     * Nutzer speichern
     *
     * @param user Nutzer Information
     */
    public void storeUser(TwitterUser user) {
        SQLiteDatabase db = getDbWrite();
        db.beginTransaction();
        storeUser(user, db);
        commit(db);
    }

    /**
     * Home Timeline speichern
     *
     * @param home Tweet Liste
     */
    public void storeHomeTimeline(List<Tweet> home) {
        SQLiteDatabase db = getDbWrite();
        db.beginTransaction();
        for (Tweet tweet : home) {
            storeStatus(tweet, homeMask, db);
        }
        commit(db);
    }

    /**
     * Erwähnungen speichern
     *
     * @param mentions Tweet Liste
     */
    public void storeMentions(List<Tweet> mentions) {
        SQLiteDatabase db = getDbWrite();
        db.beginTransaction();
        for (Tweet tweet : mentions) {
            storeStatus(tweet, mentionMask, db);
        }
        commit(db);
    }

    /**
     * Nutzer Tweets speichern
     *
     * @param stats Tweet Liste
     */
    public void storeUserTweets(List<Tweet> stats) {
        SQLiteDatabase db = getDbWrite();
        db.beginTransaction();
        for (Tweet tweet : stats) {
            storeStatus(tweet, userTweetMask, db);
        }
        commit(db);
    }

    /**
     * Nutzer Favoriten Speichern
     *
     * @param fav     Tweet Liste
     * @param ownerId User ID
     */
    public void storeUserFavs(List<Tweet> fav, long ownerId) {
        SQLiteDatabase db = getDbWrite();
        db.beginTransaction();
        for (Tweet tweet : fav) {
            storeStatus(tweet, 0, db);
            ContentValues favTable = new ContentValues();
            favTable.put("tweetID", tweet.tweetID);
            favTable.put("ownerID", ownerId);
            db.insertWithOnConflict("favorit", null, favTable, CONFLICT_IGNORE);
        }
        commit(db);
    }

    /**
     * Tweet Antworten speicher
     *
     * @param replies Tweet Antworten Liste
     */
    public void storeReplies(final List<Tweet> replies) {
        SQLiteDatabase db = getDbWrite();
        db.beginTransaction();
        for (Tweet tweet : replies) {
            storeStatus(tweet, replyMask, db);
        }
        commit(db);
    }

    /**
     * Speichere Twitter Trends
     *
     * @param trends List of Trends
     * @param woeId  Yahoo World ID
     */
    public void store(final List<Trend> trends, int woeId) {
        SQLiteDatabase db = getDbWrite();
        String query = "DELETE FROM trend WHERE woeID=" + woeId;
        db.beginTransaction();
        db.execSQL(query);
        for (Trend trend : trends) {
            storeTrends(trend, woeId, db);
        }
        commit(db);
    }

    /**
     * Speichere Tweet in Favoriten Tabelle
     *
     * @param tweetID Tweet ID
     */
    public void storeFavorite(long tweetID) {
        SQLiteDatabase db = getDbWrite();

        ContentValues favTable = new ContentValues();
        ContentValues status = new ContentValues();

        int register = getStatRegister(db, tweetID);
        register |= favoritedMask;

        favTable.put("tweetID", tweetID);
        favTable.put("ownerID", homeId);
        status.put("statusregister", register);

        db.beginTransaction();
        db.insertWithOnConflict("favorit", null, favTable, CONFLICT_IGNORE);
        db.update("tweet", status, "tweet.tweetID=" + tweetID, null);
        commit(db);
    }

    /**
     * speicher direktnachrichten
     *
     * @param messages Direktnachrichten liste
     */
    public void storeMessage(List<Message> messages) {
        SQLiteDatabase db = getDbWrite();
        db.beginTransaction();
        for (Message message : messages) {
            storeMessage(message, db);
        }
        commit(db);
    }

    /**
     * Lade Nutzer Information
     *
     * @param userId Nutzer ID
     * @return Nutzer Informationen oder NULL falls nicht vorhanden
     */
    @Nullable
    public TwitterUser getUser(long userId) {
        SQLiteDatabase db = dataHelper.getReadableDatabase();
        TwitterUser result = getUser(userId, db);
        db.close();
        return result;
    }

    /**
     * Lade Home Timeline
     *
     * @return Tweet Liste
     */
    public List<Tweet> getHomeTimeline() {
        SQLiteDatabase db = dataHelper.getReadableDatabase();
        List<Tweet> tweetList = new ArrayList<>();
        String SQL_GET_HOME = "SELECT * FROM tweet " +
                "INNER JOIN user ON tweet.userID=user.userID " +
                "WHERE statusregister&" + homeMask + ">0 " +
                "ORDER BY tweetID DESC LIMIT " + LIMIT;
        Cursor cursor = db.rawQuery(SQL_GET_HOME, null);
        if (cursor.moveToFirst()) {
            do {
                Tweet tweet = getStatus(cursor);
                tweetList.add(tweet);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tweetList;
    }

    /**
     * Erwähnungen laden
     *
     * @return Tweet Liste
     */
    public List<Tweet> getMentions() {
        SQLiteDatabase db = dataHelper.getReadableDatabase();
        List<Tweet> tweetList = new ArrayList<>();
        String SQL_GET_HOME = "SELECT * FROM tweet " +
                "INNER JOIN user ON tweet.userID=user.userID " +
                "WHERE statusregister&" + mentionMask + ">0 " +
                "ORDER BY tweetID DESC LIMIT " + LIMIT;
        Cursor cursor = db.rawQuery(SQL_GET_HOME, null);
        if (cursor.moveToFirst()) {
            do {
                Tweet tweet = getStatus(cursor);
                tweetList.add(tweet);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tweetList;
    }

    /**
     * Tweet Liste eines Nutzers
     *
     * @param userID Nutzer ID
     * @return Tweet Liste des Users
     */
    public List<Tweet> getUserTweets(long userID) {
        SQLiteDatabase db = dataHelper.getReadableDatabase();
        List<Tweet> tweetList = new ArrayList<>();
        String SQL_GET_HOME = "SELECT * FROM tweet " +
                "INNER JOIN user ON tweet.userID = user.userID " +
                "WHERE statusregister&" + userTweetMask + ">0 " +
                "AND user.userID =" + userID + " ORDER BY tweetID DESC LIMIT " + LIMIT;

        Cursor cursor = db.rawQuery(SQL_GET_HOME, null);

        if (cursor.moveToFirst()) {
            do {
                Tweet tweet = getStatus(cursor);
                tweetList.add(tweet);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tweetList;
    }

    /**
     * Lade Favorisierte Tweets eines Nutzers
     *
     * @param userID Nutzer ID
     * @return Favoriten des Nutzers
     */
    public List<Tweet> getUserFavs(long userID) {
        SQLiteDatabase db = dataHelper.getReadableDatabase();
        List<Tweet> tweetList = new ArrayList<>();
        String SQL_GET_HOME = "SELECT * FROM tweet " +
                "INNER JOIN favorit on tweet.tweetID = favorit.tweetID " +
                "INNER JOIN user ON tweet.userID = user.userID " +
                "WHERE favorit.ownerID =" + userID + " ORDER BY tweetID DESC LIMIT " + LIMIT;
        Cursor cursor = db.rawQuery(SQL_GET_HOME, null);
        if (cursor.moveToFirst()) {
            do {
                Tweet tweet = getStatus(cursor);
                tweetList.add(tweet);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tweetList;
    }

    /**
     * Lade Tweet
     *
     * @param tweetId Tweet ID
     * @return Gefundener Tweet oder NULL falls nicht vorhanden
     */
    @Nullable
    public Tweet getStatus(long tweetId) {
        SQLiteDatabase db = dataHelper.getReadableDatabase();
        Tweet result = null;
        String query = "SELECT * FROM tweet " +
                "INNER JOIN user ON user.userID = tweet.userID " +
                "WHERE tweet.tweetID==" + tweetId + " LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst())
            result = getStatus(cursor);
        cursor.close();
        return result;
    }

    /**
     * Lade Antworten
     *
     * @param tweetId Tweet ID
     * @return Antworten zur Tweet ID
     */
    public List<Tweet> getAnswers(long tweetId) {
        SQLiteDatabase db = dataHelper.getReadableDatabase();
        List<Tweet> tweetList = new ArrayList<>();
        String SQL_GET_HOME = "SELECT * FROM tweet " +
                "INNER JOIN user ON tweet.userID = user.userID " +
                "WHERE tweet.replyID=" + tweetId + " AND statusregister&" + replyMask + ">0 " +
                "ORDER BY tweetID DESC LIMIT " + LIMIT;
        Cursor cursor = db.rawQuery(SQL_GET_HOME, null);
        if (cursor.moveToFirst()) {
            do {
                Tweet tweet = getStatus(cursor);
                tweetList.add(tweet);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tweetList;
    }

    /**
     * Aktualisiere Status
     *
     * @param tweet Tweet
     */
    public void updateStatus(Tweet tweet) {
        SQLiteDatabase db = getDbWrite();
        ContentValues status = new ContentValues();
        int register = getStatRegister(db, tweet.tweetID);
        if (tweet.retweeted)
            register |= retweetedMask;
        else
            register &= ~retweetedMask;

        if (tweet.favorized)
            register |= favoritedMask;
        else
            register &= ~favoritedMask;
        status.put("retweet", tweet.retweet);
        status.put("favorite", tweet.favorit);
        status.put("statusregister", register);

        db.beginTransaction();
        db.update("tweet", status, "tweet.tweetID=" + tweet.tweetID, null);
        commit(db);
    }

    /**
     * Lösche Tweet
     *
     * @param id Tweet ID
     */
    public void removeStatus(long id) {
        SQLiteDatabase db = getDbWrite();
        db.beginTransaction();
        db.delete("tweet", "tweetID=" + id, null);
        db.delete("favorit", "tweetID=" + id + " AND ownerID=" + homeId, null);
        commit(db);
    }

    /**
     * Entferne Tweet aus der Favoriten Tabelle
     *
     * @param tweetId ID des tweets
     */
    public void removeFavorite(long tweetId) {
        SQLiteDatabase db = getDbWrite();
        int register = getStatRegister(db, tweetId);
        register &= ~favoritedMask;
        ContentValues status = new ContentValues();
        status.put("statusregister", register);

        db.beginTransaction();
        db.delete("favorit", "tweetID=" + tweetId + " AND ownerID=" + homeId, null);
        db.update("tweet", status, "tweet.tweetID=" + tweetId, null);
        commit(db);
    }

    /**
     * Delete Direct Message
     *
     * @param id Direct Message ID
     */
    public void deleteDm(long id) {
        SQLiteDatabase db = getDbWrite();
        db.beginTransaction();
        db.delete("message", "messageID=" + id, null);
        commit(db);
    }

    /**
     * Load trend List
     *
     * @param woeId Yahoo World ID
     * @return list of trends
     */
    public List<Trend> getTrends(int woeId) {
        SQLiteDatabase db = dataHelper.getReadableDatabase();
        List<Trend> trends = new ArrayList<>();
        String query = "SELECT * FROM trend WHERE woeID=" + woeId + " ORDER BY trendpos ASC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int index = cursor.getColumnIndex("trendpos");
                int position = cursor.getInt(index);
                index = cursor.getColumnIndex("trendname");
                String name = cursor.getString(index);
                index = cursor.getColumnIndex("trendlink");
                String link = cursor.getString(index);
                trends.add(new Trend(position, name, link));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return trends;
    }


    /**
     * Direkt nachrichten laden
     *
     * @return Liste Direktnachrichten
     */
    public List<Message> getMessages() {
        List<Message> result = new ArrayList<>();
        SQLiteDatabase db = dataHelper.getReadableDatabase();
        String query = "SELECT * FROM message ORDER BY messageID DESC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int index = cursor.getColumnIndex("senderID");
                long senderID = cursor.getLong(index);
                index = cursor.getColumnIndex("receiverID");
                long receiverID = cursor.getLong(index);
                index = cursor.getColumnIndex("message");
                String message = cursor.getString(index);
                index = cursor.getColumnIndex("time");
                long time = cursor.getLong(index);
                index = cursor.getColumnIndex("messageID");
                long messageId = cursor.getLong(index);

                TwitterUser sender = getUser(senderID, db);
                TwitterUser receiver = getUser(receiverID, db);

                result.add(new Message(messageId, sender, receiver, time, message));

            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return result;
    }

    /**
     * Suche Tweet in Datenbank
     *
     * @param id Tweet ID
     * @return True falls gefunden, ansonsten False
     */
    public boolean containStatus(long id) {
        SQLiteDatabase db = dataHelper.getReadableDatabase();
        String query = "SELECT tweetID FROM tweet WHERE tweetID=" + id + " LIMIT 1;";
        Cursor c = db.rawQuery(query, null);
        boolean result = c.moveToFirst();
        c.close();
        db.close();
        return result;
    }


    private Tweet getStatus(Cursor cursor) {
        int index;
        index = cursor.getColumnIndex("time");
        long time = cursor.getLong(index);
        index = cursor.getColumnIndex("tweet");
        String tweettext = cursor.getString(index);
        index = cursor.getColumnIndex("retweet");
        int retweet = cursor.getInt(index);
        index = cursor.getColumnIndex("favorite");
        int favorit = cursor.getInt(index);
        index = cursor.getColumnIndex("tweetID");
        long tweetId = cursor.getLong(index);
        index = cursor.getColumnIndex("retweetID");
        long retweetId = cursor.getLong(index);
        index = cursor.getColumnIndex("replyname");
        String replyname = cursor.getString(index);
        index = cursor.getColumnIndex("replyID");
        long replyStatusId = cursor.getLong(index);
        index = cursor.getColumnIndex("retweeterID");
        long retweeterId = cursor.getLong(index);
        index = cursor.getColumnIndex("source");
        String source = cursor.getString(index);
        index = cursor.getColumnIndex("media");
        String medialinks = cursor.getString(index);
        index = cursor.getColumnIndex("replyUserID");
        long replyUserId = cursor.getLong(index);
        index = cursor.getColumnIndex("statusregister");
        int statusregister = cursor.getInt(index);
        boolean favorited = (statusregister & favoritedMask) > 0;
        boolean retweeted = (statusregister & retweetedMask) > 0;

        String[] medias = parseMedia(medialinks);

        TwitterUser user = getUser(cursor);
        Tweet embeddedTweet = null;
        if (retweetId > 1)
            embeddedTweet = getStatus(retweetId);
        return new Tweet(tweetId, retweet, favorit, user, tweettext, time, replyname, replyUserId, medias,
                source, replyStatusId, embeddedTweet, retweeterId, retweeted, favorited);
    }

    private TwitterUser getUser(long userId, SQLiteDatabase db) {
        TwitterUser user = null;
        String query = "SELECT * FROM user WHERE userID=" + userId + " LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst())
            user = getUser(cursor);
        cursor.close();
        return user;
    }

    private TwitterUser getUser(Cursor cursor) {
        int index = cursor.getColumnIndex("userID");
        long userId = cursor.getLong(index);
        index = cursor.getColumnIndex("username");
        String username = cursor.getString(index);
        index = cursor.getColumnIndex("scrname");
        String screenname = cursor.getString(index);
        index = cursor.getColumnIndex("userregister");
        int userRegister = cursor.getInt(index);
        index = cursor.getColumnIndex("pbLink");
        String profileImg = cursor.getString(index);
        index = cursor.getColumnIndex("bio");
        String bio = cursor.getString(index);
        index = cursor.getColumnIndex("link");
        String link = cursor.getString(index);
        index = cursor.getColumnIndex("location");
        String location = cursor.getString(index);
        index = cursor.getColumnIndex("banner");
        String banner = cursor.getString(index);
        index = cursor.getColumnIndex("createdAt");
        long createdAt = cursor.getLong(index);
        index = cursor.getColumnIndex("following");
        int following = cursor.getInt(index);
        index = cursor.getColumnIndex("follower");
        int follower = cursor.getInt(index);

        boolean isVerified = (userRegister & verifiedMask) > 0;
        boolean isLocked = (userRegister & lockedMask) > 0;
        return new TwitterUser(userId, username, screenname, profileImg, bio,
                location, isVerified, isLocked, link, banner, createdAt, following, follower);
    }


    private void storeUser(TwitterUser user, SQLiteDatabase db) {
        ContentValues userColumn = new ContentValues();
        int userRegister = 0;
        if (user.isVerified)
            userRegister |= verifiedMask;
        if (user.isLocked)
            userRegister |= lockedMask;
        userColumn.put("userID", user.userID);
        userColumn.put("username", user.username);
        userColumn.put("scrname", user.screenname.substring(1));
        userColumn.put("pbLink", user.profileImg);
        userColumn.put("userregister", userRegister);
        userColumn.put("bio", user.bio);
        userColumn.put("link", user.link);
        userColumn.put("location", user.location);
        userColumn.put("banner", user.bannerImg);
        userColumn.put("createdAt", user.created);
        userColumn.put("following", user.following);
        userColumn.put("follower", user.follower);
        db.insertWithOnConflict("user", null, userColumn, CONFLICT_REPLACE);
    }


    private void storeStatus(Tweet tweet, int newStatusRegister, SQLiteDatabase db) {
        ContentValues status = new ContentValues();
        TwitterUser user = tweet.user;
        Tweet rtStat = tweet.embedded;
        long rtId = 1L;

        if (rtStat != null) {
            storeStatus(rtStat, 0, db);
            rtId = rtStat.tweetID;
        }

        ContentValues userColumn = new ContentValues();
        int userRegister = 0;
        if (user.isVerified)
            userRegister |= verifiedMask;
        if (user.isLocked)
            userRegister |= lockedMask;

        userColumn.put("userID", user.userID);
        userColumn.put("username", user.username);
        userColumn.put("scrname", user.screenname.substring(1));
        userColumn.put("pbLink", user.profileImg);
        userColumn.put("userregister", userRegister);
        userColumn.put("bio", user.bio);
        userColumn.put("link", user.link);
        userColumn.put("location", user.location);
        userColumn.put("banner", user.bannerImg);
        userColumn.put("createdAt", user.created);
        userColumn.put("following", user.following);
        userColumn.put("follower", user.follower);

        status.put("tweetID", tweet.tweetID);
        status.put("userID", user.userID);
        status.put("time", tweet.time);
        status.put("tweet", tweet.tweet);
        status.put("retweetID", rtId);
        status.put("source", tweet.source);
        status.put("replyID", tweet.replyID);
        status.put("replyname", tweet.replyName);
        status.put("retweet", tweet.retweet);
        status.put("favorite", tweet.favorit);
        status.put("retweeterID", tweet.retweetId);
        status.put("replyUserID", tweet.replyUserId);
        String[] mediaLinks = tweet.media;
        StringBuilder media = new StringBuilder();
        for (String link : mediaLinks) {
            media.append(link);
            media.append(";");
        }
        status.put("media", media.toString());
        int statusRegister = getStatRegister(db, tweet.tweetID);
        statusRegister |= newStatusRegister;
        if (tweet.favorized)
            statusRegister |= favoritedMask;
        else
            statusRegister &= ~favoritedMask;
        if (tweet.retweeted)
            statusRegister |= retweetedMask;
        else
            statusRegister &= ~retweetedMask;
        status.put("statusregister", statusRegister);

        db.insertWithOnConflict("user", null, userColumn, CONFLICT_IGNORE);
        db.insertWithOnConflict("tweet", null, status, CONFLICT_REPLACE);
    }


    private void storeMessage(Message message, SQLiteDatabase db) {
        ContentValues messageColumn = new ContentValues();
        messageColumn.put("messageID", message.messageId);
        messageColumn.put("time", message.time);
        messageColumn.put("senderID", message.sender.userID);
        messageColumn.put("receiverID", message.receiver.userID);
        messageColumn.put("message", message.message);
        storeUser(message.sender, db);
        storeUser(message.receiver, db);
        db.insertWithOnConflict("message", null, messageColumn, CONFLICT_IGNORE);
    }


    private void storeTrends(Trend trend, int woeId, SQLiteDatabase db) {
        ContentValues trendColumn = new ContentValues();
        trendColumn.put("woeID", woeId);
        trendColumn.put("trendpos", trend.position);
        trendColumn.put("trendname", trend.trend);
        trendColumn.put("trendlink", trend.link);
        db.insertWithOnConflict("trend", null, trendColumn, CONFLICT_REPLACE);
    }


    private int getStatRegister(SQLiteDatabase db, long tweetID) {
        String query = "SELECT statusregister FROM tweet WHERE tweetID=" + tweetID + " LIMIT 1;";
        Cursor c = db.rawQuery(query, null);
        int result = 0;
        if (c.moveToFirst()) {
            int pos = c.getColumnIndex("statusregister");
            result = c.getInt(pos);
        }
        c.close();
        return result;
    }


    private synchronized SQLiteDatabase getDbWrite() {
        return dataHelper.getWritableDatabase();
    }


    private void commit(SQLiteDatabase db) {
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }


    private String[] parseMedia(String media) {
        int index;
        List<String> links = new ArrayList<>();
        do {
            index = media.indexOf(';');
            if (index > 0 && index < media.length()) {
                links.add(media.substring(0, index));
                media = media.substring(index + 1);
            }
        } while (index > 0);
        String[] result = new String[links.size()];
        return links.toArray(result);
    }
}