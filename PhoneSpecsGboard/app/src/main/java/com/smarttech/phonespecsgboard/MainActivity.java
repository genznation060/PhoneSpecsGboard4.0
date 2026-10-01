package com.smarttech.phonespecsgboard;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.inputmethod.InputMethodManager;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private EditText input;
    private Spinner columnSpinner;
    private EditText startRow;
    private EditText delay;
    private TextView status;
    private Button loadButton, nextButton, stopButton;
    private Button openClipboardAppButton, switchKeyboardButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String[]> rows = new ArrayList<>();
    private List<String> selectedValues = new ArrayList<>();
    private int nextIndex = 0;
    private boolean loading = false;
    private SharedPreferences prefs;

    private final Runnable stopRunnable = () -> {
        loading = false;
        loadButton.setEnabled(true);
        nextButton.setEnabled(true);
        stopButton.setEnabled(false);
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(14);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(0, 12, 0, 6);
        return t;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    private void buildUi() {
        prefs = getSharedPreferences("PhoneSpecPrefs", MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 14, 18, 18);
        root.setBackgroundColor(Color.rgb(245,247,250));

        TextView title = new TextView(this);
        title.setText("📱 Phone Specs → Gboard");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(17,24,39));
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView sub = new TextView(this);
        sub.setText("Paste Excel/Sheets data, choose a column, then load each value as a separate clipboard item.");
        sub.setTextSize(13);
        sub.setTextColor(Color.GRAY);
        root.addView(sub);

        root.addView(label("1. Paste your spreadsheet table"));
        input = new EditText(this);
        input.setHint("Copy from Excel/Sheets and paste here…");
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(7);
        input.setPadding(12,12,12,12);
        root.addView(input, new LinearLayout.LayoutParams(-1, 0, 1));

        Button parse = button("Generate Columns");
        root.addView(parse);
        parse.setOnClickListener(v -> parseTable());

        root.addView(label("2. Select phone column"));
        columnSpinner = new Spinner(this);
        root.addView(columnSpinner);

        LinearLayout opts = new LinearLayout(this);
        opts.setOrientation(LinearLayout.HORIZONTAL);

        startRow = new EditText(this);
        startRow.setHint("Start row");
        startRow.setText(String.valueOf(prefs.getInt("startRow", 2)));
        startRow.setInputType(InputType.TYPE_CLASS_NUMBER);
        opts.addView(startRow, new LinearLayout.LayoutParams(0, -2, 1));

        delay = new EditText(this);
        delay.setHint("Delay ms");
        delay.setText(String.valueOf(prefs.getLong("delayMs", 1200)));
        delay.setInputType(InputType.TYPE_CLASS_NUMBER);
        opts.addView(delay, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(opts);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        loadButton = button("🚀 LOAD ALL TO GBOARD");
        nextButton = button("COPY NEXT");
        stopButton = button("STOP");
        stopButton.setEnabled(false);

        buttons.addView(loadButton, new LinearLayout.LayoutParams(0, -2, 1));
        buttons.addView(nextButton, new LinearLayout.LayoutParams(0, -2, 1));
        buttons.addView(stopButton, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(buttons);

        LinearLayout extraButtons = new LinearLayout(this);
        extraButtons.setOrientation(LinearLayout.HORIZONTAL);
        extraButtons.setPadding(0, 10, 0, 0);

        openClipboardAppButton = button("Open Clipboard App");
        switchKeyboardButton = button("Switch Keyboard");

        extraButtons.addView(openClipboardAppButton, new LinearLayout.LayoutParams(0, -2, 1));
        extraButtons.addView(switchKeyboardButton, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(extraButtons);


        status = new TextView(this);
        status.setText("Ready. Gboard must be enabled and its Clipboard feature should be available.");
        status.setTextSize(13);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0,10,0,0);
        root.addView(status);

        setContentView(root);

        loadButton.setOnClickListener(v -> loadAll());
        nextButton.setOnClickListener(v -> copyNext());
        stopButton.setOnClickListener(v -> stopLoading());

        openClipboardAppButton.setOnClickListener(v -> openClipboardApp());
        switchKeyboardButton.setOnClickListener(v -> switchKeyboard());

    }

    private void parseTable() {
        saveSettings();
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            status.setText("Paste your Excel/Sheets table first.");
            return;
        }

        rows.clear();
        String[] lines = text.split("\\r?\\n");
        int max = 0;

        for (String line : lines) {
            String[] cells = line.split("\\t", -1);
            if (cells.length == 1) cells = line.trim().split("\\s{2,}", -1);
            rows.add(cells);
            max = Math.max(max, cells.length);
        }

        for (int i = 0; i < rows.size(); i++) {
            String[] old = rows.get(i);
            String[] fixed = new String[max];
            for (int j = 0; j < max; j++) fixed[j] = j < old.length ? old[j].trim() : "";
            rows.set(i, fixed);
        }

        String[] headers = new String[max];
        for (int i = 0; i < max; i++) {
            String h = rows.get(0)[i];
            headers[i] = h.isEmpty() ? "Column " + (i + 1) : h;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, headers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        columnSpinner.setAdapter(adapter);

        nextIndex = 0;
        prepareValues();
        status.setText("Found " + rows.size() + " rows and " + max + " columns. Select a column.");
    }


    private void saveSettings() {
        if (prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            try {
                editor.putInt("startRow", Integer.parseInt(startRow.getText().toString().trim()));
            } catch (Exception e) {}
            try {
                editor.putLong("delayMs", Long.parseLong(delay.getText().toString().trim()));
            } catch (Exception e) {}
            editor.apply();
        }
    }

    private int getStartRow() {
        try {
            int r = Integer.parseInt(startRow.getText().toString().trim());
            return Math.max(1, r);
        } catch (Exception e) {
            return 2;
        }
    }

    private long getDelay() {
        try {
            return Math.max(500, Long.parseLong(delay.getText().toString().trim()));
        } catch (Exception e) {
            return 1200;
        }
    }

    private void prepareValues() {
        saveSettings();
        selectedValues = new ArrayList<>();
        if (rows.isEmpty() || columnSpinner.getSelectedItemPosition() < 0) return;
        int col = columnSpinner.getSelectedItemPosition();
        int start = Math.min(getStartRow() - 1, rows.size());

        for (int i = start; i < rows.size(); i++) {
            String value = rows.get(i)[col];
            if (value != null && !value.trim().isEmpty()) selectedValues.add(value.trim());
        }
        nextIndex = 0;
        status.setText(selectedValues.size() + " separate clipboard items ready.");
    }


    private void openClipboardApp() {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage("devdnua.clipboard");
        if (intent != null) {
            startActivity(intent);
        } else {

            Toast.makeText(this, "devdnua.clipboard app not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void switchKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showInputMethodPicker();
        }
    }

    private void copyToClipboard(String value) {
        ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Phone Specs", value));
    }

    private void copyNext() {
        prepareIfNeeded();
        if (nextIndex >= selectedValues.size()) {
            status.setText("Finished — all items were sent.");
            return;
        }
        String value = selectedValues.get(nextIndex);
        copyToClipboard(value);
        nextIndex++;
        status.setText("Copied item " + nextIndex + " / " + selectedValues.size() + ": " + value);
    }

    private void prepareIfNeeded() {
        if (selectedValues.isEmpty() && !rows.isEmpty()) prepareValues();
    }

    private void loadAll() {
        prepareValues();
        if (selectedValues.isEmpty()) {
            status.setText("No values found in the selected column.");
            return;
        }

        loading = true;
        loadButton.setEnabled(false);
        nextButton.setEnabled(false);
        stopButton.setEnabled(true);

        final long gap = getDelay();
        final int[] i = {0};

        Runnable r = new Runnable() {
            @Override public void run() {
                if (!loading || i[0] >= selectedValues.size()) {
                    loading = false;
                    loadButton.setEnabled(true);
                    nextButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    status.setText("Finished. " + selectedValues.size() + " clipboard items sent to Android/Gboard.");
                    return;
                }

                String value = selectedValues.get(i[0]);
                copyToClipboard(value);
                i[0]++;
                nextIndex = i[0];
                status.setText("Loading Gboard clipboard: " + i[0] + " / " + selectedValues.size() + "\n" + value);
                handler.postDelayed(this, gap);
            }
        };
        handler.post(r);
    }

    private void stopLoading() {
        loading = false;
        handler.removeCallbacksAndMessages(null);
        loadButton.setEnabled(true);
        nextButton.setEnabled(true);
        stopButton.setEnabled(false);
        status.setText("Stopped at item " + nextIndex + ".");
    }


    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loading = false;
        handler.removeCallbacksAndMessages(null);
    }
}
