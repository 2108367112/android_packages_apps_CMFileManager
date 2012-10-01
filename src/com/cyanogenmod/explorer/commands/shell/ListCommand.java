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

import com.cyanogenmod.explorer.ExplorerApplication;
import com.cyanogenmod.explorer.commands.ListExecutable;
import com.cyanogenmod.explorer.console.CommandNotFoundException;
import com.cyanogenmod.explorer.console.ConsoleAllocException;
import com.cyanogenmod.explorer.console.ExecutionException;
import com.cyanogenmod.explorer.console.InsufficientPermissionsException;
import com.cyanogenmod.explorer.console.NoSuchFileOrDirectory;
import com.cyanogenmod.explorer.console.OperationTimeoutException;
import com.cyanogenmod.explorer.console.shell.ShellConsole;
import com.cyanogenmod.explorer.model.FileSystemObject;
import com.cyanogenmod.explorer.model.ParentDirectory;
import com.cyanogenmod.explorer.model.Symlink;
import com.cyanogenmod.explorer.util.CommandHelper;
import com.cyanogenmod.explorer.util.FileHelper;
import com.cyanogenmod.explorer.util.ParseHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


/**
 * A class for list information about files and directories.
 *
 * {@link "http://unixhelp.ed.ac.uk/CGI/man-cgi?ls"}
 */
public class ListCommand extends SyncResultProgram implements ListExecutable {

    private static final String ID_LS_DIRECTORY = "ls";  //$NON-NLS-1$
    private static final String ID_LS_INFO = "fileinfo";  //$NON-NLS-1$

    private static final String SYMLINK_REF = ">SIMLINKS>";  //$NON-NLS-1$
    private static final String SYMLINK_DATA_REF = ">SIMLINKS_DATA>";  //$NON-NLS-1$

    private final LIST_MODE mMode;
    private final List<FileSystemObject> mFiles;
    private String mParentDir;

