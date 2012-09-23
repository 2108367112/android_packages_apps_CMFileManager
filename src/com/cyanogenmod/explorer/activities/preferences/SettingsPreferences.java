/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.explorer.activities.preferences;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.provider.SearchRecentSuggestions;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.explorer.R;
import com.cyanogenmod.explorer.preferences.ExplorerSettings;
import com.cyanogenmod.explorer.preferences.Preferences;
import com.cyanogenmod.explorer.providers.RecentSearchesContentProvider;

import java.util.List;

/**
 * The {@link SettingsPreferences} preferences
 */
public class SettingsPreferences extends PreferenceActivity {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initialize action bars
        initTitleActionBar();
    }

    /**
     * Method that initializes the titlebar of the activity.
     */
    private void initTitleActionBar() {
      //Configure the action bar options
        getActionBar().setBackgroundDrawable(
                getResources().getDrawable(R.drawable.bg_holo_titlebar));
        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        View customTitle = getLayoutInflater().inflate(R.layout.simple_customtitle, null, false);
        TextView title = (TextView)customTitle.findViewById(R.id.customtitle_title);
        title.setText(R.string.pref);
        title.setContentDescription(getString(R.string.pref));
        getActionBar().setCustomView(customTitle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);

        // Retrieve the about header
        Header aboutHeader = target.get(target.size()-1);
        try {
            String appver =
                    this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
            aboutHeader.summary = getString(R.string.pref_about_summary, appver);
        } catch (Exception e) {
            aboutHeader.summary = getString(R.string.pref_about_summary, ""); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()) {
          case android.R.id.home:
              finish();
              return true;
          default:
             return super.onOptionsItemSelected(item);
       }
    }

    /**
     * A class that manages the commons options of the application
     */
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        private final OnPreferenceChangeListener mOnChangeListener =
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                // Notify the change
                Intent intent = new Intent(ExplorerSettings.INTENT_SETTING_CHANGED);
                intent.putExtra(ExplorerSettings.EXTRA_SETTING_CHANGED_KEY, preference.getKey());
                getActivity().sendBroadcast(intent);

                return true;
            }
        };

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Change the preference manager
            getPreferenceManager().setSharedPreferencesName(Preferences.SETTINGS_FILENAME);
            getPreferenceManager().setSharedPreferencesMode(MODE_PRIVATE);

            // Add the preferences
            addPreferencesFromResource(R.xml.preferences_general);
        }
    }

    /**
     * A class that manages the search options
     */
    public static class SearchPreferenceFragment extends PreferenceFragment {

        // Internal keys
        private static final String REMOVE_SEARCH_TERMS_KEY =
                                            "cm_explorer_remove_saved_search_terms"; //$NON-NLS-1$

        private CheckBoxPreference mHighlightTerms;
        private CheckBoxPreference mShowRelevanceWidget;
        private CheckBoxPreference mSaveSearchTerms;
        private Preference mRemoveSearchTerms;

        private final OnPreferenceChangeListener mOnChangeListener =
                new OnPreferenceChangeListener() {
            @Override
            @SuppressWarnings("synthetic-access")
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (preference.getKey().compareTo(
                        ExplorerSettings.SETTINGS_SAVE_SEARCH_TERMS.getId()) == 0) {
                    if (!((Boolean)newValue).booleanValue()) {
                        // Remove search terms if saved search terms
                        // is not active by the user
                        clearRecentSearchTerms();
                    }
                }

                // Notify the change
                Intent intent = new Intent(ExplorerSettings.INTENT_SETTING_CHANGED);
                intent.putExtra(ExplorerSettings.EXTRA_SETTING_CHANGED_KEY, preference.getKey());
                getActivity().sendBroadcast(intent);

                return true;
            }
        };

        private final OnPreferenceClickListener mOnClickListener =
                new Preference.OnPreferenceClickListener() {
            @Override
            @SuppressWarnings("synthetic-access")
            public boolean onPreferenceClick(Preference preference) {
                if (preference.getKey().compareTo(REMOVE_SEARCH_TERMS_KEY) == 0) {
                    // Remove search terms
                    clearRecentSearchTerms();

                    // Advise the user
                    Toast.makeText(
                            getActivity(),
                            getActivity().getString(R.string.pref_remove_saved_search_terms_msg),
                            Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        };

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Change the preference manager
            getPreferenceManager().setSharedPreferencesName(Preferences.SETTINGS_FILENAME);
            getPreferenceManager().setSharedPreferencesMode(MODE_PRIVATE);

            // Add the preferences
            addPreferencesFromResource(R.xml.preferences_search);

            // Highlight terms
            this.mHighlightTerms =
                    (CheckBoxPreference)findPreference(
                            ExplorerSettings.SETTINGS_HIGHLIGHT_TERMS.getId());
            this.mHighlightTerms.setOnPreferenceChangeListener(this.mOnChangeListener);

            // Relevance widget
            this.mShowRelevanceWidget =
                    (CheckBoxPreference)findPreference(
                            ExplorerSettings.SETTINGS_SHOW_RELEVANCE_WIDGET.getId());
            this.mShowRelevanceWidget.setOnPreferenceChangeListener(this.mOnChangeListener);

            // Saved search terms
            this.mSaveSearchTerms =
                    (CheckBoxPreference)findPreference(
                            ExplorerSettings.SETTINGS_SAVE_SEARCH_TERMS.getId());
            this.mSaveSearchTerms.setOnPreferenceChangeListener(this.mOnChangeListener);

            // Remove search terms
            this.mRemoveSearchTerms = findPreference(REMOVE_SEARCH_TERMS_KEY);
            this.mRemoveSearchTerms.setOnPreferenceClickListener(this.mOnClickListener);
        }

        /**
         * Method that removes the recent suggestions on search activity
         */
        private void clearRecentSearchTerms() {
            SearchRecentSuggestions suggestions =
                    new SearchRecentSuggestions(getActivity(),
                            RecentSearchesContentProvider.AUTHORITY,
                            RecentSearchesContentProvider.MODE);
            suggestions.clearHistory();
            Preferences.setLastSearch(null);
        }
    }

    /**
     * A class that manages the debug options of the application
     */
    public static class DebugPreferenceFragment extends PreferenceFragment {

        private CheckBoxPreference mDebugTraces;

        private final OnPreferenceChangeListener mOnChangeListener =
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                // Notify the change
                Intent intent = new Intent(ExplorerSettings.INTENT_SETTING_CHANGED);
                intent.putExtra(ExplorerSettings.EXTRA_SETTING_CHANGED_KEY, preference.getKey());
                getActivity().sendBroadcast(intent);

                return true;
            }
        };

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Change the preference manager
            getPreferenceManager().setSharedPreferencesName(Preferences.SETTINGS_FILENAME);
            getPreferenceManager().setSharedPreferencesMode(MODE_PRIVATE);

            // Add the preferences
            addPreferencesFromResource(R.xml.preferences_debug);

            // Capture Debug traces
            this.mDebugTraces =
                    (CheckBoxPreference)findPreference(
                            ExplorerSettings.SETTINGS_SHOW_TRACES.getId());
            this.mDebugTraces.setOnPreferenceChangeListener(this.mOnChangeListener);
        }
    }

}
