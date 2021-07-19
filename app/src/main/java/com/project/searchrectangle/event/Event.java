package com.project.searchrectangle.event;

public class Event<T> {
  private T content;
  private boolean isHandle = false;
  public Event(T content)
  {
    this.content = content;
  }
  public T getContentIfNotHandle(){
    if(isHandle)
    {
      return null;
    }
    else{
      return content;
    }
  }
}

