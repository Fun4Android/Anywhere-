package com.absinthe.anywhere_.ui.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.absinthe.anywhere_.R;
import com.absinthe.anywhere_.model.Const;
import com.absinthe.anywhere_.model.GlobalValues;
import com.absinthe.anywhere_.utils.AppUtils;

public class LabFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    static LabFragment newInstance() {
        return new LabFragment();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_lab, rootKey);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Preference pagesPreference = findPreference(Const.PREF_PAGES);

        if (pagesPreference != null) {
            pagesPreference.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case Const.PREF_PAGES:
                GlobalValues.setsIsPages((boolean) newValue);
                AppUtils.restart();
                return true;
            default:
                break;
        }
        return false;
    }
}
