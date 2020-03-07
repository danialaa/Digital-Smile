package com.example.digitalsmile;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.ImageDecoder;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
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
    private Mat teplateR;
    private Mat teplateL;
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

    double xCenter = -1;
    double yCenter = -1;
    double smileWidth = -1;
    double smileHeight = -1;
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
                switch (method) {
                    case 0:
                        mValue.setText("TM_SQDIFF");
                        break;
                    case 1:
                        mValue.setText("TM_SQDIFF_NORMED");
                        break;
                    case 2:
                        mValue.setText("TM_CCOEFF");
                        break;
                    case 3:
                        mValue.setText("TM_CCOEFF_NORMED");
                        break;
                    case 4:
                        mValue.setText("TM_CCORR");
                        break;
                    case 5:
                        mValue.setText("TM_CCORR_NORMED");
                        break;
                }
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

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba,mGray,Imgproc.COLOR_RGB2GRAY);
        Core.transpose(mRgba,mRgbaT);
        Imgproc.resize(mRgbaT,mRgbaF,mRgbaF.size(),0,0,0);
        Core.flip(mRgbaF,mRgba,0);
        Bitmap src = ((BitmapDrawable)teeth.getDrawable()).getBitmap();
        int width = 50;
        int height = 150;
        Mat dstmat = new Mat(width, height, CvType.CV_8UC4);
        Mat srcmat = new Mat(src.getWidth(), src.getHeight(),  CvType.CV_8UC4);
teeth.setVisibility(View.VISIBLE);
        Utils.bitmapToMat(src, srcmat);
        mRgba.convertTo(mRgba,  CvType.CV_8UC4);

        //dstmat = srcmat;g

        Imgproc.resize(srcmat, dstmat, new Size( 330, 310),0,0,0);
        Imgproc.resize(dstmat, dstmat, new Size( 330, 310),0,0,0);
        Imgproc.putText(mRgba, "[" + dstmat.width()+ "," + dstmat.height() + ","+ mRgba.width()+ "]",
                new Point(30+ 20, 30 + 20),
                Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
                        255));
Bitmap ss = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
  //   Utils.matToBitmap(dstmat,ss);
        Log.d("size", dstmat.width() + " and "+ dstmat.height());
      //  dstmat.copyTo(mRgba.rowRange(100, 100 +dstmat.width()).colRange(100,100 + dstmat.height()));
    //       teeth.setImageBitmap(ss);

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
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray(), mouthsArray = mouths.toArray();

        for (int i = 0; i < mouthsArray.length; i++)
        {
            Imgproc.rectangle(mRgba, mouthsArray[i].tl(), mouthsArray[i].br(), MOUTH_RECT_COLOR, 3);
            xCenter = (mouthsArray[i].x + mouthsArray[i].width + mouthsArray[i].x) / 2;
            yCenter = (mouthsArray[i].y + mouthsArray[i].y + mouthsArray[i].height) / 2;
            Point center = new Point(xCenter, yCenter);

            smileWidth = mouthsArray[i].width;
            smileHeight = mouthsArray[i].height;
            mRgba.convertTo(mRgba,  CvType.CV_8UC4);
            src = ((BitmapDrawable)teeth.getDrawable()).getBitmap();
            width = (int)(mouthsArray[i].br().x - mouthsArray[i].tl().x);


            height =(int)( mouthsArray[i].br().y - mouthsArray[i].tl().y);

            dstmat = new Mat(width, height, CvType.CV_8UC4);
            srcmat = new Mat(src.getWidth(), src.getHeight(),  CvType.CV_8UC4);

            Utils.bitmapToMat(src, srcmat);
            mRgba.convertTo(mRgba,  CvType.CV_8UC4);

            //dstmat = srcmat;

            Imgproc.resize(srcmat, dstmat, new Size( height, height),0,0,0);
            Imgproc.resize(dstmat, dstmat, new Size( height, height),0,0,0);

            Imgproc.putText(mRgba, "[" + mouthsArray[i].x+ "," + (mouthsArray[i].x + dstmat.width()) + ","+ mRgba.width()+ "]",
                    new Point(center.x + 20, center.y + 20),
                    Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
                            255));
            Imgproc.putText(mRgba, "[" + mouthsArray[i].y+ "," + (mouthsArray[i].y + dstmat.height()) + ","+ mRgba.height()+ "]",
                    new Point(center.x + 60, center.y + 60),
                    Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
                            255));

            int r2 = mouthsArray[i].x;
            int c2 = mouthsArray[i].y;
            //mRgba = dstmat;
            if(0 <= mouthsArray[i].x  && mouthsArray[i].x < mouthsArray[i].x + dstmat.width() && dstmat.width() + mouthsArray[i].x < mRgba.width())
            {
                if(0 <= mouthsArray[i].y  && mouthsArray[i].y < mouthsArray[i].y + dstmat.width() && dstmat.height() + mouthsArray[i].y < mRgba.height()) {

                    dstmat.copyTo(mRgba.rowRange(mouthsArray[i].x, mouthsArray[i].x + dstmat.height()).colRange(mouthsArray[i].y, mouthsArray[i].y + dstmat.height()));
                }
            }
        }

        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

    public Point getSmileLocation() {
        Point point = new Point(xCenter, yCenter);
        return point;
    }

    public Point getSmileSize() {
        Point point = new Point(smileWidth, smileHeight);
        return point;
    }

    public void onRecreateClick(View view) {
        learn_frames = 0;
    }
}
