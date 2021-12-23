package com.example.terdrawerlayout;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

public class ViewExtUtil {

    public static boolean isUnder(View view, MotionEvent ev) {
        return isUnder(view, ev.getRawX(), ev.getRawY());
    }

    public static boolean isUnder(View view, Float rawX, Float rawY) {
        if (view == null) return false;
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        return rawX >= xy[0] && rawX <= xy[0] + view.getWidth() && rawY >= xy[1] && rawY <= xy[1] + view.getHeight();
    }

    public @Nullable
    static View findScrollableTarget(View view, Float rawX, Float rawY) {
        if (!isUnder(view, rawX, rawY)) {
            return null;
        } else if (view instanceof NestedScrollView || view instanceof RecyclerView) {
            return view;
        } else if (!(view instanceof ViewGroup)) {
            return null;
        } else {
            View t = null;
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                t = findScrollableTarget(viewGroup.getChildAt(i), rawX, rawY);
                if (t != null) break;
            }
            return t;
        }
    }


    /**
     * 判断布局中是否包含某个控件
     *
     * @param view
     * @return
     */
    public static boolean isViewInGroup(View view, ViewGroup viewGroup) {
        if (viewGroup != null) {
            int childCount = viewGroup.getChildCount();
            if (childCount > 0) {
                for (int i = 0; i < childCount; i++) {
                    View child = viewGroup.getChildAt(i);
                    if (child instanceof ViewGroup) {
                        if (isViewInGroup(view, (ViewGroup) child)) {
                            return true;
                        }
                    } else if (child == view) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
