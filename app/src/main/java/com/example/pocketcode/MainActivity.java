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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;

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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.json.JSONArray;

public class MainActivity extends AppCompatActivity {

    // UI Components - Simplified
    private Toolbar toolbar;
    private ImageButton newFileBtn, openBtn, saveBtn, showInputBtn, clearConsoleBtn, closeInputBtn;
    private Button runBtn;
    private Spinner langSpinner;
    private EditText codeEditor, inputField;
    private TextView outputConsole, fileNameText, cursorPositionText, statusText, lineNumbers;
    private ProgressBar progressBar;
    private LinearLayout inputSection;
    private WebView webView;

    // Variables
    private String currentFileName = "Untitled";
    private String[] supportedLanguages = {"Java", "Python", "JavaScript", "C++", "C", "HTML", "CSS"};
    private String currentLanguage = "Java";
    private StringBuilder consoleOutput = new StringBuilder();
    private boolean isExecuting = false;

    // File operations
    private ActivityResultLauncher<String> openFileLauncher;
    private ActivityResultLauncher<String> saveFileLauncher;

    // API execution helper
    private CodeExecutionAPI codeExecutionAPI;

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

        codeExecutionAPI = new CodeExecutionAPI();

        initializeViews();
        setupToolbar();
        setupSpinner();
        setupFileOperations();
        setupCodeEditor();
        setupEventListeners();
        setupWebView();
        updateUI();
    }

    private void initializeViews() {
        // UI components matching the simplified layout
        toolbar = findViewById(R.id.toolbar);
        newFileBtn = findViewById(R.id.newFileBtn);
        openBtn = findViewById(R.id.openBtn);
        saveBtn = findViewById(R.id.saveBtn);
        runBtn = findViewById(R.id.runBtn);
        langSpinner = findViewById(R.id.langSpinner);
        codeEditor = findViewById(R.id.codeEditor);
        outputConsole = findViewById(R.id.outputConsole);
        fileNameText = findViewById(R.id.fileNameText);
        cursorPositionText = findViewById(R.id.cursorPositionText);
        statusText = findViewById(R.id.statusText);
        lineNumbers = findViewById(R.id.lineNumbers);
        progressBar = findViewById(R.id.progressBar);

        // Input section components
        inputSection = findViewById(R.id.inputSection);
        inputField = findViewById(R.id.inputField);
        showInputBtn = findViewById(R.id.showInputBtn);
        clearConsoleBtn = findViewById(R.id.clearConsoleBtn);
        closeInputBtn = findViewById(R.id.closeInputBtn);

        // WebView for HTML/CSS/JS
        webView = new WebView(this);
        webView.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("PocketCode");
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
                updateStatusText("Language: " + currentLanguage);
                updateSampleCode();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFileOperations() {
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

        updateSampleCode();
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript("document.title", value -> {
                    if (value != null && value.startsWith("\"OUTPUT:")) {
                        String output = value.substring(8, value.length() - 1);
                        if (!output.equals("Code executed successfully") && !output.isEmpty()) {
                            appendToConsole(output);
                        }
                    }
                });
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                appendToConsole(consoleMessage.message());
                return true;
            }
        });
    }

    private void setupEventListeners() {
        newFileBtn.setOnClickListener(v -> createNewFile());
        openBtn.setOnClickListener(v -> openFileLauncher.launch("*/*"));
        saveBtn.setOnClickListener(v -> saveFileLauncher.launch(currentFileName + getFileExtension()));
        runBtn.setOnClickListener(v -> executeCode());

        // Input section controls
        showInputBtn.setOnClickListener(v -> toggleInputSection());
        clearConsoleBtn.setOnClickListener(v -> clearConsole());
        closeInputBtn.setOnClickListener(v -> hideInputSection());
    }

    private void toggleInputSection() {
        if (inputSection.getVisibility() == View.GONE) {
            inputSection.setVisibility(View.VISIBLE);
            updateStatusText("Input section opened");
        } else {
            inputSection.setVisibility(View.GONE);
            updateStatusText("Input section closed");
        }
    }

    private void hideInputSection() {
        inputSection.setVisibility(View.GONE);
        updateStatusText("Input section closed");
    }

    // Console methods
    private void appendToConsole(String text) {
        runOnUiThread(() -> {
            consoleOutput.append(text).append("\n");
            outputConsole.setText(consoleOutput.toString());
        });
    }

    private void clearConsole() {
        consoleOutput.setLength(0);
        outputConsole.setText("Console cleared.\n\nReady to run code...");
        updateStatusText("Console cleared");
    }

    // File operations
    private void createNewFile() {
        if (isExecuting) return;
        codeEditor.setText("");
        currentFileName = "Untitled";
        clearConsole();
        hideInputSection();
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

    private void executeCode() {
        if (isExecuting) return;

        showProgress(true);
        isExecuting = true;
        runBtn.setEnabled(false);

        clearConsole();
        String code = codeEditor.getText().toString();

        if (code.trim().isEmpty()) {
            appendToConsole("Error: No code to execute");
            resetExecutionState();
            return;
        }

        // Get input from input field if available
        String input = "";
        if (inputField.getText() != null && !inputField.getText().toString().trim().isEmpty()) {
            input = inputField.getText().toString();
            appendToConsole("Using provided input:");
            String[] inputLines = input.split("\n");
            for (String line : inputLines) {
                appendToConsole("  > " + line);
            }
            appendToConsole("");
        }

        appendToConsole("=== " + currentLanguage + " Execution Started ===");
        updateStatusText("Executing " + currentLanguage + " code...");

        // Route execution based on language
        switch (currentLanguage) {
            case "HTML":
                executeHTML(code);
                break;
            case "CSS":
                executeCSS(code);
                break;
            case "JavaScript":
                if (code.contains("console.log") || code.contains("document.")) {
                    executeJavaScript(code);
                } else {
                    executeWithAPI(code, input);
                }
                break;
            default:
                executeWithAPI(code, input);
                break;
        }
    }

    private void executeWithAPI(String code, String input) {
        codeExecutionAPI.executeWithPiston(code, currentLanguage, new CodeExecutionCallback() {
            @Override
            public void onSuccess(String output) {
                runOnUiThread(() -> {
                    appendToConsole(output.isEmpty() ? "Code executed successfully (no output)" : output);
                    appendToConsole("=== Execution Completed ===");
                    resetExecutionState();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    appendToConsole("ERROR: " + error);
                    appendToConsole("=== Execution Failed ===");
                    resetExecutionState();
                });
            }
        }, input);
    }

    private void resetExecutionState() {
        runOnUiThread(() -> {
            isExecuting = false;
            runBtn.setEnabled(true);
            showProgress(false);
            updateStatusText("Ready");
        });
    }

    // WebView execution methods
    private void executeJavaScript(String code) {
        runOnUiThread(() -> {
            String wrappedCode =
                    "<html><head></head><body>" +
                            "<script>" +
                            "var originalLog = console.log;\n" +
                            "console.log = function(msg) {\n" +
                            "    window.parent.postMessage('OUTPUT:' + msg, '*');\n" +
                            "    originalLog.apply(console, arguments);\n" +
                            "};\n" +
                            "try {\n" +
                            code + "\n" +
                            "} catch(e) {\n" +
                            "    console.log('Error: ' + e.message);\n" +
                            "}\n" +
                            "</script>" +
                            "</body></html>";
            webView.loadDataWithBaseURL(null, wrappedCode, "text/html", "UTF-8", null);
        });

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            resetExecutionState();
        }).start();
    }

    private void executeHTML(String code) {
        runOnUiThread(() -> webView.loadDataWithBaseURL(null, code, "text/html", "UTF-8", null));
        appendToConsole("HTML rendered in background WebView");
        resetExecutionState();
    }

    private void executeCSS(String code) {
        String html = "<!DOCTYPE html><html><head><style>" + code +
                "</style></head><body><h1>CSS Test</h1><p class='test'>Test paragraph.</p></body></html>";
        runOnUiThread(() -> webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null));
        appendToConsole("CSS applied and rendered in background WebView");
        resetExecutionState();
    }

    // Helper methods
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
        cursorPositionText.setText(String.format("Line %d, Column %d", lineNumber, columnNumber));
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
                        "        // Add your code here\n" +
                        "    }\n" +
                        "}";

            case "Python":
                return "# Python Sample Code\n" +
                        "print(\"Hello, World!\")\n" +
                        "# Add your code here";

            case "JavaScript":
                return "// JavaScript Sample Code\n" +
                        "console.log(\"Hello, World!\");\n" +
                        "// Add your code here";

            case "C":
                return "#include <stdio.h>\n\n" +
                        "int main() {\n" +
                        "    printf(\"Hello, World!\\n\");\n" +
                        "    return 0;\n" +
                        "}";

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
                        "    <title>My Page</title>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <h1>Hello, World!</h1>\n" +
                        "    <p>Welcome to my webpage!</p>\n" +
                        "</body>\n" +
                        "</html>";

            case "CSS":
                return "/* CSS Sample Code */\n" +
                        "body {\n" +
                        "    font-family: Arial, sans-serif;\n" +
                        "    background-color: #f0f0f0;\n" +
                        "    margin: 20px;\n" +
                        "}\n\n" +
                        "h1 {\n" +
                        "    color: #333;\n" +
                        "    text-align: center;\n" +
                        "}";

            default:
                return "// " + language + " Sample Code\n" +
                        "// Add your code here";
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
        fileNameText.setText(currentFileName + getFileExtension());
        updateLineNumbers();
        updateCursorPosition();
    }

    // API class for code execution
    private class CodeExecutionAPI {
        private static final String PISTON_BASE_URL = "https://emkc.org/api/v2/piston";

        private String getPistonLanguage(String language) {
            switch (language.toLowerCase()) {
                case "java": return "java";
                case "python": return "python";
                case "javascript": return "javascript";
                case "c++": return "cpp";
                case "c": return "c";
                default: return "python";
            }
        }

        public void executeWithPiston(String code, String language, CodeExecutionCallback callback, String input) {
            new Thread(() -> {
                try {
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("language", getPistonLanguage(language));
                    requestBody.put("version", "*");

                    if (input != null && !input.isEmpty()) {
                        requestBody.put("stdin", input);
                    }

                    JSONArray filesArray = new JSONArray();
                    JSONObject file = new JSONObject();
                    file.put("content", code);
                    filesArray.put(file);
                    requestBody.put("files", filesArray);

                    URL url = new URL(PISTON_BASE_URL + "/execute");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(15000);

                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] inputBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                        os.write(inputBytes, 0, inputBytes.length);
                    }

                    int responseCode = connection.getResponseCode();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            responseCode >= 200 && responseCode < 300 ?
                                    connection.getInputStream() : connection.getErrorStream()));

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    if (responseCode >= 200 && responseCode < 300) {
                        JSONObject result = new JSONObject(response.toString());
                        JSONObject run = result.optJSONObject("run");
                        if (run != null) {
                            String output = run.optString("stdout", "");
                            String error = run.optString("stderr", "");
                            int exitCode = run.optInt("code", 0);

                            if (exitCode != 0 || !error.isEmpty()) {
                                callback.onError(error.isEmpty() ? "Process exited with code: " + exitCode : error);
                            } else {
                                callback.onSuccess(output.isEmpty() ? "Code executed successfully (no output)" : output);
                            }
                        } else {
                            callback.onError("Invalid response format");
                        }
                    } else {
                        callback.onError("API Error (HTTP " + responseCode + ")");
                    }

                } catch (Exception e) {
                    callback.onError("Error: " + e.getMessage());
                }
            }).start();
        }
    }

    // Interface for code execution callback
    interface CodeExecutionCallback {
        void onSuccess(String output);
        void onError(String error);
    }
}