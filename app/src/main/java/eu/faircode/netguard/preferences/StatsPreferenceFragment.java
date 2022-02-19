package eu.faircode.netguard.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import eu.faircode.netguard.ActivityPro;
import eu.faircode.netguard.ActivitySettings;
import eu.faircode.netguard.IAB;
import eu.faircode.netguard.R;

public class StatsPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences_stats);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        final SwitchPreferenceCompat showStats = findPreference("show_stats");
        ActivitySettings.markPro(getContext(), showStats, ActivityPro.SKU_SPEED);
        showStats.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!IAB.isPurchased(ActivityPro.SKU_SPEED, getContext())) {
                startActivity(new Intent(getContext(), ActivityPro.class));
                return false;
            }
            return true;
        });

        final SwitchPreferenceCompat showTop = findPreference("show_top");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            showTop.setVisible(false);
        }

        final EditTextPreference statsFrequency = findPreference("stats_frequency");
        statsFrequency.setTitle(getString(
                R.string.setting_stats_frequency, prefs.getString("stats_frequency", "1000")));
        statsFrequency.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        statsFrequency.setOnPreferenceChangeListener((preference, newValue) -> {
            statsFrequency.setTitle(getString(R.string.setting_stats_frequency, prefs.getString("stats_frequency", "1000")));
            return true;
        });

        final EditTextPreference statsSamples = findPreference("stats_samples");
        statsSamples.setTitle(getString(
                R.string.setting_stats_samples, prefs.getString("stats_samples", "90")));
        statsSamples.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        statsSamples.setOnPreferenceChangeListener((preference, newValue) -> {
            statsSamples.setTitle(getString(R.string.setting_stats_samples, prefs.getString("stats_samples", "90")));
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((ActivitySettings) getActivity()).getSupportActionBar().setTitle(R.string.setting_stats_category);
    }
}
