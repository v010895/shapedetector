package com.project.searchrectangle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.project.searchrectangle.databinding.ActivityMainBinding;
import com.project.searchrectangle.processors.ShapeDetector;
import com.project.searchrectangle.viewmodels.CameraViewModel;
import com.project.searchrectangle.viewmodels.CameraViewModelFactory;

import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
  private static final String[] PERMISSION_ARRAY = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
  private static final String TAG = "myDebug";
  private CameraViewModel cameraViewModel;
  private ProcessCameraProvider cameraProvider;
  private ImageAnalysis imageAnalysis;
  private ActivityMainBinding binding;
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private double metricHeight = 0.0;
  private double metricWidth = 0.0;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    ViewModelProvider.Factory factory = new CameraViewModelFactory(this.getApplication());
    cameraViewModel = new ViewModelProvider(this, factory).get(CameraViewModel.class);
    cameraViewModel.cameraProvider.observe(this, processCameraProvider -> {
      cameraProvider = processCameraProvider;
      tryInitCamera();
    });
    requestPermissions();

    
  }
  static{
    if(!OpenCVLoader.initDebug())
    {
        Log.e(TAG,"Opencv initialize fail");
    }
    else{
        Log.e(TAG,"Opencv initialize success");
    }
  }
  private LoaderCallbackInterface callback = new LoaderCallbackInterface() {
    @Override
    public void onManagerConnected(int status) {

    }

    @Override
    public void onPackageInstall(int operation, InstallCallbackInterface callback) {

    }
  };

  private void requestPermissions() {
    ActivityResultLauncher<String[]> mStartForResult = registerForActivityResult(
        new ActivityResultContracts.RequestMultiplePermissions(), result -> {
          if(arePermissionGrant())
          {
            tryInitCamera();
          }
          else{
              Toast.makeText(this, R.string.premission_message,Toast.LENGTH_SHORT).show();
              finish();
          }
        });
    mStartForResult.launch(PERMISSION_ARRAY);
  }
  private int aspectRatio(int width, int height)
  {
      double previewRatio = (double)Math.max(width,height)/Math.min(width,height);
      if(Math.abs(previewRatio-AspectRatio.RATIO_4_3) <= Math.abs(previewRatio-AspectRatio.RATIO_16_9))
      {
        return AspectRatio.RATIO_4_3;
      }
      return AspectRatio.RATIO_16_9;
  }
  private void tryInitCamera() {
      if(cameraProvider !=null && arePermissionGrant())
      {
          Preview preview = new Preview.Builder()
              .setTargetAspectRatio(AspectRatio.RATIO_4_3)
              .build();

          metricHeight = binding.preview.getHeight()*binding.preview.getScaleY();
          metricWidth = binding.preview.getWidth()*binding.preview.getScaleX();
          int rotation = binding.preview.getDisplay().getRotation();
          if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
          {
            double temp = metricWidth;
            metricWidth = metricHeight;
            metricHeight = temp;
          }
          Log.i(TAG,"Screen metric "+ metricHeight + " x " + metricWidth);
          preview.setSurfaceProvider(binding.preview.getSurfaceProvider());
          int lenFacing = getCameraLenFacing(cameraProvider);
          CameraSelector selector = new CameraSelector.Builder()
              .requireLensFacing(lenFacing)
              .build();
          imageAnalysis = new ImageAnalysis.Builder()
              .setTargetAspectRatio(AspectRatio.RATIO_4_3)
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build();

          ImageAnalysis.Analyzer shapeDetector = setupDetector();
          imageAnalysis.setAnalyzer(executor,shapeDetector);
          cameraProvider.unbindAll();
          try{
            cameraProvider.bindToLifecycle(this,selector,preview,imageAnalysis);
          }catch (Exception exception)
          {
            Log.e(TAG,"Camera exception: "+exception.getMessage());
          }

      }
  }
  private ImageAnalysis.Analyzer setupDetector(){
    ShapeDetector detector = new ShapeDetector(metricHeight,metricWidth);
    ShapeDetector.Listener listener = new ShapeDetector.Listener() {
      @Override
      public void onShapeDetect(ArrayList<RectF> rects) {
          binding.overlay.postPoints(rects);
      }
    };
    detector.setListener(listener);
    return detector;
  }
  @Override
  protected void onDestroy() {
    super.onDestroy();
    executor.shutdown();
  }

  private int getCameraLenFacing(ProcessCameraProvider cameraProvider){
    try{
        if(cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
        {
          return CameraSelector.LENS_FACING_BACK;
        }
        if(cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA))
        {
          return CameraSelector.LENS_FACING_FRONT;
        }
    }catch(CameraInfoUnavailableException exception)
    {

    }
    throw new IllegalStateException("Invalid CameraSelector");
  }
  private Boolean arePermissionGrant() {
    Boolean isGrant = false;
    for (String permission : PERMISSION_ARRAY) {
      isGrant = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }
    return isGrant;
  }

}