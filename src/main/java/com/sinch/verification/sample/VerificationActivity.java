package com.sinch.verification.sample;

import com.sinch.verification.CodeInterceptionException;
import com.sinch.verification.Config;
import com.sinch.verification.SinchVerification;
import com.sinch.verification.Verification;
import com.sinch.verification.VerificationListener;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import jobninja.eu.analytics.Analytics;

public class VerificationActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = Verification.class.getSimpleName();
    private final String APPLICATION_KEY = "8b3e7e3b-1166-45e8-8e2e-501dfc402e04";
    private Verification mVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);
        showProgress();
        setupView();
        initiateVerification();

    }

    private void setupView() {
        EditText input = (EditText) findViewById(R.id.inputCode);

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE){
                    onSubmitClicked(v);
                }
                return false;
            }
        });

    }

    void createVerification(String phoneNumber, String method, boolean skipPermissionCheck) {
        Config config = SinchVerification.config().applicationKey(APPLICATION_KEY).context(getApplicationContext())
                .build();
        VerificationListener listener = new MyVerificationListener();

        if (method.equalsIgnoreCase(MainActivity.SMS)) {

            if (!skipPermissionCheck && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) ==
                    PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, 0);
                hideProgressBar();
            } else {
                mVerification = SinchVerification.createSmsVerification(config, phoneNumber, listener);
                mVerification.initiate();
            }

        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                Toast.makeText(this, "This application needs permission to read your SMS to automatically verify your "
                        + "phone, you may disable the permission once you have been verified.", Toast.LENGTH_LONG)
                        .show();
            }
            enableInputField(true);
        }
        initiateVerificationAndSuppressPermissionCheck();
    }

    void initiateVerification() {
        initiateVerification(false);
    }

    void initiateVerificationAndSuppressPermissionCheck() {
        initiateVerification(true);
    }

    void initiateVerification(boolean skipPermissionCheck) {
        Intent intent = getIntent();
        if (intent != null) {
            String phoneNumber = intent.getStringExtra(MainActivity.INTENT_PHONENUMBER);
            String method = intent.getStringExtra(MainActivity.INTENT_METHOD);
            TextView phoneText = (TextView) findViewById(R.id.numberText);
            phoneText.setText(phoneNumber);
            if (method != null && phoneNumber != null) {
                createVerification(phoneNumber, method, skipPermissionCheck);
            }
            else{
                enableInputField(true);
                hideProgressBar();
            }
        }
    }

    public void onSubmitClicked(View view) {
        String code = ((EditText) findViewById(R.id.inputCode)).getText().toString();

        if (!code.isEmpty()) {
            if (mVerification != null) {
                mVerification.verify(code);
                showProgress();
                TextView messageText = (TextView) findViewById(R.id.textView);
                messageText.setText(R.string.verification_loading);
                enableInputField(false);
            }
        }
    }


    void enableInputField(boolean enable) {
        View container = findViewById(R.id.inputContainer);
        if (enable) {
            container.setVisibility(View.VISIBLE);
            EditText input = (EditText) findViewById(R.id.inputCode);
            input.getBackground().setColorFilter(getResources().getColor(R.color.jobninja_red), PorterDuff.Mode.SRC_ATOP);
            input.requestFocus();
        } else {
            container.setVisibility(View.GONE);
        }
    }


    void hideProgressBarAndShowMessage(int message) {
        hideProgressBar();
        TextView messageText = (TextView) findViewById(R.id.textView);
        messageText.setText(message);
    }

    void hideProgressBar() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressIndicator);
        progressBar.setVisibility(View.INVISIBLE);
        TextView progressText = (TextView) findViewById(R.id.progressText);
        progressText.setVisibility(View.INVISIBLE);
    }

    void showProgress() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressIndicator);
        progressBar.setVisibility(View.VISIBLE);
    }

    void showCompleted() {
        ImageView checkMark = (ImageView) findViewById(R.id.checkmarkImage);
        checkMark.setVisibility(View.VISIBLE);
    }

    class MyVerificationListener implements VerificationListener {

        @Override
        public void onInitiated() {
            Log.d(TAG, "Initialized!");
            showProgress();
        }

        @Override
        public void onInitiationFailed(Exception exception) {
            Log.e(TAG, "Verification initialization failed: " + exception.getMessage());
            hideProgressBarAndShowMessage(R.string.failed);
            returnException(exception);
        }

        @Override
        public void onVerified() {
            Log.d(TAG, "Verified!");
            Analytics.phoneVerified(VerificationActivity.this);
            hideProgressBarAndShowMessage(R.string.verified);
            showCompleted();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent();
                    i.putExtra("phone", ((TextView) findViewById(R.id.numberText)).getText());
                    setResult(Activity.RESULT_OK, i);
                    finish();
                }
            }, 2000);
        }

        @Override
        public void onVerificationFailed(Exception exception) {
            Log.e(TAG, "Verification failed: " + exception.getMessage());
            if (exception instanceof CodeInterceptionException) {
                hideProgressBar();
            } else {
                hideProgressBarAndShowMessage(R.string.failed);
                //returnException(exception);
            }
            enableInputField(true);
        }
    }

    private void returnException(final Exception exception) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Analytics.verificationFailed(VerificationActivity.this);
                Intent i = new Intent();
                i.putExtra("exception", exception.getMessage());
                setResult(Activity.RESULT_CANCELED, i);
                finish();
            }
        }, 2000);
    }

}
