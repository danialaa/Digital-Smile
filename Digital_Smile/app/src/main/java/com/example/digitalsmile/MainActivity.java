package com.example.digitalsmile;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private static final Scalar MOUTH_RECT_COLOR = new Scalar(255, 0, 0, 255);
    public  static final int JAVA_DETECTOR = 0;

    private int learn_frames = 0;
    int method = 0;

    // matrix for zooming
    private Mat mZoomWindow;
    private Mat mZoomWindow2;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;

    private Mat mRgba;
    private Mat mRgbaT;
    private Mat mRgbaF;
    private Mat mGray;

    private File mCascadeFile;
    private File mCascadeFileMouth;
    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorMouth;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;
    private SeekBar mMethodSeekbar;
    private TextView mValue;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // load cascade file from application resources
                        InputStream ise = getResources().openRawResource(R.raw.haarcascade_mcs_mouth);
                        File cascadeDirMouth = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFileMouth = new File(cascadeDirMouth, "haarcascade_mcs_mouth.xml");
                        FileOutputStream ose = new FileOutputStream(mCascadeFileMouth);

                        while ((bytesRead = ise.read(buffer)) != -1) {
                            ose.write(buffer, 0, bytesRead);
                        }

                        ise.close();
                        ose.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                        }

                        mJavaDetectorMouth = new CascadeClassifier(mCascadeFileMouth.getAbsolutePath());
                        if (mJavaDetectorMouth.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier for mouth");
                            mJavaDetectorMouth = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFileMouth.getAbsolutePath());
                        }

                        cascadeDir.delete();
                        cascadeDirMouth.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private ImageView teeth;

    public MainActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        teeth = findViewById(R.id.teeth);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mMethodSeekbar = (SeekBar) findViewById(R.id.methodSeekBar);
        mMethodSeekbar.setMax(30);
        method = mMethodSeekbar.getMax();

        mValue = (TextView) findViewById(R.id.method);

        mMethodSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                method = progress;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    int computeDistance(int width, int maxValue, int currentValue)
    {
       int percentageWidth =0 ;
       percentageWidth = (currentValue * width ) / maxValue;
       return percentageWidth;
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat(height,width,CvType.CV_8UC4);
        mRgbaF = new Mat(height,width,CvType.CV_8UC4);
        mRgbaT = new Mat(width,height,CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mZoomWindow.release();
        mZoomWindow2.release();
    }

    public Mat augmentTeeth(int width , int height , int startx , int starty, Mat mRgba , int customWidth) {
        if(customWidth < 0) {
            customWidth = width;
        }

        Bitmap src = ((BitmapDrawable)teeth.getDrawable()).getBitmap();
        Mat srcmat = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(src, srcmat);

        mRgba.convertTo(mRgba, CvType.CV_8UC4);

        Bitmap teethbit = Bitmap.createScaledBitmap(src,width,height,true);
        src = teethbit;

        Bitmap ss = Bitmap.createBitmap(mRgba.width(),mRgba.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba,ss);

        for(int r = 0, r2 = startx; r < src.getWidth() && r2 < customWidth; r++, r2++ )
        {
            for( int c = 0, c2 = starty; c < src.getHeight() && c2 < ss.getHeight(); c2++, c++)
            {
                int pixval = src.getPixel(r, c);

                if( pixval != 0) {
                    ss.setPixel(r2, c2, src.getPixel(r, c));
                }
            }
        }

        Utils.bitmapToMat(ss,mRgba);

        return mRgba;
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

//        Core.transpose(mRgba,mRgbaT);
//        Imgproc.resize(mRgbaT,mRgbaF,mRgbaF.size(),0,0,0);
//        Core.flip(mRgbaF,mRgba,0);

        Imgproc.cvtColor(mRgba,mGray,Imgproc.COLOR_RGB2GRAY);
        int Cwidth;
 //       mRgba = augmentTeeth(350,350,100,100,mRgba,Cwidth);

        int height;

        if (mAbsoluteFaceSize == 0) {
            height = mGray.rows();

            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        if (mZoomWindow == null || mZoomWindow2 == null) {
            CreateAuxiliaryMats();
        }

        MatOfRect faces = new MatOfRect(), mouths = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null) {
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
                mJavaDetectorMouth.detectMultiScale(mGray, mouths, 1.1, 2, 2,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
            }
        } else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] mouthsArray = mouths.toArray();

        for (int i = 0; i < mouthsArray.length; i++)
        {
            //Imgproc.rectangle(mRgba, mouthsArray[i].tl(), mouthsArray[i].br(), MOUTH_RECT_COLOR, 3);
            Cwidth = computeDistance(mRgba.width(),mMethodSeekbar.getMax(),method);
            mRgba = augmentTeeth(mouthsArray[i].width,mouthsArray[i].height,mouthsArray[i].x,mouthsArray[i].y,mRgba,Cwidth );
            break;
        }

        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50) {
            setMinFaceSize(0.5f);
        } else if (item == mItemFace40) {
            setMinFaceSize(0.4f);
        } else if (item == mItemFace30) {
            setMinFaceSize(0.3f);
        } else if (item == mItemFace20) {
            setMinFaceSize(0.2f);
        }

        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void CreateAuxiliaryMats() {
        if (mGray.empty()) {
            return;
        }

        int rows = mGray.rows();
        int cols = mGray.cols();

        if (mZoomWindow == null) {
            mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2
                    + cols / 10, cols);
            mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2
                    + cols / 10, cols);
        }
    }

    public void onRecreateClick(View view) {
        learn_frames = 0;
    }
}
