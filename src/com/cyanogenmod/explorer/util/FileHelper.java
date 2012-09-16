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

package com.cyanogenmod.explorer.util;

import android.content.SharedPreferences;
import android.content.res.Resources;

import com.cyanogenmod.explorer.ExplorerApplication;
import com.cyanogenmod.explorer.R;
import com.cyanogenmod.explorer.model.Directory;
import com.cyanogenmod.explorer.model.FileSystemObject;
import com.cyanogenmod.explorer.model.ParentDirectory;
import com.cyanogenmod.explorer.model.Symlink;
import com.cyanogenmod.explorer.model.SystemFile;
import com.cyanogenmod.explorer.preferences.ExplorerSettings;
import com.cyanogenmod.explorer.preferences.NavigationSortMode;
import com.cyanogenmod.explorer.preferences.ObjectIdentifier;
import com.cyanogenmod.explorer.preferences.Preferences;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A helper class with useful methods for deal with files.
 */
public final class FileHelper {

    /**
     * Constructor of <code>FileHelper</code>.
     */
    private FileHelper() {
        super();
    }

    /**
     * The root directory.
     * @hide
     */
    public static final String ROOT_DIRECTORY = File.separator;

    /**
     * The parent directory string.
     * @hide
     */
    public static final String PARENT_DIRECTORY = "..";  //$NON-NLS-1$

    /**
     * The current directory string.
     * @hide
     */
    public static final String CURRENT_DIRECTORY = ".";  //$NON-NLS-1$

    /**
     * The administrator user.
     * @hide
     */
    public static final String USER_ROOT = "root";  //$NON-NLS-1$

    /**
     * The newline string.
     * @hide
     */
    public static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * Method that check if a file is a symbolic link.
     *
     * @param file File to check
     * @return boolean If file is a symbolic link
     * @throws IOException If real file can't not be checked
     */
    public static boolean isSymlink(File file) throws IOException {
        return file.getAbsolutePath().compareTo(file.getCanonicalPath()) != 0;
    }

    /**
     * Method that resolves a symbolic link to the real file or directory.
     *
     * @param file File to check
     * @return File The real file or directory
     * @throws IOException If real file can't not be resolved
     */
    public static File resolveSymlink(File file) throws IOException {
        return file.getCanonicalFile();
    }

    /**
     * Method that returns a more human readable of the size
     * of a file system object.
     *
     * @param fso File system object
     * @return String The human readable size (void if fso don't supports size)
     */
    public static String getHumanReadableSize(FileSystemObject fso) {
        //Only if has size
        if (fso instanceof Directory) {
            return ""; //$NON-NLS-1$
        }
        if (hasSymlinkRef(fso)) {
            if (isSymlinkRefDirectory(fso)) {
                return ""; //$NON-NLS-1$
            }
            return getHumanReadableSize(((Symlink)fso).getLinkRef().getSize());
        }
        return getHumanReadableSize(fso.getSize());
    }

    /**
     * Method that returns a more human readable of a size in bytes.
     *
     * @param size The size in bytes
     * @return String The human readable size
     */
    public static String getHumanReadableSize(long size) {
        Resources res = ExplorerApplication.getInstance().getResources();
        final String format = "%d %s"; //$NON-NLS-1$
        final int[] magnitude = {
                                 R.string.size_bytes,
                                 R.string.size_kilobytes,
                                 R.string.size_megabytes,
                                 R.string.size_gigabytes
                                };

        long aux = size;
        for (int i = 0; i < magnitude.length; i++) {
            long s = aux / 1024;
            if (aux < 1024) {
                return String.format(format, Long.valueOf(aux), res.getString(magnitude[i]));
            }
            aux = s;
        }
        return String.format(
                format, Long.valueOf(aux), res.getString(magnitude[magnitude.length - 1]));
    }

    /**
     * Method that returns if an file system object requires elevated privileges.
     * This occurs when the user is "root" or when the user console doesn't have
     * sufficient permissions over the file system object.
     *
     * @param fso File system object
     * @return boolean If the file system object requires elevated privileges
     */
    public static boolean isPrivileged(FileSystemObject fso) {
        //Parent directory doesn't require privileges
        if (fso instanceof ParentDirectory) {
            return false;
        }

        //Checks if user is the administrator user
        if (fso.getUser().getName().compareTo(USER_ROOT) == 0) {
            return true;
        }

        //No privileged
        return false;
    }

