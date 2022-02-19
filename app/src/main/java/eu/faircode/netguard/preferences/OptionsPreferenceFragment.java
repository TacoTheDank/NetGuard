package eu.faircode.netguard.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import eu.faircode.netguard.ActivityPro;
import eu.faircode.netguard.ActivitySettings;
import eu.faircode.netguard.IAB;
import eu.faircode.netguard.R;
import eu.faircode.netguard.Util;

public class OptionsPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences_options);

        final ListPreference screenTheme = findPreference("theme");
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
        autoEnable.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

        final EditTextPreference screenDelay = findPreference("screen_delay");
        screenDelay.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

        if (Util.isPlayStoreInstall(getContext()) || !Util.hasValidFingerprint(getContext())) {
            final SwitchPreferenceCompat updateCheck = findPreference("update_check");
            updateCheck.setVisible(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((ActivitySettings) getActivity()).getSupportActionBar().setTitle(R.string.setting_options);
    }
}
