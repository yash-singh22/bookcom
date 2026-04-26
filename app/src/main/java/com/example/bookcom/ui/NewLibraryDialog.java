package com.example.bookcom.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.bookcom.R;

public class NewLibraryDialog {

    private Context context;
    private AlertDialog dialog;
    private EditText etLibraryName;
    private Button btnCancel;
    private Button btnCreate;
    private OnLibraryCreatedListener listener;

    public interface OnLibraryCreatedListener {
        void onLibraryCreated(String name, String color);
    }

    public NewLibraryDialog(Context context, OnLibraryCreatedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_new_library, null);
        builder.setView(view);

        etLibraryName = view.findViewById(R.id.et_library_name);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnCreate = view.findViewById(R.id.btn_create);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnCreate.setOnClickListener(v -> {
            String name = etLibraryName.getText().toString().trim();
            if (name.isEmpty()) {
                etLibraryName.setError("Name cannot be empty");
                return;
            }
            // For simplicity, we'll assign a default color. You could also generate a random one.
            String defaultColor = "#FFB347";
            listener.onLibraryCreated(name, defaultColor);
            dialog.dismiss();
        });

        dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
}
