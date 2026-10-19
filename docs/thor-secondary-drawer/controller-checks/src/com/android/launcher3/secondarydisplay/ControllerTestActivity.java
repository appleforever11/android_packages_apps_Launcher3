// Copyright (C) 2026 The LineageOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.launcher3.secondarydisplay;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Rect;
import android.view.View;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public final class ControllerTestActivity extends Activity {
 Root root;
 @Override public void onCreate(Bundle state){super.onCreate(state);root=new Root(this);setContentView(root);}
 static final class Root extends FrameLayout {
  final View drawer;
  SecondaryDrawerResizeController controller;
  final Rect usable=new Rect();
  Root(Activity activity){super(activity);drawer=new View(activity);drawer.setBackgroundColor(0xff454545);addView(drawer);controller=new SecondaryDrawerResizeController(this,drawer);}
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();controller.attach();}
  @Override protected void onDetachedFromWindow(){controller.detach();super.onDetachedFromWindow();}
  @Override protected void onMeasure(int w,int h){
   int width=MeasureSpec.getSize(w),height=MeasureSpec.getSize(h);setMeasuredDimension(width,height);
   android.view.WindowInsets insets=getRootWindowInsets();
   android.graphics.Insets bars=insets==null?android.graphics.Insets.NONE:insets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());
   usable.set(36,Math.max(36,bars.top+23),width-36,height-Math.max(36,bars.bottom+23));
   drawer.measure(MeasureSpec.makeMeasureSpec(Math.round(usable.width()*controller.getWidthRatio()),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.round(usable.height()*controller.getHeightRatio()),MeasureSpec.EXACTLY));
  }
  @Override protected void onLayout(boolean changed,int l,int t,int r,int b){drawer.layout(usable.right-drawer.getMeasuredWidth(),usable.bottom-drawer.getMeasuredHeight(),usable.right,usable.bottom);controller.layout(usable);}
  @Override public boolean dispatchTouchEvent(MotionEvent event){return controller.touch(event)||super.dispatchTouchEvent(event);}
 }
}
