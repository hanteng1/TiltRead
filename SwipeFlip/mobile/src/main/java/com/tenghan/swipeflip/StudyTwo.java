package com.tenghan.swipeflip;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;

import com.eschao.android.widget.pageflip.GLPoint;
import com.eschao.android.widget.pageflip.PageFlipState;
import com.eschao.android.widget.pageflip.modify.PageModify;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by hanteng on 2017-09-08.
 */

public class StudyTwo extends PageFlipModifyAbstract{

    private final static String TAG = "Study Two";
    private final static int pageSize = 2;

    //conditions
    public ArrayList<Integer> tasks;
    public ArrayList<int[]> conditions;
    private ArrayList<Integer> tempTasks;
    public int taskCount;  // 6
    public int currentAttempt = 0;
    //this should be currentTask, just to keep the same with study one
    public int currentCondition = -1;
    public int currentTask = -1;
    private Random rand;
    private int repeat = 5;

    public PointF cursor = new PointF();


    //1 - start, 2 - move, 3 - end
    public int trialState;
    public long trialDuration;
    public long trialStartTime = 0;
    public long trialEndTime = 0;
    public int isCorrect;  // 1 - correct, 0 - incorrect

    //these willn not be used
    public int numVistedCells;
    public int numOvershoot;


    public StudyTwo(Context context)
    {
        super(context, pageSize);

        tasks = new ArrayList<Integer>();
        tempTasks = new ArrayList<Integer>();
        rand = new Random();

        conditions = new ArrayList<int[]>();

        for(int itrt = 0; itrt < repeat; itrt++)
        {
            //discrete
            tempTasks.add(1);
            tempTasks.add(2);
            tempTasks.add(3);
            //continuous
            tempTasks.add(4);
            tempTasks.add(5);
            tempTasks.add(6);
        }

        taskCount = tempTasks.size();

        //randomize the order
        ArrayList<Integer> order = new ArrayList<Integer>();
        for(int itro = 0; itro < taskCount; itro++)
        {
            order.add(itro);
        }

        for(int itrc = 0; itrc < taskCount; itrc++)
        {
            int picked = rand.nextInt(order.size());
            int temp = order.get(picked);
            tasks.add(tempTasks.get(temp));

            for(int itrr = 0; itrr < order.size(); itrr++)
            {
                if(order.get(itrr) == temp)
                {
                    order.remove(itrr);
                    continue;
                }
            }
        }

    }

    public int obtainNextTask()
    {
        currentTask++;
        currentAttempt = 1;

        trialState = 0;
        trialDuration = 0;
        trialStartTime = 0;
        trialEndTime = 0;
        isCorrect = 0;

        if(currentTask == tasks.size())
        {
            //save

            currentTask = 0;
        }

        return tasks.get(currentTask);
    }

    //fake function
    public int[] obtainNextCondition()
    {
        return new int[]{};
    }

    public int[] obtainCurrentCondition()
    {
        return new int[]{};
    }



