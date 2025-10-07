package com.example.coderunner;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import android.view.animation.Animation;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.Menu;
import android.view.MenuItem;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import okhttp3.*;
import org.json.JSONObject;
import java.io.*;

public class MainActivity extends AppCompatActivity {

    Spinner langSpinner;
    EditText codeInput, stdinInput;
    Button runButton, saveButton, openButton;
    TextView outputView;
    View runOverlay;
    ProgressBar overlayProgress;
    TextView overlayText;
    String[] languageIds;
    DrawerLayout drawerLayout;
    androidx.recyclerview.widget.RecyclerView drawerRecycler;
    com.example.coderunner.ui.db.SnippetAdapter drawerAdapter;

    // private final String CLIENT_ID = "ef9b6ba01486c49d61cdcb1af81d9e07";
    private final String CLIENT_ID = "d8add5b56ca67e483442f088aab7753e";

    // private final String CLIENT_SECRET =
    // "49e89521188427b9ef30232b5e9c954a5e11bd3dff169d479b33ad87cb469791";
    private final String CLIENT_SECRET = "76fa32950f1cd5ca590c0475e35eb74c452d9be7ce745491490a62431f127401";
    private final String API_URL = "https://api.jdoodle.com/v1/execute";

    private final int REQUEST_SAVE_FILE = 1;
    private final int REQUEST_OPEN_FILE = 2;
    private final int REQUEST_OPEN_SNIPPET = 3;
    private Uri saveUri;
    private static final String PREF_THEME = "pref_theme"; // values: light/dark

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme from preferences before view inflation
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String t = prefs.getString(PREF_THEME, "dark");
        if ("light".equals(t)) {
            setTheme(R.style.AppTheme_Light);
        } else {
            setTheme(R.style.AppTheme_Dark);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        langSpinner = findViewById(R.id.langSpinner);
        codeInput = findViewById(R.id.codeInput);
        stdinInput = findViewById(R.id.stdinInput);
        runButton = findViewById(R.id.runButton);
        saveButton = findViewById(R.id.saveButton);
        openButton = findViewById(R.id.openButton);
        outputView = findViewById(R.id.outputView);
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerRecycler = findViewById(R.id.snippetDrawerRecycler);
        drawerRecycler.setLayoutManager(new LinearLayoutManager(this));
        drawerAdapter = new com.example.coderunner.ui.db.SnippetAdapter(
                new java.util.ArrayList<com.example.coderunner.data.Snippet>(),
                new com.example.coderunner.ui.db.SnippetAdapter.OnSelect() {
                    @Override
                    public void onSelect(com.example.coderunner.data.Snippet s) {
                        runOnUiThread(() -> {
                            codeInput.setText(s.code == null ? "" : s.code);
                            selectLanguageById(s.languageId);
                            Toast.makeText(MainActivity.this,
                                    "Loaded: " + (s.title == null ? "Untitled" : s.title), Toast.LENGTH_SHORT).show();
                            drawerLayout.closeDrawers();
                        });
                    }
                },
                new com.example.coderunner.ui.db.SnippetAdapter.OnLongAction() {
                    @Override
                    public void onLongAction(com.example.coderunner.data.Snippet snippet, View anchor) {
                        runOnUiThread(() -> {
                            String[] opts = new String[] { "Rename", "Delete" };
                            new android.app.AlertDialog.Builder(MainActivity.this)
                                    .setTitle(snippet.title == null || snippet.title.isEmpty() ? "Untitled"
                                            : snippet.title)
                                    .setItems(opts, (dialog, which) -> {
                                        if (which == 0) {
                                            // no-op: user can tap the edit icon to rename inline
                                        } else if (which == 1) {
                                            new Thread(() -> {
                                                com.example.coderunner.data.SnippetDbHelper dbh = new com.example.coderunner.data.SnippetDbHelper(
                                                        MainActivity.this);
                                                dbh.deleteSnippet(snippet.id);
                                                java.io.File dir = new java.io.File(getExternalFilesDir(null),
                                                        "snippets");
                                                if (dir.exists()) {
                                                    java.io.File[] files = dir.listFiles();
                                                    if (files != null) {
                                                        for (java.io.File f : files) {
                                                            if (f.getName().startsWith("snippet_" + snippet.id + "_")) {
                                                                f.delete();
                                                            }
                                                        }
                                                    }
                                                }
                                                loadDrawerSnippets();
                                            }).start();
                                        }
                                    })
                                    .show();
                        });
                    }
                },
                new com.example.coderunner.ui.db.SnippetAdapter.OnAction() {
                    @Override
                    public void onRename(com.example.coderunner.data.Snippet snippet, String newTitle) {
                        snippet.title = newTitle;
                        snippet.lastEdited = System.currentTimeMillis();
                        new Thread(() -> {
                            com.example.coderunner.data.SnippetDbHelper dbh = new com.example.coderunner.data.SnippetDbHelper(
                                    MainActivity.this);
                            dbh.updateSnippet(snippet);
                            loadDrawerSnippets();
                        }).start();
                    }

                    @Override
                    public void onDelete(com.example.coderunner.data.Snippet snippet) {
                        // confirm deletion on UI thread
                        runOnUiThread(() -> {
                            new android.app.AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Delete snippet")
                                    .setMessage("Are you sure you want to delete this snippet?")
                                    .setPositiveButton("Delete", (dialog, which) -> {
                                        new Thread(() -> {
                                            com.example.coderunner.data.SnippetDbHelper dbh = new com.example.coderunner.data.SnippetDbHelper(
                                                    MainActivity.this);
                                            dbh.deleteSnippet(snippet.id);
                                            java.io.File dir = new java.io.File(getExternalFilesDir(null), "snippets");
                                            if (dir.exists()) {
                                                java.io.File[] files = dir.listFiles();
                                                if (files != null) {
                                                    for (java.io.File f : files) {
                                                        if (f.getName().startsWith("snippet_" + snippet.id + "_")) {
                                                            f.delete();
                                                        }
                                                    }
                                                }
                                            }
                                            runOnUiThread(MainActivity.this::loadDrawerSnippets);
                                        }).start();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });
                    }
                });
        drawerRecycler.setAdapter(drawerAdapter);

        // ...existing UI initialization...

        // Setup toolbar as action bar and fade in header
        try {
            androidx.appcompat.widget.Toolbar header = findViewById(R.id.header);
            // set as support action bar so menu works and we can set navigation icon
            try {
                setSupportActionBar(header);
                // use a simple menu/hamburger icon from AppCompat if available
                header.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_menu_overflow_material);
                header.setNavigationOnClickListener(v -> {
                    if (drawerLayout.isDrawerOpen(android.view.Gravity.START))
                        drawerLayout.closeDrawers();
                    else {
                        loadDrawerSnippets();
                        drawerLayout.openDrawer(android.view.Gravity.START);
                    }
                });
            } catch (Exception ignored) {
            }
            Animation fade = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in);
            header.startAnimation(fade);
        } catch (Exception ignored) {
        }

        // Overlay UI
        runOverlay = findViewById(R.id.runOverlay);
        overlayProgress = findViewById(R.id.overlayProgress);
        overlayText = findViewById(R.id.overlayText);

        // Load language ids from resources (must align with spinner order)
        languageIds = getResources().getStringArray(R.array.language_ids);

        // Output action buttons removed per request

        // Prefill from example extras (if any)
        Intent incoming = getIntent();
        if (incoming != null) {
            String exampleCode = incoming.getStringExtra("example_code");
            String exampleLang = incoming.getStringExtra("example_lang");
            if (exampleCode != null && !exampleCode.trim().isEmpty()) {
                codeInput.setText(exampleCode);
            }
            if (exampleLang != null && !exampleLang.trim().isEmpty()) {
                // spinner entries are defined in res/values/strings.xml as: Python, Java, C,
                // C++
                int index = 0; // default Python
                switch (exampleLang) {
                    case "Python":
                        index = 0;
                        break;
                    case "Java":
                        index = 1;
                        break;
                    case "C":
                        index = 2;
                        break;
                    case "C++":
                        index = 3;
                        break;
                    default:
                        index = 0;
                        break;
                }
                try {
                    langSpinner.setSelection(index);
                } catch (Exception ignored) {
                }
            }
        }

        // Run code
        runButton.setOnClickListener(v -> {
            String lang = langSpinner.getSelectedItem().toString();
            String code = codeInput.getText().toString();
            String stdin = stdinInput.getText().toString();

            if ((code.contains("input") || code.contains("scanf") || code.contains("cin") || code.contains("System.in"))
                    && stdin.trim().isEmpty()) {
                Toast.makeText(MainActivity.this,
                        "Your code seems to require input! Please enter values below.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Show overlay and disable inputs
            setRunningState(true);
            outputView.setText("");
            runCodeOnline(lang, code, stdin);
        });

        // Save: automatically save to DB and to a file inside app external files
        // directory
        saveButton.setOnClickListener(v -> {
            final String title = ""; // use empty title field; we can derive one from first line or timestamp
            final String code = codeInput.getText().toString();
            final String lang = getSelectedLanguageId();
            final long now = System.currentTimeMillis();

            // Build snippet object
            final com.example.coderunner.data.Snippet s = new com.example.coderunner.data.Snippet(title, lang, code,
                    now);

            // Save to DB and file on background thread
            new Thread(() -> {
                try {
                    com.example.coderunner.data.SnippetDbHelper dbh = new com.example.coderunner.data.SnippetDbHelper(
                            this);
                    long id = dbh.insertSnippet(s);

                    // Also write to external files dir under "snippets"
                    java.io.File dir = new java.io.File(getExternalFilesDir(null), "snippets");
                    if (!dir.exists())
                        dir.mkdirs();

                    String sanitized = "snippet_" + id + "_" + Long.toString(now);
                    java.io.File out = new java.io.File(dir, sanitized + ".txt");
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(out)) {
                        fos.write(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        fos.flush();
                    }

                    final long savedId = id;
                    runOnUiThread(() -> Toast.makeText(this,
                            "Saved to DB (id=" + savedId + ") and file: " + out.getName(), Toast.LENGTH_SHORT).show());
                } catch (Exception ex) {
                    runOnUiThread(
                            () -> Toast.makeText(this, "Save error: " + ex.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        });

        // Open: launch system file picker to open a file from device
        openButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] { "text/plain" });
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_OPEN_FILE);
        });
    }

    private void loadDrawerSnippets() {
        new Thread(() -> {
            com.example.coderunner.data.SnippetDbHelper dbh = new com.example.coderunner.data.SnippetDbHelper(this);
            java.util.List<com.example.coderunner.data.Snippet> list = dbh.getAllSnippets();
            runOnUiThread(() -> {
                drawerAdapter.setItems(list);
                if (list.isEmpty()) {
                    Toast.makeText(this, "No snippets saved yet.", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_theme) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String t = prefs.getString(PREF_THEME, "dark");
            String newTheme = "dark".equals(t) ? "light" : "dark";
            prefs.edit().putString(PREF_THEME, newTheme).apply();
            // Recreate activity to apply theme
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_OPEN_SNIPPET) {
                // snippet selected from list
                String code = data.getStringExtra("snippet_code");
                String lang = data.getStringExtra("snippet_lang");
                String title = data.getStringExtra("snippet_title");
                if (code != null)
                    codeInput.setText(code);
                if (lang != null)
                    selectLanguageById(lang);
                if (title != null && !title.isEmpty())
                    Toast.makeText(this, "Loaded: " + title, Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = data.getData();
            if (requestCode == REQUEST_SAVE_FILE) {
                saveUri = uri;
                try {
                    OutputStream os = getContentResolver().openOutputStream(saveUri);
                    os.write(codeInput.getText().toString().getBytes());
                    os.close();
                    Toast.makeText(this, "File saved successfully!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else if (requestCode == REQUEST_OPEN_FILE) {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    reader.close();
                    codeInput.setText(sb.toString());
                    Toast.makeText(this, "File opened successfully!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Error opening file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void runCodeOnline(String lang, String code, String stdin) {
        String jdoodleLang = mapLanguage(lang);
        String versionIndex = "0";

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        try {
            JSONObject json = new JSONObject();
            json.put("clientId", CLIENT_ID);
            json.put("clientSecret", CLIENT_SECRET);
            json.put("script", code);
            json.put("language", jdoodleLang);
            json.put("versionIndex", versionIndex);
            json.put("stdin", stdin);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        setRunningState(false);
                        outputView.setText("Network error: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String respBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        setRunningState(false);
                        try {
                            JSONObject res = new JSONObject(respBody);
                            String output = res.optString("output", respBody);

                            // Display input + output cleanly
                            String display = "";
                            if (!stdin.trim().isEmpty()) {
                                String[] inputs = stdin.split("\n");
                                String[] prompts = new String[] { "Enter first number: ", "Enter second number: " };
                                for (int i = 0; i < inputs.length && i < prompts.length; i++) {
                                    display += prompts[i] + inputs[i] + "\n";
                                }
                            }
                            display += "Output:\n" + output;
                            outputView.setText(display.trim());

                        } catch (Exception ex) {
                            outputView.setText("Parse error: " + ex.getMessage());
                        }
                    });
                }
            });

        } catch (Exception e) {
            setRunningState(false);
            outputView.setText("Error: " + e.getMessage());
        }
    }

    private String mapLanguage(String lang) {
        // Map using the languageIds array if possible
        try {
            int pos = langSpinner.getSelectedItemPosition();
            if (pos >= 0 && pos < languageIds.length)
                return languageIds[pos];
        } catch (Exception ignored) {
        }
        // fallback mapping
        switch (lang) {
            case "C":
                return "c";
            case "C++":
                return "cpp17";
            case "Java":
                return "java";
            case "Python":
                return "python3";
            default:
                return "python3";
        }
    }

    // Return the language id string for the currently selected spinner position
    private String getSelectedLanguageId() {
        try {
            int pos = langSpinner.getSelectedItemPosition();
            if (pos >= 0 && pos < languageIds.length)
                return languageIds[pos];
        } catch (Exception ignored) {
        }
        // fallback to map by displayed name
        return mapLanguage(langSpinner.getSelectedItem().toString());
    }

    // Select spinner entry by JDoodle language id (best-effort)
    private void selectLanguageById(String langId) {
        try {
            for (int i = 0; i < languageIds.length; i++) {
                if (languageIds[i].equalsIgnoreCase(langId)) {
                    langSpinner.setSelection(i);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void setRunningState(boolean running) {
        if (running) {
            runOverlay.setVisibility(View.VISIBLE);
            overlayProgress.setIndeterminate(true);
            runButton.setEnabled(false);
            saveButton.setEnabled(false);
            openButton.setEnabled(false);
            langSpinner.setEnabled(false);
            codeInput.setEnabled(false);
            stdinInput.setEnabled(false);
        } else {
            runOverlay.setVisibility(View.GONE);
            overlayProgress.setIndeterminate(false);
            runButton.setEnabled(true);
            saveButton.setEnabled(true);
            openButton.setEnabled(true);
            langSpinner.setEnabled(true);
            codeInput.setEnabled(true);
            stdinInput.setEnabled(true);
        }
    }
}
