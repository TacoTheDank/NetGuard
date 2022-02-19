package eu.faircode.netguard.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import eu.faircode.netguard.ActivityPro;
import eu.faircode.netguard.ActivitySettings;
import eu.faircode.netguard.IAB;
import eu.faircode.netguard.R;
import eu.faircode.netguard.Util;

public class OptionsPreferenceFragment extends PreferenceFragmentCompat {

    private SharedPreferences prefs;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences_options);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        final ListPreference screenTheme = findPreference("theme");
        setScreenThemeTitle(screenTheme);
        screenTheme.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!IAB.isPurchased(ActivityPro.SKU_THEME, getContext())) {
                startActivity(new Intent(getContext(), ActivityPro.class));
                return false;
            }
            return true;
        });
        ActivitySettings.markPro(getContext(), screenTheme, ActivityPro.SKU_THEME);

        final SwitchPreferenceCompat install = findPreference("install");
        install.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!IAB.isPurchased(ActivityPro.SKU_NOTIFY, getContext())) {
                startActivity(new Intent(getContext(), ActivityPro.class));
                return false;
            }
            return true;
        });
        ActivitySettings.markPro(getContext(), install, ActivityPro.SKU_NOTIFY);

        final EditTextPreference autoEnable = findPreference("auto_enable");
        autoEnable.setTitle(getString(R.string.setting_auto, prefs.getString("auto_enable", "0")));
        autoEnable.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        autoEnable.setOnPreferenceChangeListener((preference, newValue) -> {
            autoEnable.setTitle(getString(R.string.setting_auto, prefs.getString("auto_enable", "0")));
            return true;
        });

        final EditTextPreference screenDelay = findPreference("screen_delay");
        screenDelay.setTitle(getString(R.string.setting_delay, prefs.getString("screen_delay", "0")));
        screenDelay.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        screenDelay.setOnPreferenceChangeListener((preference, newValue) -> {
            screenDelay.setTitle(getString(R.string.setting_delay, prefs.getString("screen_delay", "0")));
            return true;
        });

        if (Util.isPlayStoreInstall(getContext()) || !Util.hasValidFingerprint(getContext())) {
            final SwitchPreferenceCompat updateCheck = findPreference("update_check");
            updateCheck.setVisible(false);
        }
    }

    private void setScreenThemeTitle(final ListPreference screenTheme) {
        final String theme = prefs.getString("theme", "teal");
        final String[] themeNames = getResources().getStringArray(R.array.themeNames);
        final String[] themeValues = getResources().getStringArray(R.array.themeValues);
        for (int i = 0; i < themeNames.length; i++) {
            if (theme.equals(themeValues[i])) {
                screenTheme.setTitle(getString(R.string.setting_theme, themeNames[i]));
                break;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((ActivitySettings) getActivity()).getSupportActionBar().setTitle(R.string.setting_options);
    }
}
