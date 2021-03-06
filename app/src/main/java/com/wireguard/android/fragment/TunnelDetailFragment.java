/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package com.wireguard.android.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wireguard.android.R;
import com.wireguard.android.databinding.TunnelDetailFragmentBinding;
import com.wireguard.android.model.Tunnel;
import com.wireguard.config.Config;

/**
 * Fragment that shows details about a specific tunnel.
 */

public class TunnelDetailFragment extends BaseFragment {
    private TunnelDetailFragmentBinding binding;

    private void onConfigLoaded(final String name, final Config config) {
        binding.setConfig(new Config.Observable(config, name));
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.tunnel_detail, menu);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = TunnelDetailFragmentBinding.inflate(inflater, container, false);
        binding.executePendingBindings();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onSelectedTunnelChanged(final Tunnel oldTunnel, final Tunnel newTunnel) {
        if (binding == null)
            return;
        binding.setTunnel(newTunnel);
        if (newTunnel == null)
            binding.setConfig(null);
        else
            newTunnel.getConfigAsync().thenAccept(a -> onConfigLoaded(newTunnel.getName(), a));
    }

    @Override
    public void onViewStateRestored(final Bundle savedInstanceState) {
        onSelectedTunnelChanged(null, getSelectedTunnel());
        super.onViewStateRestored(savedInstanceState);
    }
}
