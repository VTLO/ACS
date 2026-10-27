package com.acs.laravelcheatsheet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/** Copy, share and documentation actions shared by both screens. */
final class EntryActions {
    private EntryActions() {}

    static void copy(Activity activity, String text) {
        ClipboardManager clipboard = activity.getSystemService(ClipboardManager.class);
        clipboard.setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.app_name), text));
        Toast.makeText(activity, activity.getString(R.string.copied, text), Toast.LENGTH_SHORT).show();
    }

    static void openUrl(Activity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, activity.getString(R.string.no_browser, url), Toast.LENGTH_LONG).show();
        }
    }

    static void showMenu(Activity activity, Cheatsheet.Entry entry) {
        String docs = entry.docsUrl();
        String[] labels = docs == null
                ? new String[] {activity.getString(R.string.action_copy), activity.getString(R.string.action_copy_full),
                        activity.getString(R.string.action_share)}
                : new String[] {activity.getString(R.string.action_copy), activity.getString(R.string.action_copy_full),
                        activity.getString(R.string.action_share), activity.getString(R.string.action_docs)};
        new AlertDialog.Builder(activity)
                .setTitle(entry.code)
                .setItems(labels, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            copy(activity, entry.code);
                            break;
                        case 1:
                            copy(activity, entry.fullText());
                            break;
                        case 2:
                            Intent send = new Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, entry.fullText());
                            activity.startActivity(Intent.createChooser(send, null));
                            break;
                        default:
                            openUrl(activity, docs);
                    }
                })
                .show();
    }
}
