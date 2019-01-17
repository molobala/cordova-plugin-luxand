package com.luxand.dsi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.luxand.FSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ProcessImageAndDrawResults extends View {
    public FSDK.HTracker mTracker;

    final int MAX_FACES = 5;
    final FaceRectangle[] mFacePositions = new FaceRectangle[MAX_FACES];
    final long[] mIDs = new long[MAX_FACES];
    final Lock faceLock = new ReentrantLock();
    int mTouchedIndex;
    long mTouchedID;
    int mStopping;
    int mStopped;
    private final String[] mAttributeValues = new String[MAX_FACES];
    Context mContext;
    Paint mPaintGreen, mPaintBlue, mPaintBlueTransparent;
    byte[] mYUVData;
    byte[] mRGBData;
    int mImageWidth, mImageHeight;
    boolean first_frame_saved;
    boolean rotated;
    boolean identifying = false;

    private  OnImageProcessListener onImageProcessListener;
    private int registerCheckCount = 0;
    private int loginCount = 0;
    private  String generatedName;
    private boolean identified = false;

    public static final int ALREADY_REGISTERED = 1;
    public static final int REGISTERED = 2;
    public static final int NOT_REGISTERED = 3;

    public static final int RECOGNIZED = 4;
    public static final int NOT_RECOGNIZED = 5;
    private long correspondingId;
    private String name;
    private int loginTryCount = 4;
    private int timeOut;
    private long startTime = 0;
    int GetFaceFrame(FSDK.FSDK_Features Features, FaceRectangle fr)
    {
        if (Features == null || fr == null)
            return FSDK.FSDKE_INVALID_ARGUMENT;

        float u1 = Features.features[0].x;
        float v1 = Features.features[0].y;
        float u2 = Features.features[1].x;
        float v2 = Features.features[1].y;
        float xc = (u1 + u2) / 2;
        float yc = (v1 + v2) / 2;
        int w = (int)Math.pow((u2 - u1) * (u2 - u1) + (v2 - v1) * (v2 - v1), 0.5);

        fr.x1 = (int)(xc - w * 1.6 * 0.9);
        fr.y1 = (int)(yc - w * 1.1 * 0.9);
        fr.x2 = (int)(xc + w * 1.6 * 0.9);
        fr.y2 = (int)(yc + w * 2.1 * 0.9);
        if (fr.x2 - fr.x1 > fr.y2 - fr.y1) {
            fr.x2 = fr.x1 + fr.y2 - fr.y1;
        } else {
            fr.y2 = fr.y1 + fr.x2 - fr.x1;
        }
        return 0;
    }

    public void setOnImageProcessListener(OnImageProcessListener onImageProcessListener) {
        this.onImageProcessListener = onImageProcessListener;
    }

    public ProcessImageAndDrawResults(Context context, boolean identifyiong, int loginTryCount, int timeOut) {
        super(context);
        this.timeOut = timeOut;

        this.identifying = identifyiong;
        this.loginTryCount = loginTryCount;
        Log.e("com.luxand.dsi::", "identifying:"+identifyiong);
        mTouchedIndex = -1;

        mStopping = 0;
        mStopped = 0;
        rotated = false;
        mContext = context;
        mPaintGreen = new Paint();
        mPaintGreen.setStyle(Paint.Style.FILL);
        mPaintGreen.setColor(Color.GREEN);
        mPaintGreen.setTextSize(18 * Constants.sDensity);
        mPaintGreen.setTextAlign(Paint.Align.CENTER);
        mPaintBlue = new Paint();
        mPaintBlue.setStyle(Paint.Style.FILL);
        mPaintBlue.setColor(Color.BLUE);
        mPaintBlue.setTextSize(18 * Constants.sDensity);
        mPaintBlue.setTextAlign(Paint.Align.CENTER);

        mPaintBlueTransparent = new Paint();
        mPaintBlueTransparent.setStyle(Paint.Style.STROKE);
        mPaintBlueTransparent.setStrokeWidth(2);
        mPaintBlueTransparent.setColor(Color.argb(255,255,200,0));
        mPaintBlueTransparent.setTextSize(25);

        //mBitmap = null;
        mYUVData = null;
        mRGBData = null;

        first_frame_saved = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(this.startTime<=0) {
            this.startTime = System.currentTimeMillis();
        }
        if(timeOut!=-1 && System.currentTimeMillis()-this.startTime>timeOut) {
            //timeout
            response(true,"Timeout while finding face", "{\"timeout\": true}");
            return;
        }
        if (mStopping == 1) {
            mStopped = 1;
            super.onDraw(canvas);
            return;
        }

        if (mYUVData == null || mTouchedIndex != -1) {
            super.onDraw(canvas);
            return; //nothing to process or name is being entered now
        }

        int canvasWidth = getWidth();
        //int canvasHeight = canvas.getHeight();

        // Convert from YUV to RGB
        decodeYUV420SP(mRGBData, mYUVData, mImageWidth, mImageHeight);

        // Load image to FaceSDK
        FSDK.HImage Image = new FSDK.HImage();
        FSDK.FSDK_IMAGEMODE imagemode = new FSDK.FSDK_IMAGEMODE();
        imagemode.mode = FSDK.FSDK_IMAGEMODE.FSDK_IMAGE_COLOR_24BIT;
        FSDK.LoadImageFromBuffer(Image, mRGBData, mImageWidth, mImageHeight, mImageWidth*3, imagemode);
        FSDK.MirrorImage(Image, false);
        FSDK.HImage RotatedImage = new FSDK.HImage();
        FSDK.CreateEmptyImage(RotatedImage);

        //it is necessary to work with local variables (onDraw called not the time when mImageWidth,... being reassigned, so swapping mImageWidth and mImageHeight may be not safe)
        int ImageWidth = mImageWidth;
        //int ImageHeight = mImageHeight;
        if (rotated) {
            ImageWidth = mImageHeight;
            //ImageHeight = mImageWidth;
            FSDK.RotateImage90(Image, -1, RotatedImage);
        } else {
            FSDK.CopyImage(Image, RotatedImage);
        }
        FSDK.FreeImage(Image);

        // Save first frame to gallery to debug (e.g. rotation angle)
		/*
		if (!first_frame_saved) {
			first_frame_saved = true;
			String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
			FSDK.SaveImageToFile(RotatedImage, galleryPath + "/first_frame.jpg"); //frame is rotated!
		}
		*/

        long IDs[] = new long[MAX_FACES];
        long face_count[] = new long[1];

        FSDK.FeedFrame(mTracker, 0, RotatedImage, face_count, IDs);
        FSDK.FreeImage(RotatedImage);

        faceLock.lock();
        for (int i = 0; i < MAX_FACES; ++i) {
            mAttributeValues[i] = "";
        }

        for (int i=0; i<MAX_FACES; ++i) {
            mFacePositions[i] = new FaceRectangle();
            mFacePositions[i].x1 = 0;
            mFacePositions[i].y1 = 0;
            mFacePositions[i].x2 = 0;
            mFacePositions[i].y2 = 0;
            mIDs[i] = IDs[i];
        }
        if(face_count[0]<=0) {
            return;
        }

        this.startTime = System.currentTimeMillis();

        float ratio = (canvasWidth * 1.0f) / ImageWidth;
        for (int i = 0; i < (int)face_count[0]; ++i) {
            FSDK.FSDK_Features Eyes = new FSDK.FSDK_Features();
            FSDK.GetTrackerEyes(mTracker, 0, mIDs[i], Eyes);
            String values[] = new String[1];

            FSDK.GetTrackerFacialAttribute(mTracker, 0, IDs[i], "Gender", values, 1024);
            float[] confidenceMale = new float[1];
            float[] confidenceFemale = new float[1];
            FSDK.GetValueConfidence(values[0], "Male", confidenceMale);
            FSDK.GetValueConfidence(values[0], "Female", confidenceFemale);

            FSDK.GetTrackerFacialAttribute(mTracker, 0, IDs[i], "Age", values, 1024);
            float[] confidenceAge = new float[1];
            FSDK.GetValueConfidence(values[0], "Age", confidenceAge);
            int confidenceMalePercent = (int)(confidenceMale[0] * 100);
            int confidenceFemalePercent = (int)(confidenceFemale[0] * 100);
            values[0] = "";
            FSDK.GetTrackerFacialAttribute(mTracker, 0, IDs[i], "Expression", values, 1024);
            float[] confidenceSmile = new float[1];
            float[] confidenceEyesOpen = new float[1];
            FSDK.GetValueConfidence(values[0], "Smile", confidenceSmile);
            FSDK.GetValueConfidence(values[0], "EyesOpen", confidenceEyesOpen);
            int confidenceSmilePercent = (int)(confidenceSmile[0] * 100);
            int confidenceEyesOpenPercent = (int)(confidenceEyesOpen[0] * 100);
            if (confidenceMalePercent <= confidenceFemalePercent)
                mAttributeValues[i] = String.format(Locale.ENGLISH,"{\"AGE\":%d,\"GENDER\":\"%s\",\"SMILE\":%d,\"EYESOPENED\":%d}", (int) confidenceAge[0], "Female",confidenceSmilePercent, confidenceEyesOpenPercent);
            else
                mAttributeValues[i] = String.format(Locale.ENGLISH,"{\"AGE\":%d,\"GENDER\":\"%s\",\"SMILE\":%d,\"EYESOPENED\":%d}", (int) confidenceAge[0], "Male",confidenceSmilePercent, confidenceEyesOpenPercent);
            GetFaceFrame(Eyes, mFacePositions[i]);
            mFacePositions[i].x1 *= ratio;
            mFacePositions[i].y1 *= ratio;
            mFacePositions[i].x2 *= ratio;
            mFacePositions[i].y2 *= ratio;
        }

        faceLock.unlock();

        int shift = (int)(22 * Constants.sDensity);

        if(!this.identifying) {
            identified = false;
            if(face_count[0]>1) {
                if(loginCount<loginTryCount) Toast.makeText(getContext(), "Mutltiple faces detected ...", Toast.LENGTH_LONG).show();
                //return;
            }else if(face_count[0]==1) {
                // Mark and name faces
                for (int i=0; i<face_count[0]; ++i) {
                    canvas.drawRect(mFacePositions[i].x1, mFacePositions[i].y1, mFacePositions[i].x2, mFacePositions[i].y2, mPaintBlueTransparent);
                    identified = identified || this.recognize(IDs[i]);
                }
                Log.e("com.luxand.dsi.Ident", identified+"");
                if(this.loginCount<=loginTryCount && this.identified) {
                    response(false, "logged successfully", mAttributeValues[0]);
                    mStopping = 1;
                    return;
                }
                this.loginCount++;
                if(this.loginCount>=loginTryCount) {
                    response(true ,  "Unable to Login", mAttributeValues[0]);
                    mStopping = 1;
                    return;
                }
            }
        }else {
            if(face_count[0]>1) {
                if(registerCheckCount<loginTryCount) Toast.makeText(getContext(), "Mutltiple faces detected ...", Toast.LENGTH_LONG).show();
                //return;
            }else if(face_count[0]==1){
                if(registerCheckCount<1) {
                    canvas.drawRect(mFacePositions[0].x1, mFacePositions[0].y1, mFacePositions[0].x2, mFacePositions[0].y2, mPaintBlueTransparent);
                    int r = this.register(IDs[0]);
                    if(r==REGISTERED) {
                        registerCheckCount = 1;
                    }else if(r==ALREADY_REGISTERED){
                        response(true, "Already registered", mAttributeValues[0]);
                        //mStopped = 1;
                        mStopping = 1;
                        return;
                    }
                }else {
                    String name = this.performRegistrationAgain(IDs[0]);
                    Log.e("com.luxand.dsi::", name);
                    if(name==null || !name.equals(generatedName)) {
                        //Toast.makeText(getContext(), "Unable to identify you", Toast.LENGTH_LONG);
                        //purge id
                        remove(IDs[0]);
                        response(true,"Unable to identify you", mAttributeValues[0]);
                        mStopping = 1;
                        return;
                    }
                    registerCheckCount++;
                    canvas.drawRect(mFacePositions[0].x1, mFacePositions[0].y1, mFacePositions[0].x2, mFacePositions[0].y2, mPaintBlueTransparent);
                    if(registerCheckCount>=loginTryCount) {
                        this.name = name;
                        this.correspondingId = IDs[0];
                        response(false, "Identified successfully", mAttributeValues[0]);
                        mStopping = 1;
                        return;
                    }
                }
            }
        }

        super.onDraw(canvas);
    } // end onDraw method

    private void response(boolean error, String message,  String extra) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("error", error);
            obj.put("message", message);
            obj.put("name", name);
            obj.put("id", this.correspondingId);
            obj.put("extra", new JSONObject(extra));
        } catch (JSONException e) {
            e.printStackTrace();
            obj = new JSONObject();
        }
        if(this.onImageProcessListener != null) {
            this.onImageProcessListener.handle(obj);
        }
    }
    private String generateName () {
        return "OML-LUXAND"+new Date().getTime();
    }
    private boolean remove(long id) {
        FSDK.LockID(mTracker, id);
        int ok = FSDK.PurgeID(mTracker, id);
        FSDK.UnlockID(mTracker, id);
        return ok == FSDK.FSDKE_OK;
    }

    private String performRegistrationAgain(long id) {
        FSDK.LockID(mTracker, id);
        String names[] = new String[1];
        FSDK.GetAllNames(mTracker, id, names, 1024);
        FSDK.UnlockID(mTracker, id);
        if (names[0] != null && names[0].length() > 0) {
            return names[0];
        }else {
            return null;
        }
    }
    private int register(long id) {
        String userName = null;
        userName = performRegistrationAgain(id);
        if(userName!=null) {
            return ALREADY_REGISTERED;
        }
        FSDK.LockID(mTracker, id);
        userName = generateName();
        generatedName = userName;
        boolean r;
        r = FSDK.SetName(mTracker, id, userName)==FSDK.FSDKE_OK;
        if (userName.length() <= 0){
            r = false;
            FSDK.PurgeID(mTracker, id);
        }
        FSDK.UnlockID(mTracker, id);
        return r ? REGISTERED : NOT_REGISTERED;
    }
    private boolean recognize(long id) {
        String names[] = new String[1];
        FSDK.GetAllNames(mTracker, id, names, 1024);
        if (names[0] != null && names[0].length() > 0) {
            Log.e("com.luxand.dsi::", names[0]);
            this.name = names[0];
            this.correspondingId = id;
            return true;
        }else {
            return false;
        }
    }
    /*@Override
    public boolean onTouchEvent(MotionEvent event) { //NOTE: the method can be implemented in Preview class
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int x = (int)event.getX();
                int y = (int)event.getY();

                faceLock.lock();
                FaceRectangle rects[] = new FaceRectangle[MAX_FACES];
                long IDs[] = new long[MAX_FACES];
                for (int i=0; i<MAX_FACES; ++i) {
                    rects[i] = new FaceRectangle();
                    rects[i].x1 = mFacePositions[i].x1;
                    rects[i].y1 = mFacePositions[i].y1;
                    rects[i].x2 = mFacePositions[i].x2;
                    rects[i].y2 = mFacePositions[i].y2;
                    IDs[i] = mIDs[i];
                }
                faceLock.unlock();

                for (int i=0; i<MAX_FACES; ++i) {
                    if (rects[i] != null && rects[i].x1 <= x && x <= rects[i].x2 && rects[i].y1 <= y && y <= rects[i].y2 + 30) {
                        mTouchedID = IDs[i];

                        mTouchedIndex = i;

                        // requesting name on tapping the face
                        final EditText input = new EditText(mContext);
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        builder.setMessage("Enter person's name" )
                                .setView(input)
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface dialogInterface, int j) {
                                        FSDK.LockID(mTracker, mTouchedID);
                                        String userName = input.getText().toString();
                                        FSDK.SetName(mTracker, mTouchedID, userName);
                                        if (userName.length() <= 0) FSDK.PurgeID(mTracker, mTouchedID);
                                        FSDK.UnlockID(mTracker, mTouchedID);
                                        mTouchedIndex = -1;
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface dialogInterface, int j) {
                                        mTouchedIndex = -1;
                                    }
                                })
                                .setCancelable(false) // cancel with button only
                                .show();

                        break;
                    }
                }
        }
        return true;
    }*/

    static public void decodeYUV420SP(byte[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        int yp = 0;
        for (int j = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);
                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;

                rgb[3*yp] = (byte) ((r >> 10) & 0xff);
                rgb[3*yp+1] = (byte) ((g >> 10) & 0xff);
                rgb[3*yp+2] = (byte) ((b >> 10) & 0xff);
                ++yp;
            }
        }
    }
} // end of ProcessImageAndDrawResults class

