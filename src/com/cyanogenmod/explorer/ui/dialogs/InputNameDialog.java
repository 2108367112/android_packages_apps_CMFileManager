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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.cyanogenmod.explorer.R;
import com.cyanogenmod.explorer.model.FileSystemObject;
import com.cyanogenmod.explorer.util.DialogHelper;

import java.io.File;
import java.util.List;

/**
 * A class that wraps a dialog for input a name for file, folder, ...
 */
public class InputNameDialog
    implements TextWatcher, DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

    private final Context mContext;
    private final AlertDialog mDialog;
    private final TextView mMsg;
    private final EditText mEditText;
    private final List<FileSystemObject> mFiles;

    private DialogInterface.OnCancelListener mOnCancelListener;
    private DialogInterface.OnDismissListener mOnDismissListener;
    private boolean mCancelled;

    /**
     * Constructor of <code>InputNameDialog</code>.
     *
     * @param context The current context
     * @param files The files of the current directory (used to validate the name)
     * @param dialogTitle The dialog title
     */
    public InputNameDialog(
            final Context context, List<FileSystemObject> files, String dialogTitle) {
        super();

        //Save the context
        this.mContext = context;

        //Save the files
        this.mFiles = files;
        this.mCancelled = true;

        //Create the
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = li.inflate(R.layout.input_name_dialog, null);
        TextView title = (TextView)v.findViewById(R.id.input_name_dialog_label);
        title.setText(R.string.input_name_dialog_label);
        this.mEditText = (EditText)v.findViewById(R.id.input_name_dialog_edit);
        this.mEditText.setText(dialogTitle);
        this.mEditText.selectAll();
        this.mEditText.addTextChangedListener(this);
        this.mMsg = (TextView)v.findViewById(R.id.input_name_dialog_message);

        //Create the dialog
        this.mDialog = DialogHelper.createDialog(
                                        context,
                                        R.drawable.ic_holo_light_edit,
                                        dialogTitle,
                                        v);
        this.mDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                context.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    @SuppressWarnings("synthetic-access")
                    public void onClick(DialogInterface dialog, int which) {
                        InputNameDialog.this.mCancelled = false;
                    }
                });
        this.mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            @SuppressWarnings("synthetic-access")
            public void onShow(DialogInterface dialog) {
                InputMethodManager mgr =
                        (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.showSoftInput(InputNameDialog.this.mEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        this.mDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                context.getString(android.R.string.cancel),
                (DialogInterface.OnClickListener)null);
        this.mDialog.setOnCancelListener(this);
        this.mDialog.setOnDismissListener(this);
    }

    /**
     * Set a listener to be invoked when the dialog is canceled.
     * <p>
     * This will only be invoked when the dialog is canceled, if the creator
     * needs to know when it is dismissed in general, use
     * {@link #setOnDismissListener}.
     *
     * @param onCancelListener The {@link "OnCancelListener"} to use.
     */
    public void setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        this.mOnCancelListener = onCancelListener;
    }

    /**
     * Set a listener to be invoked when the dialog is dismissed.
     *
     * @param onDismissListener The {@link "OnDismissListener"} to use.
     */
    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.mOnDismissListener = onDismissListener;
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        this.mDialog.show();
    }

    /**
     * Method that returns the name that the user inputted.
     *
     * @return String The name that the user inputted
     */
    public String getName() {
        return this.mEditText.getText().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        /**NON BLOCK**/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        /**NON BLOCK**/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTextChanged(Editable s) {
        String txt = s.toString().trim();
        if (txt.length() == 0) {
            //The name is empty
            setMsg(
                InputNameDialog.this.mContext.getString(
                      R.string.input_name_dialog_message_empty_name), false);
            return;
        }
        if (txt.indexOf("/") != -1) { //$NON-NLS-1$
            setMsg(
                InputNameDialog.this.mContext.getString(
                      R.string.input_name_dialog_message_invalid_path_name,
                      File.separator), false);
            return;
        }
        if (txt.compareTo(".") == 0 || txt.compareTo("..") == 0) { //$NON-NLS-1$ //$NON-NLS-2$
            setMsg(
                InputNameDialog.this.mContext.getString(
                        R.string.input_name_dialog_message_invalid_name), false);
            return;
        }
        if (isNameExists(txt)) {
            setMsg(
                InputNameDialog.this.mContext.getString(
                        R.string.input_name_dialog_message_name_exists), false);
            return;
        }

        //Valid name
        setMsg(null, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        if (!InputNameDialog.this.mCancelled) {
            if (this.mOnDismissListener != null) {
                this.mOnDismissListener.onDismiss(dialog);
            }
            return;
        }
        if (this.mOnCancelListener != null) {
            this.mOnCancelListener.onCancel(dialog);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        if (this.mOnCancelListener != null) {
            this.mOnCancelListener.onCancel(dialog);
        }
    }

    /**
     * Method that shows the alert message with the validation warning.
     *
     * @param msg The message to show
     * @param activate If the positive button must be activate
     */
    private void setMsg(String msg, boolean activate) {
        if (msg == null || msg.length() == 0) {
            this.mMsg.setVisibility(View.GONE);
            this.mMsg.setText(""); //$NON-NLS-1$
        } else {
            this.mMsg.setText(msg);
            this.mMsg.setVisibility(View.VISIBLE);
        }
        this.mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(activate);
    }

    /**
     * Method that checks if a name exists in the current directory.
     *
     * @param name The name to check
     * @return boolean Indicate if the name exists in the current directory
     */
    private boolean isNameExists(String name) {
        //Verify if the name exists in the current file list
        for (int i = 0; i < this.mFiles.size(); i++) {
            FileSystemObject fso = this.mFiles.get(i);
            if (fso.getName().compareTo(name) == 0) {
                return true;
            }
        }
        return false;
    }
}
