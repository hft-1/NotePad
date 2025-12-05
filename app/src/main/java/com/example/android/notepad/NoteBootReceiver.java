package com.example.android.notepad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.content.ContentUris;
import android.app.AlarmManager;
import android.app.PendingIntent;

public class NoteBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                long now = System.currentTimeMillis();
                Cursor c = context.getContentResolver().query(
                        NotePad.Notes.CONTENT_URI,
                        new String[]{ NotePad.Notes._ID, NotePad.Notes.COLUMN_NAME_REMINDER_MILLIS },
                        NotePad.Notes.COLUMN_NAME_REMINDER_MILLIS + " > ?",
                        new String[]{ String.valueOf(now) },
                        null
                );
                if (c != null) {
                    int idIdx = c.getColumnIndex(NotePad.Notes._ID);
                    int rIdx = c.getColumnIndex(NotePad.Notes.COLUMN_NAME_REMINDER_MILLIS);
                    for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                        long id = c.getLong(idIdx);
                        long when = c.getLong(rIdx);
                        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        Intent i = new Intent(context, NoteReminderReceiver.class);
                        i.setData(ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, id));
                        i.putExtra("note_id", id);
                        PendingIntent pi = PendingIntent.getBroadcast(context, (int) id, i, PendingIntent.FLAG_UPDATE_CURRENT);
                        if (am != null) {
                            if (android.os.Build.VERSION.SDK_INT >= 23) {
                                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                            } else if (android.os.Build.VERSION.SDK_INT >= 19) {
                                am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
                            } else {
                                am.set(AlarmManager.RTC_WAKEUP, when, pi);
                            }
                        }
                    }
                    c.close();
                }
            } catch (Exception ignored) {}
        }
    }
}
