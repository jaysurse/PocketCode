package com.example.pocketcode;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private Toolbar toolbar;
    private ImageButton newFileBtn, openBtn, saveBtn, formatBtn;
    private Button runBtn;
    private Spinner langSpinner;
    private EditText codeEditor;
    private TextView outputConsole, fileNameText, cursorPositionText,
            encodingText, statusText, memoryUsage, lineNumbers;
    private ProgressBar progressBar;

    // Variables
    private String currentFileName = "Untitled";
    private String[] supportedLanguages = {"Java", "Python", "JavaScript", "C++", "C", "HTML", "CSS"};
    private String currentLanguage = "Java";

    // File operations
    private ActivityResultLauncher<String> openFileLauncher;
    private ActivityResultLauncher<String> saveFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupToolbar();
        setupSpinner();
        setupFileOperations();
        setupCodeEditor();
        setupEventListeners();
        updateUI();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        newFileBtn = findViewById(R.id.newFileBtn);
        openBtn = findViewById(R.id.openBtn);
        saveBtn = findViewById(R.id.saveBtn);
        formatBtn = findViewById(R.id.formatBtn);
        runBtn = findViewById(R.id.runBtn);
        langSpinner = findViewById(R.id.langSpinner);
        codeEditor = findViewById(R.id.codeEditor);
        outputConsole = findViewById(R.id.outputConsole);
        fileNameText = findViewById(R.id.fileNameText);
        cursorPositionText = findViewById(R.id.cursorPositionText);
        encodingText = findViewById(R.id.encodingText);
        statusText = findViewById(R.id.statusText);
        memoryUsage = findViewById(R.id.memoryUsage);
        lineNumbers = findViewById(R.id.lineNumbers);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("PocketCode IDE");
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, supportedLanguages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        langSpinner.setAdapter(adapter);

        langSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLanguage = supportedLanguages[position];
                updateStatusText("Language changed to " + currentLanguage);
                updateSampleCode();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFileOperations() {
        // Initialize file operation launchers
        openFileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        openFile(uri);
                    }
                }
        );

        saveFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/*"),
                uri -> {
                    if (uri != null) {
                        saveFile(uri);
                    }
                }
        );
    }

    private void setupCodeEditor() {
        codeEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLineNumbers();
                updateCursorPosition();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Set initial sample code
        updateSampleCode();
    }

    private void setupEventListeners() {
        newFileBtn.setOnClickListener(v -> createNewFile());
        openBtn.setOnClickListener(v -> openFileLauncher.launch("*/*"));
        saveBtn.setOnClickListener(v -> saveFileLauncher.launch(currentFileName + getFileExtension()));
        formatBtn.setOnClickListener(v -> formatCode());
        runBtn.setOnClickListener(v -> runCode());
    }

    private void createNewFile() {
        codeEditor.setText("");
        currentFileName = "Untitled";
        updateStatusText("New file created");
        updateUI();
    }

    private void openFile(Uri uri) {
        try {
            showProgress(true);
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            codeEditor.setText(stringBuilder.toString());
            currentFileName = getFileName(uri);
            updateStatusText("File opened: " + currentFileName);
            reader.close();

        } catch (IOException e) {
            updateStatusText("Error opening file: " + e.getMessage());
            Toast.makeText(this, "Failed to open file", Toast.LENGTH_SHORT).show();
        } finally {
            showProgress(false);
            updateUI();
        }
    }

    private void saveFile(Uri uri) {
        try {
            showProgress(true);
            FileOutputStream fileOutputStream = (FileOutputStream) getContentResolver().openOutputStream(uri);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

            outputStreamWriter.write(codeEditor.getText().toString());
            outputStreamWriter.close();

            currentFileName = getFileName(uri);
            updateStatusText("File saved: " + currentFileName);
            Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            updateStatusText("Error saving file: " + e.getMessage());
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
        } finally {
            showProgress(false);
            updateUI();
        }
    }

    private void formatCode() {
        String code = codeEditor.getText().toString();
        String formattedCode = performBasicFormatting(code);
        codeEditor.setText(formattedCode);
        updateStatusText("Code formatted");
        Toast.makeText(this, "Code formatted", Toast.LENGTH_SHORT).show();
    }

    private void runCode() {
        showProgress(true);
        String code = codeEditor.getText().toString();

        if (code.trim().isEmpty()) {
            outputConsole.setText("Error: No code to execute");
            showProgress(false);
            return;
        }

        // Simulate code execution based on language
        new Thread(() -> {
            try {
                Thread.sleep(1500); // Simulate execution time

                runOnUiThread(() -> {
                    String output = simulateCodeExecution(code, currentLanguage);
                    outputConsole.setText(output);
                    updateStatusText("Code executed");
                    showProgress(false);
                });

            } catch (InterruptedException e) {
                runOnUiThread(() -> {
                    outputConsole.setText("Execution interrupted");
                    showProgress(false);
                });
            }
        }).start();
    }

    private String simulateCodeExecution(String code, String language) {
        StringBuilder output = new StringBuilder();
        output.append("=== ").append(language).append(" Execution Result ===\n\n");

        switch (language) {
            case "Java":
                if (code.contains("System.out.print")) {
                    output.append("Hello, World!\n");
                    output.append("Java program executed successfully.\n");
                } else {
                    output.append("Java compilation successful.\n");
                }
                break;

            case "Python":
                if (code.contains("print")) {
                    output.append("Hello, World!\n");
                    output.append("Python script executed successfully.\n");
                } else {
                    output.append("Python script completed.\n");
                }
                break;

            case "JavaScript":
                if (code.contains("console.log")) {
                    output.append("Hello, World!\n");
                    output.append("JavaScript executed successfully.\n");
                } else {
                    output.append("JavaScript completed.\n");
                }
                break;

            default:
                output.append("Code executed successfully.\n");
                output.append("Language: ").append(language).append("\n");
                break;
        }

        output.append("\nExecution completed at: ").append(new java.util.Date().toString());
        return output.toString();
    }

    private String performBasicFormatting(String code) {
        // Basic code formatting
        return code.replaceAll("\\{", " {\n")
                .replaceAll("\\}", "}\n")
                .replaceAll(";", ";\n")
                .replaceAll("\\n\\s*\\n", "\n");
    }

    private void updateLineNumbers() {
        String text = codeEditor.getText().toString();
        int lines = text.split("\n").length;
        StringBuilder lineNumberText = new StringBuilder();

        for (int i = 1; i <= Math.max(lines, 20); i++) {
            lineNumberText.append(i).append("\n");
        }

        lineNumbers.setText(lineNumberText.toString());
    }

    private void updateCursorPosition() {
        int selectionStart = codeEditor.getSelectionStart();
        String textBeforeCursor = codeEditor.getText().toString().substring(0, selectionStart);
        String[] lines = textBeforeCursor.split("\n");
        int lineNumber = lines.length;
        int columnNumber = lines[lines.length - 1].length() + 1;

        cursorPositionText.setText(String.format("Ln %d, Col %d", lineNumber, columnNumber));
    }

    private void updateSampleCode() {
        String sampleCode = getSampleCode(currentLanguage);
        if (codeEditor.getText().toString().trim().isEmpty()) {
            codeEditor.setText(sampleCode);
        }
    }

    private String getSampleCode(String language) {
        switch (language) {
            case "Java":
                return "public class Main {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        System.out.println(\"Hello, World!\");\n" +
                        "    }\n" +
                        "}";

            case "Python":
                return "# Python Sample Code\n" +
                        "def main():\n" +
                        "    print(\"Hello, World!\")\n\n" +
                        "if __name__ == \"__main__\":\n" +
                        "    main()";

            case "JavaScript":
                return "// JavaScript Sample Code\n" +
                        "function main() {\n" +
                        "    console.log(\"Hello, World!\");\n" +
                        "}\n\n" +
                        "main();";

            case "C++":
                return "#include <iostream>\n" +
                        "using namespace std;\n\n" +
                        "int main() {\n" +
                        "    cout << \"Hello, World!\" << endl;\n" +
                        "    return 0;\n" +
                        "}";

            case "HTML":
                return "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "    <title>Hello World</title>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <h1>Hello, World!</h1>\n" +
                        "</body>\n" +
                        "</html>";

            default:
                return "// " + language + " Sample Code\n" +
                        "// Write your code here...";
        }
    }

    private String getFileExtension() {
        switch (currentLanguage) {
            case "Java": return ".java";
            case "Python": return ".py";
            case "JavaScript": return ".js";
            case "C++": return ".cpp";
            case "C": return ".c";
            case "HTML": return ".html";
            case "CSS": return ".css";
            default: return ".txt";
        }
    }

    private String getFileName(Uri uri) {
        String path = uri.getLastPathSegment();
        return path != null ? path : "Unknown";
    }

    private void updateStatusText(String message) {
        statusText.setText(message);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateUI() {
        fileNameText.setText(currentFileName);
        encodingText.setText("UTF-8");

        // Update memory usage (simulated)
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        memoryUsage.setText(String.format("Memory: %dMB", usedMemory / (1024 * 1024)));

        updateLineNumbers();
        updateCursorPosition();
    }
}