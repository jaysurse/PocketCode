package com.example.coderunner.ui.db;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coderunner.R;
import com.example.coderunner.data.Snippet;
import com.example.coderunner.data.SnippetDbHelper;

import java.util.ArrayList;
import java.util.List;

public class SnippetListActivity extends AppCompatActivity {
    private RecyclerView recycler;
    private SnippetAdapter adapter;
    private SnippetDbHelper dbh;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snippet_list);

        dbh = new SnippetDbHelper(this);

        recycler = findViewById(R.id.snippetRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SnippetAdapter(new ArrayList<>(), s -> {
            // return snippet as result
            Intent out = new Intent();
            out.putExtra("snippet_id", s.id);
            out.putExtra("snippet_title", s.title);
            out.putExtra("snippet_code", s.code);
            out.putExtra("snippet_lang", s.languageId);
            setResult(Activity.RESULT_OK, out);
            finish();
        });
        recycler.setAdapter(adapter);

        loadSnippets();
    }

    private void loadSnippets() {
        new Thread(() -> {
            java.util.List<Snippet> list = dbh.getAllSnippets();
            runOnUiThread(() -> adapter.setItems(list));
        }).start();
    }
}
