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

import com.cyanogenmod.explorer.console.NoSuchFileOrDirectory;
import com.cyanogenmod.explorer.util.CommandHelper;

/**
 * A class for testing the {@link DeleteDirCommand} command.
 *
 * @see DeleteDirCommand
 */
public class DeleteDirCommandTest extends AbstractConsoleTest {

    private static final String PATH_DELDIR_OK = "/mnt/sdcard/deltestfolder"; //$NON-NLS-1$
    private static final String PATH_DELDIR_ERROR = "/mnt/sdcard121212/deltestfolder"; //$NON-NLS-1$

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRootConsoleNeeded() {
        return false;
    }

    /**
     * Method that performs a test to delete a directory.
     *
     * @throws Exception If test failed
     */
    public void testDeleteDirOk() throws Exception {
        CommandHelper.createDirectory(getContext(), PATH_DELDIR_OK, getConsole());
        boolean ret = CommandHelper.deleteDirectory(getContext(), PATH_DELDIR_OK, getConsole());
        assertTrue("response==false", ret); //$NON-NLS-1$
    }

    /**
     * Method that performs a test to delete an invalid directory.
     *
     * @throws Exception If test failed
     */
    public void testDeleteDirFail() throws Exception {
        try {
            CommandHelper.deleteDirectory(getContext(), PATH_DELDIR_ERROR, getConsole());
            assertTrue("exit code==0", false); //$NON-NLS-1$
        } catch (NoSuchFileOrDirectory error) {
          //This command must failed. exit code !=0
        }
    }


}
