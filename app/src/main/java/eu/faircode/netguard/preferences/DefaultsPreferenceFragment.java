package eu.faircode.netguard.preferences;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import eu.faircode.netguard.ActivitySettings;
import eu.faircode.netguard.R;

public class DefaultsPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences_defaults);
    }

    @Override
    public void onStart() {
        super.onStart();
        ((ActivitySettings) getActivity()).getSupportActionBar().setTitle(R.string.setting_defaults);
    }
}