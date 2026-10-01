package com.smarttech.phonespecsgboard;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String PREFS = "PhoneSpecsPrefs";
    private static final String KEY_DELAY = "delay_ms";
    private static final String KEY_START_ROW = "start_row";
    private static final String KEY_COLUMN = "last_column";
    private static final String CLIPBOARD_PKG = "devdnua.clipboard";

    private EditText input;
    private Spinner columnSpinner;
    private EditText startRow;
    private EditText delay;
    private TextView status;
    private TextView preview;
    private Button loadButton, nextButton, stopButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<String[]> rows = new ArrayList<>();
    private List<String> selectedValues = new ArrayList<>();
    private int nextIndex = 0;
    private boolean loading = false;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        restoreSettings();
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(14);
        t.setTextColor(Color.DKGRAY);
        t.setPadding(0, 14, 0, 4);
        return t;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 14, 18, 24);
        root.setBackgroundColor(Color.rgb(245, 247, 250));

        TextView title = new TextView(this);
        title.setText("📱 Phone Specs → Gboard");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(17, 24, 39));
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Paste Excel/Sheets data → choose column → load to clipboard (or open Clipboard Manager).");
        sub.setTextSize(13);
        sub.setTextColor(Color.GRAY);
        sub.setPadding(0, 4, 0, 8);
        root.addView(sub);

        // 1. Paste area
        root.addView(label("1. Paste your spreadsheet table"));
        input = new EditText(this);
        input.setHint("Copy from Excel/Sheets and paste here…");
        input.setGravity(Gravity.TOP);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(5);
        input.setMaxLines(8);
        input.setPadding(12, 12, 12, 12);
        input.setBackgroundColor(Color.WHITE);
        root.addView(input, new LinearLayout.LayoutParams(-1, -2));

        Button parse = button("Generate Columns");
        parse.setOnClickListener(v -> parseTable());
        root.addView(parse);

        // 2. Column + options
        root.addView(label("2. Select phone / spec column"));
        columnSpinner = new Spinner(this);
        columnSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prepareValues();
                updatePreview();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        root.addView(columnSpinner);

        LinearLayout opts = new LinearLayout(this);
        opts.setOrientation(LinearLayout.HORIZONTAL);
        opts.setPadding(0, 6, 0, 0);

        startRow = new EditText(this);
        startRow.setHint("Start row");
        startRow.setText("2");
        startRow.setInputType(InputType.TYPE_CLASS_NUMBER);
        startRow.setPadding(10, 10, 10, 10);
        opts.addView(startRow, new LinearLayout.LayoutParams(0, -2, 1));

        delay = new EditText(this);
        delay.setHint("Delay ms");
        delay.setText("1200");
        delay.setInputType(InputType.TYPE_CLASS_NUMBER);
        delay.setPadding(10, 10, 10, 10);
        opts.addView(delay, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(opts);

        // Preview box
        root.addView(label("Column Preview (sample values)"));
        preview = new TextView(this);
        preview.setText("Parse a table and select a column to see sample values here.");
        preview.setTextSize(12);
        preview.setTextColor(Color.rgb(55, 65, 81));
        preview.setPadding(12, 10, 12, 10);
        preview.setBackgroundColor(Color.rgb(239, 246, 255));
        preview.setMinHeight(80);
        root.addView(preview);

        // Action buttons
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, 10, 0, 0);

        loadButton = button("🚀 LOAD ALL");
        nextButton = button("COPY NEXT");
        stopButton = button("STOP");
        stopButton.setEnabled(false);

        buttons.addView(loadButton, new LinearLayout.LayoutParams(0, -2, 1));
        buttons.addView(nextButton, new LinearLayout.LayoutParams(0, -2, 1));
        buttons.addView(stopButton, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(buttons);

        loadButton.setOnClickListener(v -> loadAll());
        nextButton.setOnClickListener(v -> copyNext());
        stopButton.setOnClickListener(v -> stopLoading());

        // Settings row
        LinearLayout settingsRow = new LinearLayout(this);
        settingsRow.setOrientation(LinearLayout.HORIZONTAL);
        settingsRow.setPadding(0, 8, 0, 0);

        Button saveBtn = button("💾 Save Settings");
        saveBtn.setOnClickListener(v -> saveSettings());
        settingsRow.addView(saveBtn, new LinearLayout.LayoutParams(0, -2, 1));

        Button copyColBtn = button("📋 Copy Column Name");
        copyColBtn.setOnClickListener(v -> copyColumnName());
        settingsRow.addView(copyColBtn, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(settingsRow);

        // Clipboard Manager section
        root.addView(label("3. Clipboard Manager (devdnua.clipboard)"));
        TextView cmHint = new TextView(this);
        cmHint.setText("Open the app → go to Categories → create a new category with the column name. Then use LOAD ALL to fill notes.");
        cmHint.setTextSize(12);
        cmHint.setTextColor(Color.GRAY);
        cmHint.setPadding(0, 0, 0, 6);
        root.addView(cmHint);

        LinearLayout cmRow = new LinearLayout(this);
        cmRow.setOrientation(LinearLayout.HORIZONTAL);

        Button openCm = button("📂 Open Clipboard Manager");
        openCm.setOnClickListener(v -> openClipboardManager());
        cmRow.addView(openCm, new LinearLayout.LayoutParams(0, -2, 1));

        Button openCats = button("📁 Try Categories");
        openCats.setOnClickListener(v -> openClipboardManagerCategories());
        cmRow.addView(openCats, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(cmRow);

        // Keyboard section
        root.addView(label("4. Keyboard / Input options"));
        LinearLayout kbRow = new LinearLayout(this);
        kbRow.setOrientation(LinearLayout.HORIZONTAL);

        Button imePicker = button("⌨️ Switch Keyboard");
        imePicker.setOnClickListener(v -> showInputMethodPicker());
        kbRow.addView(imePicker, new LinearLayout.LayoutParams(0, -2, 1));

        Button imeSettings = button("⚙️ Input Settings");
        imeSettings.setOnClickListener(v -> openInputSettings());
        kbRow.addView(imeSettings, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(kbRow);

        // Status
        status = new TextView(this);
        status.setText("Ready. Parse a table to begin.");
        status.setTextSize(13);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0, 14, 0, 0);
        root.addView(status);

        scroll.addView(root);
        setContentView(scroll);
    }

    // ---------- Settings ----------
    private void saveSettings() {
        SharedPreferences.Editor ed = prefs.edit();
        try {
            ed.putLong(KEY_DELAY, Long.parseLong(delay.getText().toString().trim()));
        } catch (Exception ignored) {}
        try {
            ed.putInt(KEY_START_ROW, Integer.parseInt(startRow.getText().toString().trim()));
        } catch (Exception ignored) {}
        if (columnSpinner.getSelectedItemPosition() >= 0) {
            ed.putInt(KEY_COLUMN, columnSpinner.getSelectedItemPosition());
        }
        ed.apply();
        status.setText("Settings saved (delay, start row, last column).");
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void restoreSettings() {
        long d = prefs.getLong(KEY_DELAY, 1200);
        int sr = prefs.getInt(KEY_START_ROW, 2);
        delay.setText(String.valueOf(d));
        startRow.setText(String.valueOf(sr));
    }

    // ---------- Parsing & Preview ----------
    private void parseTable() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            status.setText("Paste your Excel/Sheets table first.");
            return;
        }

        rows.clear();
        String[] lines = text.split("\\r?\\n");
        int max = 0;

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] cells = line.split("\\t", -1);
            if (cells.length == 1) cells = line.trim().split("\\s{2,}", -1);
            rows.add(cells);
            max = Math.max(max, cells.length);
        }

        if (rows.isEmpty()) {
            status.setText("No valid rows found.");
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            String[] old = rows.get(i);
            String[] fixed = new String[max];
            for (int j = 0; j < max; j++) {
                fixed[j] = j < old.length ? old[j].trim() : "";
            }
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

        // restore last column if possible
        int lastCol = prefs.getInt(KEY_COLUMN, 0);
        if (lastCol >= 0 && lastCol < max) {
            columnSpinner.setSelection(lastCol);
        }

        nextIndex = 0;
        prepareValues();
        updatePreview();
        status.setText("Found " + rows.size() + " rows × " + max + " columns. Select a column.");
    }

    private void updatePreview() {
        if (rows.isEmpty() || columnSpinner.getSelectedItemPosition() < 0) {
            preview.setText("Parse a table and select a column to see sample values here.");
            return;
        }

        int col = columnSpinner.getSelectedItemPosition();
        String colName = columnSpinner.getSelectedItem() != null
                ? columnSpinner.getSelectedItem().toString() : ("Column " + (col + 1));

        int start = Math.min(getStartRow() - 1, rows.size());
        List<String> samples = new ArrayList<>();
        for (int i = start; i < rows.size() && samples.size() < 8; i++) {
            String v = rows.get(i)[col];
            if (v != null && !v.trim().isEmpty()) {
                samples.add(v.trim());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Column: ").append(colName).append("\n");
        sb.append("Samples (").append(samples.size()).append(" shown):\n");
        if (samples.isEmpty()) {
            sb.append("(no non-empty values from start row)");
        } else {
            for (int i = 0; i < samples.size(); i++) {
                sb.append("• ").append(samples.get(i));
                if (i < samples.size() - 1) sb.append("\n");
            }
        }
        preview.setText(sb.toString());
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
        selectedValues = new ArrayList<>();
        if (rows.isEmpty() || columnSpinner.getSelectedItemPosition() < 0) return;
        int col = columnSpinner.getSelectedItemPosition();
        int start = Math.min(getStartRow() - 1, rows.size());

        for (int i = start; i < rows.size(); i++) {
            String value = rows.get(i)[col];
            if (value != null && !value.trim().isEmpty()) {
                selectedValues.add(value.trim());
            }
        }
        nextIndex = 0;
        status.setText(selectedValues.size() + " clipboard items ready from column \""
                + (columnSpinner.getSelectedItem() != null ? columnSpinner.getSelectedItem() : "?") + "\".");
    }

    // ---------- Clipboard actions ----------
    private void copyToClipboard(String value) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Phone Specs", value));
    }

    private void copyColumnName() {
        if (columnSpinner.getSelectedItem() == null) {
            status.setText("Select a column first.");
            return;
        }
        String name = columnSpinner.getSelectedItem().toString();
        copyToClipboard(name);
        status.setText("Column name copied: \"" + name + "\" — paste it as a new Category in Clipboard Manager.");
        Toast.makeText(this, "Column name copied", Toast.LENGTH_SHORT).show();
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
            @Override
            public void run() {
                if (!loading || i[0] >= selectedValues.size()) {
                    loading = false;
                    loadButton.setEnabled(true);
                    nextButton.setEnabled(true);
                    stopButton.setEnabled(false);
                    status.setText("Finished. " + selectedValues.size() + " clipboard items sent.");
                    return;
                }

                String value = selectedValues.get(i[0]);
                copyToClipboard(value);
                i[0]++;
                nextIndex = i[0];
                status.setText("Loading: " + i[0] + " / " + selectedValues.size() + "\n" + value);
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

    // ---------- Clipboard Manager integration ----------
    private boolean isAppInstalled(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void openClipboardManager() {
        if (!isAppInstalled(CLIPBOARD_PKG)) {
            status.setText("Clipboard Manager (devdnua.clipboard) not installed.");
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + CLIPBOARD_PKG)));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + CLIPBOARD_PKG)));
            }
            return;
        }

        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(CLIPBOARD_PKG);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                status.setText("Opened Clipboard Manager. Go to Categories → create new category.");
            } else {
                status.setText("Could not launch Clipboard Manager.");
            }
        } catch (Exception e) {
            status.setText("Error opening Clipboard Manager: " + e.getMessage());
        }
    }

    private void openClipboardManagerCategories() {
        if (!isAppInstalled(CLIPBOARD_PKG)) {
            openClipboardManager();
            return;
        }

        // Try a few common patterns; most third-party apps block deep links.
        // We attempt known-style component names and fall back to main launcher.
        String[] candidates = {
                CLIPBOARD_PKG + ".ui.CategoriesActivity",
                CLIPBOARD_PKG + ".CategoriesActivity",
                CLIPBOARD_PKG + ".activity.CategoriesActivity",
                CLIPBOARD_PKG + ".ui.category.CategoryListActivity",
                CLIPBOARD_PKG + ".CategoryActivity"
        };

        for (String cls : candidates) {
            try {
                Intent intent = new Intent();
                intent.setClassName(CLIPBOARD_PKG, cls);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                status.setText("Opened Categories screen (if supported by the app).");
                return;
            } catch (Exception ignored) {}
        }

        // Fallback
        openClipboardManager();
        status.setText("Direct Categories screen not exposed. Opened main app — tap Categories in the menu.");
    }

    // ---------- Keyboard helpers ----------
    private void showInputMethodPicker() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
                status.setText("Keyboard picker opened.");
            }
        } catch (Exception e) {
            status.setText("Could not open keyboard picker.");
        }
    }

    private void openInputSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
            status.setText("Opened Input Method settings.");
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(intent);
            } catch (Exception ex) {
                status.setText("Could not open settings.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loading = false;
        handler.removeCallbacksAndMessages(null);
    }
}
