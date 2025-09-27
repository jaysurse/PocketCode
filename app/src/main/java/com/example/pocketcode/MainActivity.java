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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class MainActivity extends AppCompatActivity {

    // UI Components - Original
    private Toolbar toolbar;
    private ImageButton newFileBtn, openBtn, saveBtn, formatBtn;
    private Button runBtn, stopBtn;
    private Spinner langSpinner;
    private EditText codeEditor;
    private TextView outputConsole, fileNameText, cursorPositionText,
            encodingText, statusText, memoryUsage, lineNumbers;
    private ProgressBar progressBar;
    private WebView webView;
    private EditText terminalInput;

    // New UI Components for enhanced input handling
    private EditText currentInputField;
    private TextView inputQueueDisplay;
    private Button addInputBtn, clearAllInputBtn;
    private ArrayList<String> inputQueue = new ArrayList<>();

    // Variables
    private String currentFileName = "Untitled";
    private String[] supportedLanguages = {"Java", "Python", "JavaScript", "C++", "C", "HTML", "CSS"};
    private String currentLanguage = "Java";
    private StringBuilder consoleOutput = new StringBuilder();
    private boolean isWaitingForInput = false;
    private boolean isExecuting = false;
    private InteractiveExecutor currentExecutor = null;

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
        // Original UI components
        toolbar = findViewById(R.id.toolbar);
        newFileBtn = findViewById(R.id.newFileBtn);
        openBtn = findViewById(R.id.openBtn);
        saveBtn = findViewById(R.id.saveBtn);
        formatBtn = findViewById(R.id.formatBtn);
        runBtn = findViewById(R.id.runBtn);
        stopBtn = findViewById(R.id.stopBtn);
        langSpinner = findViewById(R.id.langSpinner);
        codeEditor = findViewById(R.id.codeEditor);
        outputConsole = findViewById(R.id.outputConsole);
        terminalInput = findViewById(R.id.terminalInput);
        fileNameText = findViewById(R.id.fileNameText);
        cursorPositionText = findViewById(R.id.cursorPositionText);
        encodingText = findViewById(R.id.encodingText);
        statusText = findViewById(R.id.statusText);
        memoryUsage = findViewById(R.id.memoryUsage);
        lineNumbers = findViewById(R.id.lineNumbers);
        progressBar = findViewById(R.id.progressBar);

        // New enhanced input handling components
        currentInputField = findViewById(R.id.currentInputField);
        inputQueueDisplay = findViewById(R.id.inputQueueDisplay);
        addInputBtn = findViewById(R.id.addInputBtn);
        clearAllInputBtn = findViewById(R.id.clearAllInputBtn);

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
                updateStatusText("Language changed to " + currentLanguage);
                updateSampleCode();
                showInputInstructions();
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
        // Original event listeners
        newFileBtn.setOnClickListener(v -> createNewFile());
        openBtn.setOnClickListener(v -> openFileLauncher.launch("*/*"));
        saveBtn.setOnClickListener(v -> saveFileLauncher.launch(currentFileName + getFileExtension()));
        formatBtn.setOnClickListener(v -> formatCode());
        runBtn.setOnClickListener(v -> executeCode());
        stopBtn.setOnClickListener(v -> stopExecution());

        // New enhanced input handling listeners
        addInputBtn.setOnClickListener(v -> addInputToQueue());
        clearAllInputBtn.setOnClickListener(v -> clearInputQueue());

        // Allow Enter key to add input
        currentInputField.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                addInputToQueue();
                return true;
            }
            return false;
        });

        // Keep legacy terminal input for backward compatibility
        terminalInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (isWaitingForInput && currentExecutor != null) {
                    String input = terminalInput.getText().toString();
                    terminalInput.setText("");
                    appendToConsole(input);
                    currentExecutor.provideInput(input);
                    isWaitingForInput = false;
                }
                return true;
            }
            return false;
        });
    }

    // New enhanced input handling methods
    private void addInputToQueue() {
        String input = currentInputField.getText().toString();
        if (!input.trim().isEmpty()) {
            inputQueue.add(input);
            currentInputField.setText("");
            updateInputQueueDisplay();
            updateStatusText("Added input: " + input);
        }
    }

    private void clearInputQueue() {
        inputQueue.clear();
        updateInputQueueDisplay();
        updateStatusText("Input queue cleared");
    }

    private void updateInputQueueDisplay() {
        if (inputQueue.isEmpty()) {
            inputQueueDisplay.setText("(No inputs added yet)\n\nTip: For programs that need multiple inputs,\nadd them one by one before clicking RUN");
        } else {
            StringBuilder display = new StringBuilder("Input Queue (" + inputQueue.size() + " items):\n\n");
            for (int i = 0; i < inputQueue.size(); i++) {
                display.append((i + 1)).append(". ").append(inputQueue.get(i)).append("\n");
            }
            display.append("\nThese will be sent to your program in order.");
            inputQueueDisplay.setText(display.toString());
        }
    }

    private void showInputInstructions() {
        String instructions = getInputInstructionsForLanguage(currentLanguage);
        appendToConsole(instructions);
    }

    private String getInputInstructionsForLanguage(String language) {
        switch (language) {
            case "Java":
                return "Input Instructions:\n" +
                        "- For Scanner.nextLine(): Add each line as separate input\n" +
                        "- For Scanner.nextInt(): Add numbers as separate inputs\n" +
                        "- For Scanner.next(): Add words as separate inputs\n";
            case "Python":
                return "Input Instructions:\n" +
                        "- For input(): Add each input as separate line\n" +
                        "- For int(input()): Add numbers as separate inputs\n";
            case "C":
            case "C++":
                return "Input Instructions:\n" +
                        "- For scanf(\"%s\"): Add strings as separate inputs\n" +
                        "- For scanf(\"%d\"): Add numbers as separate inputs\n" +
                        "- For getline(): Add full lines as separate inputs\n";
            default:
                return "Add inputs that your program will request, one per line.";
        }
    }

    // Console and utility methods
    private void appendToConsole(String text) {
        runOnUiThread(() -> {
            consoleOutput.append(text).append("\n");
            outputConsole.setText(consoleOutput.toString());
            // Auto-scroll to bottom
            outputConsole.post(() -> {
                if (outputConsole.getLayout() != null) {
                    int scrollY = outputConsole.getLayout().getLineTop(outputConsole.getLineCount()) - outputConsole.getHeight();
                    if (scrollY > 0) {
                        outputConsole.scrollTo(0, scrollY);
                    }
                }
            });
        });
    }

    private void clearConsole() {
        consoleOutput.setLength(0);
        outputConsole.setText("");
    }

    private void showInputField() {
        runOnUiThread(() -> {
            terminalInput.setVisibility(View.VISIBLE);
            terminalInput.requestFocus();
            isWaitingForInput = true;
        });
    }

    private void hideInputField() {
        runOnUiThread(() -> {
            terminalInput.setVisibility(View.GONE);
            isWaitingForInput = false;
        });
    }

    // File operations
    private void createNewFile() {
        if (isExecuting) stopExecution();
        codeEditor.setText("");
        currentFileName = "Untitled";
        clearConsole();
        clearInputQueue(); // Clear input queue on new file
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

    // Updated executeCode method with enhanced input handling
    private void executeCode() {
        if (isExecuting) return;

        showProgress(true);
        isExecuting = true;
        runBtn.setEnabled(false);
        stopBtn.setEnabled(true);

        clearConsole();
        String code = codeEditor.getText().toString();

        if (code.trim().isEmpty()) {
            appendToConsole("Error: No code to execute");
            showProgress(false);
            resetExecutionState();
            return;
        }

        // Prepare input string from queue
        StringBuilder inputString = new StringBuilder();
        for (String input : inputQueue) {
            inputString.append(input).append("\n");
        }

        appendToConsole("=== " + currentLanguage + " Execution Started ===");
        if (!inputQueue.isEmpty()) {
            appendToConsole("Using " + inputQueue.size() + " pre-defined inputs");
        }
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
                    executeWithAPI(code, inputString.toString());
                }
                break;
            default:
                executeWithAPI(code, inputString.toString());
                break;
        }
    }

    // New method for API execution with input
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
            public void onInputRequired() {
                runOnUiThread(() -> {
                    appendToConsole("Waiting for input...");
                    showInputField();
                });
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

    // Updated resetExecutionState method
    private void resetExecutionState() {
        runOnUiThread(() -> {
            isExecuting = false;
            runBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            hideInputField();
            // Keep input queue intact for reuse
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
    private String performBasicFormatting(String code) {
        return code.replaceAll("\\{", " {\n")
                .replaceAll("\\}", "}\n")
                .replaceAll(";", ";\n")
                .replaceAll("\\n\\s*\\n", "\n")
                .trim();
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

    // Updated sample code with input examples
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
                        "}\n\n" +
                        "// Pre-add inputs: Your Name, 25";

            case "Python":
                return "# Interactive Python Example\n" +
                        "print(\"=== Python Interactive Demo ===\")\n" +
                        "name = input(\"Enter your name: \")\n" +
                        "age = int(input(\"Enter your age: \"))\n" +
                        "city = input(\"Enter your city: \")\n\n" +
                        "print(f\"Hello {name}!\")\n" +
                        "print(f\"You are {age} years old and live in {city}\")\n\n" +
                        "# Count example\n" +
                        "count = int(input(\"How many times to count? \"))\n" +
                        "for i in range(count):\n" +
                        "    print(f\"Count: {i + 1}\")\n\n" +
                        "print(\"Program completed!\")\n\n" +
                        "# Pre-add inputs: Your Name, 25, Your City, 3";

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
                        "}\n\n" +
                        "/* Pre-add inputs: YourName, 25 */";

            default:
                return "// " + language + " Sample Code\n" +
                        "// Add any required inputs in the input queue below\n" +
                        "// before running the code\n\n" +
                        "console.log('Hello World!');";
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
        fileNameText.setText("File: " + currentFileName + getFileExtension());
        encodingText.setText("UTF-8");
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        memoryUsage.setText(String.format("Memory: %dMB", usedMemory / (1024 * 1024)));
        updateLineNumbers();
        updateCursorPosition();
        updateInputQueueDisplay();
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
            codeExecutionAPI.executeWithPiston(code, language, new CodeExecutionCallback() {
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
            }, getAllQueuedInput());
        }

        private String getAllQueuedInput() {
            StringBuilder allInput = new StringBuilder();
            while (!inputQueue.isEmpty()) {
                allInput.append(inputQueue.poll()).append("\n");
            }
            return allInput.toString();
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

    // API class
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

    // Interfaces
    interface CodeExecutionCallback {
        void onSuccess(String output);
        void onError(String error);
    }

    interface ExecutionCallback {
        void onOutput(String output);
        void onError(String error);
        void onInputRequired();
        void onComplete();
    }
}