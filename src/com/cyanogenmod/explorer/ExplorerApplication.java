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

package com.cyanogenmod.explorer;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.util.Log;

import com.cyanogenmod.explorer.console.Console;
import com.cyanogenmod.explorer.console.ConsoleAllocException;
import com.cyanogenmod.explorer.console.ConsoleBuilder;
import com.cyanogenmod.explorer.console.ConsoleHolder;
import com.cyanogenmod.explorer.console.shell.PrivilegedConsole;
import com.cyanogenmod.explorer.preferences.ExplorerSettings;
import com.cyanogenmod.explorer.preferences.Preferences;
import com.cyanogenmod.explorer.util.ExceptionUtil;
import com.cyanogenmod.explorer.util.FileHelper;
import com.cyanogenmod.explorer.util.MimeTypeHelper;

/**
 * A class that wraps the information of the application (constants,
 * identifiers, statics variables, ...).
 * @hide
 */
public final class ExplorerApplication extends Application {

    private static final String TAG = "ExplorerApplication"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    /**
     * A constant that contains the main process name.
     * @hide
     */
    public static final String MAIN_PROCESS = "com.cyanogenmod.explorer"; //$NON-NLS-1$

    //Static resources
    private static ExplorerApplication sApp;
    private static ConsoleHolder sBackgroundConsole;

    private static boolean sDebuggable = false;