    public boolean onFingerMove(float touchX, float touchY)
    {
        touchX = mViewRect.toOpenGLX(touchX);
        touchY = mViewRect.toOpenGLY(touchY);

        float dy = (touchY - mStartTouchP.y);
        float dx = (touchX - mStartTouchP.x);

        // begin to flip
        if (mFlipState == PageFlipState.BEGIN_FLIP
//             && (Math.abs(dx) > mViewRect.width * 0.05f)
                ) {

            //for (int itrp = 0; itrp < PAGE_SIZE; itrp++) {
            PageModify page = mPages[currentPageLock];
            GLPoint originP = page.originP;
            GLPoint diagonalP = page.diagonalP;

            // set OriginP and DiagonalP points
            //page.setOriginAndDiagonalPoints(dy);
            page.setOriginAndDiagonalPoints(dx, dy, mStartTouchP.x, mStartTouchP.y);

            //clear the variables
            MainActivity.getSharedInstance().mGestureService.reset();
            //starting from one page
            singlePageMode = true;
            //set origin
            MainActivity.getSharedInstance().mGestureService.setOrigin(new float[]{touchX, touchY});
            MainActivity.getSharedInstance().mGestureService.handleData(new float[]{touchX, touchY});

            //reload the first page texture
            MainActivity.getSharedInstance().mStudyView.mPageRender.ReloadFirstPageTexture();

            //save data, a trial starts
            long timestamp = System.currentTimeMillis();
            trialState = 1;
            trialStartTime = timestamp;
//            DataStorage.AddSample(currentCondition, currentAttempt,
//                    MainActivity.getSharedInstance().mStudyView.mPageRender.mCorner,
//                    MainActivity.getSharedInstance().mStudyView.mPageRender.mAngleNum,
//                    MainActivity.getSharedInstance().mStudyView.mPageRender.mDistanceNum,
//                    MainActivity.getSharedInstance().mStudyView.mPageRender.mClose,
//                    MainActivity.getSharedInstance().mStudyView.mPageRender.mAngleTarget,
//                    MainActivity.getSharedInstance().mStudyView.mPageRender.mDistanceTargert,
//                    -1, -1, trialState,
//                    timestamp);

            // compute max degree between X axis and line from TouchP to OriginP
            // and max degree between X axis and line from TouchP to
            // (OriginP.x, DiagonalP.Y)
            float y2o = Math.abs(mStartTouchP.y - originP.y);
            float y2d = Math.abs(mStartTouchP.y - diagonalP.y);
            page.mMaxT2OAngleTan = page.computeTanOfCurlAngle(y2o);
            page.mMaxT2DAngleTan = page.computeTanOfCurlAngle(y2d);

            // moving at the top and bottom screen have different tan value of
            // angle
            if ((originP.y < 0 && page.right > 0) ||
                    (originP.y > 0 && page.right <= 0)) {
                page.mMaxT2OAngleTan = -page.mMaxT2OAngleTan;
            } else {
                page.mMaxT2DAngleTan = -page.mMaxT2DAngleTan;
            }

            if (0 == FIRST_PAGE)  //first page
            {
                // determine if it is moving backward or forward

                if(Math.abs(dx) > Math.abs(dy))
                {
                    if (dx > 0 &&
                            mListener != null &&
                            mListener.canFlipBackward()) {
                        mStartTouchP.x = originP.x;
                        dx = (touchX - mStartTouchP.x);
                        mFlipState = PageFlipState.BACKWARD_FLIP;
                        //Log.d(TAG, "back FLIP");
                    } else if (mListener != null &&
                            mListener.canFlipForward() &&
                            (dx < 0 && originP.x > 0 || dx > 0 && originP.x < 0)) {
                        mFlipState = PageFlipState.FORWARD_FLIP;
                        //Log.d(TAG, "forward FLIP");
                    }
                }else
                {
                    if(mListener != null)
                    {
                        mFlipState = PageFlipState.UPWARD_FLIP;
                    }
                }


            }

            //} // end of for loop
        }

        if (mFlipState == PageFlipState.FORWARD_FLIP ||
                mFlipState == PageFlipState.BACKWARD_FLIP ||
                mFlipState == PageFlipState.UPWARD_FLIP ||
                mFlipState == PageFlipState.RESTORE_FLIP) {


            MainActivity.getSharedInstance().mGestureService.handleData(new float[]{touchX, touchY});

            //temporary solution
            if(Math.abs(dy) <= 0.1f)
                dy = dy > 0 ? 0.11f : -0.11f;
            if(Math.abs(dx) <= 0.1f)
                dx = dx > 0 ? 0.11f : -0.11f;

            mIsVertical = Math.abs(dy) <= 0.1f;
            if(mIsVertical == false)
            {
                //Log.d(TAG, "abs dx " + Math.abs(dx));
                mIsHorizontal = Math.abs(dx) <= 0.1f;
            }
            //skip the calculation when the flip is either horizontal or vertical
            if(mIsHorizontal || mIsVertical) {
                Log.d(TAG, "skipping a frame ..............");
                return false;
            }

            dx = (touchX - mStartTouchP.x) * (float)Math.pow(TOUCH_DIFF_COEFFICIENT, FIRST_PAGE);  //forwards or backwards
            dy = (touchY - mStartTouchP.y) * (float)Math.pow(TOUCH_DIFF_COEFFICIENT, FIRST_PAGE);  //upwards or downwards

            if (PageFlipState.FORWARD_FLIP == mFlipState || PageFlipState.BACKWARD_FLIP == mFlipState
                    || PageFlipState.UPWARD_FLIP == mFlipState) {
                dx *= 1.2f;
                dy *= 1.2f;
            }
            else {
                dx *= 1.1f;
                dy *= 1.1f;
            }

            PageModify page = mPages[currentPageLock];
            GLPoint originP = page.originP;
            GLPoint diagonalP = page.diagonalP;

            // moving direction is changed:
            // 1. invert max curling angle
            // 2. invert Y of original point and diagonal point

            if(mFlipState == PageFlipState.FORWARD_FLIP || mFlipState == PageFlipState.BACKWARD_FLIP)
            {
                if ((dy < 0 && originP.y < 0) || (dy > 0 && originP.y > 0)
                        ) {
                    float t = page.mMaxT2DAngleTan;
                    page.mMaxT2DAngleTan = page.mMaxT2OAngleTan;
                    page.mMaxT2OAngleTan = t;
                    page.invertYOfOriginPoint();
                }
            }else if(mFlipState == PageFlipState.UPWARD_FLIP)
            {
                if((dx < 0 && originP.x < 0) || (dx > 0 && originP.x > 0))
                {
                    //invert the x of original point and diagonal point
                    page.invertXOfOriginPoint();
                }
            }

            // set touchP(x, y) and middleP(x, y)
            if(0 == FIRST_PAGE)
            {
                mLastTouchP.set(dx + originP.x, dy + originP.y);  //used to store the value temporarily
            }
            mTouchP.set(dx + originP.x, dy + originP.y);

            //Log.d(TAG, "origin point" + originP.x);
            //Log.d(TAG, "moving touch point " + mTouchP.x);

            page.mFakeTouchP.set(mTouchP.x, mTouchP.y);
            page.mMiddleP.x = (mTouchP.x + originP.x) * 0.5f;
            page.mMiddleP.y = (mTouchP.y + originP.y) * 0.5f;


            //detect whether the back page has reached front page
            float travelDis = calDistance(dx, dy);
            if(travelDis > maxTravelDis)
            {
                maxTravelDis = travelDis;
                //Log.d(TAG, "max travel " + maxTravelDis);

                singlePageMode = false;
            }

            mTouchP.set(mLastTouchP.x, mLastTouchP.y);
            mLastTouchP.set(touchX, touchY);

            // continue to compute points to drawing flip
            computeVertexesAndBuildPage();

            return true;

        }


        return false;
    }

    private float calDistance(float disx, float disy)
    {
        return (float)Math.sqrt(disx * disx + disy * disy);
    }


    public void computeVertexesAndBuildPage()
    {
        mPages[currentPageLock].computeKeyVertexesWhenSlope();
        mPages[currentPageLock].computeVertexesWhenSlope();
    }

}
