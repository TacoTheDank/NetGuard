package eu.faircode.netguard;

/*
    This file is part of NetGuard.

    NetGuard is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetGuard is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetGuard.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2015-2019 by Marcel Bokhorst (M66B)
*/

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import eu.faircode.netguard.preferences.AdvancedPreferenceFragment;
import eu.faircode.netguard.preferences.BackupPreferenceFragment;
import eu.faircode.netguard.preferences.DefaultsPreferenceFragment;
import eu.faircode.netguard.preferences.DevPreferenceFragment;
import eu.faircode.netguard.preferences.MainPreferenceFragment;
import eu.faircode.netguard.preferences.NetworkPreferenceFragment;
import eu.faircode.netguard.preferences.OptionsPreferenceFragment;
import eu.faircode.netguard.preferences.StatsPreferenceFragment;
import eu.faircode.netguard.preferences.TechnicalPreferenceFragment;

public class ActivitySettings extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final BroadcastReceiver interactiveStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Util.logExtras(intent);
        }
    };
    private final BroadcastReceiver connectivityChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Util.logExtras(intent);
        }
    };
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted -> {
                if (isGranted) {
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().putBoolean("disable_on_call", true).apply();
                    ServiceSinkhole.reload("permission granted", this, false);
                }
            });
    private SharedPreferences prefs;

    public static void markPro(final Context context, final Preference pref, final String sku) {
        if (sku == null || !IAB.isPurchased(sku, context)) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean dark = prefs.getBoolean("dark_theme", false);
            pref.setIcon(dark ? R.drawable.ic_shopping_cart_white_24dp : R.drawable.ic_shopping_cart_black_24dp);
        }
    }

    private static int getTitleOfPage(final int preferences) {
        // Main settings page
        if (preferences == R.xml.preferences_defaults) {
            return R.string.setting_defaults;
        } else if (preferences == R.xml.preferences_options) {
            return R.string.setting_options;
        } else if (preferences == R.xml.preferences_network) {
            return R.string.setting_network_options;
        } else if (preferences == R.xml.preferences_advanced) {
            return R.string.setting_advanced_options;
        } else if (preferences == R.xml.preferences_stats) {
            return R.string.setting_stats_category;
        } else if (preferences == R.xml.preferences_backup) {
            return R.string.setting_backup;
        } else if (preferences == R.xml.preferences_dev) {
            return R.string.setting_development_options;
        } else if (preferences == R.xml.preferences_technical) {
            return R.string.setting_technical;
        }
        return R.string.menu_settings;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new MainPreferenceFragment())
                .commit();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // This is to make sure recreate() doesn't trigger when opening for first time
        PreferenceManager.setDefaultValues(this, R.xml.preferences_options, true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPermissions(null);

        // Listen for preference changes
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Listen for interactive state changes
        final IntentFilter ifInteractive = new IntentFilter();
        ifInteractive.addAction(Intent.ACTION_SCREEN_ON);
        ifInteractive.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(interactiveStateReceiver, ifInteractive);

        // Listen for connectivity updates
        final IntentFilter ifConnectivity = new IntentFilter();
        ifConnectivity.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityChangedReceiver, ifConnectivity);
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(interactiveStateReceiver);
        unregisterReceiver(connectivityChangedReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                finish();
            } else {
                getSupportFragmentManager().popBackStack();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String name) {
        final Object value = prefs.getAll().get(name);
        if (value instanceof String && "".equals(value))
            prefs.edit().remove(name).apply();

        // Dependencies
        switch (name) {
            case "screen_on":
            case "whitelist_wifi":
            case "screen_wifi":
            case "whitelist_other":
            case "screen_other":
            case "whitelist_roaming":
            case "subnet":
            case "tethering":
            case "lan":
            case "ip6":
            case "use_metered":
            case "unmetered_2g":
            case "unmetered_3g":
            case "unmetered_4g":
            case "eu_roaming":
            case "national_roaming":
            case "lockdown_wifi":
            case "lockdown_other":
            case "notify_access":
            case "use_hosts":
            case "socks5_enabled":
            case "loglevel":
            case "wifi_homes":
            case "watchdog":
            case "rcode":
            case "socks5_addr":
            case "socks5_port":
            case "socks5_username":
            case "socks5_password":
            case "vpn4":
            case "vpn6":
            case "dns":
            case "dns2":
            case "validate":
                ServiceSinkhole.reload("changed " + name, this, false);
                break;
            case "theme":
            case "dark_theme":
                ActivityCompat.recreate(this);
                break;
            case "disable_on_call":
                if (prefs.getBoolean(name, false)) {
                    if (checkPermissions(name))
                        ServiceSinkhole.reload("changed " + name, this, false);
                } else
                    ServiceSinkhole.reload("changed " + name, this, false);
                break;
            case "manage_system":
                boolean manage = prefs.getBoolean(name, false);
                if (!manage)
                    prefs.edit().putBoolean("show_user", true).apply();
                prefs.edit().putBoolean("show_system", manage).apply();
                ServiceSinkhole.reload("changed " + name, this, false);
                break;
            case "log_app":
                final Intent ruleset = new Intent(ActivityMain.ACTION_RULES_CHANGED);
                LocalBroadcastManager.getInstance(this).sendBroadcast(ruleset);
                ServiceSinkhole.reload("changed " + name, this, false);
                break;
            case "filter":
                // Show dialog
                if (prefs.getBoolean(name, false)) {
                    final LayoutInflater inflater = LayoutInflater.from(ActivitySettings.this);
                    final View view = inflater.inflate(R.layout.filter, null, false);
                    new AlertDialog.Builder(ActivitySettings.this)
                            .setView(view)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                ServiceSinkhole.reload("changed " + name, this, false);
                break;
            case "show_stats":
                ServiceSinkhole.reloadStats("changed " + name, this);
                break;
        }
    }

    private boolean checkPermissions(final String name) {
        // Check if permission was revoked
        if ((name == null || "disable_on_call".equals(name)) && prefs.getBoolean("disable_on_call", false))
            if (!Util.hasPhoneStatePermission(this)) {
                prefs.edit().putBoolean("disable_on_call", false).apply();
                requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE);
                return name == null;
            }
        return true;
    }

    public void openSettingsScreen(final int screen) {
        final PreferenceFragmentCompat fragment = getSettingsScreen(screen);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, fragment)
                .addToBackStack(getString(getTitleOfPage(screen)))
                .commit();
    }

    private PreferenceFragmentCompat getSettingsScreen(final int screen) {
        PreferenceFragmentCompat prefFragment = null;

        if (screen == R.xml.preferences_defaults) {
            prefFragment = new DefaultsPreferenceFragment();
        } else if (screen == R.xml.preferences_options) {
            prefFragment = new OptionsPreferenceFragment();
        } else if (screen == R.xml.preferences_network) {
            prefFragment = new NetworkPreferenceFragment();
        } else if (screen == R.xml.preferences_advanced) {
            prefFragment = new AdvancedPreferenceFragment();
        } else if (screen == R.xml.preferences_stats) {
            prefFragment = new StatsPreferenceFragment();
        } else if (screen == R.xml.preferences_backup) {
            prefFragment = new BackupPreferenceFragment();
        } else if (screen == R.xml.preferences_dev) {
            prefFragment = new DevPreferenceFragment();
        } else if (screen == R.xml.preferences_technical) {
            prefFragment = new TechnicalPreferenceFragment();
        }
        return prefFragment;
    }
}