    /**
     * Constructor of <code>ListCommand</code>.
     *
     * @param src The file system object to be listed
     * @param mode The listing mode
     * @param console The console in which retrieve the parent directory information.
     * <code>null</code> to attach to the default console
     * @throws InvalidCommandDefinitionException If the command has an invalid definition
     * @throws FileNotFoundException If the initial directory not exists
     * @throws NoSuchFileOrDirectory If the file or directory was not found
     * @throws IOException If initial directory can't not be checked
     * @throws ConsoleAllocException If the console can't be allocated
     * @throws InsufficientPermissionsException If an operation requires elevated permissions
     * @throws CommandNotFoundException If the command was not found
     * @throws OperationTimeoutException If the operation exceeded the maximum time of wait
     * @throws ExecutionException If the operation returns a invalid exit code
     */
    public ListCommand(String src, LIST_MODE mode, ShellConsole console)
            throws InvalidCommandDefinitionException, FileNotFoundException,
            NoSuchFileOrDirectory, IOException, ConsoleAllocException,
            InsufficientPermissionsException, CommandNotFoundException,
            OperationTimeoutException, ExecutionException {
        //If the mode is listing directory, for avoid problems with symlink,
        //always append a / to the end of the path (if not exists)
        super(mode.compareTo(LIST_MODE.DIRECTORY) == 0 ? ID_LS_DIRECTORY : ID_LS_INFO,
                new String[]{ mode.compareTo(LIST_MODE.DIRECTORY) == 0
                ? (src.endsWith(File.separator)
                        ? src
                        : src + File.separator)
                : src});

        //Initialize files to something distinct of null
        this.mFiles = new ArrayList<FileSystemObject>();
        this.mMode = mode;

        //Retrieve parent directory information
        if (mode.compareTo(LIST_MODE.DIRECTORY) == 0) {
            //Resolve the parent directory
            if (src.compareTo(FileHelper.ROOT_DIRECTORY) == 0) {
                this.mParentDir = null;
            } else {
                this.mParentDir =
                    CommandHelper.getAbsolutePath(
                            ExplorerApplication.
                                getInstance().getApplicationContext(), src, console);
            }
        } else {
            //Get the absolute path
            try {
                this.mParentDir = new File(src).getCanonicalFile().getParent();

            } catch (Exception e) {
                // Try to resolve from a console
                String abspath =
                    CommandHelper.getAbsolutePath(
                            ExplorerApplication.getInstance().
                                getApplicationContext(), src, console);
                //Resolve the parent directory
                this.mParentDir =
                    CommandHelper.getParentDir(
                            ExplorerApplication.getInstance().getApplicationContext(),
                            abspath, console);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void parse(String in, String err) throws ParseException {
        //Release the array
        this.mFiles.clear();

        // Check the in buffer to extract information
        BufferedReader br = null;
        int line = 0;
        try {
            br = new BufferedReader(new StringReader(in));
            String szLine = null;
            boolean symlinks = false;
            int symlinksCount = 0;
            while ((szLine = br.readLine()) != null) {
                //Checks that there is some text in the line. Otherwise ignore it
                if (szLine.trim().length() == 0) {
                    break;
                }

                //For a fast recovery, command return non symlink first and
                //symlinks files, the resolution and the his info
                //Is now symlinks?
                if (szLine.startsWith(SYMLINK_REF)) {
                    //Ignore the control line
                    szLine = br.readLine();
                    line++;
                    symlinks = true;
                }

                //Parse the line into a FileSystemObject reference
                if (!symlinks) {
                    try {
                        this.mFiles.add(ParseHelper.toFileSystemObject(this.mParentDir, szLine));
                    } catch (ParseException pEx) {
                        throw new ParseException(pEx.getMessage(), line);
                    }
                } else {
                    //Is ending symlink reference
                    if (szLine.startsWith(SYMLINK_DATA_REF)) {
                        if (symlinksCount == 0) {
                            //No more data
                            break;
                        }
                        //Ignore the control line
                        szLine = br.readLine();
                        line++;

                        //The next information is known:  symlinksCount * 2
                        String[] absPath = new String[symlinksCount];
                        String[] refPath = new String[symlinksCount];
                        for (int i = 0; i < symlinksCount; i++) {
                            if (szLine == null || szLine.trim().length() == 0) {
                                absPath[i] = null;
                                continue;
                            }
                            absPath[i] = szLine;
                            szLine = br.readLine();
                            line++;
                        }
                        for (int i = 0; i < symlinksCount; i++) {
                            if (szLine == null || szLine.trim().length() == 0) {
                                refPath[i] = null;
                                continue;
                            }
                            refPath[i] = szLine;
                            szLine = br.readLine();
                            line++;
                        }

                        //Fill the data
                        for (int i = 0; i < symlinksCount; i++) {
                            try {
                                String parent = new File(absPath[i]).getParent();
                                String info = refPath[i];
                                FileSystemObject fsoRef =
                                        ParseHelper.toFileSystemObject(parent, info);
                                Symlink symLink =
                                        ((Symlink)this.mFiles.get(
                                                this.mFiles.size() - symlinksCount + i));
                                symLink.setLinkRef(fsoRef);
                            } catch (Throwable ex) {
                                //If parsing the file failed, ignore it and threat as a regular
                                //file (the destination file not exists or can't be resolved)
                            }
                        }
                        break;
                    }

                    //Add the symlink
                    try {
                        this.mFiles.add(ParseHelper.toFileSystemObject(this.mParentDir, szLine));
                    } catch (ParseException pEx) {
                        throw new ParseException(pEx.getMessage(), line);
                    }
                    symlinksCount++;
                }

                line++;
            }

            //Now if not is the root directory
            if (this.mParentDir != null &&
                    this.mParentDir.compareTo(FileHelper.ROOT_DIRECTORY) != 0 &&
                    this.mMode.compareTo(LIST_MODE.DIRECTORY) == 0) {
                this.mFiles.add(0, new ParentDirectory(new File(this.mParentDir).getParent()));
            }

        } catch (IOException ioEx) {
            throw new ParseException(ioEx.getMessage(), line);

        } catch (ParseException pEx) {
            throw pEx;

        } catch (Exception ex) {
            throw new ParseException(ex.getMessage(), line);

        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (Throwable ex) {
                /**NON BLOCK**/
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileSystemObject> getResult() {
        return this.mFiles;
    }

    /**
     * Method that returns a single result of the program invocation.
     * Only must be called within a <code>FILEINFO</code> mode listing.
     *
     * @return FileSystemObject The file system object reference
     */
    public FileSystemObject getSingleResult() {
        return this.mFiles.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkExitCode(int exitCode)
            throws InsufficientPermissionsException, CommandNotFoundException, ExecutionException {
        if (exitCode != 0 && exitCode != 1) {
            throw new ExecutionException("exitcode != 0 && != 1"); //$NON-NLS-1$
        }
    }
}
