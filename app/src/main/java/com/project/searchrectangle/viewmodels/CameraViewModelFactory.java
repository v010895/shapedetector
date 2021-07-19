package com.project.searchrectangle.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class CameraViewModelFactory implements ViewModelProvider.Factory {
  private Application app;
  public CameraViewModelFactory(Application application)
  {
    this.app = application;
  }
  @NonNull
  @Override
  public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
    if(modelClass.isAssignableFrom(CameraViewModel.class))
      return (T) new CameraViewModel(this.app);
    else{
      throw new IllegalArgumentException("Unknown class");
    }
  }
}
