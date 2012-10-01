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

package com.cyanogenmod.explorer.ui.policy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.cyanogenmod.explorer.R;
import com.cyanogenmod.explorer.console.RelaunchableException;
import com.cyanogenmod.explorer.listeners.OnRequestRefreshListener;
import com.cyanogenmod.explorer.listeners.OnSelectionListener;
import com.cyanogenmod.explorer.model.Bookmark;
import com.cyanogenmod.explorer.model.Bookmark.BOOKMARK_TYPE;
import com.cyanogenmod.explorer.model.FileSystemObject;
import com.cyanogenmod.explorer.preferences.Bookmarks;
import com.cyanogenmod.explorer.ui.dialogs.FsoPropertiesDialog;
import com.cyanogenmod.explorer.util.CommandHelper;
import com.cyanogenmod.explorer.util.DialogHelper;
import com.cyanogenmod.explorer.util.ExceptionUtil;
import com.cyanogenmod.explorer.util.ExceptionUtil.OnRelaunchCommandResult;
import com.cyanogenmod.explorer.util.FileHelper;
import com.cyanogenmod.explorer.util.MimeTypeHelper;

import java.io.File;
import java.util.List;


/**
 * A class with the convenience methods for resolve actions
 */
public final class ActionsPolicy {

    // A class for listen the relaunch of commands
    private static class RemoveItemListenter implements OnRelaunchCommandResult {
        /**
         * @hide
         */
        OnRequestRefreshListener mOnRequestRefreshListener;
        /**
         * @hide
         */
        FileSystemObject mFso;

        RemoveItemListenter() {/**NON BLOCK**/}

        @Override
        public void onSuccess() {
            //Operation complete. Refresh
            if (this.mOnRequestRefreshListener != null) {
                this.mOnRequestRefreshListener.onRequestRemove(this.mFso);
            }
        }
        @Override
        public void onCancelled() {/**NON BLOCK**/}
        @Override
        public void onFailed() {/**NON BLOCK**/}
    }


    private static final String TAG = "ActionPolicy"; //$NON-NLS-1$

    private static boolean DEBUG = false;

    /**
     * Constructor of <code>ActionsPolicy</code>.
     */
    private ActionsPolicy() {
        super();
    }

    /**
     * Method that show a {@link Toast} with the content description of a {@link FileSystemObject}.
     *
     * @param ctx The current context
     * @param fso The file system object
     */
    public static void showContentDescription(final Context ctx, final FileSystemObject fso) {
        String contentDescription = fso.getFullPath();
        Toast.makeText(ctx, contentDescription, Toast.LENGTH_SHORT).show();
    }

