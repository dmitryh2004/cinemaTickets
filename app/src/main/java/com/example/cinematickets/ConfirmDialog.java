package com.example.cinematickets;
import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

public class ConfirmDialog {
    public interface ConfirmDialogCallback {
        void onConfirmation();
    }

    Context context;
    ConfirmDialogCallback callback;

    public ConfirmDialog(Context context, ConfirmDialogCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void showDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        callback.onConfirmation();
                    }
                })
                .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}

