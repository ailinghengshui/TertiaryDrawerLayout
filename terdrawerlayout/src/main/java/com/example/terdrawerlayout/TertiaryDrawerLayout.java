package com.example.terdrawerlayout;

import static androidx.drawerlayout.widget.DrawerLayout.STATE_DRAGGING;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.customview.widget.ViewDragHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Set;

/**
 * 三级抽屉，提供悬停功能，通过VerticalDrawerLayout修改
 */
public class TertiaryDrawerLayout extends RelativeLayout {

    private static final String TAG = TertiaryDrawerLayout.class.getSimpleName();

    private View drawerView;
    private ViewDragHelper dragHelper;
    private ViewState drawerState = ViewState.CLOSE;

    private boolean touchOnDrawerTop = false;//如果点击了抽屉
    private boolean canDrag = true;//可拖动
    private boolean enableDrag = true;//可拖动
    private boolean closeOnEdge = false; //是否可边缘侧滑关闭
    private boolean canFollowingScroll = true;
    private boolean isFollowingScroll = false;
    private final Set<OnStatusChangeListener> statusChangeListenerSet = new HashSet<>();
    private OnVisibilityChangeListener visibilityChangeListener;

    private int touchSlop;//认为滚动的最小距离
    private int lastY;
    private int lastY2;
    private int lastX2;
    private int fillVisHeight;
    private int hoverVisHeight;

    public TertiaryDrawerLayout(Context context) {
        this(context, null);
    }