    /**
     * Method that show a new dialog for show {@link FileSystemObject} properties.
     *
     * @param ctx The current context
     * @param fso The file system object
     * @param onRequestRefreshListener The listener for request a refresh after properties
     * of the {@link FileSystemObject} were changed (optional)
     */
    public static void showPropertiesDialog(
            final Context ctx, final FileSystemObject fso,
            final OnRequestRefreshListener onRequestRefreshListener) {
        //Show a the filesystem info dialog
        final FsoPropertiesDialog dialog = new FsoPropertiesDialog(ctx, fso);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dlg) {
                // Any change?
                if (dialog.isHasChanged()) {
                    if (onRequestRefreshListener != null) {
                        onRequestRefreshListener.onRequestRefresh(dialog.getFso());
                    }
                }
            }
        });
        dialog.show();
    }

    /**
     * Method that opens a {@link FileSystemObject} with the default application registered
     * by the system.
     *
     * @param ctx The current context
     * @param fso The file system object
     * @param choose If allow the user to select the app to open with
     */
    public static void openFileSystemObject(
            final Context ctx, final FileSystemObject fso, final boolean choose) {
        try {
            // Create the intent to
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);

            // Obtain the mime/type and passed it to intent
            String mime = MimeTypeHelper.getMimeType(ctx, fso);
            File file = new File(fso.getFullPath());
            if (mime != null) {
                intent.setDataAndType(Uri.fromFile(file), mime);
            } else {
                intent.setData(Uri.fromFile(file));
            }

            //Retrieve the activities that can handle the file
            final PackageManager packageManager = ctx.getPackageManager();
            if (DEBUG) {
                intent.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
            }
            List<ResolveInfo> info =
                    packageManager.
                        queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            // Now we have the list of activities that can handle the file. The next steps are:
            //
            // 1.- If choose, then show open with dialog
            // 1.- If info size == 0. No default application, then show open with dialog
            // 2.- If !choose, seek inside our database the default activity for the extension
            //     and open the file with this application
            // 3.- If no default activity saved, then use system default

            if (choose || info.size() == 0) {
                // Show open with dialog
                return;
            }



            // 2.- info size == 0



//            if (info.size() == 0) {
//                DialogHelper.createWarningDialog(ctx, R.string.msgs_not_registered_app);
//            }
//
//            // Open with?
//            if (choose) {
//                String title = ctx.getString(R.string.actions_menu_open_with);
//                ((Activity)ctx).startActivity(Intent.createChooser(intent,title));
//
//            } else {
//                ((Activity)ctx).startActivity(intent);
//            }

        } catch (Exception e) {
            ExceptionUtil.translateException(ctx, e);
        }
    }

    /**
     * Method that adds the {@link FileSystemObject} to the bookmarks database.
     *
     * @param ctx The current context
     * @param fso The file system object
     */
    public static void addToBookmarks(final Context ctx, final FileSystemObject fso) {
        try {
            // Create the bookmark
            Bookmark bookmark =
                    new Bookmark(BOOKMARK_TYPE.USER_DEFINED, fso.getName(), fso.getFullPath());
            bookmark = Bookmarks.addBookmark(ctx, bookmark);
            if (bookmark == null) {
                // The operation fails
                Toast.makeText(
                        ctx,
                        R.string.msgs_operation_failure,
                        Toast.LENGTH_SHORT).show();
            } else {
                // Success
                Toast.makeText(
                        ctx,
                        R.string.bookmarks_msgs_add_success,
                        Toast.LENGTH_SHORT).show();

            }

        } catch (Exception e) {
            ExceptionUtil.translateException(ctx, e);
        }
    }

    /**
     * Method that create the a new file system object.
     *
     * @param ctx The current context
     * @param name The name of the file to be created
     * @param onSelectionListener The selection listener (required)
     * @param onRequestRefreshListener The listener for request a refresh after the new
     * file was created (option)
     */
    public static void createNewFile(
            final Context ctx, final String name,
            final OnSelectionListener onSelectionListener,
            final OnRequestRefreshListener onRequestRefreshListener) {
        createNewFileSystemObject(ctx, name, false, onSelectionListener, onRequestRefreshListener);
    }

    /**
     * Method that create the a new folder system object.
     *
     * @param ctx The current context
     * @param name The name of the file to be created
     * @param onSelectionListener The selection listener (required)
     * @param onRequestRefreshListener The listener for request a refresh after the new
     * folder was created (option)
     */
    public static void createNewDirectory(
            final Context ctx, final String name,
            final OnSelectionListener onSelectionListener,
            final OnRequestRefreshListener onRequestRefreshListener) {
        createNewFileSystemObject(ctx, name, true, onSelectionListener, onRequestRefreshListener);
    }

    /**
     * Method that create the a new file system object.
     *
     * @param ctx The current context
     * @param name The name of the file to be created
     * @param folder If the new {@link FileSystemObject} to create is a folder (true) or a
     * file (false).
     * @param onSelectionListener The selection listener (required)
     * @param onRequestRefreshListener The listener for request a refresh after the new
     * folder was created (option)
     */
    private static void createNewFileSystemObject(
            final Context ctx, final String name, final boolean folder,
            final OnSelectionListener onSelectionListener,
            final OnRequestRefreshListener onRequestRefreshListener) {

        //Create the absolute file name
        File newFso = new File(
                onSelectionListener.onRequestCurrentDirOfSelectionData(), name);
        final String newName = newFso.getAbsolutePath();

        try {
            if (folder) {
                if (DEBUG) {
                    Log.d(TAG, String.format("Creating new directory: %s", newName)); //$NON-NLS-1$
                }
                CommandHelper.createDirectory(ctx, newName, null);
            } else {
                if (DEBUG) {
                    Log.d(TAG, String.format("Creating new file: %s", newName)); //$NON-NLS-1$
                }
                CommandHelper.createFile(ctx, newName, null);
            }

            //Operation complete. Show refresh
            if (onRequestRefreshListener != null) {
                FileSystemObject fso = null;
                try {
                    fso = CommandHelper.getFileInfo(ctx, newName, null);
                } catch (Throwable ex2) {
                    /**NON BLOCK**/
                }
                onRequestRefreshListener.onRequestRefresh(fso);
            }

        } catch (Throwable ex) {
            //Capture the exception
            if (ex instanceof RelaunchableException) {
                ExceptionUtil.attachAsyncTask(ex, new AsyncTask<Object, Integer, Boolean>() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    protected Boolean doInBackground(Object... params) {
                        //Operation complete. Show refresh
                        if (onRequestRefreshListener != null) {
                            FileSystemObject fso = null;
                            try {
                                fso =
                                    CommandHelper.getFileInfo(ctx, newName, null);
                            } catch (Throwable ex2) {
                                /**NON BLOCK**/
                            }
                            onRequestRefreshListener.onRequestRefresh(fso);
                        }
                        return Boolean.TRUE;
                    }

                });
            }
            ExceptionUtil.translateException(ctx, ex);
        }
    }

    /**
     * Method that remove an existing file system object.
     *
     * @param ctx The current context
     * @param fso The file system object
     * @param onRequestRefreshListener The listener for request a refresh after the new
     * folder was created (option)
     */
    public static void removeFileSystemObject(
            final Context ctx, final FileSystemObject fso,
            final OnRequestRefreshListener onRequestRefreshListener) {

        final boolean isFolder = FileHelper.isDirectory(fso);
        int msg =
                isFolder ?
                R.string.actions_ask_remove_folder :
                R.string.actions_ask_remove_file;

        // Ask the user before remove
        AlertDialog dialog =DialogHelper.createYesNoDialog(
            ctx, msg,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface alertDialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        try {
                            // Remove the item
                            if (isFolder) {
                                CommandHelper.deleteDirectory(ctx, fso.getFullPath(), null);
                            } else {
                                CommandHelper.deleteFile(ctx, fso.getFullPath(), null);
                            }

                            //Operation complete. Refresh
                            if (onRequestRefreshListener != null) {
                                onRequestRefreshListener.onRequestRemove(fso);
                            }

                        } catch (Throwable ex) {
                            // Capture the exception
                            RemoveItemListenter listener = new RemoveItemListenter();
                            listener.mOnRequestRefreshListener = onRequestRefreshListener;
                            listener.mFso = fso;
                            ExceptionUtil.translateException(ctx, ex, false, true, listener);
                        }
                    }
                }
           });
        dialog.show();
    }
}