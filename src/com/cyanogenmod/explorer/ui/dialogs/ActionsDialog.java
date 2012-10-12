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

package com.cyanogenmod.explorer.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.explorer.R;
import com.cyanogenmod.explorer.adapters.TwoColumnsMenuListAdapter;
import com.cyanogenmod.explorer.listeners.OnRequestRefreshListener;
import com.cyanogenmod.explorer.listeners.OnSelectionListener;
import com.cyanogenmod.explorer.model.FileSystemObject;
import com.cyanogenmod.explorer.ui.policy.ActionsPolicy;
import com.cyanogenmod.explorer.ui.policy.ActionsPolicy.LinkedResource;
import com.cyanogenmod.explorer.util.DialogHelper;
import com.cyanogenmod.explorer.util.FileHelper;
import com.cyanogenmod.explorer.util.SelectionHelper;
import com.cyanogenmod.explorer.util.StorageHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that wraps a dialog for showing the list of actions that
 * the user can do.
 */
public class ActionsDialog implements OnItemClickListener, OnItemLongClickListener {

    /**
     * @hide
     */
    final Context mContext;
    private final boolean mGlobal;

    /**
     * @hide
     */
    AlertDialog mDialog;
    private ListView mListView;
    /**
     * @hide
     */
    final FileSystemObject mFso;

    /**
     * @hide
     */
    OnRequestRefreshListener mOnRequestRefreshListener;
    /**
     * @hide
     */
    OnSelectionListener mOnSelectionListener;

    /**
     * Constructor of <code>ActionsDialog</code>.
     *
     * @param context The current context
     * @param fso The file system object associated
     * @param global If the menu to display will be the global one (Global actions)
     */
    public ActionsDialog(Context context, FileSystemObject fso, boolean global) {
        super();

        //Save the data
        this.mFso = fso;
        this.mContext = context;
        this.mGlobal = global;

        //Initialize dialog
        init(context, global ? R.id.mnu_actions_global : R.id.mnu_actions_fso);
    }

    /**
     * Method that initializes the dialog.
     *
     * @param context The current context
     * @param group The group of action menus to show
     */
    private void init(Context context, int group) {
        //Create the menu adapter
        TwoColumnsMenuListAdapter adapter =
                new TwoColumnsMenuListAdapter(context, R.menu.actions, group);
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);

        //Create the list view
        this.mListView = new ListView(context);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        this.mListView.setLayoutParams(params);
        this.mListView.setAdapter(adapter);

