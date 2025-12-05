package com.example.android.notepad;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.widget.RemoteViews;

public class NoteWidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences("notepad_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        for (int id : appWidgetIds) e.remove("widget_note_" + id);
        e.apply();
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences("notepad_prefs", Context.MODE_PRIVATE);
        long noteId = prefs.getLong("widget_note_" + appWidgetId, -1);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.note_widget);
        String title = "请选择笔记";
        String body = "";
        Intent launch;
        if (noteId != -1) {
            ContentResolver cr = context.getContentResolver();
            Cursor c = cr.query(ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, noteId),
                    new String[]{ NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE }, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    int t = c.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                    int b = c.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                    title = c.getString(t);
                    body = c.getString(b);
                }
                c.close();
            }
            launch = new Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, noteId));
        } else {
            launch = new Intent(context, NotesList.class);
        }
        views.setTextViewText(R.id.widget_title, title);
        views.setTextViewText(R.id.widget_body, body);
        Intent i = launch;
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(context, appWidgetId, i, android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_title, pi);
        views.setOnClickPendingIntent(R.id.widget_body, pi);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
