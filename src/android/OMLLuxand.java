package com.luxand.dsi;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import android.graphics.drawable.ColorDrawable;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

import com.luxand.FSDK;

import org.json.JSONObject;

public class OMLLuxand extends Activity implements OnClickListener {

    private boolean mIsFailed = false;
    private Preview mPreview;
    private ProcessImageAndDrawResults mDraw;
    private  String database = "Memory50.dat";
    private int loginTryCount = 3;
    private int timeOut;
    private String launchType = "FOR_REGISTER";
    private final String help_text = "Luxand Face Recognition\n\nJust tap any detected face and name it. The app will recognize this face further. For best results, hold the device at arm's length. You may slowly rotate the head for the app to memorize you at multiple views. The app can memorize several persons. If a face is not recognized, tap and name it again.\n\nThe SDK is available for mobile developers: www.luxand.com/facesdk";


    public void showErrorAndClose(String error, int code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(error + ": " + code)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .show();
    }

    public void showMessage(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setCancelable(false) // cancel with button only
                .show();
    }

    private void resetTrackerParameters() {
        int errpos[] = new int[1];
        FSDK.SetTrackerMultipleParameters(mDraw.mTracker, "DetectFacialFeatures=true;ContinuousVideoFeed=true;FacialFeatureJitterSuppression=0;RecognitionPrecision=1;Threshold=0.996;Threshold2=0.9995;ThresholdFeed=0.97;MemoryLimit=2000;HandleArbitraryRotations=false;DetermineFaceRotationAngle=true;InternalResizeWidth=70;FaceDetectionThreshold=3;", errpos);
        if (errpos[0] != 0) {
            showErrorAndClose("Error setting tracker parameters, position", errpos[0]);
        }
        FSDK.SetTrackerMultipleParameters(mDraw.mTracker, "DetectAge=true;DetectGender=true;DetectExpression=true", errpos);
        if (errpos[0] != 0) {
            showErrorAndClose("Error setting tracker parameters 2, position", errpos[0]);
        }

        // faster smile detection
        FSDK.SetTrackerMultipleParameters(mDraw.mTracker, "AttributeExpressionSmileSmoothingSpatial=0.5;AttributeExpressionSmileSmoothingTemporal=10;", errpos);
        if (errpos[0] != 0) {
            showErrorAndClose("Error setting tracker parameters 3, position", errpos[0]);
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Constants.sDensity = getResources().getDisplayMetrics().scaledDensity;

        int res;
        // = FSDK.ActivateLibrary("");
        // if (res != FSDK.FSDKE_OK) {
        //     mIsFailed = true;
        //     showErrorAndClose("FaceSDK activation failed", res);
        // } else {
        //     FSDK.Initialize();

            

        // }
        // Hide the window title (it is done in manifest too)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Lock orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Camera layer and drawing layer
        Bundle data = getIntent().getExtras();
        if(data != null) {
            this.database = data.getString("DB_NAME", "memory.dat");
            Log.e("com.luxand.dsi", database);
            this.loginTryCount = data.getInt("LOGIN_TRY_COUNT", 3);
            this.launchType = data.getString("TYPE", "FOR_REGISTER");
            this.timeOut = data.getInt("TIMEOUT", 15000);
        }
        mDraw = new ProcessImageAndDrawResults(this, this.launchType.equals("FOR_REGISTER"), loginTryCount, timeOut);
        mDraw.setOnImageProcessListener(new OnImageProcessListener() {
            @Override
            public void handle(JSONObject obj) {
                Intent data = new Intent();
                if(obj==null) obj = new JSONObject();
                data.putExtra("data", obj.toString());
                setResult(RESULT_OK, data);
                _pause();
                finish();
            }
        });
        mPreview = new Preview(this, mDraw);
        mDraw.mTracker = new FSDK.HTracker();

        String templatePath = this.getApplicationInfo().dataDir + "/" + database;
        if (FSDK.FSDKE_OK != FSDK.LoadTrackerMemoryFromFile(mDraw.mTracker, templatePath)) {
            res = FSDK.CreateTracker(mDraw.mTracker);
            if (FSDK.FSDKE_OK != res) {
                showErrorAndClose("Error creating tracker", res);
            }
        }

        resetTrackerParameters();

        this.getWindow().setBackgroundDrawable(new ColorDrawable()); //black background

        setContentView(mPreview); //creates MainActivity contents
        addContentView(mDraw, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));


        // Menu
        LayoutInflater inflater = (LayoutInflater)this.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View buttons = inflater.inflate(getAppResource("bottom_menu", "layout"), null );
        buttons.findViewById(getAppResource("helpButton", "id")).setOnClickListener(this);
        buttons.findViewById(getAppResource("clearButton", "id")).setOnClickListener(this);
        addContentView(buttons, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == getAppResource("helpButton", "id")) {
            showMessage(help_text);
        } else if (view.getId() == getAppResource("clearButton", "id")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Are you sure to clear the memory?" )
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialogInterface, int j) {
                            pauseProcessingFrames();
                            FSDK.ClearTracker(mDraw.mTracker);
                            resetTrackerParameters();
                            resumeProcessingFrames();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialogInterface, int j) {
                        }
                    })
                    .setCancelable(false) // cancel with button only
                    .show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        _pause();
    }
    private void _pause() {
        pauseProcessingFrames();
        String templatePath = this.getApplicationInfo().dataDir + "/" + database;
        FSDK.SaveTrackerMemoryToFile(mDraw.mTracker, templatePath);
    }
    @Override
    public void onResume() {
        super.onResume();
        if (mIsFailed)
            return;
        resumeProcessingFrames();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        //Intent intent = new Intent("xper.activity.ACTIVITY_BAR_RESULT_INTENT");
        //intent.putExtra("codBar", "bar");
        //setResult(Activity.RESULT_CANCELED, intent);
        setResult(RESULT_CANCELED);
        _pause();
        finish();
    }

    private void pauseProcessingFrames() {
        mDraw.mStopping = 1;

        // It is essential to limit wait time, because mStopped will not be set to 0, if no frames are feeded to mDraw
        for (int i=0; i<100; ++i) {
            if (mDraw.mStopped != 0) break;
            try { Thread.sleep(10); }
            catch (Exception ex) {}
        }
    }

    private void resumeProcessingFrames() {
        mDraw.mStopped = 0;
        mDraw.mStopping = 0;
    }
    private int getAppResource(String name, String type) {
        return getResources().getIdentifier(name, type, getPackageName());
    }
}




