package eu.faircode.netguard.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.util.PatternsCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import eu.faircode.netguard.ActivityDns;
import eu.faircode.netguard.ActivityForwarding;
import eu.faircode.netguard.ActivitySettings;
import eu.faircode.netguard.DatabaseHelper;
import eu.faircode.netguard.R;
import eu.faircode.netguard.ServiceSinkhole;
import eu.faircode.netguard.Util;

public class AdvancedPreferenceFragment extends PreferenceFragmentCompat {

    private static final String TAG = "NetGuard.Settings";
    private SharedPreferences prefs;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences_advanced);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (!Util.canFilter(getContext())) {
            final SwitchPreferenceCompat logApp = findPreference("log_app");
            final SwitchPreferenceCompat filter = findPreference("filter");
            logApp.setEnabled(false);
            logApp.setSummary(getString(R.string.summary_log_app)
                    + "\n\n" + getString(R.string.msg_unavailable));
            filter.setEnabled(false);
            filter.setSummary(getString(R.string.summary_filter)
                    + "\n\n" + getString(R.string.msg_unavailable));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final SwitchPreferenceCompat handover = findPreference("handover");
            handover.setVisible(false);
        }

        final Preference resetUsage = findPreference("reset_usage");
        resetUsage.setOnPreferenceClickListener(preference -> {
            Util.areYouSure(getContext(), R.string.setting_reset_usage, () -> {
                try {
                    DatabaseHelper.getInstance(getContext()).resetUsage(-1);
                } catch (final Throwable ex) {
                    Toast.makeText(getContext(), ex.toString(), Toast.LENGTH_LONG).show();
                }
            });
            return false;
        });

        final EditTextPreference rcode = findPreference("rcode");
        rcode.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

        final Preference forwarding = findPreference("forwarding");
        forwarding.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getContext(), ActivityForwarding.class));
            return true;
        });

        if (Util.isPlayStoreInstall(getContext())) {
            Log.i(TAG, "Play store install");
            final SwitchPreferenceCompat blockDomains = findPreference("use_hosts");
            blockDomains.setVisible(false);
            rcode.setVisible(false);
            forwarding.setVisible(false);
        }


        final EditTextPreference vpn4 = findPreference("vpn4");
        vpn4.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_PHONE));
        vpn4.setOnPreferenceChangeListener((preference, newValue) -> {
            updateVpnPref("vpn4", vpn4);
            return true;
        });

        final EditTextPreference vpn6 = findPreference("vpn6");
        vpn6.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT));
        vpn6.setOnPreferenceChangeListener((preference, newValue) -> {
            updateVpnPref("vpn6", vpn6);
            return true;
        });

        final EditTextPreference dns1 = findPreference("dns");
        dns1.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT));
        dns1.setOnPreferenceChangeListener((preference, newValue) -> {
            updateDnsPref("dns", dns1);
            return true;
        });

        final EditTextPreference dns2 = findPreference("dns2");
        dns2.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT));
        dns2.setOnPreferenceChangeListener((preference, newValue) -> {
            updateDnsPref("dns2", dns2);
            return true;
        });

        final EditTextPreference validate = findPreference("validate");
        validate.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT));
        validate.setOnPreferenceChangeListener((preference, newValue) -> {
            updateValidatePref("validate", validate);
            return true;
        });

        final EditTextPreference ttl = findPreference("ttl");
        ttl.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

        final EditTextPreference socks5Addr = findPreference("socks5_addr");
        socks5Addr.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT));
        socks5Addr.setOnPreferenceChangeListener((preference, newValue) -> {
            updateSocksPref("socks5_addr", socks5Addr);
            return true;
        });

        final EditTextPreference socks5Port = findPreference("socks5_port");
        socks5Port.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

        final EditTextPreference socks5Username = findPreference("socks5_username");
        socks5Username.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT));

        final EditTextPreference socks5Password = findPreference("socks5_password");
        socks5Password.setSummary(TextUtils.isEmpty(prefs.getString("socks5_username", "")) ? "" : "*****");
        socks5Password.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD));
        socks5Password.setOnPreferenceChangeListener((preference, newValue) -> {
            socks5Password.setSummary(TextUtils.isEmpty(prefs.getString("socks5_username", "")) ? "" : "*****");
            return true;
        });

        final EditTextPreference pcapRecordSize = findPreference("pcap_record_size");
        pcapRecordSize.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        pcapRecordSize.setOnPreferenceChangeListener((preference, newValue) -> {
            updatePcapPref();
            return true;
        });

        final EditTextPreference pcapFileSize = findPreference("pcap_file_size");
        pcapFileSize.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        pcapFileSize.setOnPreferenceChangeListener((preference, newValue) -> {
            updatePcapPref();
            return true;
        });

        final EditTextPreference watchdog = findPreference("watchdog");
        watchdog.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

        final Preference showResolved = findPreference("show_resolved");
        if (Util.isPlayStoreInstall(getContext())) {
            showResolved.setVisible(false);
        } else {
            showResolved.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getContext(), ActivityDns.class));
                return true;
            });
        }
    }

    private void updateVpnPref(final String key, final EditTextPreference pref) {
        final String vpn = prefs.getString(key, null);
        try {
            checkAddress(vpn, false);
            prefs.edit().putString(key, vpn.trim()).apply();
        } catch (final Throwable ex) {
            prefs.edit().remove(key).apply();
            pref.setText(null);
            if (!TextUtils.isEmpty(vpn)) {
                Toast.makeText(getContext(), ex.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateDnsPref(final String key, final EditTextPreference pref) {
        final String dns = prefs.getString(key, null);
        try {
            checkAddress(dns, true);
            prefs.edit().putString(key, dns.trim()).apply();
        } catch (final Throwable ex) {
            prefs.edit().remove(key).apply();
            pref.setText(null);
            if (!TextUtils.isEmpty(dns)) {
                Toast.makeText(getContext(), ex.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateValidatePref(final String key, final EditTextPreference pref) {
        final String host = prefs.getString(key, "www.google.com");
        try {
            checkDomain(host);
            prefs.edit().putString(key, host.trim()).apply();
        } catch (final Throwable ex) {
            prefs.edit().remove(key).apply();
            pref.setText(null);
            if (!TextUtils.isEmpty(host)) {
                Toast.makeText(getContext(), ex.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateSocksPref(final String key, final EditTextPreference pref) {
        final String socks5_addr = prefs.getString(key, null);
        try {
            if (!TextUtils.isEmpty(socks5_addr) && !Util.isNumericAddress(socks5_addr))
                throw new IllegalArgumentException("Bad address");
        } catch (final Throwable ex) {
            prefs.edit().remove(key).apply();
            pref.setText(null);
            if (!TextUtils.isEmpty(socks5_addr))
                Toast.makeText(getContext(), ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkAddress(String address, final boolean allow_local)
            throws IllegalArgumentException, UnknownHostException {
        if (address != null)
            address = address.trim();
        if (TextUtils.isEmpty(address) || !Util.isNumericAddress(address))
            throw new IllegalArgumentException("Bad address");
        if (!allow_local) {
            final InetAddress iaddr = InetAddress.getByName(address);
            if (iaddr.isLoopbackAddress() || iaddr.isAnyLocalAddress())
                throw new IllegalArgumentException("Bad address");
        }
    }

    private void checkDomain(String address) throws IllegalArgumentException {
        if (address != null)
            address = address.trim();
        if (TextUtils.isEmpty(address)
                || Util.isNumericAddress(address)
                || !PatternsCompat.DOMAIN_NAME.matcher(address).matches()
        ) throw new IllegalArgumentException("Bad address");
    }

    private void updatePcapPref() {
        ServiceSinkhole.setPcap(false, getContext());

        final File pcap_file = new File(getContext().getDir("data", Context.MODE_PRIVATE), "netguard.pcap");
        if (pcap_file.exists() && !pcap_file.delete())
            Log.w(TAG, "Delete PCAP failed");

        if (prefs.getBoolean("pcap", false))
            ServiceSinkhole.setPcap(true, getContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        ((ActivitySettings) getActivity()).getSupportActionBar().setTitle(R.string.setting_advanced_options);
    }
}
