/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package com.wireguard.android.activity;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.view.MenuItem;

import com.wireguard.android.Application;
import com.wireguard.android.R;
import com.wireguard.android.backend.WgQuickBackend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface for changing application-global persistent settings.
 */

public class SettingsActivity extends ThemeChangeAwareActivity {
    private final Map<Integer, PermissionRequestCallback> permissionRequestCallbacks = new HashMap<>();
    private int permissionRequestCounter;

    public void ensurePermissions(final String[] permissions, final PermissionRequestCallback cb) {
        final List<String> needPermissions = new ArrayList<>(permissions.length);
        for (final String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED)
                needPermissions.add(permission);
        }
        if (needPermissions.isEmpty()) {
            final int[] granted = new int[permissions.length];
            Arrays.fill(granted, PackageManager.PERMISSION_GRANTED);
            cb.done(permissions, granted);
            return;
        }
        final int idx = permissionRequestCounter++;
        permissionRequestCallbacks.put(idx, cb);
        ActivityCompat.requestPermissions(this,
                needPermissions.toArray(new String[needPermissions.size()]), idx);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        final PermissionRequestCallback f = permissionRequestCallbacks.get(requestCode);
        if (f != null) {
            permissionRequestCallbacks.remove(requestCode);
            f.done(permissions, grantResults);
        }
    }

    public interface PermissionRequestCallback {
        void done(String[] permissions, int[] grantResults);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(final Bundle savedInstanceState, final String key) {
            addPreferencesFromResource(R.xml.preferences);
            final Preference wgQuickOnlyPrefs[] = {
                getPreferenceManager().findPreference("tools_installer"),
                getPreferenceManager().findPreference("restore_on_boot")
            };
            for (final Preference pref : wgQuickOnlyPrefs)
                pref.setVisible(false);
            final PreferenceScreen screen = getPreferenceScreen();
            Application.onHaveBackend(backend -> {
                for (final Preference pref : wgQuickOnlyPrefs) {
                    if (backend.getClass() == WgQuickBackend.class)
                        pref.setVisible(true);
                    else
                        screen.removePreference(pref);
                }
            });
        }
    }
}
