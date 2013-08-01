/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;

/**
 * A dialog fragment with an EditText for user text input.
 */
public class ScrumChatterInputDialogFragment extends DialogFragment {

    private static final String TAG = Constants.TAG + "/" + ScrumChatterInputDialogFragment.class.getSimpleName();
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_ACTION_ID = "action_id";
    private static final String EXTRA_INPUT_HINT = "input_hint";
    private static final String EXTRA_INPUT_VALIDATOR_CLASS = "input_validator_class";
    private static final String EXTRA_EXTRAS = "extras";

    private static final String EXTRA_ENTERED_TEXT = "entered_text";

    private String mEnteredText;

    public interface InputValidator {
        /**
         * @param input the text entered by the user.
         * @return an error string if the input has a problem, null if the input is valid.
         */
        String getError(Context context, int actionId, CharSequence input, Bundle extras);
    };

    public interface ScrumChatterDialogInputListener {
        void onInputEntered(int actionId, String input, Bundle extras);
    }



    /**
     * @param validatorClass will be called with each text event on the edit text, to validate the user's input.
     */
    public static ScrumChatterInputDialogFragment showInputDialog(FragmentActivity activity, String title, String inputHint, String prefilledText,
            Class<?> inputValidatorClass, int actionId, Bundle extras) {
        Log.v(TAG, "showInputDialog: title = " + title + ", prefilledText =  " + prefilledText + ", actionId = " + actionId + ", extras = " + extras);
        Bundle arguments = new Bundle(6);
        arguments.putString(EXTRA_TITLE, title);
        arguments.putString(EXTRA_INPUT_HINT, inputHint);
        arguments.putInt(EXTRA_ACTION_ID, actionId);
        arguments.putString(EXTRA_ENTERED_TEXT, prefilledText);
        if (inputValidatorClass != null) arguments.putSerializable(EXTRA_INPUT_VALIDATOR_CLASS, inputValidatorClass);
        arguments.putBundle(EXTRA_EXTRAS, extras);
        ScrumChatterInputDialogFragment result = new ScrumChatterInputDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), ScrumChatterInputDialogFragment.class.getSimpleName());
        return result;
    }

    public ScrumChatterInputDialogFragment() {
        super();
    }


    @Override
    public void onActivityCreated(Bundle bundle) {
        Log.v(TAG, "onActivityCreated: bundle = " + bundle);
        super.onActivityCreated(bundle);
        if (bundle != null) mEnteredText = bundle.getString(EXTRA_ENTERED_TEXT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate: savedInstanceState = " + savedInstanceState);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) mEnteredText = savedInstanceState.getString(EXTRA_ENTERED_TEXT);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        Log.v(TAG, "onSaveInstanceState: bundle = " + bundle);
        super.onSaveInstanceState(bundle);
        bundle.putString(EXTRA_ENTERED_TEXT, mEnteredText);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView: savedInstanceState = " + savedInstanceState);
        if (savedInstanceState != null) mEnteredText = savedInstanceState.getString(EXTRA_ENTERED_TEXT);
        return super.onCreateView(inflater, container, savedInstanceState);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        if (savedInstanceState != null) mEnteredText = savedInstanceState.getString(EXTRA_ENTERED_TEXT);
        Bundle arguments = getArguments();
        final int actionId = arguments.getInt(EXTRA_ACTION_ID);
        final EditText input = new EditText(getActivity());
        final Bundle extras = arguments.getBundle(EXTRA_EXTRAS);
        final Class<?> inputValidatorClass = (Class<?>) arguments.getSerializable(EXTRA_INPUT_VALIDATOR_CLASS);
        final String prefilledText = arguments.getString(EXTRA_ENTERED_TEXT);
        final Context context = getActivity().getApplicationContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(arguments.getString(EXTRA_TITLE));
        builder.setView(input);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint(arguments.getString(EXTRA_INPUT_HINT));
        input.setText(prefilledText);
        if (!TextUtils.isEmpty(mEnteredText)) input.setText(mEnteredText);

        OnClickListener listener = null;
        if ((getActivity() instanceof ScrumChatterDialogInputListener)) {
            listener = new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((ScrumChatterDialogInputListener) getActivity()).onInputEntered(actionId, input.getText().toString(), extras);
                }
            };
        }
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, listener);

        final AlertDialog dialog = builder.create();
        // Show the keyboard when the EditText gains focus.
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        try {
            final InputValidator validator = inputValidatorClass == null ? null : (InputValidator) inputValidatorClass.newInstance();
            Log.v(TAG, "input validator = " + validator);
            // Validate the text as the user types.
            input.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {}

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mEnteredText = input.getText().toString();
                    if (validator != null) validateText(context, dialog, input, validator, actionId, extras);
                }
            });
            dialog.getContext().setTheme(R.style.dialogStyle);
            dialog.setOnShowListener(new OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Log.v(TAG, "onShow");
                    DialogStyleHacks.uglyHackReplaceBlueHoloBackground(getActivity(), (ViewGroup) dialog.getWindow().getDecorView());
                    validateText(context, dialog, input, validator, actionId, extras);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Could not instantiate validator " + inputValidatorClass + ": " + e.getMessage(), e);
        }

        return dialog;
    }

    private static void validateText(final Context context, AlertDialog dialog, final EditText input, final InputValidator validator, final int actionId,
            final Bundle extras) {
        Log.v(TAG, "validateText: input = " + input.getText().toString() + ", actionId = " + actionId + ", extras = " + extras);
        // Start off with everything a-ok.
        input.setError(null);
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        okButton.setEnabled(true);

        // Search for an error in background thread, update the dialog in the UI thread.
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

            /**
             * @return an error String if the input is invalid.
             */
            @Override
            protected String doInBackground(Void... params) {
                return validator.getError(context, actionId, input.getText().toString().trim(), extras);
            }

            @Override
            protected void onPostExecute(String error) {
                // If the input is invalid, highlight the error
                // and disable the OK button.
                if (!TextUtils.isEmpty(error)) {
                    input.setError(error);
                    okButton.setEnabled(false);
                }
            }
        };
        task.execute();
    }
}