    private final BroadcastReceiver mOnSettingChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null &&
                intent.getAction().compareTo(ExplorerSettings.INTENT_SETTING_CHANGED) == 0) {

                // The settings has changed
                String key = intent.getStringExtra(ExplorerSettings.EXTRA_SETTING_CHANGED_KEY);
                if (key != null &&
                    key.compareTo(ExplorerSettings.SETTINGS_SHOW_TRACES.getId()) == 0) {

                    // The debug traces setting has changed. Notify to consoles
                    Console c = null;
                    try {
                        c = getBackgroundConsole();
                    } catch (Exception e) {/**NON BLOCK**/}
                    if (c != null) {
                        c.reloadTrace();
                    }
                    try {
                        c = ConsoleBuilder.getConsole(context, false);
                        if (c != null) {
                            c.reloadTrace();
                        }
                    } catch (Throwable _throw) {/**NON BLOCK**/}
                } else if (key != null && key.compareTo(
                        ExplorerSettings.SETTINGS_ADVANCE_MODE.getId()) == 0) {
                    // Force to change to a privileged console.
                    boolean advancedMode = isAdvancedMode();
                    if (!advancedMode) {
                        // First change to non-privileged console
                        if (!ConsoleBuilder.changeToNonPrivilegedConsole(context)) {
                            // Try with a privileged console
                            ConsoleBuilder.changeToPrivilegedConsole(context);
                            try {
                                Preferences.savePreference(
                                        ExplorerSettings.SETTINGS_SUPERUSER_MODE,
                                        Boolean.TRUE, true);
                            } catch (Throwable ex) {
                                Log.w(TAG, "can't save console preference", ex); //$NON-NLS-1$
                            }
                        } else {
                            try {
                                Preferences.savePreference(
                                        ExplorerSettings.SETTINGS_SUPERUSER_MODE,
                                        Boolean.FALSE, true);
                            } catch (Throwable ex) {
                                Log.w(TAG, "can't save console preference", ex); //$NON-NLS-1$
                            }
                        }

                    }
                }
            }
        }
    };


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        if (DEBUG) {
            Log.d(TAG, "ExplorerApplication.onCreate"); //$NON-NLS-1$
        }
        register();
        init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (DEBUG) {
            Log.d(TAG, "ExplorerApplication.onConfigurationChanged"); //$NON-NLS-1$
        }
        register();
        init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTerminate() {
        if (DEBUG) {
            Log.d(TAG, "onTerminate"); //$NON-NLS-1$
        }
        try {
            unregisterReceiver(this.mOnSettingChangeReceiver);
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
        try {
            destroyBackgroundConsole();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
        try {
            ConsoleBuilder.destroyConsole();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
        super.onTerminate();
    }

    /**
     * Method that register the application context.
     */
    private void register() {
        //Save the static application reference
        sApp = this;

        // Check if the application is debuggable
        sDebuggable = (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));

        // Register the broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(ExplorerSettings.INTENT_SETTING_CHANGED);
        registerReceiver(this.mOnSettingChangeReceiver, filter);
    }

    /**
     * Method that initializes the application.
     */
    private void init() {
        //Sets the default preferences if no value is set yet
        FileHelper.ROOT_DIRECTORY = getString(R.string.root_dir);
        Preferences.loadDefaults();

        //Create a console for background tasks
        allocBackgroundConsole(getApplicationContext());

        //Force the load of mime types
        try {
            MimeTypeHelper.loadMimeTypes(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Mime-types failed.", e); //$NON-NLS-1$
        }
    }

    /**
     * Method that returns the singleton reference of the application.
     *
     * @return Application The application singleton reference
     * @hide
     */
    public static ExplorerApplication getInstance() {
        return sApp;
    }

    /**
     * Method that returns if the application is debuggable
     *
     * @return boolean If the application is debuggable
     */
    public static boolean isDebuggable() {
        return sDebuggable;
    }

    /**
     * Method that returns the application background console.
     *
     * @return Console The background console
     */
    public static Console getBackgroundConsole() {
        if (!sBackgroundConsole.getConsole().isActive()) {
            allocBackgroundConsole(getInstance().getApplicationContext());
        }
        return sBackgroundConsole.getConsole();
    }

    /**
     * Method that destroy the background console
     */
    public static void destroyBackgroundConsole() {
        try {
            sBackgroundConsole.dispose();
        } catch (Throwable ex) {
            /**NON BLOCK**/
        }
    }

    /**
     * Method that allocate a new background console
     *
     * @param ctx The current context
     */
    private static synchronized void allocBackgroundConsole(Context ctx) {
        try {
            // Dispose the current console
            if (sBackgroundConsole != null) {
                sBackgroundConsole.dispose();
                sBackgroundConsole = null;
            }

            //Create a console for background tasks
            if (ConsoleBuilder.isPrivileged()) {
                sBackgroundConsole =
                        new ConsoleHolder(
                                ConsoleBuilder.createPrivilegedConsole(
                                        ctx, FileHelper.ROOT_DIRECTORY));
            } else {
                sBackgroundConsole =
                        new ConsoleHolder(
                                ConsoleBuilder.createNonPrivilegedConsole(
                                        ctx, FileHelper.ROOT_DIRECTORY));
            }
        } catch (Exception e) {
            Log.e(TAG,
                    "Background console creation failed. " +  //$NON-NLS-1$
                    "This probably will cause a force close.", e); //$NON-NLS-1$
        }
    }

    /**
     * Method that changes the background console to a privileged console
     *
     * @throws ConsoleAllocException If the console can't be allocated
     */
    public static void changeBackgroundConsoleToPriviligedConsole()
            throws ConsoleAllocException {
        if (sBackgroundConsole == null ||
              !(sBackgroundConsole.getConsole() instanceof PrivilegedConsole)) {
            try {
                if (sBackgroundConsole != null) {
                    sBackgroundConsole.dispose();
                }
            } catch (Throwable ex) {/**NON BLOCK**/}

            // Change the privileged console
            try {
                sBackgroundConsole =
                        new ConsoleHolder(
                                ConsoleBuilder.createPrivilegedConsole(
                                        getInstance().getApplicationContext(),
                                        FileHelper.ROOT_DIRECTORY));
            } catch (Exception e) {
                try {
                    if (sBackgroundConsole != null) {
                        sBackgroundConsole.dispose();
                    }
                } catch (Throwable ex) {/**NON BLOCK**/}
                sBackgroundConsole = null;
                throw new ConsoleAllocException(
                        "Failed to alloc background console", e); //$NON-NLS-1$
            }
        }
    }



    /**
     * Method that check if the app is signed with the platform signature
     *
     * @param ctx The current context
     * @return boolean If the app is signed with the platform signature
     */
    public static boolean isAppPlatformSignature(Context ctx) {
        // TODO This need to be improved, checking if the app is really with the platform signature
        try {
            // For now only check that the app is installed in system directory
            PackageManager pm = ctx.getPackageManager();
            String appDir = pm.getApplicationInfo(ctx.getPackageName(), 0).sourceDir;
            String systemDir = ctx.getString(R.string.system_dir);
            return appDir.startsWith(systemDir);

        } catch (Exception e) {
            ExceptionUtil.translateException(ctx, e, true, false);
        }
        return false;
    }

    /**
     * Method that returns if the application is running in superuser mode
     *
     * @return boolean If the application is running in superuser mode
     */
    public static boolean isSuperuserMode() {
        boolean defaultValue =
                ((Boolean)ExplorerSettings.
                        SETTINGS_SUPERUSER_MODE.
                            getDefaultValue()).booleanValue();
       String id = ExplorerSettings.SETTINGS_SUPERUSER_MODE.getId();
       return Preferences.getSharedPreferences().getBoolean(id, defaultValue);
    }

    /**
     * Method that returns if the application is running in advanced mode
     *
     * @return boolean If the application is running in advanced mode
     */
    public static boolean isAdvancedMode() {
        boolean defaultValue =
                ((Boolean)ExplorerSettings.
                        SETTINGS_ADVANCE_MODE.
                            getDefaultValue()).booleanValue();
        String id = ExplorerSettings.SETTINGS_ADVANCE_MODE.getId();
       return Preferences.getSharedPreferences().getBoolean(id, defaultValue);
    }

}
