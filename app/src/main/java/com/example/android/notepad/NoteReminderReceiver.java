package com.example.android.notepad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentUris;

public class NoteReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra("note_id", -1);
        String title = "笔记提醒";
        String text = "";
        if (id != -1) {
            Uri uri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, id);
            Cursor c = context.getContentResolver().query(uri,
                    new String[]{ NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE },
                    null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    int tIdx = c.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                    int nIdx = c.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                    if (tIdx != -1) title = c.getString(tIdx);
                    if (nIdx != -1) text = c.getString(nIdx);
                }
                c.close();
            }
        }

        Intent open = new Intent(context, NoteEditor.class);
        if (id != -1) {
            open.setAction(Intent.ACTION_EDIT);
            open.setData(ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, id));
        }
        PendingIntent content = PendingIntent.getActivity(context, (int) id, open, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.app_notes)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(content);
        builder.setDefaults(Notification.DEFAULT_ALL);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int) id, builder.build());
        }
    }
}
