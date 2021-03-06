/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package com.wireguard.android.preference;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.wireguard.android.Application;
import com.wireguard.android.R;
import com.wireguard.android.activity.SettingsActivity;
import com.wireguard.android.util.ExceptionLoggers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Preference implementing a button that asynchronously exports logs.
 */

public class LogExporterPreference extends Preference {
    private static final String TAG = "WireGuard/" + LogExporterPreference.class.getSimpleName();

    private String exportedFilePath;

    public LogExporterPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    private static SettingsActivity getPrefActivity(final Preference preference) {
        final Context context = preference.getContext();
        if (context instanceof ContextThemeWrapper) {
            if (((ContextThemeWrapper) context).getBaseContext() instanceof SettingsActivity) {
                return ((SettingsActivity) ((ContextThemeWrapper) context).getBaseContext());
            }
        }
        return null;
    }

    private void exportLog() {
        Application.getAsyncWorker().supplyAsync(() -> {
            final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            final File file = new File(path, "wireguard-log.txt");
            if (!path.isDirectory() && !path.mkdirs())
                throw new IOException("Cannot create output directory");

            /* We would like to simply run `builder.redirectOutput(file);`, but this is API 26.
             * Instead we have to do this dance, since logcat appends.
             */
            new FileOutputStream(file).close();

            try {
                final Process process = Runtime.getRuntime().exec(new String[]{
                        "logcat", "-b", "all", "-d", "-v", "threadtime", "-f", file.getAbsolutePath(), "*:V"});
                if (process.waitFor() != 0) {
                    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        final StringBuilder errors = new StringBuilder();
                        errors.append("Unable to run logcat: ");
                        String line;
                        while ((line = reader.readLine()) != null)
                            errors.append(line);
                        throw new Exception(errors.toString());
                    }
                }
            } catch (final Exception e) {
                // noinspection ResultOfMethodCallIgnored
                file.delete();
                throw e;
            }
            return file.getAbsolutePath();
        }).whenComplete(this::exportLogComplete);
    }

    private void exportLogComplete(final String filePath, final Throwable throwable) {
        if (throwable != null) {
            final String error = ExceptionLoggers.unwrapMessage(throwable);
            final String message = getContext().getString(R.string.log_export_error, error);
            Log.e(TAG, message, throwable);
            Snackbar.make(
                    getPrefActivity(this).findViewById(android.R.id.content),
                    message, Snackbar.LENGTH_LONG).show();
            setEnabled(true);
        } else {
            exportedFilePath = filePath;
            notifyChanged();
        }
    }

    @Override
    public CharSequence getSummary() {
        return exportedFilePath == null ?
                getContext().getString(R.string.log_export_summary) :
                getContext().getString(R.string.log_export_success, exportedFilePath);
    }

    @Override
    public CharSequence getTitle() {
        return getContext().getString(R.string.log_exporter_title);
    }

    @Override
    protected void onClick() {
        getPrefActivity(this).ensurePermissions(
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                (permissions, granted) -> {
                    if (granted.length > 0 && granted[0] == PackageManager.PERMISSION_GRANTED) {
                        setEnabled(false);
                        exportLog();
                    }
                });
    }

}