    /**
     * Method that returns if the file system object if the root directory.
     *
     * @param fso The file system object to check
     * @return boolean if the file system object if the root directory
     */
    public static boolean isRootDirectory(FileSystemObject fso) {
        return fso.getParent().compareTo(FileHelper.ROOT_DIRECTORY) == 0;
    }

    /**
     * Method that returns the extension of a file system object.
     *
     * @param fso The file system object
     * @return The extension of the file system object, or <code>null</code>
     * if <code>fso</code> has no extension.
     */
    public static String getExtension(FileSystemObject fso) {
        final char dot = '.';
        int pos = fso.getName().lastIndexOf(dot);
        if (pos == -1) {
            return null;
        }
        return fso.getName().substring(pos + 1);
    }

    /**
     * Method that evaluates if a path is relative.
     *
     * @param src The path to check
     * @return boolean If a path is relative
     */
    public static boolean isRelativePath(String src) {
        if (src.startsWith(CURRENT_DIRECTORY + File.separator)) {
            return true;
        }
        if (src.startsWith(PARENT_DIRECTORY + File.separator)) {
            return true;
        }
        if (src.indexOf(File.separator + CURRENT_DIRECTORY + File.separator) != -1) {
            return true;
        }
        if (src.indexOf(File.separator + PARENT_DIRECTORY + File.separator) != -1) {
            return true;
        }
        if (!src.startsWith(ROOT_DIRECTORY)) {
            return true;
        }
        return false;
    }

    /**
     * Method that check if the file system object is a {@link Symlink} and
     * has a link reference.
     *
     * @param fso The file system object to check
     * @return boolean If file system object the has a link reference
     */
    public static boolean hasSymlinkRef(FileSystemObject fso) {
        if (fso instanceof Symlink) {
            return ((Symlink)fso).getLinkRef() != null;
        }
        return false;
    }


    /**
     * Method that check if the file system object is a {@link Symlink} and
     * the link reference is a directory.
     *
     * @param fso The file system object to check
     * @return boolean If file system object the link reference is a directory
     */
    public static boolean isSymlinkRefDirectory(FileSystemObject fso) {
        if (!hasSymlinkRef(fso)) {
            return false;
        }
        return ((Symlink)fso).getLinkRef() instanceof Directory;
    }

    /**
     * Method that checks if a file system object is a directory (real o symlink).
     *
     * @param fso The file system object to check
     * @return boolean If file system object is a directory
     */
    public static boolean isDirectory(FileSystemObject fso) {
        if (fso instanceof Directory) {
            return true;
        }
        if (isSymlinkRefDirectory(fso)) {
            return true;
        }
        return false;
    }

    /**
     * Method that returns the real reference of a file system object
     * (the reference file system object if the file system object is a symlink.
     * Otherwise the same reference).
     *
     * @param fso The file system object to check
     * @return FileSystemObject The real file system object reference
     */
    public static FileSystemObject getReference(FileSystemObject fso) {
        if (hasSymlinkRef(fso)) {
            return ((Symlink)fso).getLinkRef();
        }
        return fso;
    }

