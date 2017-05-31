package com.tenghan.swipeflip;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.SyncStateContract;
import android.util.Log;

import com.eschao.android.widget.pageflip.PageFlip;
import com.eschao.android.widget.pageflip.PageFlipException;

import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by hanteng on 2017-05-30.
 */

public class PageFlipView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private final static String TAG = "PageFlipView";

    int mPageNo;
    int mDuration;
    Handler mHandler;
    PageFlip mPageFlip;
    PageRender mPageRender;
    ReentrantLock mDrawLock;

    public PageFlipView(Context context)
    {
        super(context);
        newHandler();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        mDuration = pref.getInt(Constants.PREF_DURATION, 1000);
        int pixelsOfMesh = pref.getInt(Constants.PREF_MESH_PIXELS, 10);
        boolean isAuto = pref.getBoolean(Constants.PREF_PAGE_MODE, true);

        //create pageflip
        mPageFlip = new PageFlip(context);
        mPageFlip.setSemiPerimeterRatio(0.8f)
                .setShadowWidthOfFoldEdges(5, 60, 0.3f)
                .setShadowWidthOfFoldBase(5, 80, 0.4f)
                .setPixelsOfMesh(pixelsOfMesh)
                .enableAutoPage(isAuto);
        setEGLContextClientVersion(2);

        // init others
        mPageNo = 1;
        mDrawLock = new ReentrantLock();
        mPageRender = new SinglePageRender(context, mPageFlip,
                mHandler, mPageNo);
        // configure render
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


    }

    public boolean isAutoPageEnabled(){
        return mPageFlip.isAutoPageEnabled();
    }

    public void enableAutoPage(boolean enable) {
        if (mPageFlip.enableAutoPage(enable)) {
            try {
                mDrawLock.lock();
                if (mPageFlip.getSecondPage() != null &&
                        mPageRender instanceof SinglePageRender) {
                    mPageRender = new DoublePageRender(getContext(),
                            mPageFlip,
                            mHandler,
                            mPageNo);
                    mPageRender.onSurfaceChanged(mPageFlip.getSurfaceWidth(),
                            mPageFlip.getSurfaceHeight());
                }
                else if (mPageFlip.getSecondPage() == null &&
                        mPageRender instanceof DoublePageRender) {
                    mPageRender = new SinglePageRender(getContext(),
                            mPageFlip,
                            mHandler,
                            mPageNo);
                    mPageRender.onSurfaceChanged(mPageFlip.getSurfaceWidth(),
                            mPageFlip.getSurfaceHeight());
                }
                requestRender();
            }
            finally {
                mDrawLock.unlock();
            }
        }
    }

    public int getAnimateDuration(){
        return mDuration;
    }

    public void setAnimateDuration(int duration)
    {
        mDuration = duration;
    }

    public int getPixelsOfMesh()
    {
        return mPageFlip.getPixelsOfMesh();
    }

    public void onFingerDown(float x, float y)
    {

    }

    public void onFingerMove(float x, float y)
    {

    }

    public void onFingerUp(float x, float y)
    {

    }

    @Override
    public void onDrawFrame(GL10 gl)
    {
        try {
            mDrawLock.lock();
            if (mPageRender != null)
            {
                mPageRender.onDrawFrame();
            }
        }finally {
            mDrawLock.unlock();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        try{
            mPageFlip.onSurfaceChanged(width, height);

            int pageNo = mPageRender.getPageNo();
            if(mPageFlip.getSecondPage() != null && width > height)
            {
                if (!(mPageRender instanceof DoublePageRender)) {
                    mPageRender.release();
                    mPageRender = new DoublePageRender(getContext(),
                            mPageFlip,
                            mHandler,
                            pageNo);
                }
            }else if(!(mPageRender instanceof SinglePageRender)){
                mPageRender.release();
                mPageRender = new SinglePageRender(getContext(),
                        mPageFlip,
                        mHandler,
                        pageNo);
            }

            mPageRender.onSurfaceChanged(width, height);
        }catch (PageFlipException e)
        {
            Log.e(TAG, "Failed to run PageFlipRender:onSurfaceChanged");
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        try {
            mPageFlip.onSurfaceCreated();
        }
        catch (PageFlipException e) {
            Log.e(TAG, "Failed to run PageFlipRender:onSurfaceCreated");
        }
    }

    private void newHandler()
    {
        mHandler = new Handler()
        {
          public void handleMessage(Message msg)
          {
              switch (msg.what){
                  case PageRender.MSG_ENDED_DRAWING_FRAME:
                      try{
                          mDrawLock.lock();
                          if(mPageRender != null && mPageRender.onEndedDrawing(msg.arg1)){
                              requestRender();
                          }
                      }
                      finally {
                          mDrawLock.unlock();
                      }
                      break;
                  default:
                      break;
              }
          }
        };
    }

}