package com.example.digitalsmile;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.logging.Filter;

import static org.opencv.core.CvType.CV_8UC3;

public class Tooth {
    private int tooth_ID;
    private String tooth_Name;
    private Point tooth_Position;
    private Size tooth_Size;
    private boolean isImageAssigned;
    private Bitmap tooth_DefaultAugmentedImage;
    private Bitmap tooth_OverlayImage;

    public Tooth(int x, int y, int width, int height, Bitmap defaultImage) {
        setTooth_ID(0);
        setTooth_Name("DefaultTooth");
        setTooth_Size(new Size(width, height));
        setTooth_Position(new Point(x, y));
        isImageAssigned = false;
        setTooth_DefaultAugmentedImage(defaultImage);
    }

    public Tooth(int tooth_ID, String tooth_Name, int x, int y , int width, int height, Bitmap defaultImage) {
        setTooth_ID(tooth_ID);
        setTooth_Name(tooth_Name);
        setTooth_Size(new Size(width, height));
        setTooth_Position(new Point(x, y));
        isImageAssigned = false;
        setTooth_DefaultAugmentedImage(defaultImage);
    }

    public int getTooth_ID() {
        return tooth_ID;
    }

    public void setTooth_ID(int tooth_ID) {
        this.tooth_ID = tooth_ID;
    }

    public String getTooth_Name() {
        return tooth_Name;
    }

    public void setTooth_Name(String tooth_Name) {
        this.tooth_Name = tooth_Name;
    }

    public Point getTooth_Position() {
        return tooth_Position;
    }

    public void setTooth_Position(Point tooth_Position) {
        this.tooth_Position = tooth_Position;
    }

    public Size getTooth_Size() {
        return tooth_Size;
    }

    public void setTooth_Size(Size tooth_Size) {
        this.tooth_Size = tooth_Size;
    }

    public Bitmap getTooth_DefaultAugmentedImage() {
        return tooth_DefaultAugmentedImage;
    }

    public void setTooth_DefaultAugmentedImage(Bitmap tooth_DefaultAugmentedImage) {
        this.tooth_DefaultAugmentedImage = tooth_DefaultAugmentedImage;
        adjust_OverlayImage();
    }

    public boolean isImageAssigned() {
        return isImageAssigned;
    }

    public Bitmap getTooth_OverlayImage() {
        return tooth_OverlayImage;
    }

    public void setTooth_OverlayImage(Bitmap tooth_OverlayImage) {
        this.tooth_OverlayImage = tooth_OverlayImage;
    }

    private void adjust_OverlayImage() {
        if(tooth_DefaultAugmentedImage != null) {
            tooth_OverlayImage = tooth_DefaultAugmentedImage.copy(Bitmap.Config.ARGB_8888, true);
            resize_Tooth((int)tooth_Size.width, (int)tooth_Size.height);

            if (!isImageAssigned) {
                isImageAssigned = true;
            }
        }
    }

    public void reset_Teeth() {
        if(isImageAssigned)
        {
            tooth_OverlayImage = tooth_DefaultAugmentedImage.copy(Bitmap.Config.ARGB_8888,true);
            adjust_OverlayImage();
        }
    }

    public void translate_Tooth(int x, int y) {
        tooth_Position = new Point(tooth_Position.x + x , tooth_Position.y + y);
    }

    public void resize_Tooth(int width, int height) {
        Bitmap teethBit = Bitmap.createScaledBitmap(tooth_OverlayImage, width, height, true);
        tooth_OverlayImage = teethBit;
    }

    public void change_ImageWidth(int width, int height) {
        Bitmap teethBit = Bitmap.createScaledBitmap(tooth_OverlayImage, width, height, true);
        tooth_OverlayImage = teethBit;
    }

