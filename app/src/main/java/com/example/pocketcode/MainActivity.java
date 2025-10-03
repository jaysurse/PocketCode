package com.example.pocketcode;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.Python;
import com.chaquo.python.PyObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private EditText codeEditor;
    private TextView consoleOutput;
    private Spinner languageSpinner;
    private Button runButton, openButton, saveButton, newButton;

    // Language constants - Start with Python only
    private static final String[] LANGUAGES = {"Python"};
    private String currentLanguage = "Python";

    // File operation constants
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_OPEN_FILE = 1002;
    private static final int REQUEST_SAVE_FILE = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            initializeUI();
            setupLanguageSpinner();
            setupButtonClickListeners();
            requestStoragePermissions();
            showWelcomeMessage();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeUI() {
        try {
            codeEditor = findViewById(R.id.code_editor);
            consoleOutput = findViewById(R.id.console_output);
            languageSpinner = findViewById(R.id.language_spinner);
            runButton = findViewById(R.id.run_button);
            openButton = findViewById(R.id.open_button);
            saveButton = findViewById(R.id.save_button);
            newButton = findViewById(R.id.new_button);

            // Check if views were found
            if (codeEditor == null || consoleOutput == null || languageSpinner == null || 
                runButton == null || openButton == null || saveButton == null || newButton == null) {
                throw new RuntimeException("Some UI elements not found in layout");
            }

            // Make console scrollable
            consoleOutput.setMovementMethod(new ScrollingMovementMethod());

            // Set initial focus to code editor
            codeEditor.requestFocus();
            
            // Set initial template code
            codeEditor.setText("# Welcome to PocketCode!\nprint(\"Hello, Python World!\")\n\n# Try your Python code here:\nx = 10\ny = 20\nprint(f\"Sum: {x + y}\")");
            
        } catch (Exception e) {
            Toast.makeText(this, "UI Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupLanguageSpinner() {
        try {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, LANGUAGES);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            languageSpinner.setAdapter(adapter);

            languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentLanguage = LANGUAGES[position];
                    appendToConsole("Switched to " + currentLanguage);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Spinner Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupButtonClickListeners() {
        runButton.setOnClickListener(v -> runCode());
        openButton.setOnClickListener(v -> openFile());
        saveButton.setOnClickListener(v -> saveFile());
        newButton.setOnClickListener(v -> newFile());
    }

    private void showWelcomeMessage() {
        appendToConsole("PocketCode Mobile IDE");
        appendToConsole("Python coding on your phone!");
        appendToConsole("Click RUN to execute your code");
        appendToConsole("==============================");
    }

    private void runCode() {
        try {
            String code = codeEditor.getText().toString().trim();
            if (code.isEmpty()) {
                appendToConsole("No code to execute");
                return;
            }

            appendToConsole("\nRunning " + currentLanguage + " code...");
            appendToConsole("==============================");

            // For now, only Python is supported
            runPython(code);
        } catch (Exception e) {
            appendToConsole("Error: " + e.getMessage());
        }
    }

    private void runPython(String code) {
        new Thread(() -> {
            try {
                Python py = Python.getInstance();

                // Simple expression check
                if (isSimpleExpression(code)) {
                    try {
                        PyObject result = py.getModule("builtins").callAttr("eval", code);
                        runOnUiThread(() -> appendToConsole("➤ " + result.toString()));
                        return;
                    } catch (Exception ignored) {
                        // Fall through to exec
                    }
                }

                // Execute code
                py.getModule("builtins").callAttr("exec", code);
                runOnUiThread(() -> appendToConsole("✅ Python code executed successfully"));

            } catch (Exception e) {
                String error = e.getMessage();
                runOnUiThread(() -> {
                    if (error.contains("SyntaxError")) {
                        appendToConsole("❌ Syntax Error: Check your Python syntax");
                    } else if (error.contains("IndentationError")) {
                        appendToConsole("❌ Indentation Error: Check your code indentation");
                    } else if (error.contains("NameError")) {
                        appendToConsole("❌ Name Error: Variable not defined");
                    } else {
                        appendToConsole("❌ Error: " + error);
                    }
                });
            }
        }).start();
    }

    private boolean isSimpleExpression(String code) {
        return !code.contains("\n") && !code.contains("print") && !code.contains("=")
                && !code.startsWith("import") && !code.startsWith("for")
                && !code.startsWith("if") && !code.startsWith("def")
                && !code.startsWith("while") && !code.startsWith("try");
    }

    private void appendToConsole(String text) {
        runOnUiThread(() -> {
            try {
                consoleOutput.append(text + "\n");

                // Auto-scroll to bottom
                if (consoleOutput.getLayout() != null) {
                    int scrollAmount = consoleOutput.getLayout().getLineTop(consoleOutput.getLineCount())
                                     - consoleOutput.getHeight();
                    if (scrollAmount > 0) {
                        consoleOutput.scrollTo(0, scrollAmount);
                    } else {
                        consoleOutput.scrollTo(0, 0);
                    }
                }
            } catch (Exception e) {
                // Handle console error silently
            }
        });
    }    private void newFile() {
        codeEditor.setText("# Welcome to PocketCode!\nprint(\"Hello, Python World!\")\n\n# Write your Python code here:");
        consoleOutput.setText("");
        appendToConsole("New file created");
    }

    private void openFile() {
        if (!hasStoragePermission()) {
            requestStoragePermissions();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_OPEN_FILE);
    }

    private void saveFile() {
        if (!hasStoragePermission()) {
            requestStoragePermissions();
            return;
        }

        String extension = getFileExtension();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "code" + extension);
        startActivityForResult(intent, REQUEST_SAVE_FILE);
    }

    private String getFileExtension() {
        switch (currentLanguage) {
            case "Python":
                return ".py";
            case "C":
                return ".c";
            case "C++":
                return ".cpp";
            default:
                return ".txt";
        }
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermissions() {
        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                }, REQUEST_STORAGE_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appendToConsole("✅ Storage permissions granted");
            } else {
                appendToConsole("❌ Storage permissions denied");
                Toast.makeText(this, "Storage permissions needed for file operations",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                switch (requestCode) {
                    case REQUEST_OPEN_FILE:
                        openFileFromUri(uri);
                        break;
                    case REQUEST_SAVE_FILE:
                        saveFileToUri(uri);
                        break;
                }
            }
        }
    }

    private void openFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            reader.close();
            codeEditor.setText(content.toString());
            appendToConsole("📂 File opened successfully");

        } catch (IOException e) {
            appendToConsole("❌ Error opening file: " + e.getMessage());
        }
    }

    private void saveFileToUri(Uri uri) {
        try {
            FileOutputStream outputStream = (FileOutputStream) getContentResolver().openOutputStream(uri);
            String content = codeEditor.getText().toString();
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            outputStream.close();

            appendToConsole("💾 File saved successfully");
            Toast.makeText(this, "File saved!", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            appendToConsole("❌ Error saving file: " + e.getMessage());
        }
    }
}