    public TertiaryDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public TertiaryDrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        ViewConfiguration vc = ViewConfiguration.get(getContext());
        touchSlop = vc.getScaledTouchSlop();
        dragHelper = ViewDragHelper.create(this, 2.0f, new ViewDragHelperCallBack());
        //只能从viewGroup的下边缘拖动
        dragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_BOTTOM);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TertiaryDrawerLayout, defStyleAttr, 0);
            canFollowingScroll = a.getBoolean(R.styleable.TertiaryDrawerLayout_following_scroll, true);
            fillVisHeight = a.getDimensionPixelSize(R.styleable.TertiaryDrawerLayout_fill_vis_height, 0);
            hoverVisHeight = a.getDimensionPixelSize(R.styleable.TertiaryDrawerLayout_hover_vis_height, 0);
            int drawStateInt = a.getInt(R.styleable.TertiaryDrawerLayout_default_state, 0);
            if (drawStateInt == 0) {
                drawerState = ViewState.CLOSE;
            } else if (drawStateInt == 1) {
                drawerState = ViewState.HOVER;
            } else {
                drawerState = ViewState.FILL;
            }
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        drawerView = getChildAt(0);
        post(new Runnable() {
            @Override
            public void run() {
                resetDrawerViewLayout();
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (dragHelper.getViewDragState() == STATE_DRAGGING) {
            onReLayoutDrawView(getLeft(), drawerView.getTop(), getRight(), drawerView.getMeasuredHeight() + drawerView.getTop());
            return;
        }
        onReLayoutDrawView(getLeft(), drawerState.getTop(this), getRight(), drawerView.getMeasuredHeight() + drawerState.getTop(this));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = ViewExtUtil.findScrollableTarget(drawerView, ev.getRawX(), ev.getRawY());
        if (view != null) {
            if (view instanceof NestedScrollView) {
                canDrag = view.getScrollY() == 0;
            }
            if (view instanceof RecyclerView) {
                canDrag = !view.canScrollVertically(-1);
            }
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            touchOnDrawerTop = isUnderDrawerTop(ev.getRawX(), ev.getRawY());
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isFollowingScroll) {
            return super.onInterceptTouchEvent(ev);
        }
        boolean interceptTap = true;
        if (touchOnDrawerTop) {
            interceptTap = false;
            switch (ev.getAction()) {
                case MotionEvent.ACTION_MOVE: {
                    final float yOff = Math.abs(lastY - ev.getRawY());
                    if (yOff > touchSlop) {
                        // 只有手指滑动距离大于阈值时，才会开始拦截
                        // Start scrolling!
                        interceptTap = true;
                    }
                    break;
                }
                case MotionEvent.ACTION_DOWN:
                    lastY = (int) ev.getRawY();
                    break;
            }
        }
        if (closeOnEdge) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_MOVE: {
                    final float xOff = Math.abs(lastX2 - ev.getRawX());
                    final float yOff = Math.abs(lastY2 - ev.getRawY());
                    if (isTouchStartOnEdge((float) lastX2) && xOff > touchSlop / 2F && yOff < xOff) {
                        changeDrawerState(ViewState.CLOSE);
                        return true;
                    }
                    break;
                }
                case MotionEvent.ACTION_DOWN:
                    lastX2 = (int) ev.getRawX();
                    lastY2 = (int) ev.getRawY();
                    break;
            }
        }
        boolean interceptForDrag = dragHelper.shouldInterceptTouchEvent(ev);
        return interceptTap && interceptForDrag;
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        dragHelper.processTouchEvent(ev);
        if (touchOnDrawerTop) {
            return true;
        }
        return canCapture(ViewExtUtil.isUnder(drawerView, ev));
    }

    @Override
    public void computeScroll() {
        if (dragHelper.continueSettling(true)) {
            invalidate();
        }
    }


    public void changeDrawerState(ViewState viewState) {
        if (isFollowingScroll)
            return;//如果正在跟随，不能直接打开
        this.drawerState = viewState;
        dragHelper.smoothSlideViewTo(drawerView, drawerView.getLeft(), viewState.getTop(this));
        invalidate();
        for (OnStatusChangeListener changeListener : statusChangeListenerSet) {
            if (changeListener != null) {
                changeListener.onStateChange(viewState);
            }
        }
    }


    public void addOnStatusChangeListener(OnStatusChangeListener onStatusChangeListener) {
        if (statusChangeListenerSet.contains(onStatusChangeListener))
            return;
        statusChangeListenerSet.add(onStatusChangeListener);
    }

    @Override
    public void setVisibility(int visibility) {
        int oldVisibility = getVisibility();
        super.setVisibility(visibility);
        if (visibilityChangeListener != null && visibility != oldVisibility) {
            visibilityChangeListener.onVisibilityChange(visibility);
        }
    }

    public interface OnVisibilityChangeListener {
        void onVisibilityChange(int visibility);
    }

    public void setVisibilityChangeListener(OnVisibilityChangeListener visibilityChangeListener) {
        this.visibilityChangeListener = visibilityChangeListener;
    }

    /**
     * 可以拖动的条件
     * 外部设置可拖动，当前的view，不在跟随滑动
     *
     * @return
     */
    private boolean canCapture(boolean isHandleDrawer) {
        if (!enableDrag)
            return false;
        //点击了上层，不用管canDrag
        if (touchOnDrawerTop) {
            return isHandleDrawer;
        }
        boolean canCapture = canDrag && isHandleDrawer;
        Log.i(TAG, "canCapture：" + canCapture + "_canDrag：" + canDrag + "isHandleDrawer：" + isHandleDrawer);
        return canCapture;
    }

    private class ViewDragHelperCallBack extends ViewDragHelper.Callback {

        private long startTime;
        private boolean isWantOpen = true;

        //确定当前子view是否可拖动
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            startTime = System.currentTimeMillis();
            return canCapture(child == drawerView) && !isFollowingScroll;
        }

        /**
         * ACTION_DOWN或ACTION_POINTER_DOWN事件发生时如果触摸到监听的边缘会调用此方法。
         * edgeFlags的取值为EDGE_LEFT、EDGE_TOP、EDGE_RIGHT、EDGE_BOTTOM的组合
         *
         * @param edgeFlags
         * @param pointerId
         */
        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            if (edgeFlags == ViewDragHelper.EDGE_BOTTOM) {
                dragHelper.captureChildView(drawerView, pointerId);
            }
        }

        /**
         * 分别在 clampViewPositionVertical 和clampViewPositionHorizontal 方法中对它的可滑动边界进行控制
         *
         * @param child
         * @param top   距离顶部的距离
         * @param dy    变化量
         * @return
         */
        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            isWantOpen = dy < 0; //向上滑
            return Math.min(Math.max(top, 0), ViewState.CLOSE.getTop(TertiaryDrawerLayout.this));
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            //            int screenHeight = getMeasuredHeight();
            //            int offset = screenHeight - top;
            postOnDraggingState(top);
            //            LogUtils.e("onViewPositionChanged--offset-->" + top);
        }

        /**
         * 当View停止拖拽的时候调用
         *
         * @param releasedChild
         * @param xvel          x轴的速率
         * @param yvel          y轴的速率
         */
        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            // 快速拖动逻辑
            long endTime = System.currentTimeMillis();
            // 拖动后定位逻辑
            int childTop = releasedChild.getTop();
            setClosestStateIfBetween(ViewState.FILL, ViewState.HOVER, childTop);
            setClosestStateIfBetween(ViewState.HOVER, ViewState.CLOSE, childTop);
        }

        private void setClosestStateIfBetween(ViewState beginState, ViewState endState, int curTop) {
            int beginTop = beginState.getTop(TertiaryDrawerLayout.this), endTop = endState.getTop(TertiaryDrawerLayout.this);
            if (curTop >= beginTop && curTop <= endTop)
                if (iContentHeightProxy != null) {
                    if (beginState == ViewState.FILL) {
                        if (isWantOpen) {
                            changeDrawerState(curTop < endTop - iContentHeightProxy.getTopExpandHeight() ? beginState : endState);
                        } else {
                            changeDrawerState(curTop < beginTop + iContentHeightProxy.getTopExpandHeight() ? beginState : endState);
                        }
                    } else if (endState == ViewState.CLOSE) {
                        if (isWantOpen) {
                            changeDrawerState(curTop < endTop - iContentHeightProxy.getBottomCollapseHeight() ? beginState : endState);
                        } else {
                            changeDrawerState(curTop < beginTop + iContentHeightProxy.getBottomCollapseHeight() ? beginState : endState);
                        }
                    }
                } else {
                    changeDrawerState(curTop < (beginTop + endTop) / 2 ? beginState : endState);
                }
        }

        /**
         * 限制子 View 纵向拖拽范围。
         * 如果返回 0，则不能进行纵向拖动， 我们要实现拖拽，返回值 > 0 即可。
         */
        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            if (child == drawerView) {
                return 1;
            } else {
                return 0;
            }
        }
    }


    /**
     * 是否可以拖拽
     *
     * @param canDrag
     */
    @Deprecated
    public void setCanDrag(boolean canDrag) {
        //        this.canDrag = canDrag;
    }

    public void setEnableDrag(boolean enableDrag) {
        this.enableDrag = enableDrag;
    }

    public void setCloseOnEdge(boolean closeOnEdge) {
        this.closeOnEdge = closeOnEdge;
    }

    public boolean isCloseOnEdge() {
        return closeOnEdge;
    }

    public boolean isCanDrag() {
        return canDrag;
    }

    /**
     * 可以跟随滚动
     *
     * @param canFollowingScroll
     */
    public void setCanFollowingScroll(boolean canFollowingScroll) {
        this.canFollowingScroll = canFollowingScroll;
    }

    public boolean isCanFollowingScroll() {
        return canFollowingScroll;
    }

    public ViewState getDrawState() {
        return drawerState;
    }

    /**
     * 是否跟随滚动
     *
     * @param isFollowingScroll
     */
    public void setIsFollowingScroll(boolean isFollowingScroll) {
        this.isFollowingScroll = isFollowingScroll;
    }

    public boolean isFollowingScroll() {
        return isFollowingScroll;
    }

    /**
     * 跟随的时候，抽屉一定是在底部的
     * 如果不在底部,却调用了scroll，drawerView.getTop() != maxY成立
     * 这种情况有可能是在跟随的时候，调用了弹起，需要将滚动初始化
     *
     * @param toY
     */
    public void scroll(int toY) {
        int maxY = drawerView.getHeight() - ViewState.FILL.getTop(TertiaryDrawerLayout.this);
        if (toY > maxY) {
            toY = maxY;
        }
        if (drawerView.getTop() != maxY) {
            toY = 0;
        }
        this.scrollToY = toY;
        scrollTo(0, scrollToY);
    }

    private int scrollToY;

    public int getDrawerScrollY() {
        return scrollToY;
    }


    public void resetDrawerViewLayout() {//关闭
        changeDrawerState(drawerState);
    }

    private void onReLayoutDrawView(int left, int top, int right, int bottom) {
        scroll(0);
        drawerView.layout(left, top, right, bottom);
    }

    private void postOnDraggingState(int state) {
        for (OnStatusChangeListener changeListener : statusChangeListenerSet) {
            if (changeListener != null) {
                changeListener.onDragging(state);
            }
        }
    }

    private void postMoving(int top) {
        for (OnStatusChangeListener changeListener : statusChangeListenerSet) {
            if (changeListener != null) {
                changeListener.onMoving(top);
            }
        }
    }


    public boolean isUnderDrawerTop(Float rawX, Float rawY) {
        if (drawerView == null)
            return false;
        int[] xy = new int[2];
        drawerView.getLocationOnScreen(xy);
        boolean isUnderDrawerTop = rawX >= xy[0] && rawX <= xy[0] + drawerView.getWidth() && rawY >= xy[1] && rawY <= xy[1] + drawerState.getHeight(TertiaryDrawerLayout.this);
        return isUnderDrawerTop;
    }

    public boolean isUnderDrawerBlank(Float rawX, Float rawY) {
        if (drawerView == null)
            return false;
        int[] xy = new int[2];
        drawerView.getLocationOnScreen(xy);
        boolean isUnderDrawerTop = rawX >= xy[0] && rawX <= xy[0] + drawerView.getWidth() && rawY >= ViewState.FILL.getTop(this) && rawY <= xy[1];
        return isUnderDrawerTop;
    }

    private final int edgeWidth = dp2px(50F); //边缘触发宽度

    public int dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * 是否从边缘开始
     *
     * @param rawX 开始时的 X 坐标
     * @return true or false
     */
    private boolean isTouchStartOnEdge(Float rawX) {
        if (drawerView == null)
            return false;
        int[] xy = new int[2];
        drawerView.getLocationOnScreen(xy);
        return (rawX <= edgeWidth && rawX >= xy[0]) || (rawX >= (xy[0] + drawerView.getWidth() - edgeWidth) && rawX <= (xy[0] + drawerView.getWidth()));
    }

    /**
     * 高度动态配置
     */
    public interface IContentHeightProxy {
        int getHoverHeight();

        int getFillHeight();

        int getCloseHeight();

        int getTopExpandHeight();

        int getBottomCollapseHeight();
    }

    public interface OnStatusChangeListener {

        default void onMoving() {
        }

        default void onMoving(int top) {
            onMoving();
        }

        default void onDragging(int offset) {
        }

        default void onStateChange(ViewState viewState) {
        }

        default void onScrollOnBlank() {
        }
    }


    private IContentHeightProxy iContentHeightProxy;

    public void setContentHeightProxy(IContentHeightProxy iContentHeightProxy) {
        this.iContentHeightProxy = iContentHeightProxy;
    }

    public int getTopStateFill() {
        if (iContentHeightProxy != null) {
            return iContentHeightProxy.getFillHeight();
        } else {
            return fillVisHeight;
        }
    }

    public int getTopStateHover() {
        if (iContentHeightProxy != null) {
            return iContentHeightProxy.getHoverHeight();
        } else {
            return hoverVisHeight;
        }
    }

    public int getTopStateClose() {
        if (iContentHeightProxy != null) {
            return iContentHeightProxy.getCloseHeight();
        } else {
            return 0;
        }
    }
}