package eu.faircode.netguard.preferences;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import eu.faircode.netguard.ActivitySettings;
import eu.faircode.netguard.R;
import eu.faircode.netguard.Util;

public class TechnicalPreferenceFragment extends PreferenceFragmentCompat {

    private static final Intent INTENT_VPN_SETTINGS = new Intent("android.net.vpn.SETTINGS");

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences_technical);

        final Preference technicalInfo = findPreference("technical_info");
        if (technicalInfo != null) {
            technicalInfo.setEnabled(INTENT_VPN_SETTINGS.resolveActivity(getActivity().getPackageManager()) != null);
            technicalInfo.setIntent(INTENT_VPN_SETTINGS);
            technicalInfo.setSummary(Util.getGeneralInfo(getContext()));
        }

        final Preference technicalNetwork = findPreference("technical_network");
        if (technicalNetwork != null) {
            technicalNetwork.setSummary(Util.getNetworkInfo(getContext()));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((ActivitySettings) getActivity()).getSupportActionBar().setTitle(R.string.setting_technical);
    }
}
