package eu.faircode.netguard.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.faircode.netguard.ActivitySettings;
import eu.faircode.netguard.R;

public class NetworkPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences_network);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        final MultiSelectListPreference wifiHomes = findPreference("wifi_homes");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wifiHomes.setVisible(false);
        } else {
            final Set<String> ssids = prefs.getStringSet("wifi_homes", new HashSet<>());
            if (ssids.size() > 0) {
                wifiHomes.setTitle(getString(R.string.setting_wifi_home, TextUtils.join(", ", ssids)));
            } else {
                wifiHomes.setTitle(getString(R.string.setting_wifi_home, "-"));
            }
            final WifiManager wm = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final List<CharSequence> listSSID = new ArrayList<>();
            final List<WifiConfiguration> configs = wm.getConfiguredNetworks();
            if (configs != null) {
                for (final WifiConfiguration config : configs) {
                    listSSID.add(config.SSID == null ? "NULL" : config.SSID);
                }
            }
            for (final String ssid : ssids) {
                if (!listSSID.contains(ssid)) {
                    listSSID.add(ssid);
                }
            }
            wifiHomes.setEntries(listSSID.toArray(new CharSequence[0]));
            wifiHomes.setEntryValues(listSSID.toArray(new CharSequence[0]));
            wifiHomes.setOnPreferenceChangeListener((preference, newValue) -> {
                final Set<String> ssid = prefs.getStringSet("wifi_homes", new HashSet<>());
                if (ssid.size() > 0) {
                    wifiHomes.setTitle(getString(R.string.setting_wifi_home, TextUtils.join(", ", ssid)));
                } else {
                    wifiHomes.setTitle(getString(R.string.setting_wifi_home, "-"));
                }
                return true;
            });
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final SwitchPreferenceCompat reloadOnConnectivity =
                    findPreference("reload_onconnectivity");
            reloadOnConnectivity.setChecked(true);
            reloadOnConnectivity.setEnabled(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((ActivitySettings) getActivity()).getSupportActionBar().setTitle(R.string.setting_network_options);
    }
}