        //Create the dialog
        this.mDialog = DialogHelper.createDialog(
                                        context,
                                        R.drawable.ic_holo_light_actions,
                                        R.string.actions_dialog_title,
                                        this.mListView);
    }

    /**
     * Method that sets the listener for communicate a refresh request.
     *
     * @param onRequestRefreshListener The request refresh listener
     */
    public void setOnRequestRefreshListener(OnRequestRefreshListener onRequestRefreshListener) {
        this.mOnRequestRefreshListener = onRequestRefreshListener;
    }

    /**
     * Method that sets the listener for requesting selection data
     *
     * @param onSelectionListener The request selection data  listener
     */
    public void setOnSelectionListener(OnSelectionListener onSelectionListener) {
        this.mOnSelectionListener = onSelectionListener;
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        TwoColumnsMenuListAdapter adapter =
                (TwoColumnsMenuListAdapter)this.mListView.getAdapter();
        configureMenu(adapter.getMenu());
        this.mDialog.show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {

        //Retrieve the menu item
        MenuItem menuItem = ((TwoColumnsMenuListAdapter)parent.getAdapter()).getItemById((int)id);

        //What action was selected?
        switch ((int)id) {
            //- Create new object
            case R.id.mnu_actions_new_directory:
            case R.id.mnu_actions_new_file:
                // Dialog is dismissed inside showInputNameDialog
                if (this.mOnSelectionListener != null) {
                    showInputNameDialog(menuItem);
                    return;
                }
                break;

            //- Rename
            case R.id.mnu_actions_rename:
                // Dialog is dismissed inside showInputNameDialog
                if (this.mOnSelectionListener != null) {
                    showFsoInputNameDialog(menuItem, this.mFso, false);
                    return;
                }
                break;

            //- Create link
            case R.id.mnu_actions_create_link:
                // Dialog is dismissed inside showInputNameDialog
                if (this.mOnSelectionListener != null) {
                    showFsoInputNameDialog(menuItem, this.mFso, true);
                    return;
                }
                break;
            case R.id.mnu_actions_create_link_global:
                // Dialog is dismissed inside showInputNameDialog
                if (this.mOnSelectionListener != null) {
                    // The selection must be only 1 item
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    if (selection != null && selection.size() == 1) {
                        showFsoInputNameDialog(menuItem, selection.get(0), true);
                    }
                    return;
                }
                break;

            //- Delete
            case R.id.mnu_actions_delete:
                ActionsPolicy.removeFileSystemObject(
                        this.mContext,
                        this.mFso,
                        this.mOnSelectionListener,
                        this.mOnRequestRefreshListener);
                break;

            //- Refresh
            case R.id.mnu_actions_refresh:
                if (this.mOnRequestRefreshListener != null) {
                    this.mOnRequestRefreshListener.onRequestRefresh(null); //Refresh all
                }
                break;

            //- Select/Deselect
            case R.id.mnu_actions_select:
            case R.id.mnu_actions_deselect:
                if (this.mOnSelectionListener != null) {
                    this.mOnSelectionListener.onToggleSelection(this.mFso);
                }
                break;
            case R.id.mnu_actions_select_all:
                if (this.mOnSelectionListener != null) {
                    this.mOnSelectionListener.onSelectAllVisibleItems();
                }
                break;
            case R.id.mnu_actions_deselect_all:
                if (this.mOnSelectionListener != null) {
                    this.mOnSelectionListener.onDeselectAllVisibleItems();
                }
                break;

            //- Open
            case R.id.mnu_actions_open:
                ActionsPolicy.openFileSystemObject(this.mContext, this.mFso, false);
                break;
            //- Open with
            case R.id.mnu_actions_open_with:
                ActionsPolicy.openFileSystemObject(this.mContext, this.mFso, true);
                break;
            //- Send
            case R.id.mnu_actions_send:
                ActionsPolicy.sendFileSystemObject(this.mContext, this.mFso);
                break;


            // Paste selection
            case R.id.mnu_actions_paste_selection:
                if (this.mOnSelectionListener != null) {
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    ActionsPolicy.copyFileSystemObjects(
                            this.mContext,
                            createLinkedResource(selection, this.mFso),
                            this.mOnSelectionListener,
                            this.mOnRequestRefreshListener);
                }
                break;
            // Move selection
            case R.id.mnu_actions_move_selection:
                if (this.mOnSelectionListener != null) {
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    ActionsPolicy.moveFileSystemObjects(
                            this.mContext,
                            createLinkedResource(selection, this.mFso),
                            this.mOnSelectionListener,
                            this.mOnRequestRefreshListener);
                }
                break;
            // Delete selection
            case R.id.mnu_actions_delete_selection:
                if (this.mOnSelectionListener != null) {
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    ActionsPolicy.removeFileSystemObjects(
                            this.mContext,
                            selection,
                            this.mOnSelectionListener,
                            this.mOnRequestRefreshListener);
                }
                break;

            //- Create copy
            case R.id.mnu_actions_create_copy:
                // Create a copy of the fso
                if (this.mOnSelectionListener != null) {
                    ActionsPolicy.createCopyFileSystemObject(
                                this.mContext,
                                this.mFso,
                                this.mOnSelectionListener,
                                this.mOnRequestRefreshListener);
                }
                break;

            //- Add to bookmarks
            case R.id.mnu_actions_add_to_bookmarks:
            case R.id.mnu_actions_add_to_bookmarks_current_folder:
                ActionsPolicy.addToBookmarks(this.mContext, this.mFso);
                break;

            //- Properties
            case R.id.mnu_actions_properties:
            case R.id.mnu_actions_properties_current_folder:
                ActionsPolicy.showPropertiesDialog(
                        this.mContext, this.mFso, this.mOnRequestRefreshListener);
                break;

            default:
                break;
        }

        //Dismiss the dialog
        this.mDialog.dismiss();
    }

    /**
     * Method that show a new dialog for input a name.
     *
     * @param menuItem The item menu associated
     */
    private void showInputNameDialog(final MenuItem menuItem) {
        //Hide the dialog
        this.mDialog.hide();

        //Show the input name dialog
        final InputNameDialog inputNameDialog =
                new InputNameDialog(
                        this.mContext,
                        this.mOnSelectionListener.onRequestCurrentItems(),
                        menuItem.getTitle().toString());
        inputNameDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //Show the menu again
                ActionsDialog.this.mDialog.show();
            }
        });
        inputNameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //Retrieve the name an execute the action
                try {
                    String name = inputNameDialog.getName();
                    createNewFileSystemObject(menuItem.getItemId(), name);

                } finally {
                    ActionsDialog.this.mDialog.dismiss();
                }
            }
        });
        inputNameDialog.show();
    }

    /**
     * Method that show a new dialog for input a name for an existing fso.
     *
     * @param menuItem The item menu associated
     * @param fso The file system object
     * @param allowFsoName If allow that the name of the fso will be returned
     */
    private void showFsoInputNameDialog(
            final MenuItem menuItem, final FileSystemObject fso, final boolean allowFsoName) {
        //Hide the dialog
        this.mDialog.hide();

        //Show the input name dialog
        final InputNameDialog inputNameDialog =
                new InputNameDialog(
                        this.mContext,
                        this.mOnSelectionListener.onRequestCurrentItems(),
                        fso,
                        allowFsoName,
                        menuItem.getTitle().toString());
        inputNameDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //Show the menu again
                ActionsDialog.this.mDialog.show();
            }
        });
        inputNameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //Retrieve the name an execute the action
                try {
                    String name = inputNameDialog.getName();
                    switch (menuItem.getItemId()) {
                        case R.id.mnu_actions_rename:
                            // Rename the fso
                            if (ActionsDialog.this.mOnSelectionListener != null) {
                                ActionsPolicy.renameFileSystemObject(
                                        ActionsDialog.this.mContext,
                                        inputNameDialog.mFso,
                                        name,
                                        ActionsDialog.this.mOnSelectionListener,
                                        ActionsDialog.this.mOnRequestRefreshListener);
                            }
                            break;

                        case R.id.mnu_actions_create_link:
                        case R.id.mnu_actions_create_link_global:
                            // Create a link to the fso
                            if (ActionsDialog.this.mOnSelectionListener != null) {
                                ActionsPolicy.createSymlink(
                                        ActionsDialog.this.mContext,
                                        inputNameDialog.mFso,
                                        name,
                                        ActionsDialog.this.mOnSelectionListener,
                                        ActionsDialog.this.mOnRequestRefreshListener);
                            }
                            break;

                        default:
                            break;
                    }

                } finally {
                    ActionsDialog.this.mDialog.dismiss();
                }
            }
        });
        inputNameDialog.show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        DialogHelper.showToast(
                this.mContext,
                ((TextView)view).getText().toString(),
                Toast.LENGTH_SHORT);
        return true;
    }

    /**
     * Method that create the a new file system object.
     *
     * @param menuId The menu identifier (need to determine the fso type)
     * @param name The name of the file system object
     * @hide
     */
    void createNewFileSystemObject(final int menuId, final String name) {
        switch (menuId) {
            case R.id.mnu_actions_new_directory:
                ActionsPolicy.createNewDirectory(
                        this.mContext, name,
                        this.mOnSelectionListener, this.mOnRequestRefreshListener);
                break;
            case R.id.mnu_actions_new_file:
                ActionsPolicy.createNewFile(
                        this.mContext, name,
                        this.mOnSelectionListener, this.mOnRequestRefreshListener);
                break;
            default:
                break;
        }
    }

    /**
     * Method that configure the menu to show according the actual information,
     * the kind of request, the file selection, the mount point, ...
     *
     * @param menu The menu to configure
     */
    private void configureMenu(Menu menu) {
        // Check actions that needs a valid reference
        if (!this.mGlobal && this.mFso != null) {
            //- Select/Deselect -> Only one of them
            if (this.mOnSelectionListener != null) {
                boolean selected =
                        SelectionHelper.isFileSystemObjectSelected(
                                this.mOnSelectionListener.onRequestSelectedFiles(),
                                this.mFso);
                menu.removeItem(selected ? R.id.mnu_actions_select : R.id.mnu_actions_deselect);
            } else {
                // Remove both menus
                menu.removeItem(R.id.mnu_actions_select);
                menu.removeItem(R.id.mnu_actions_deselect);
            }

            //- Open/Open with -> Only when the fso is not a folder and is not a system file
            if (FileHelper.isDirectory(this.mFso) || FileHelper.isSystemFile(this.mFso)) {
                menu.removeItem(R.id.mnu_actions_open);
                menu.removeItem(R.id.mnu_actions_open_with);
                menu.removeItem(R.id.mnu_actions_send);
            }
            
            // Create link (not allow in storage volume)
            if (StorageHelper.isPathInStorageVolume(this.mFso.getFullPath())) {
                menu.removeItem(R.id.mnu_actions_create_link);
            }
        }

        //- Add to bookmarks -> Only directories
        if (this.mFso != null && FileHelper.isRootDirectory(this.mFso)) {
            menu.removeItem(R.id.mnu_actions_add_to_bookmarks);
            menu.removeItem(R.id.mnu_actions_add_to_bookmarks_current_folder);
        }

        // Paste/Move only when have a selection
        // Create link
        if (this.mGlobal) {
            List<FileSystemObject> selection = null;
            if (this.mOnSelectionListener != null) {
                selection = this.mOnSelectionListener.onRequestSelectedFiles();
            }
            if (selection == null || selection.size() == 0 ||
                    (this.mFso != null && !FileHelper.isDirectory(this.mFso))) {
                // Remove paste/move actions
                menu.removeItem(R.id.mnu_actions_paste_selection);
                menu.removeItem(R.id.mnu_actions_move_selection);
                menu.removeItem(R.id.mnu_actions_delete_selection);
            }
            if (selection == null || selection.size() == 0 || selection.size() > 1) {
                // Only when one item is selected
                menu.removeItem(R.id.mnu_actions_create_link_global);
            } else {
                // Create link (not allow in storage volume)
                FileSystemObject fso = selection.get(0);
                if (StorageHelper.isPathInStorageVolume(fso.getFullPath())) {
                    menu.removeItem(R.id.mnu_actions_create_link);
                }
            }
        }
    }

    /**
     * Method that creates a {@link LinkedResource} for the list of object to the
     * destination directory
     *
     * @param items The list of the source items
     * @param directory The destination directory
     */
    private static List<LinkedResource> createLinkedResource(
            List<FileSystemObject> items, FileSystemObject directory) {
        List<LinkedResource> resources =
                new ArrayList<ActionsPolicy.LinkedResource>(items.size());
        for (int i = 0; i < items.size(); i++) {
            FileSystemObject fso = items.get(i);
            File src = new File(fso.getFullPath());
            File dst = new File(directory.getFullPath(), fso.getName());
            resources.add(new LinkedResource(src, dst));
        }
        return resources;
    }
}
