package com.example.android.notepad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public class TodoBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                long now = System.currentTimeMillis();
                Cursor c = context.getContentResolver().query(
                        NotePad.ToDos.CONTENT_URI,
                        new String[]{ NotePad.ToDos._ID, NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS, NotePad.ToDos.COLUMN_NAME_DONE },
                        NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS + " > ? AND " + NotePad.ToDos.COLUMN_NAME_DONE + " = 0",
                        new String[]{ String.valueOf(now) },
                        null
                );
                if (c != null) {
                    int idIdx = c.getColumnIndex(NotePad.ToDos._ID);
                    int rIdx = c.getColumnIndex(NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS);
                    for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                        long id = c.getLong(idIdx);
                        long when = c.getLong(rIdx);
                        android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        android.content.Intent i = new android.content.Intent(context, TodoReminderReceiver.class);
                        i.setData(android.content.ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, id));
                        i.putExtra("todo_id", id);
                        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(context, (int) id, i, android.app.PendingIntent.FLAG_UPDATE_CURRENT);
                        if (am != null) {
                            if (android.os.Build.VERSION.SDK_INT >= 23) {
                                am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, when, pi);
                            } else if (android.os.Build.VERSION.SDK_INT >= 19) {
                                am.setExact(android.app.AlarmManager.RTC_WAKEUP, when, pi);
                            } else {
                                am.set(android.app.AlarmManager.RTC_WAKEUP, when, pi);
                            }
                        }
                    }
                    c.close();
                }
            } catch (Exception ignored) {}
        }
    }
}