    public Mat augment_Tooth(Mat destination, int cWidth) {
        if (isImageAssigned) {
            Mat srcmat = new Mat(tooth_OverlayImage.getWidth(), tooth_OverlayImage.getHeight(), CvType.CV_8UC4);
            Utils.bitmapToMat(tooth_OverlayImage, srcmat);

            destination.convertTo(destination, CvType.CV_8UC4);

            Bitmap Destination_Bitmap = Bitmap.createBitmap(destination.width(), destination.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(destination, Destination_Bitmap);
            Mat filtered = new Mat(destination.width(), destination.height(), CvType.CV_8UC4);

            Mat mask = new Mat(destination.width(), destination.height(), CvType.CV_8UC4);
            Imgproc.cvtColor(destination, filtered, Imgproc.COLOR_BGR2GRAY);

            Imgproc.cvtColor(destination, mask, Imgproc.COLOR_BGR2GRAY);
            Imgproc.blur(filtered, filtered, new Size(5, 5));
            Imgproc.adaptiveThreshold(filtered, mask, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 11, 2);

            for (int r = 0, r2 = (int)tooth_Position.x; r < tooth_OverlayImage.getWidth() && r2 < cWidth; r++, r2++) {
                for (int c = 0, c2 = (int)tooth_Position.y; c < tooth_OverlayImage.getHeight() && c2 < Destination_Bitmap.getHeight(); c2++, c++) {
                    int pixVal = tooth_OverlayImage.getPixel(r, c);
                    int pix2Val = Destination_Bitmap.getPixel(r2, c2);
                    int red = Color.red(pix2Val);
                    int green = Color.green((pix2Val));
                    int blue = Color.blue((pix2Val));
                    int white = Color.WHITE;
                    int yellow = Color.YELLOW;

                    if (pixVal != 0 && red > 120 && blue > 120 && green > 120){
                        Destination_Bitmap.setPixel(r2, c2, tooth_OverlayImage.getPixel(r, c));
                    }
                }
            }

            //Utils.bitmapToMat(Destination_Bitmap, destination);
            destination = mask;
        }

        return destination;
    }

    public Mat augment_Tooth(Mat destination) {
        if (isImageAssigned) {
            Mat srcmat = new Mat(tooth_OverlayImage.getWidth(), tooth_OverlayImage.getHeight(), CvType.CV_8UC4);
            Utils.bitmapToMat(tooth_OverlayImage, srcmat);

            destination.convertTo(destination, CvType.CV_8UC4);
            Bitmap Destination_Bitmap = Bitmap.createBitmap(destination.width(), destination.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(destination, Destination_Bitmap);

            for (int r = 0, r2 = (int) tooth_Position.x; r < tooth_OverlayImage.getWidth() && r2 < Destination_Bitmap.getWidth(); r++, r2++) {
                for (int c = 0, c2 = (int) tooth_Position.y; c < tooth_OverlayImage.getHeight() && c2 < Destination_Bitmap.getHeight(); c2++, c++) {
                    int pixVal = tooth_OverlayImage.getPixel(r, c);

                    if (pixVal != 0) {
                        Destination_Bitmap.setPixel(r2, c2, tooth_OverlayImage.getPixel(r, c));
                    }
                }
            }

            Utils.bitmapToMat(Destination_Bitmap, destination);
        }

        return destination;
    }

    public Mat augment_Tooth(int X, int Y, Mat destination) {
        if (isImageAssigned) {
            Mat srcmat = new Mat(tooth_OverlayImage.getWidth(), tooth_OverlayImage.getHeight(), CvType.CV_8UC4);
            Utils.bitmapToMat(tooth_OverlayImage, srcmat);

            destination.convertTo(destination, CvType.CV_8UC4);
            Bitmap Destination_Bitmap = Bitmap.createBitmap(destination.width(), destination.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(destination, Destination_Bitmap);

            for (int r = 0, r2 = X; r < tooth_OverlayImage.getWidth() && r2 < Destination_Bitmap.getWidth(); r++, r2++) {
                for (int c = 0, c2 = Y; c < tooth_OverlayImage.getHeight() && c2 < Destination_Bitmap.getHeight(); c2++, c++) {
                    int pixVal = tooth_OverlayImage.getPixel(r, c);

                    if (pixVal != 0) {
                        Destination_Bitmap.setPixel(r2, c2, tooth_OverlayImage.getPixel(r, c));
                    }
                }
            }

            Utils.bitmapToMat(Destination_Bitmap, destination);
        }

        return destination;
    }

    public void rotate_Tooth(float angle) {
        Matrix toothMat = new Matrix();
        toothMat.postRotate(angle);
        tooth_OverlayImage = Bitmap.createBitmap(tooth_OverlayImage, 0, 0, tooth_OverlayImage.getWidth(),
                tooth_OverlayImage.getHeight(), toothMat, true);
    }

    public void color_Tooth(int red, int green, int blue, double alpha) {
        int color = Color.argb(200, red, green, blue);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        Bitmap bitmapResult = Bitmap.createBitmap(tooth_OverlayImage.getWidth(), tooth_OverlayImage.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapResult);
        canvas.drawBitmap(tooth_OverlayImage, 0, 0, paint);
        tooth_OverlayImage = bitmapResult;
    }
}