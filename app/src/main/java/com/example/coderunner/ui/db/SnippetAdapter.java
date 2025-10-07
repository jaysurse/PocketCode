package com.example.coderunner.ui.db;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.coderunner.R;
import com.example.coderunner.data.Snippet;

import java.text.DateFormat;
import java.util.List;

public class SnippetAdapter extends RecyclerView.Adapter<SnippetAdapter.VH> {
    public interface OnSelect {
        void onSelect(Snippet s);
    }

    public interface OnLongAction {
        void onLongAction(Snippet s, View anchor);
    }

    public interface OnAction {
        void onRename(Snippet s, String newTitle);

        void onDelete(Snippet s);
    }

    private List<Snippet> items;
    private final OnSelect onSelect;
    private final OnLongAction onLongAction;
    private final OnAction onAction;

    public SnippetAdapter(List<Snippet> items, OnSelect onSelect) {
        this(items, onSelect, null);
    }

    public SnippetAdapter(List<Snippet> items, OnSelect onSelect, OnLongAction onLongAction) {
        this(items, onSelect, onLongAction, null);
    }

    public SnippetAdapter(List<Snippet> items, OnSelect onSelect, OnLongAction onLongAction, OnAction onAction) {
        this.items = items;
        this.onSelect = onSelect;
        this.onLongAction = onLongAction;
        this.onAction = onAction;
    }

    public void setItems(List<Snippet> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_snippet, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Snippet s = items.get(position);
        holder.title.setText(s.title == null || s.title.isEmpty() ? "Untitled" : s.title);
        String meta = s.languageId + " • " + DateFormat.getDateTimeInstance()
                .format(s.lastEdited == 0 ? System.currentTimeMillis() : s.lastEdited);
        holder.meta.setText(meta);
        holder.itemView.setOnClickListener(v -> onSelect.onSelect(s));
        holder.itemView.setOnLongClickListener(v -> {
            if (onLongAction != null) {
                onLongAction.onLongAction(s, v);
                return true;
            }
            return false;
        });

        // Edit/Delete buttons
        holder.btnEdit.setOnClickListener(v -> {
            // show inline rename UI
            holder.renameRow.setVisibility(View.VISIBLE);
            holder.renameInput.setText(s.title == null ? "" : s.title);
        });

        holder.btnCancel.setOnClickListener(v -> {
            holder.renameRow.setVisibility(View.GONE);
        });

        holder.btnConfirm.setOnClickListener(v -> {
            String newTitle = holder.renameInput.getText() == null ? "" : holder.renameInput.getText().toString();
            holder.renameRow.setVisibility(View.GONE);
            if (onAction != null)
                onAction.onRename(s, newTitle);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (onAction != null)
                onAction.onDelete(s);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, meta;
        android.widget.ImageButton btnEdit, btnDelete, btnConfirm, btnCancel;
        android.widget.EditText renameInput;
        View renameRow;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.snippetTitle);
            meta = itemView.findViewById(R.id.snippetMeta);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            renameInput = itemView.findViewById(R.id.renameInput);
            renameRow = itemView.findViewById(R.id.renameRow);
        }
    }
}
