package eu.faircode.netguard.preferences;

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

import android.os.Bundle;

import androidx.annotation.XmlRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import eu.faircode.netguard.ActivitySettings;
import eu.faircode.netguard.R;
import eu.faircode.netguard.Util;

public class MainPreferenceFragment extends PreferenceFragmentCompat {

    private ActivitySettings activity;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences_main);

        activity = (ActivitySettings) getActivity();

        declareSettingsScreen("screen_defaults", R.xml.preferences_defaults);
        declareSettingsScreen("screen_options", R.xml.preferences_options);
        declareSettingsScreen("screen_network_options", R.xml.preferences_network);
        declareSettingsScreen("screen_advanced_options", R.xml.preferences_advanced);
        declareSettingsScreen("screen_stats", R.xml.preferences_stats);
        declareSettingsScreen("screen_backup", R.xml.preferences_backup);
        if (!Util.isDebuggable(activity)) {
            final Preference development = findPreference("screen_development");
            development.setVisible(false);
        } else {
            declareSettingsScreen("screen_development", R.xml.preferences_dev);
        }
        declareSettingsScreen("screen_technical", R.xml.preferences_technical);
    }

    private void declareSettingsScreen(final String key, @XmlRes final int screen) {
        final Preference pref = findPreference(key);
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                activity.openSettingsScreen(screen);
                return true;
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.getSupportActionBar().setTitle(R.string.menu_settings);
    }
}
