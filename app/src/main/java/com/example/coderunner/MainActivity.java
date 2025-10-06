package com.example.coderunner;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import okhttp3.*;
import org.json.JSONObject;
import java.io.*;

public class MainActivity extends AppCompatActivity {

    Spinner langSpinner;
    EditText codeInput, stdinInput;
    Button runButton, saveButton, openButton;
    TextView outputView;
    ProgressBar progressBar;

    private final String CLIENT_ID = "ef9b6ba01486c49d61cdcb1af81d9e07";
    private final String CLIENT_SECRET = "49e89521188427b9ef30232b5e9c954a5e11bd3dff169d479b33ad87cb469791";
    private final String API_URL = "https://api.jdoodle.com/v1/execute";

    private final int REQUEST_SAVE_FILE = 1;
    private final int REQUEST_OPEN_FILE = 2;
    private Uri saveUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        progressBar = findViewById(R.id.progressBar);

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

            progressBar.setVisibility(View.VISIBLE);
            outputView.setText("");
            runCodeOnline(lang, code, stdin);
        });

        // Save file
        saveButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, ""); // user enters filename + extension
            startActivityForResult(intent, REQUEST_SAVE_FILE);
        });

        // Open file
        openButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*"); // allow all code file types
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/plain"});
            startActivityForResult(intent, REQUEST_OPEN_FILE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
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
                        progressBar.setVisibility(View.GONE);
                        outputView.setText("Network error: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String respBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        try {
                            JSONObject res = new JSONObject(respBody);
                            String output = res.optString("output", respBody);

                            // Display input + output cleanly
                            String display = "";
                            if (!stdin.trim().isEmpty()) {
                                String[] inputs = stdin.split("\n");
                                String[] prompts = new String[]{"Enter first number: ", "Enter second number: "};
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
            progressBar.setVisibility(View.GONE);
            outputView.setText("Error: " + e.getMessage());
        }
    }

    private String mapLanguage(String lang) {
        switch (lang) {
            case "C": return "c";
            case "C++": return "cpp17";
            case "Java": return "java";
            case "Python": return "python3";
            default: return "python3";
        }
    }
}
