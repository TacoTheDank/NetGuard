package eu.faircode.netguard.preferences;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import eu.faircode.netguard.ActivitySettings;
import eu.faircode.netguard.R;

public class DevPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences_dev);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((ActivitySettings) getActivity()).getSupportActionBar().setTitle(R.string.setting_development_options);
    }
}
