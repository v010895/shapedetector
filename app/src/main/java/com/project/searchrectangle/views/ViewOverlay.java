package com.project.searchrectangle.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.Nullable;

public class ViewOverlay extends View {
  private ArrayList<RectF> rects = new ArrayList<>();
  private Paint paint;

  public ViewOverlay(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    paint = new Paint();
    paint.setStrokeWidth(10.0f);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(Color.GREEN);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    for(RectF rect: rects)
    {
      canvas.drawRect(rect,paint);
    }

  }

  public void postPoints(ArrayList<RectF> rectangles)
  {
    rects.clear();
    rects.addAll(rectangles);
    invalidate();
  }
}
