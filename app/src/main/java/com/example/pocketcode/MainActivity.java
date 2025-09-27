package com.example.pocketcode;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;
import org.json.JSONArray;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private Toolbar toolbar;
    private ImageButton newFileBtn, openBtn, saveBtn, clearConsoleBtn;
    private Button runBtn, stopBtn;
    private Spinner langSpinner;
    private EditText codeEditor, consoleInputField;
    private TextView outputConsole, fileNameText, cursorPositionText, statusText,
            lineNumbers, consoleStatusText, inputPromptText;
    private LinearLayout inputPromptLayout;
    private ScrollView consoleScrollView;
    private ProgressBar progressBar;
    private WebView webView;

    // Variables
    private String currentFileName = "Untitled";
    private String[] supportedLanguages = {"Java", "Python", "JavaScript", "C++", "C", "HTML", "CSS"};
    private String currentLanguage = "Java";
    private StringBuilder consoleOutput = new StringBuilder();
    private boolean isExecuting = false;
    private boolean isWaitingForInput = false;

    // Interactive execution
    private InteractiveExecutor currentExecutor = null;
    private BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

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
        toolbar = findViewById(R.id.toolbar);
        newFileBtn = findViewById(R.id.newFileBtn);
        openBtn = findViewById(R.id.openBtn);
        saveBtn = findViewById(R.id.saveBtn);
        runBtn = findViewById(R.id.runBtn);
        stopBtn = findViewById(R.id.stopBtn);
        langSpinner = findViewById(R.id.langSpinner);
        codeEditor = findViewById(R.id.codeEditor);
        outputConsole = findViewById(R.id.outputConsole);
        fileNameText = findViewById(R.id.fileNameText);
        cursorPositionText = findViewById(R.id.cursorPositionText);
        statusText = findViewById(R.id.statusText);
        lineNumbers = findViewById(R.id.lineNumbers);
        progressBar = findViewById(R.id.progressBar);

        // Interactive console components
        clearConsoleBtn = findViewById(R.id.clearConsoleBtn);
        consoleStatusText = findViewById(R.id.consoleStatusText);
        inputPromptLayout = findViewById(R.id.inputPromptLayout);
        consoleInputField = findViewById(R.id.consoleInputField);
        inputPromptText = findViewById(R.id.inputPromptText);
        consoleScrollView = findViewById(R.id.consoleScrollView);

        // WebView for HTML/CSS/JS
        webView = new WebView(this);
        webView.setVisibility(View.GONE);
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
        stopBtn.setOnClickListener(v -> stopExecution());
        clearConsoleBtn.setOnClickListener(v -> clearConsole());

        // Interactive console input handler
        consoleInputField.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (isWaitingForInput) {
                    sendInputToProgram();
                }
                return true;
            }
            return false;
        });
    }

    // Interactive Console Methods
    private void showInputPrompt(String prompt) {
        runOnUiThread(() -> {
            isWaitingForInput = true;
            inputPromptText.setText(prompt.isEmpty() ? "➤ " : prompt + " ");
            inputPromptLayout.setVisibility(View.VISIBLE);
            consoleInputField.setText("");
            consoleInputField.requestFocus();
            updateConsoleStatus("Waiting for input...");
            scrollConsoleToBottom();
        });
    }

    private void hideInputPrompt() {
        runOnUiThread(() -> {
            isWaitingForInput = false;
            inputPromptLayout.setVisibility(View.GONE);
            updateConsoleStatus("Running...");
        });
    }

    private void sendInputToProgram() {
        String input = consoleInputField.getText().toString();

        // Show the input in console (like real IDEs do)
        appendToConsole(inputPromptText.getText().toString() + input);

        // Send input to the executor
        if (currentExecutor != null) {
            currentExecutor.provideInput(input);
        }

        hideInputPrompt();
    }

    private void updateConsoleStatus(String status) {
        runOnUiThread(() -> consoleStatusText.setText(status));
    }

    // Console methods
    private void appendToConsole(String text) {
        runOnUiThread(() -> {
            consoleOutput.append(text).append("\n");
            outputConsole.setText(consoleOutput.toString());
            scrollConsoleToBottom();
        });
    }

    private void scrollConsoleToBottom() {
        consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void clearConsole() {
        consoleOutput.setLength(0);
        outputConsole.setText("Console cleared.\n\nInteractive Console Ready\n• Run code to see output here\n• Type input when prompted\n• Press Enter to send input");
        hideInputPrompt();
        updateConsoleStatus("Ready");
        updateStatusText("Console cleared");
    }

    // File operations
    private void createNewFile() {
        if (isExecuting) stopExecution();
        codeEditor.setText("");
        currentFileName = "Untitled";
        clearConsole();
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
        stopBtn.setEnabled(true);
        updateConsoleStatus("Starting...");

        clearConsole();
        String code = codeEditor.getText().toString();

        if (code.trim().isEmpty()) {
            appendToConsole("Error: No code to execute");
            resetExecutionState();
            return;
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
                    startInteractiveExecution(code);
                }
                break;
            default:
                startInteractiveExecution(code);
                break;
        }
    }

    private void startInteractiveExecution(String code) {
        currentExecutor = new InteractiveExecutor(code, currentLanguage, new ExecutionCallback() {
            @Override
            public void onOutput(String output) {
                appendToConsole(output);
            }

            @Override
            public void onError(String error) {
                appendToConsole("ERROR: " + error);
                resetExecutionState();
            }

            @Override
            public void onInputRequired(String prompt) {
                showInputPrompt(prompt);
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    appendToConsole("=== Execution Completed ===");
                    resetExecutionState();
                });
            }
        });

        currentExecutor.execute();
    }

    private void stopExecution() {
        if (currentExecutor != null) {
            currentExecutor.stop();
            currentExecutor = null;
        }
        resetExecutionState();
        appendToConsole("\n=== Execution Stopped ===");
    }

    private void resetExecutionState() {
        runOnUiThread(() -> {
            isExecuting = false;
            runBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            hideInputPrompt();
            showProgress(false);
            updateConsoleStatus("Ready");
            updateStatusText("Ready");
        });
    }

    // WebView execution methods (unchanged)
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

    // Helper methods (unchanged)
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
                return "import java.util.Scanner;\n\n" +
                        "public class Main {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        Scanner scanner = new Scanner(System.in);\n" +
                        "        \n" +
                        "        System.out.print(\"Enter your name: \");\n" +
                        "        String name = scanner.nextLine();\n" +
                        "        \n" +
                        "        System.out.print(\"Enter your age: \");\n" +
                        "        int age = scanner.nextInt();\n" +
                        "        \n" +
                        "        System.out.println(\"Hello \" + name + \", you are \" + age + \" years old!\");\n" +
                        "        \n" +
                        "        scanner.close();\n" +
                        "    }\n" +
                        "}";

            case "Python":
                return "# Interactive Python Example\n" +
                        "print(\"=== Python Interactive Demo ===\")\n" +
                        "name = input(\"Enter your name: \")\n" +
                        "age = int(input(\"Enter your age: \"))\n" +
                        "city = input(\"Enter your city: \")\n\n" +
                        "print(f\"Hello {name}!\")\n" +
                        "print(f\"You are {age} years old and live in {city}\")\n\n" +
                        "# Loop example\n" +
                        "count = int(input(\"How many times to count? \"))\n" +
                        "for i in range(count):\n" +
                        "    print(f\"Count: {i + 1}\")\n\n" +
                        "print(\"Program completed!\")";

            case "C":
                return "#include <stdio.h>\n\n" +
                        "int main() {\n" +
                        "    char name[100];\n" +
                        "    int age;\n" +
                        "    \n" +
                        "    printf(\"Enter your name: \");\n" +
                        "    scanf(\"%s\", name);\n" +
                        "    \n" +
                        "    printf(\"Enter your age: \");\n" +
                        "    scanf(\"%d\", &age);\n" +
                        "    \n" +
                        "    printf(\"Hello %s, you are %d years old!\\n\", name, age);\n" +
                        "    \n" +
                        "    return 0;\n" +
                        "}";

            case "C++":
                return "#include <iostream>\n" +
                        "#include <string>\n" +
                        "using namespace std;\n\n" +
                        "int main() {\n" +
                        "    string name;\n" +
                        "    int age;\n" +
                        "    \n" +
                        "    cout << \"Enter your name: \";\n" +
                        "    getline(cin, name);\n" +
                        "    \n" +
                        "    cout << \"Enter your age: \";\n" +
                        "    cin >> age;\n" +
                        "    \n" +
                        "    cout << \"Hello \" << name << \", you are \" << age << \" years old!\" << endl;\n" +
                        "    \n" +
                        "    return 0;\n" +
                        "}";

            case "JavaScript":
                return "// JavaScript Sample Code\n" +
                        "console.log(\"Hello, World!\");\n" +
                        "console.log(\"Interactive JavaScript example\");\n" +
                        "// Add your code here";

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

    // Interactive Executor Class
    private class InteractiveExecutor {
        private String code;
        private String language;
        private ExecutionCallback callback;
        private BlockingQueue<String> inputQueue;
        private boolean isRunning = false;
        private Thread executionThread;

        public InteractiveExecutor(String code, String language, ExecutionCallback callback) {
            this.code = code;
            this.language = language;
            this.callback = callback;
            this.inputQueue = new LinkedBlockingQueue<>();
        }

        public void execute() {
            isRunning = true;
            executionThread = new Thread(() -> {
                try {
                    executeWithPistonInteractive();
                } catch (Exception e) {
                    if (isRunning) {
                        callback.onError(e.getMessage());
                    }
                }
            });
            executionThread.start();
        }

        private void executeWithPistonInteractive() {
            codeExecutionAPI.executeInteractive(code, language, new CodeExecutionCallback() {
                @Override
                public void onSuccess(String output) {
                    if (isRunning) {
                        callback.onOutput(output);
                        callback.onComplete();
                    }
                }

                @Override
                public void onError(String error) {
                    if (isRunning) {
                        callback.onError(error);
                    }
                }

                @Override
                public void onInputRequired(String prompt) {
                    if (isRunning) {
                        callback.onInputRequired(prompt);
                        // Wait for input
                        try {
                            String input = inputQueue.take();
                            // Continue execution with input
                            // This is simplified - in a real implementation you'd need
                            // to handle the interactive nature of the API
                        } catch (InterruptedException e) {
                            // Handle interruption
                        }
                    }
                }
            });
        }

        public void provideInput(String input) {
            inputQueue.offer(input);
        }

        public void stop() {
            isRunning = false;
            if (executionThread != null && executionThread.isAlive()) {
                executionThread.interrupt();
            }
        }
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

        public void executeInteractive(String code, String language, CodeExecutionCallback callback) {
            new Thread(() -> {
                try {
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("language", getPistonLanguage(language));
                    requestBody.put("version", "*");

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
                                // Check if program is waiting for input
                                if (output.contains("Enter") || output.contains("Input") ||
                                        output.endsWith(": ") || output.endsWith("? ")) {
                                    callback.onInputRequired(output.trim());
                                } else {
                                    callback.onSuccess(output.isEmpty() ? "Code executed successfully (no output)" : output);
                                }
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

    // Interfaces
    interface CodeExecutionCallback {
        void onSuccess(String output);
        void onError(String error);
        default void onInputRequired(String prompt) {}
    }

    interface ExecutionCallback {
        void onOutput(String output);
        void onError(String error);
        void onInputRequired(String prompt);
        void onComplete();
    }
}