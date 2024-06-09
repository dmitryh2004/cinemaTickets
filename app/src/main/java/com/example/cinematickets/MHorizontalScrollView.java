package com.example.cinematickets;

import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.view.MotionEvent;
import android.content.Context;
import android.util.AttributeSet;

public class MHorizontalScrollView extends HorizontalScrollView
{
    public ScrollView sv;
    public MHorizontalScrollView(Context context)
    {
        super(context);
    }

    public MHorizontalScrollView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MHorizontalScrollView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override public boolean onTouchEvent(MotionEvent event)
    {
        boolean ret = super.onTouchEvent(event);
        ret = ret | sv.onTouchEvent(event);
        return ret;
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event)
    {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        boolean ret = super.onInterceptTouchEvent(event);
        ret = ret | sv.onInterceptTouchEvent(event);
        return ret;
    }
}