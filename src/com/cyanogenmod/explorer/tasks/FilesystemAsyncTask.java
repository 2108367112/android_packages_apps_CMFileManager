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

package com.cyanogenmod.explorer.tasks;

import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.cyanogenmod.explorer.R;
import com.cyanogenmod.explorer.model.DiskUsage;
import com.cyanogenmod.explorer.model.MountPoint;
import com.cyanogenmod.explorer.util.MountPointHelper;

/**
 * A class for recovery information about filesystem status (mount point, disk usage, ...).
 */
public class FilesystemAsyncTask extends AsyncTask<String, Integer, Boolean> {

    private final ImageView mMountPointInfo;
    private final ProgressBar mDiskUsageInfo;
    private boolean mRunning;

    /**
     * Constructor of <code>FilesystemAsyncTask</code>.
     *
     * @param mountPointInfo The mount point info view
     * @param diskUsageInfo The mount point info view
     */
    public FilesystemAsyncTask(ImageView mountPointInfo, ProgressBar diskUsageInfo) {
        super();
        this.mMountPointInfo = mountPointInfo;
        this.mDiskUsageInfo = diskUsageInfo;
        this.mRunning = false;
    }

    /**
     * Method that returns if there is a task running.
     *
     * @return boolean If there is a task running
     */
    public boolean isRunning() {
        return this.mRunning;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Boolean doInBackground(String... params) {
        //Running
        this.mRunning = true;

        //Extract the directory from arguments
        String dir = params[0];

        //Extract filesystem mount point from directory
        if (isCancelled()) {
            return Boolean.TRUE;
        }
        final MountPoint mp = MountPointHelper.getMountPointFromDirectory(dir);
        if (mp == null) {
            //There is no information about
            if (isCancelled()) {
                return Boolean.TRUE;
            }
            this.mMountPointInfo.post(new Runnable() {
                @Override
                @SuppressWarnings("synthetic-access")
                public void run() {
                    FilesystemAsyncTask.this.mMountPointInfo.setImageResource(
                            R.drawable.ic_holo_light_fs_warning);
                    FilesystemAsyncTask.this.mMountPointInfo.setTag(null);
                }
            });
        } else {
            //Set image icon an save the mount point info
            if (isCancelled()) {
                return Boolean.TRUE;
            }
            this.mMountPointInfo.post(new Runnable() {
                @Override
                @SuppressWarnings("synthetic-access")
                public void run() {
                    FilesystemAsyncTask.this.mMountPointInfo.setImageResource(
                            MountPointHelper.isReadOnly(mp)
                            ? R.drawable.ic_holo_light_fs_locked
                            : R.drawable.ic_holo_light_fs_unlocked);
                    FilesystemAsyncTask.this.mMountPointInfo.setTag(mp);
                }
            });

            //Load information about disk usage
            if (isCancelled()) {
                return Boolean.TRUE;
            }
            this.mDiskUsageInfo.post(new Runnable() {
                @Override
                @SuppressWarnings("synthetic-access")
                public void run() {
                    final DiskUsage du = MountPointHelper.getMountPointDiskUsage(mp);
                    if (du != null && du.getTotal() != 0) {
                        FilesystemAsyncTask.this.mDiskUsageInfo.setProgress(
                                (int)(du.getUsed() * 100 / du.getTotal()));
                        FilesystemAsyncTask.this.mDiskUsageInfo.setTag(du);
                    } else {
                        FilesystemAsyncTask.this.mDiskUsageInfo.setProgress(du == null ? 0 : 100);
                        FilesystemAsyncTask.this.mDiskUsageInfo.setTag(null);
                    }
                }
            });
        }
        return Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPostExecute(Boolean result) {
        this.mRunning = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCancelled(Boolean result) {
        this.mRunning = false;
        super.onCancelled(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCancelled() {
        this.mRunning = false;
        super.onCancelled();
    }

}
