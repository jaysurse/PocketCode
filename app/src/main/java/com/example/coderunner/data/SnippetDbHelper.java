package com.example.coderunner.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class SnippetDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "pocketcode_sql.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE = "snippets";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_LANG = "languageId";
    public static final String COL_CODE = "code";
    public static final String COL_LAST = "lastEdited";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_TITLE + " TEXT,"
            + COL_LANG + " TEXT,"
            + COL_CODE + " TEXT,"
            + COL_LAST + " INTEGER"
            + ");";

    public SnippetDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For now, destructive upgrade. Add migrations for production.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public long insertSnippet(Snippet s) {
        SQLiteDatabase db = getWritableDatabase();
        long id = -1;
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_TITLE, s.title);
            cv.put(COL_LANG, s.languageId);
            cv.put(COL_CODE, s.code);
            cv.put(COL_LAST, s.lastEdited);
            id = db.insert(TABLE, null, cv);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return id;
    }

    public List<Snippet> getAllSnippets() {
        SQLiteDatabase db = getReadableDatabase();
        List<Snippet> out = new ArrayList<>();
        Cursor c = null;
        try {
            c = db.query(TABLE, null, null, null, null, null, COL_LAST + " DESC");
            if (c != null && c.moveToFirst()) {
                do {
                    Snippet s = new Snippet();
                    s.id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
                    s.title = c.getString(c.getColumnIndexOrThrow(COL_TITLE));
                    s.languageId = c.getString(c.getColumnIndexOrThrow(COL_LANG));
                    s.code = c.getString(c.getColumnIndexOrThrow(COL_CODE));
                    s.lastEdited = c.getLong(c.getColumnIndexOrThrow(COL_LAST));
                    out.add(s);
                } while (c.moveToNext());
            }
        } finally {
            if (c != null)
                c.close();
        }
        return out;
    }

    public Snippet findById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE, null, COL_ID + " = ?", new String[] { String.valueOf(id) }, null, null, null);
            if (c != null && c.moveToFirst()) {
                Snippet s = new Snippet();
                s.id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
                s.title = c.getString(c.getColumnIndexOrThrow(COL_TITLE));
                s.languageId = c.getString(c.getColumnIndexOrThrow(COL_LANG));
                s.code = c.getString(c.getColumnIndexOrThrow(COL_CODE));
                s.lastEdited = c.getLong(c.getColumnIndexOrThrow(COL_LAST));
                return s;
            }
        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }

    public void updateSnippet(Snippet s) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_TITLE, s.title);
            cv.put(COL_LANG, s.languageId);
            cv.put(COL_CODE, s.code);
            cv.put(COL_LAST, s.lastEdited);
            db.update(TABLE, cv, COL_ID + " = ?", new String[] { String.valueOf(s.id) });
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteSnippet(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE, COL_ID + " = ?", new String[] { String.valueOf(id) });
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
