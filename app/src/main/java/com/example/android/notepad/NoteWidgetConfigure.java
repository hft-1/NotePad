package com.example.android.notepad;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AdapterView;

public class NoteWidgetConfigure extends Activity {
    private int appWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_widget_configure);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        ListView list = (ListView) findViewById(R.id.widget_list);
        Cursor cursor = getContentResolver().query(NotePad.Notes.CONTENT_URI,
                new String[]{ NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_TITLE }, null, null, NotePad.Notes.DEFAULT_SORT_ORDER);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor,
                new String[]{ NotePad.Notes.COLUMN_NAME_TITLE }, new int[]{ android.R.id.text1 });
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences prefs = getSharedPreferences("notepad_prefs", MODE_PRIVATE);
                prefs.edit().putLong("widget_note_" + appWidgetId, id).apply();
                AppWidgetManager awm = AppWidgetManager.getInstance(NoteWidgetConfigure.this);
                NoteWidgetProvider.updateAppWidget(NoteWidgetConfigure.this, awm, appWidgetId);
                Intent result = new Intent();
                result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }
}
