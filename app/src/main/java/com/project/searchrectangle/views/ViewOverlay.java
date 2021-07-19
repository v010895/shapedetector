package com.project.searchrectangle.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class ViewOverlay extends View {
  private ArrayList<Point> points = new ArrayList<>();
  private Paint paint;

  public ViewOverlay(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    paint = new Paint();
    paint.setStrokeWidth(5.0f);
    paint.setStyle(Paint.Style.STROKE);
    paint.setColor(Color.GREEN);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    for(Point point: points)
    {
      canvas.drawPoint((float)point.x,(float)point.y,paint);
    }

  }

  public void postPoints(List<Point> contours,double ratioX,double ratioY)
  {
    points.clear();
    points.addAll(contours);
    invalidate();
  }
}
