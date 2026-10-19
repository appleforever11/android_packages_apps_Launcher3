// Copyright (C) 2026 The LineageOS Project
// SPDX-License-Identifier: Apache-2.0
package com.android.launcher3.secondarydisplay;
import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;

public final class DrawerChecks extends Instrumentation {
 private ControllerTestActivity activity;
 private int assertions;
 private boolean verifySaved;
 private final StringBuilder report=new StringBuilder();
 @Override public void onCreate(Bundle args){super.onCreate(args);verifySaved=args!=null && "true".equals(args.getString("verifySaved"));start();}
 private void check(boolean condition,String name){assertions++;if(!condition)throw new AssertionError(name);report.append("PASS ").append(name).append('\n');}
 private void near(float a,float b,String name){check(Math.abs(a-b)<.002f,name+" ("+a+", "+b+")");}
 private void event(int action,float x,float y){long now=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(now,now,action,x,y,0);check(activity.root.controller.touch(e),"gesture consumed "+action);e.recycle();}
 private void settle(){waitForIdleSync();}
 @Override public void onStart(){
  Bundle result=new Bundle();
  try{
   Intent intent=new Intent(getTargetContext(),ControllerTestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   ActivityOptions options=ActivityOptions.makeBasic();options.setLaunchDisplayId(1);
   activity=(ControllerTestActivity)startActivitySync(intent,options.toBundle());settle();
   runOnMainSync(()->{check(activity.getDisplay().getDisplayId()==1,"test attached to real lower display");check(activity.root.drawer.isShown(),"test drawer visible");if(verifySaved){near(activity.root.controller.getWidthRatio(),.74f,"width survives process restart");near(activity.root.controller.getHeightRatio(),.68f,"height survives process restart");}activity.root.controller.reset();});settle();
   runOnMainSync(()->{near(activity.root.controller.getWidthRatio(),.94f,"default width");near(activity.root.controller.getHeightRatio(),.88f,"default height");
    MotionEvent outside=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,activity.root.usable.right-10,activity.root.usable.bottom-10,0);check(!activity.root.controller.touch(outside),"ordinary app area not intercepted");outside.recycle();
    float x=activity.root.drawer.getLeft()+12,y=activity.root.drawer.getTop()+12;event(0,x,y);event(2,x-3000,y-3000);event(1,x-3000,y-3000);
    near(activity.root.controller.getWidthRatio(),1f,"oversized width clamped");near(activity.root.controller.getHeightRatio(),1f,"oversized height clamped");});settle();
   runOnMainSync(()->{check(activity.root.drawer.getRight()<=activity.root.usable.right,"maximum width within usable area");check(activity.root.drawer.getBottom()<=activity.root.usable.bottom,"maximum height within usable area");
    activity.root.controller.detach();activity.root.controller=new SecondaryDrawerResizeController(activity.root,activity.root.drawer);activity.root.controller.attach();near(activity.root.controller.getWidthRatio(),1f,"width restored by new source controller");near(activity.root.controller.getHeightRatio(),1f,"height restored by new source controller");
    activity.root.controller.layout(activity.root.usable);
    float x=activity.root.drawer.getLeft()+12,y=activity.root.drawer.getTop()+12;event(0,x,y);event(2,x+5000,y+5000);event(1,x+5000,y+5000);
    near(activity.root.controller.getWidthRatio(),.5f,"minimum width clamped");near(activity.root.controller.getHeightRatio(),.45f,"minimum height clamped");});settle();
   runOnMainSync(()->{float x=activity.root.drawer.getLeft()+12,y=activity.root.drawer.getTop()+12;event(0,x,y);event(2,x-200,y-200);event(3,x-200,y-200);
    near(activity.root.controller.getWidthRatio(),.5f,"cancel restores original width");near(activity.root.controller.getHeightRatio(),.45f,"cancel restores original height");
    activity.root.controller.resizeBy(10);near(activity.root.controller.getWidthRatio(),1f,"direct enlarge method clamp");activity.root.controller.resizeBy(-10);near(activity.root.controller.getHeightRatio(),.45f,"direct shrink method clamp");activity.root.controller.reset();near(activity.root.controller.getWidthRatio(),.94f,"reset default width");near(activity.root.controller.getHeightRatio(),.88f,"reset default height");activity.root.controller.resizeBy(-.2f);near(activity.root.controller.getWidthRatio(),.74f,"saved restart width fixture");near(activity.root.controller.getHeightRatio(),.68f,"saved restart height fixture");
   });settle();
   check(getTargetContext().getSharedPreferences("secondary_drawer_display_1",0).edit().commit(),"preference disk writes flushed before force-stop");
   result.putString("stream",report.toString()+"Assertions: "+assertions+"\nExact source controller only; not full Launcher3 integration.\n");
   runOnMainSync(()->activity.finish());finish(Activity.RESULT_OK,result);
  }catch(Throwable error){result.putString("stream",report.toString()+"FAIL "+error+"\n");if(activity!=null)runOnMainSync(()->activity.finish());finish(Activity.RESULT_CANCELED,result);}
 }
}
