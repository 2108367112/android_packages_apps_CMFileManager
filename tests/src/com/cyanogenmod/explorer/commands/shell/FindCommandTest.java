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

package com.cyanogenmod.explorer.commands.shell;

import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.test.suitebuilder.annotation.LargeTest;

import com.cyanogenmod.explorer.commands.AsyncResultExecutable;
import com.cyanogenmod.explorer.commands.AsyncResultListener;
import com.cyanogenmod.explorer.model.FileSystemObject;
import com.cyanogenmod.explorer.model.Query;
import com.cyanogenmod.explorer.util.CommandHelper;

/**
 * A class for testing find command.
 *
 * @see FindCommand
 */
public class FindCommandTest extends AbstractConsoleTest {

    private static final String FIND_PATH =
            Environment.getDataDirectory().getAbsolutePath();
    private static final String FIND_TERM_PARTIAL = "shared"; //$NON-NLS-1$

    /**
     * @hide
     */
    final Object mSync = new Object();
    /**
     * @hide
     */
    boolean mNewPartialData;
    /**
     * @hide
     */
    boolean mNormalEnd;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRootConsoleNeeded() {
        return true;
    }

    /**
     * Method that performs a test over known search results.
     *
     * @throws Exception If test failed
     */
    @LargeTest
    public void testFindWithPartialResult() throws Exception {
        this.mNewPartialData = false;
        this.mNormalEnd = false;
        Query query = new Query().setSlot(FIND_TERM_PARTIAL, 0);
        final List<FileSystemObject> files = new ArrayList<FileSystemObject>();
        AsyncResultExecutable cmd =
                CommandHelper.findFiles(getContext(), FIND_PATH, query, new AsyncResultListener() {
                        public void onAsyncStart() {
                            /**NON BLOCK**/
                        }
                        public void onAsyncEnd(boolean canceled) {
                            synchronized (FindCommandTest.this.mSync) {
                                FindCommandTest.this.mNormalEnd = true;
                                FindCommandTest.this.mSync.notifyAll();
                            }
                        }
                        public void onException(Exception cause) {
                            fail(cause.toString());
                        }
                        @SuppressWarnings("unchecked")
                        public void onPartialResult(Object results) {
                            FindCommandTest.this.mNewPartialData = true;
                            files.addAll((List<FileSystemObject>)results);
                        }
                   }, getConsole());
        synchronized (FindCommandTest.this.mSync) {
            FindCommandTest.this.mSync.wait(15000L);
        }
        try {
            if (!this.mNormalEnd && cmd != null && cmd.isCancelable() && !cmd.isCanceled()) {
                cmd.cancel();
            }
        } catch (Exception e) {/**NON BLOCK**/}
        assertTrue("no new partial data", this.mNewPartialData); //$NON-NLS-1$
        assertNotNull("files==null", files); //$NON-NLS-1$
        assertTrue("no objects returned", files.size() > 0); //$NON-NLS-1$
    }

}
