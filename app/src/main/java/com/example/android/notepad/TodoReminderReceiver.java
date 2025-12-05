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

public class TodoReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra("todo_id", -1);
        String text = "待办提醒";
        if (id != -1) {
            Uri uri = ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, id);
            Cursor c = context.getContentResolver().query(uri,
                    new String[]{ NotePad.ToDos.COLUMN_NAME_TEXT },
                    null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    int idx = c.getColumnIndex(NotePad.ToDos.COLUMN_NAME_TEXT);
                    if (idx != -1) text = c.getString(idx);
                }
                c.close();
            }
        }

        Intent open = new Intent(context, ToDoList.class);
        open.putExtra("todo_id", id);
        PendingIntent content = PendingIntent.getActivity(context, (int) id, open, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.app_notes)
                .setContentTitle("待办提醒")
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
