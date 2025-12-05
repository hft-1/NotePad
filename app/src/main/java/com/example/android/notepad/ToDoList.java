package com.example.android.notepad;

import android.app.Activity;
import android.os.Bundle;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter;
import android.widget.SearchView;
import android.widget.FilterQueryProvider;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.net.Uri;
import android.os.Build;
import android.widget.AdapterView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.content.Intent;
import android.content.ContentValues;
import android.content.ContentUris;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import android.widget.Toast;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ToDoList extends Activity {
    private ListView listView;
    private ListView doneListView;
    private SimpleCursorAdapter adapter;
    private SimpleCursorAdapter doneAdapter;
    private Long selectedReminder;
    private final int[] ITEM_COLORS = new int[] {
            android.graphics.Color.parseColor("#FFEDE7"),
            android.graphics.Color.parseColor("#E8F0FE"),
            android.graphics.Color.parseColor("#E8F5E9"),
            android.graphics.Color.parseColor("#FFF3E0"),
            android.graphics.Color.parseColor("#F3E5F5"),
            android.graphics.Color.parseColor("#FFF9C4"),
            android.graphics.Color.parseColor("#E0F7FA"),
            android.graphics.Color.parseColor("#FCE4EC")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todo_list);
        setTitle("To-Do List");

        listView = (ListView) findViewById(R.id.todo_list);
        Cursor cursor = getContentResolver().query(
                NotePad.ToDos.CONTENT_URI,
                new String[]{
                        NotePad.ToDos._ID,
                        NotePad.ToDos.COLUMN_NAME_TEXT,
                        NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS,
                        NotePad.ToDos.COLUMN_NAME_DONE
                },
                NotePad.ToDos.COLUMN_NAME_DONE + " = 0",
                null,
                NotePad.ToDos.DEFAULT_SORT_ORDER
        );

        String[] dataColumns = {
                NotePad.ToDos.COLUMN_NAME_TEXT,
                NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS,
                NotePad.ToDos.COLUMN_NAME_DONE,
                NotePad.ToDos._ID
        };
        int[] viewIds = { R.id.text, R.id.reminder, R.id.checkbox, R.id.btn_delete };
        adapter = new SimpleCursorAdapter(this, R.layout.todo_item, cursor, dataColumns, viewIds);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor c, int columnIndex) {
                int vid = view.getId();
                if (vid == R.id.text) {
                    TextView tv = (TextView) view;
                    tv.setText(c.getString(columnIndex));
                    int idCol = c.getColumnIndex(NotePad.ToDos._ID);
                    if (idCol != -1) {
                        long todoId = c.getLong(idCol);
                        int idx = (int) (Math.abs(todoId) % ITEM_COLORS.length);
                        Object parent = view.getParent();
                        if (parent instanceof View) {
                            ((View) parent).setBackgroundColor(ITEM_COLORS[idx]);
                        }
                        tv.setTextColor(android.graphics.Color.parseColor("#212121"));
                    }
                    return true;
                }
                if (vid == R.id.reminder) {
                    long millis = 0;
                    try { millis = c.getLong(columnIndex); } catch (Exception ignored) {}
                    TextView tv = (TextView) view;
                    if (millis > 0) {
                        String t = formatReminder(millis);
                        if (millis <= System.currentTimeMillis()) {
                            t = t + "  已过期";
                        }
                        tv.setText(t);
                    } else {
                        tv.setText("");
                    }
                    tv.setTextColor(android.graphics.Color.parseColor("#616161"));
                    return true;
                } else if (vid == R.id.checkbox) {
                    int done = 0;
                    try { done = c.getInt(columnIndex); } catch (Exception ignored) {}
                    CheckBox cb = (CheckBox) view;
                    cb.setEnabled(true);
                    cb.setOnCheckedChangeListener(null);
                    cb.setChecked(done != 0);
                    final long id = c.getLong(c.getColumnIndex(NotePad.ToDos._ID));
                    cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        ContentValues v = new ContentValues();
                        v.put(NotePad.ToDos.COLUMN_NAME_DONE, isChecked ? 1 : 0);
                        v.put(NotePad.ToDos.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
                        getContentResolver().update(ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, id), v, null, null);
                        if (isChecked) cancelReminder(id);
                    });
                    return true;
                } else if (vid == R.id.btn_delete) {
                    final long id = c.getLong(c.getColumnIndex(NotePad.ToDos._ID));
                    view.setOnClickListener(v2 -> {
                        cancelReminder(id);
                        getContentResolver().delete(ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, id), null, null);
                        Cursor cur = ((SimpleCursorAdapter) listView.getAdapter()).getCursor();
                        if (cur != null) cur.requery();
                    });
                    return true;
                }
                return false;
            }
        });
        listView.setAdapter(adapter);

        doneListView = (ListView) findViewById(R.id.done_list);
        Cursor doneCursor = getContentResolver().query(
                NotePad.ToDos.CONTENT_URI,
                new String[]{
                        NotePad.ToDos._ID,
                        NotePad.ToDos.COLUMN_NAME_TEXT,
                        NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS,
                        NotePad.ToDos.COLUMN_NAME_DONE
                },
                NotePad.ToDos.COLUMN_NAME_DONE + " = 1",
                null,
                NotePad.ToDos.DEFAULT_SORT_ORDER
        );
        doneAdapter = new SimpleCursorAdapter(this, R.layout.todo_item, doneCursor, dataColumns, viewIds);
        doneAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor c, int columnIndex) {
                int vid = view.getId();
                if (vid == R.id.text) {
                    TextView tv = (TextView) view;
                    tv.setText(c.getString(columnIndex));
                    tv.setPaintFlags(tv.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    tv.setTextColor(android.graphics.Color.parseColor("#BDBDBD"));
                    Object parent = view.getParent();
                    if (parent instanceof View) {
                        ((View) parent).setBackgroundColor(android.graphics.Color.parseColor("#F0F0F0"));
                    }
                    return true;
                } else if (vid == R.id.reminder) {
                    long millis = 0;
                    try { millis = c.getLong(columnIndex); } catch (Exception ignored) {}
                    TextView tv = (TextView) view;
                    if (millis > 0) {
                        String t = formatReminder(millis);
                        if (millis <= System.currentTimeMillis()) t = t + "  已过期";
                        tv.setText(t);
                    } else {
                        tv.setText("");
                    }
                    tv.setTextColor(android.graphics.Color.parseColor("#BDBDBD"));
                    return true;
                } else if (vid == R.id.checkbox) {
                    CheckBox cb = (CheckBox) view;
                    cb.setEnabled(true);
                    cb.setOnCheckedChangeListener(null);
                    cb.setChecked(true);
                    final long id = c.getLong(c.getColumnIndex(NotePad.ToDos._ID));
                    cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        ContentValues v = new ContentValues();
                        v.put(NotePad.ToDos.COLUMN_NAME_DONE, isChecked ? 1 : 0);
                        v.put(NotePad.ToDos.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
                        getContentResolver().update(ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, id), v, null, null);
                        if (isChecked) {
                            cancelReminder(id);
                        } else {
                            // reschedule if still in future
                            Cursor one = getContentResolver().query(ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, id),
                                    new String[]{ NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS }, null, null, null);
                            if (one != null) {
                                if (one.moveToFirst()) {
                                    long m = 0;
                                    if (!one.isNull(0)) { try { m = one.getLong(0);} catch(Exception ignored){} }
                                    if (m > System.currentTimeMillis()) scheduleReminder(id, m);
                                }
                                one.close();
                            }
                        }
                        refreshLists();
                    });
                    return true;
                } else if (vid == R.id.btn_delete) {
                    final long id = c.getLong(c.getColumnIndex(NotePad.ToDos._ID));
                    view.setOnClickListener(v2 -> {
                        cancelReminder(id);
                        getContentResolver().delete(ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, id), null, null);
                        refreshLists();
                    });
                    return true;
                }
                return false;
            }
        });
        doneListView.setAdapter(doneAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showEditDialog(id);
            }
        });
        doneListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showEditDialog(id);
            }
        });

        TextView doneHeader = (TextView) findViewById(R.id.done_header);
        TextView doneToggle = (TextView) findViewById(R.id.done_toggle);
        updateDoneHeader();
        if (doneToggle != null) {
            doneToggle.setOnClickListener(v -> {
                if (doneListView.getVisibility() == View.VISIBLE) {
                    doneListView.setVisibility(View.GONE);
                } else {
                    doneListView.setVisibility(View.VISIBLE);
                }
                updateDoneHeader();
            });
        }

        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                String selection = null;
                String[] selectionArgs = null;
                if (constraint != null && constraint.length() > 0) {
                    String q = "%" + constraint.toString() + "%";
                    selection = NotePad.ToDos.COLUMN_NAME_TEXT + " LIKE ?";
                    selectionArgs = new String[]{ q };
                }
                return managedQuery(
                        NotePad.ToDos.CONTENT_URI,
                        new String[]{
                                NotePad.ToDos._ID,
                                NotePad.ToDos.COLUMN_NAME_TEXT,
                                NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS,
                                NotePad.ToDos.COLUMN_NAME_DONE
                        },
                        selection,
                        selectionArgs,
                        NotePad.ToDos.DEFAULT_SORT_ORDER
                );
            }
        });

        Button btnNote = (Button) findViewById(R.id.btn_note);
        Button btnTodo = (Button) findViewById(R.id.btn_todo);
        View btnAdd = findViewById(R.id.fab_add_todo);
        if (btnNote != null) {
            btnNote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(ToDoList.this, NotesList.class));
                }
            });
        }
        if (btnTodo != null) {
            btnTodo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(ToDoList.this, ToDoList.class));
                }
            });
        }
        if (btnAdd != null) {
            btnAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddDialog();
                }
            });
        }
        applySavedTheme();

        long targetId = getIntent().getLongExtra("todo_id", -1);
        if (targetId != -1) {
            Cursor cur = adapter.getCursor();
            if (cur != null) {
                int pos = -1;
                int idIdx = cur.getColumnIndex(NotePad.ToDos._ID);
                for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
                    if (cur.getLong(idIdx) == targetId) { pos = cur.getPosition(); break; }
                }
                if (pos >= 0) listView.setSelection(pos);
            }
        }

        rescheduleAll();
    }

    private void showAddDialog() {
        selectedReminder = null;
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_add_todo, null);
        EditText input = (EditText) content.findViewById(R.id.input);
        ListView preview = (ListView) content.findViewById(R.id.preview_list);
        Button btnSetReminder = (Button) content.findViewById(R.id.btn_set_reminder);
        Button btnDone = (Button) content.findViewById(R.id.btn_done);
        btnDone.setEnabled(false);
        btnSetReminder.setText("设置提醒");

        ArrayList<String> previewItems = new ArrayList<>();
        BaseAdapter previewAdapter = new BaseAdapter() {
            @Override public int getCount() { return previewItems.size(); }
            @Override public Object getItem(int position) { return previewItems.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) v = LayoutInflater.from(ToDoList.this).inflate(R.layout.todo_item, parent, false);
                CheckBox cb = (CheckBox) v.findViewById(R.id.checkbox);
                TextView tv = (TextView) v.findViewById(R.id.text);
                TextView rv = (TextView) v.findViewById(R.id.reminder);
                cb.setEnabled(false);
                tv.setText(previewItems.get(position));
                if (selectedReminder != null) {
                    rv.setText(formatReminder(selectedReminder));
                } else {
                    rv.setText("");
                }
                return v;
            }
        };
        preview.setAdapter(previewAdapter);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                previewItems.clear();
                String[] lines = s.toString().split("\n");
                for (String line : lines) {
                    String t = line.trim();
                    if (t.length() > 0) previewItems.add(t);
                }
                previewAdapter.notifyDataSetChanged();
                btnDone.setEnabled(previewItems.size() > 0);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();

        btnSetReminder.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pickReminder(new Runnable() {
                    @Override public void run() {
                        if (selectedReminder != null) {
                            btnSetReminder.setText(formatReminder(selectedReminder));
                            previewAdapter.notifyDataSetChanged();
                        } else {
                            btnSetReminder.setText("设置提醒");
                            previewAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                for (String t : previewItems) {
                    ContentValues values = new ContentValues();
                    values.put(NotePad.ToDos.COLUMN_NAME_TEXT, t);
                    if (selectedReminder != null) {
                        values.put(NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS, selectedReminder);
                    }
                    values.put(NotePad.ToDos.COLUMN_NAME_DONE, 0);
                    Uri uri = getContentResolver().insert(NotePad.ToDos.CONTENT_URI, values);
                    if (uri != null && selectedReminder != null && selectedReminder > System.currentTimeMillis()) {
                        long id = ContentUris.parseId(uri);
                        scheduleReminder(id, selectedReminder);
                    }
                }
                // Refresh cursor
                refreshLists();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showEditDialog(long todoId) {
        selectedReminder = null;
        Cursor cur = getContentResolver().query(ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, todoId),
                new String[]{ NotePad.ToDos.COLUMN_NAME_TEXT, NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS },
                null, null, null);
        String initialText = "";
        Long initialReminder = null;
        if (cur != null) {
            if (cur.moveToFirst()) {
                int tIdx = cur.getColumnIndex(NotePad.ToDos.COLUMN_NAME_TEXT);
                int rIdx = cur.getColumnIndex(NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS);
                if (tIdx != -1) initialText = cur.getString(tIdx);
                if (rIdx != -1) {
                    if (!cur.isNull(rIdx)) {
                        long v = 0; try { v = cur.getLong(rIdx); } catch (Exception ignored) {}
                        if (v > 0) initialReminder = v; else initialReminder = null;
                    } else {
                        initialReminder = null;
                    }
                }
            }
            cur.close();
        }
        selectedReminder = initialReminder;

        View content = LayoutInflater.from(this).inflate(R.layout.dialog_add_todo, null);
        EditText input = (EditText) content.findViewById(R.id.input);
        ListView preview = (ListView) content.findViewById(R.id.preview_list);
        Button btnSetReminder = (Button) content.findViewById(R.id.btn_set_reminder);
        Button btnDone = (Button) content.findViewById(R.id.btn_done);
        input.setText(initialText);
        input.setSelection(initialText.length());
        btnDone.setEnabled(initialText.trim().length() > 0);
        if (selectedReminder != null) {
            btnSetReminder.setText(formatReminder(selectedReminder));
        } else {
            btnSetReminder.setText("设置提醒");
        }

        final ArrayList<String> previewItems = new ArrayList<>();
        previewItems.add(initialText);
        BaseAdapter previewAdapter = new BaseAdapter() {
            @Override public int getCount() { return previewItems.size(); }
            @Override public Object getItem(int position) { return previewItems.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) v = LayoutInflater.from(ToDoList.this).inflate(R.layout.todo_item, parent, false);
                CheckBox cb = (CheckBox) v.findViewById(R.id.checkbox);
                TextView tv = (TextView) v.findViewById(R.id.text);
                TextView rv = (TextView) v.findViewById(R.id.reminder);
                cb.setEnabled(false);
                tv.setText(previewItems.get(position));
                if (selectedReminder != null) {
                    rv.setText(formatReminder(selectedReminder));
                } else {
                    rv.setText("");
                }
                return v;
            }
        };
        preview.setAdapter(previewAdapter);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String t = s.toString();
                previewItems.clear();
                String tt = t.trim();
                if (tt.length() > 0) previewItems.add(t);
                previewAdapter.notifyDataSetChanged();
                btnDone.setEnabled(tt.length() > 0);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();

        btnSetReminder.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pickReminder(new Runnable() {
                    @Override public void run() {
                        if (selectedReminder != null) {
                            btnSetReminder.setText(formatReminder(selectedReminder));
                            previewAdapter.notifyDataSetChanged();
                        } else {
                            btnSetReminder.setText("设置提醒");
                            previewAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String t = input.getText().toString().trim();
                if (t.length() == 0) return;
                ContentValues values = new ContentValues();
                values.put(NotePad.ToDos.COLUMN_NAME_TEXT, t);
                values.put(NotePad.ToDos.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
                if (selectedReminder != null) {
                    values.put(NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS, selectedReminder);
                } else {
                    values.putNull(NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS);
                }
                getContentResolver().update(ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, todoId), values, null, null);
                if (selectedReminder != null && selectedReminder > System.currentTimeMillis()) {
                    scheduleReminder(todoId, selectedReminder);
                } else {
                    cancelReminder(todoId);
                }
                refreshLists();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void scheduleReminder(long id, long whenMillis) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        android.content.Intent intent = new android.content.Intent(this, TodoReminderReceiver.class);
        intent.setData(ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, id));
        intent.putExtra("todo_id", id);
        PendingIntent pi = PendingIntent.getBroadcast(this, (int) id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 23) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMillis, pi);
        } else if (Build.VERSION.SDK_INT >= 19) {
            am.setExact(AlarmManager.RTC_WAKEUP, whenMillis, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, whenMillis, pi);
        }
    }

    private void cancelReminder(long id) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        android.content.Intent intent = new android.content.Intent(this, TodoReminderReceiver.class);
        intent.setData(ContentUris.withAppendedId(NotePad.ToDos.CONTENT_ID_URI_BASE, id));
        PendingIntent pi = PendingIntent.getBroadcast(this, (int) id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);
    }

    private void rescheduleAll() {
        long now = System.currentTimeMillis();
        Cursor c = getContentResolver().query(
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
                scheduleReminder(id, when);
            }
        }
    }

    private void refreshLists() {
        Cursor newTodo = getContentResolver().query(
                NotePad.ToDos.CONTENT_URI,
                new String[]{
                        NotePad.ToDos._ID,
                        NotePad.ToDos.COLUMN_NAME_TEXT,
                        NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS,
                        NotePad.ToDos.COLUMN_NAME_DONE
                },
                NotePad.ToDos.COLUMN_NAME_DONE + " = 0",
                null,
                NotePad.ToDos.DEFAULT_SORT_ORDER
        );
        adapter.changeCursor(newTodo);

        Cursor newDone = getContentResolver().query(
                NotePad.ToDos.CONTENT_URI,
                new String[]{
                        NotePad.ToDos._ID,
                        NotePad.ToDos.COLUMN_NAME_TEXT,
                        NotePad.ToDos.COLUMN_NAME_REMINDER_MILLIS,
                        NotePad.ToDos.COLUMN_NAME_DONE
                },
                NotePad.ToDos.COLUMN_NAME_DONE + " = 1",
                null,
                NotePad.ToDos.DEFAULT_SORT_ORDER
        );
        doneAdapter.changeCursor(newDone);

        updateDoneHeader();
    }

    private void updateDoneHeader() {
        TextView doneHeader = (TextView) findViewById(R.id.done_header);
        TextView doneToggle = (TextView) findViewById(R.id.done_toggle);
        if (doneHeader != null) {
            Cursor curDone = ((SimpleCursorAdapter) doneListView.getAdapter()).getCursor();
            int count = curDone != null ? curDone.getCount() : 0;
            doneHeader.setText("已完成 " + count);
            if (doneToggle != null) {
                doneToggle.setText(doneListView.getVisibility() == View.VISIBLE ? "^" : "v");
            }
        }
    }

    private void pickReminder(Runnable onFinish) {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                TimePickerDialog tp = new TimePickerDialog(ToDoList.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.YEAR, year);
                        c.set(Calendar.MONTH, month);
                        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        c.set(Calendar.MINUTE, minute);
                        c.set(Calendar.SECOND, 0);
                        long millis = c.getTimeInMillis();
                        String preview = formatFull(c);
                        new AlertDialog.Builder(ToDoList.this)
                                .setMessage(preview)
                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface dialog, int which) {
                                        selectedReminder = null;
                                        onFinish.run();
                                    }
                                })
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface dialog, int which) {
                                        long nowMillis = System.currentTimeMillis();
                                        if (millis <= nowMillis) {
                                            Toast.makeText(ToDoList.this, "所选时间已过期", Toast.LENGTH_LONG).show();
                                            selectedReminder = null;
                                        } else {
                                            selectedReminder = millis;
                                        }
                                        onFinish.run();
                                    }
                                })
                                .show();
                    }
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);
                tp.show();
            }
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private String formatReminder(long millis) {
        return new SimpleDateFormat("M/d HH：mm", Locale.getDefault()).format(new java.util.Date(millis));
    }

    private String formatFull(Calendar c) {
        SimpleDateFormat f1 = new SimpleDateFormat("M/d  EEEE", Locale.getDefault());
        SimpleDateFormat f2 = new SimpleDateFormat("HH 'h' mm 'min'", Locale.getDefault());
        return f1.format(c.getTime()) + "  " + f2.format(c.getTime());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.todo_options_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null) {
            View actionView = searchItem.getActionView();
            if (actionView instanceof SearchView) {
                SearchView searchView = (SearchView) actionView;
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        adapter.getFilter().filter(query);
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        adapter.getFilter().filter(newText);
                        return true;
                    }
                });
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_change_theme) {
            showThemeChooser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showThemeChooser() {
        final String[] items = new String[]{"Light", "Dark", "Blue", "Green"};
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_change_theme)
                .setItems(items, (dialog, which) -> {
                    applyTheme(which);
                    saveTheme(which);
                })
                .show();
    }

    private void applySavedTheme() {
        android.content.SharedPreferences prefs = getSharedPreferences("notepad_prefs", MODE_PRIVATE);
        int which = prefs.getInt("todolist_theme", 0);
        applyTheme(which);
    }

    private void saveTheme(int which) {
        android.content.SharedPreferences prefs = getSharedPreferences("notepad_prefs", MODE_PRIVATE);
        prefs.edit().putInt("todolist_theme", which).apply();
    }

    private void applyTheme(int which) {
        int res;
        int dividerColor;
        switch (which) {
            case 1:
                res = R.drawable.bg_dark;
                dividerColor = android.graphics.Color.parseColor("#424242");
                break;
            case 2:
                res = R.drawable.bg_blue;
                dividerColor = android.graphics.Color.parseColor("#90CAF9");
                break;
            case 3:
                res = R.drawable.bg_green;
                dividerColor = android.graphics.Color.parseColor("#A5D6A7");
                break;
            default:
                res = R.drawable.bg_light;
                dividerColor = android.graphics.Color.parseColor("#E0E0E0");
        }
        listView.setBackgroundResource(res);
        listView.setCacheColorHint(android.graphics.Color.TRANSPARENT);
        listView.setDivider(new android.graphics.drawable.ColorDrawable(dividerColor));
        listView.setDividerHeight(1);
        listView.invalidateViews();
    }

    // removed array adapter; using provider-backed cursor adapter
}
