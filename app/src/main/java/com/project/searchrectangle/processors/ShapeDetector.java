package com.project.searchrectangle.processors;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.RectF;
import android.media.Image;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

public class ShapeDetector implements ImageAnalysis.Analyzer {
  private static final boolean DEBUG = false;
  private int analyImageHeight;
  private int analyImageWidth;
  private int counter = 0;
  private Listener listener;
  private double metricHeight, metricWidth,ratioX,ratioY;
  private int epsilon=20;
  private int cannyThreshold=100;

  public ShapeDetector(double metricHeight, double metricWidth) {
    this.metricHeight = metricHeight;
    this.metricWidth = metricWidth;

  }

  @Override
  public void analyze(@NonNull @NotNull ImageProxy image) {

    ByteBuffer bufferY = image.getPlanes()[0].getBuffer();
    //create cv Mat
    int rotation = image.getImageInfo().getRotationDegrees();
    int imageWidth = (rotation == 90 || rotation == 270) ? image.getHeight():image.getWidth();
    int imageHeight = (rotation == 90 || rotation == 270)? image.getWidth():image.getHeight();
    ratioX = this.metricWidth / imageWidth;
    ratioY = this.metricHeight / imageHeight;
    Mat cvImage = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1, bufferY);
    Mat rotateImage;
    if(rotation == 90)
      rotateImage = rotateMat(cvImage, 90);
    else
      rotateImage = cvImage;
    //Imgproc.warpAffine(cvImage,cvImageDst,rotateMat,cvImageDst.size());
    Imgproc.GaussianBlur(rotateImage, rotateImage, new Size(5, 5), 0.0);
    Mat cannyOutput = new Mat();
    Imgproc.Canny(rotateImage, cannyOutput, cannyThreshold, cannyThreshold * 2);
    List<MatOfPoint> contours = new ArrayList<>();
    Mat hierarchy = new Mat();
    Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
    Mat drawing = new Mat().zeros(cannyOutput.height(), cannyOutput.width(), CvType.CV_8UC3);
    /*
    for (int i = 0; i < contours.size(); i++) {
      Scalar color = new Scalar(0, 255, 0);
      Imgproc.drawContours(drawing, contours, i, color, 2, Imgproc.LINE_8, hierarchy, 0);
    }

     */
    ArrayList<RectF> rects = new ArrayList<>();
    findRect(rects,contours);
    saveDebugImage(rotateImage);
    //output ARGB_8888
    /*
    int[] resultArray = new int[drawing.cols() * drawing.rows()];
    for (int i = 0; i < drawing.rows(); i++) {
      for (int j = 0; j < drawing.cols(); j++) {
        double[] array = drawing.get(i, j);
        resultArray[j + i * drawing.cols()] = (((int) array[0]) << 11 | ((int) array[1]) << 5 | ((int) array[2])) << 8;
      }
    }

    Bitmap result = Bitmap.createBitmap(resultArray, image.getHeight(), image.getWidth(), Bitmap.Config.RGB_565);
    */
    listener.onShapeDetect(rects);
    image.close();
  }
  private void findRect(ArrayList<RectF> rects, List<MatOfPoint> contours)
  {
    for(int i=0;i<contours.size();i++)
    {
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint2f contourPoints = new MatOfPoint2f(contours.get(i).toArray());
        Imgproc.approxPolyDP(contourPoints,approxCurve,epsilon,true);
        if(approxCurve.toArray().length == 4)
        {
          MatOfPoint points = new MatOfPoint(approxCurve.toArray());
          Rect rect = Imgproc.boundingRect(points);
          RectF rectF = new RectF((float)(rect.tl().x * ratioX),(float)(rect.tl().y * ratioY),
              (float)(rect.br().x*ratioX), (float)(rect.br().y*ratioY));
          rects.add(rectF);
        }
    }
  }
  private Mat rotateMat(Mat inputMat, int degree) {
    //rotate Image 90 degree
    Mat outputMat = new Mat();
    switch (degree) {
      case 90:
        Core.transpose(inputMat, outputMat);
        Core.flip(outputMat, outputMat, 1);
        break;
      case 180:
        Core.flip(inputMat, outputMat, -1);
        break;
      case 270:
        Core.transpose(inputMat,outputMat);
        Core.flip(outputMat,outputMat,0);
        break;
      default:
        break;
    }
    return outputMat;
  }

  private void saveDebugImage(Mat saveImage) {
    if (!DEBUG)
      return;
    String fileName = String.format("sdcard/Download/tempImage/%05d.jpg", counter);
    Imgcodecs.imwrite(fileName, saveImage);
    counter++;
  }
  public void setEpsilon(int epsilon)
  {
    this.epsilon = epsilon;
  }
  public void setCannyThreshold(int threshold)
  {
    this.cannyThreshold = threshold;
  }
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public interface Listener {
    void onShapeDetect(ArrayList<RectF> rects);
  }
}
