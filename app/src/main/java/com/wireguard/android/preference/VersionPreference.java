/*
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package com.wireguard.android.preference;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

import com.wireguard.android.Application;
import com.wireguard.android.BuildConfig;
import com.wireguard.android.R;
import com.wireguard.android.backend.Backend;
import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.WgQuickBackend;

import java.util.ArrayList;
import java.util.List;

public class VersionPreference extends Preference {
    private String versionSummary;

    public VersionPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        Application.onHaveBackend(backend -> {
            versionSummary = getContext().getString(R.string.version_summary_checking, backend.getTypeName().toLowerCase());
            Application.getAsyncWorker().supplyAsync(backend::getVersion).whenComplete((version, exception) -> {
                versionSummary = exception == null
                        ? getContext().getString(R.string.version_summary, backend.getTypeName(), version)
                        : getContext().getString(R.string.version_summary_unknown, backend.getTypeName().toLowerCase());
                notifyChanged();
            });
        });
    }

    @Override
    public CharSequence getSummary() {
        return versionSummary;
    }

    @Override
    public CharSequence getTitle() {
        return getContext().getString(R.string.version_title, BuildConfig.VERSION_NAME);
    }

    @Override
    protected void onClick() {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.wireguard.com/"));
        getContext().startActivity(intent);
    }

}
