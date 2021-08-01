package com.project.searchrectangle.viewmodels;

import android.app.Application;

import com.google.common.util.concurrent.ListenableFuture;
import com.project.searchrectangle.event.Event;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class CameraViewModel extends AndroidViewModel {

  public MutableLiveData<ProcessCameraProvider> cameraProvider = new MutableLiveData();
  public MutableLiveData<Event<Boolean>> initFinish = new MutableLiveData<>();
  public CameraViewModel(Application application)
  {
       super(application);
       initCameraProvider(application);

  }
  private void initCameraProvider(Application application){
      ListenableFuture<ProcessCameraProvider> provider = ProcessCameraProvider.getInstance(application);
      provider.addListener(new Runnable() {
        @Override
        public void run() {
          try {
            cameraProvider.postValue(provider.get());
          } catch (ExecutionException e) {
            e.printStackTrace();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }, ContextCompat.getMainExecutor(application));
  }
  public void setInitFinish(boolean isFinish)
  {
    initFinish.postValue(new Event<Boolean>(isFinish));
  }
}
