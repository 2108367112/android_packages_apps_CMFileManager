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
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.cyanogenmod.explorer.R;
import com.cyanogenmod.explorer.util.DialogHelper;

/**
 * A class that wraps a dialog for showing a progress with text message (non graphical).
 */
public class MessageProgressDialog implements DialogInterface.OnClickListener {

    /**
     * A class for listen program cancellation events.
     */
    public interface OnCancelListener {
        /**
         * Fires when a cancel were requested.
         *
         *  @return boolean If the cancel can be done
         */
        boolean onCancel();
    }

    private final Context mContext;
    private final AlertDialog mDialog;
    private final TextView mProgress;
    private final int mProgressResourceId;
    private OnCancelListener mOnCancelListener;

    /**
     * Constructor of <code>MessageProgressDialog</code>.
     *
     *
     * @param context The current context
     * @param iconResourceId The icon dialog resource identifier
     * @param titleResourceId The title dialog resource identifier
     * @param labelResourceId The label resource identifier
     * @param progressResourceId The message resource identifier
     */
    public MessageProgressDialog(
            Context context, int iconResourceId, int titleResourceId,
            int labelResourceId, int progressResourceId) {
        this(context, iconResourceId, titleResourceId,
                context.getResources().getString(labelResourceId), progressResourceId);
    }

    /**
     * Constructor of <code>MessageProgressDialog</code>.
     *
     *
     * @param context The current context
     * @param iconResourceId The icon dialog resource identifier
     * @param titleResourceId The title dialog resource identifier
     * @param labelMsg The label message
     * @param progressResourceId The message resource identifier
     */
    public MessageProgressDialog(
            Context context, int iconResourceId, int titleResourceId,
            String labelMsg, int progressResourceId) {
        super();

        //Save the context
        this.mContext = context;

        //Saved progress message id
        this.mProgressResourceId = progressResourceId;

        //Create the layout
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup layout = (ViewGroup)li.inflate(R.layout.message_progress_dialog, null);
        final TextView labelView =
                (TextView)layout.findViewById(R.id.message_progress_dialog_label);
        labelView.setText(labelMsg);
        this.mProgress = (TextView)layout.findViewById(R.id.message_progress_dialog_progress);

        //Create the dialog
        this.mDialog = DialogHelper.createDialog(
                                        context,
                                        iconResourceId,
                                        titleResourceId,
                                        layout);
        this.mDialog.setButton(
                DialogInterface.BUTTON_NEUTRAL, context.getString(android.R.string.cancel), this);
        this.mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            @SuppressWarnings("synthetic-access")
            public void onCancel(DialogInterface dialog) {
                if (MessageProgressDialog.this.mOnCancelListener != null) {
                    if (!MessageProgressDialog.this.mOnCancelListener.onCancel()) {
                        //The operation can't not be cancelled
                        DialogHelper.showToast(
                                MessageProgressDialog.this.mContext,
                                R.string.msgs_operation_can_not_be_cancelled, Toast.LENGTH_SHORT);
                    }
                }
            }
        });


        //Initialize the progress
        setProgress(0);
    }

    /**
     * Method that sets the cancel listener.
     *
     * @param onCancelListener The cancel listener
     */
    public void setOnCancelListener(OnCancelListener onCancelListener) {
        this.mOnCancelListener = onCancelListener;
    }

    /**
     * Method that sets the progress of the action.
     *
     * @param progress The progress of progress of the action
     */
    public void setProgress(int progress) {
        Resources res = this.mContext.getResources();
        this.mProgress.setText(
                res.getQuantityString(
                        this.mProgressResourceId,
                        progress,
                        Integer.valueOf(progress)));
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        this.mDialog.show();
    }

    /**
     * Method that dismiss the dialog.
     */
    public void dismiss() {
        this.mDialog.dismiss();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEUTRAL:
                this.mDialog.cancel();
                break;

            default:
                break;
        }
    }


}