    /**
     * Method that applies the configuration modes to the listed files
     * (sort mode, hidden files, ...).
     *
     * @param files The listed files
     * @return List<FileSystemObject> The applied mode listed files
     * @hide
     */
    public static List<FileSystemObject> applyUserPreferences(List<FileSystemObject> files) {
        //Retrieve user preferences
        SharedPreferences prefs = Preferences.getSharedPreferences();
        ExplorerSettings sortModePref = ExplorerSettings.SETTINGS_SORT_MODE;
        ExplorerSettings showDirsFirstPref = ExplorerSettings.SETTINGS_SHOW_DIRS_FIRST;
        ExplorerSettings showHiddenPref = ExplorerSettings.SETTINGS_SHOW_HIDDEN;
        ExplorerSettings showSystemPref = ExplorerSettings.SETTINGS_SHOW_SYSTEM;
        ExplorerSettings showSymlinksPref = ExplorerSettings.SETTINGS_SHOW_SYMLINKS;

        //Remove all unnecessary files (no required by the user)
        for (int i = files.size() - 1; i >= 0; i--) {
            FileSystemObject file = files.get(i);

            //Hidden files
            if (!prefs.getBoolean(
                    showHiddenPref.getId(),
                    ((Boolean)showHiddenPref.getDefaultValue()).booleanValue())) {
                if (file.isHidden()) {
                    files.remove(i);
                    continue;
                }
            }

            //System files
            if (!prefs.getBoolean(
                    showSystemPref.getId(),
                    ((Boolean)showSystemPref.getDefaultValue()).booleanValue())) {
                if (file instanceof SystemFile) {
                    files.remove(i);
                    continue;
                }
            }

            //Symlinks files
            if (!prefs.getBoolean(
                    showSymlinksPref.getId(),
                    ((Boolean)showSymlinksPref.getDefaultValue()).booleanValue())) {
                if (file instanceof Symlink) {
                    files.remove(i);
                    continue;
                }
            }
        }

        //Apply sort mode
        final boolean showDirsFirst =
                prefs.getBoolean(
                        showDirsFirstPref.getId(),
                    ((Boolean)showDirsFirstPref.getDefaultValue()).booleanValue());
        final NavigationSortMode sortMode =
                NavigationSortMode.fromId(
                        prefs.getInt(sortModePref.getId(),
                        ((ObjectIdentifier)sortModePref.getDefaultValue()).getId()));
        Collections.sort(files, new Comparator<FileSystemObject>() {
            @Override
            @SuppressWarnings("synthetic-access")
            public int compare(FileSystemObject lhs, FileSystemObject rhs) {
                //Parent directory always goes first
                boolean isLhsParentDirectory = lhs instanceof ParentDirectory;
                boolean isRhsParentDirectory = rhs instanceof ParentDirectory;
                if (isLhsParentDirectory || isRhsParentDirectory) {
                    if (isLhsParentDirectory && isRhsParentDirectory) {
                        return 0;
                    }
                    return (isLhsParentDirectory) ? -1 : 1;
                }

                //Need to sort directory first?
                if (showDirsFirst) {
                    boolean isLhsDirectory = FileHelper.isDirectory(lhs);
                    boolean isRhsDirectory = FileHelper.isDirectory(rhs);
                    if (isLhsDirectory || isRhsDirectory) {
                        if (isLhsDirectory && isRhsDirectory) {
                            //Apply sort mode
                            return FileHelper.doCompare(lhs, rhs, sortMode);
                        }
                        return (isLhsDirectory) ? -1 : 1;
                    }
                }

                //Apply sort mode
                return FileHelper.doCompare(lhs, rhs, sortMode);
            }

        });

        //Return the files
        return files;
    }



    /**
     * Method that do a comparison between 2 file system objects.
     *
     * @param fso1 The first file system objects
     * @param fso2 The second file system objects
     * @param mode The sort mode
     * @return int a negative integer if {@code fso1} is less than {@code fso2};
     *         a positive integer if {@code fso1} is greater than {@code fso2};
     *         0 if {@code fso1} has the same order as {@code fso2}.
     */
    private static int doCompare(
            FileSystemObject fso1, FileSystemObject fso2, NavigationSortMode mode) {

        //Resolve references
        FileSystemObject o1 = FileHelper.getReference(fso1);
        FileSystemObject o2 = FileHelper.getReference(fso2);

        //Name (ascending)
        if (mode.getId() == NavigationSortMode.NAME_ASC.getId()) {
            return o1.getName().compareTo(o2.getName());
        }
        //Name (descending)
        if (mode.getId() == NavigationSortMode.NAME_DESC.getId()) {
            return o1.getName().compareTo(o2.getName()) * -1;
        }

        //Date (ascending)
        if (mode.getId() == NavigationSortMode.DATE_ASC.getId()) {
            return o1.getLastModifiedTime().compareTo(o2.getLastModifiedTime());
        }
        //Date (descending)
        if (mode.getId() == NavigationSortMode.DATE_DESC.getId()) {
            return o1.getLastModifiedTime().compareTo(o2.getLastModifiedTime()) * -1;
        }

        //Comparison between files directly
        return o1.compareTo(fso2);
    }

}
