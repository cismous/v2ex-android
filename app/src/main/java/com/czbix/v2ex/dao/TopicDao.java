package com.czbix.v2ex.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.czbix.v2ex.model.Topic;
import com.czbix.v2ex.model.db.ViewHistory;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.List;

public class TopicDao extends DaoBase {
    private static final String TABLE_NAME = "topic";

    private static final String KEY_TOPIC_ID = "topic_id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_LAST_READ_TIME = "last_read_time";
    private static final String KEY_LAST_READ_REPLY = "last_read_reply";

    private static final String SQL_GET_REPLY_BY_TOPIC_ID = SQLiteQueryBuilder.buildQueryString(false,
            TABLE_NAME, new String[]{KEY_LAST_READ_REPLY}, KEY_TOPIC_ID + " = ?", null, null, null, null);
    private static final String SQL_GET_HISTORY = SQLiteQueryBuilder.buildQueryString(false,
            TABLE_NAME, new String[]{KEY_TOPIC_ID, KEY_TITLE, KEY_LAST_READ_TIME}, null, null, null,
            KEY_LAST_READ_TIME + " DESC", "20");

    static void createTable(SQLiteDatabase db) {
        Preconditions.checkState(db.inTransaction());

        String sql = "CREATE TABLE " + TABLE_NAME + "(" +
                KEY_TOPIC_ID + " INTEGER PRIMARY KEY," +
                KEY_TITLE + " TEXT NOT NULL," +
                KEY_LAST_READ_REPLY + " INTEGER NOT NULL," +
                KEY_LAST_READ_TIME + " INTEGER NOT NULL" +
                ")";
        db.execSQL(sql);

        sql = String.format("CREATE INDEX %1$s_%2$s ON %1$s(%2$s DESC)", TABLE_NAME, KEY_LAST_READ_TIME);
        db.execSQL(sql);
    }

    static void upgradeTableZero2One(SQLiteDatabase db) {
        String sql = "DROP TABLE " + TABLE_NAME;
        db.execSQL(sql);

        createTable(db);
    }

    public static int getLastReadReply(final int topicId) {
        return execute(new SqlOperation<Integer>() {
            @Override
            public Integer execute(SQLiteDatabase db) {
                Cursor cursor = null;
                try {
                    cursor = db.rawQuery(SQL_GET_REPLY_BY_TOPIC_ID, new String[]{Integer.toString(topicId)});
                    if (!cursor.moveToFirst()) {
                        return -1;
                    }

                    return cursor.getInt(0);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }

    public static void updateLastRead(final Topic topic) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(topic.getTitle()));

        execute(new SqlOperation<Void>() {
            @Override
            public Void execute(SQLiteDatabase db) {
                final ContentValues values = new ContentValues();
                values.put(KEY_TOPIC_ID, topic.getId());
                values.put(KEY_TITLE, topic.getTitle());
                values.put(KEY_LAST_READ_REPLY, topic.getReplyCount());
                values.put(KEY_LAST_READ_TIME, System.currentTimeMillis());

                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

                return null;
            }
        }, true);
    }

    public static List<ViewHistory> getViewHistory() {
        return execute(new SqlOperation<List<ViewHistory>>() {
            @Override
            public List<ViewHistory> execute(SQLiteDatabase db) {
                List<ViewHistory> result = Lists.newArrayList();

                Cursor cursor = null;
                try {
                    cursor = db.rawQuery(SQL_GET_HISTORY, null);

                    while (cursor.moveToNext()) {
                        final int id = cursor.getInt(0);
                        final String title = cursor.getString(1);
                        final long time = cursor.getLong(2);

                        final Topic topic = new Topic.Builder().setId(id).setTitle(title).createTopic();
                        result.add(new ViewHistory(topic, time));
                    }

                    return result;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }
}
