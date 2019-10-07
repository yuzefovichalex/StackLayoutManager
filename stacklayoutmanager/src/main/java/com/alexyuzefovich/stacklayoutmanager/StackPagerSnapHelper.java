package com.alexyuzefovich.stacklayoutmanager;

import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

/**
 * Helper class provides pager behavior like as PagerSnapHelper,
 * but for using with StackLayoutManager. StackPagerSnapHelper can't guarantee correct behavior with
 * another implementations of RecyclerView.LayoutManager, so using everything except StackLayoutManager
 * will throw IllegalStateException
 * **/
public class StackPagerSnapHelper extends SnapHelper {

    private static final float MILLISECONDS_PER_INCH = 100f;
    private static final int MAX_SCROLLING_TIME = 100;

    private RecyclerView recyclerView;

    @NonNull
    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        int[] out = new int[2];
        View firstView = layoutManager.getChildAt(0);
        if (firstView != null) {
            int firstViewPosition = layoutManager.getPosition(firstView);
            int targetViewPosition = layoutManager.getPosition(targetView);
            if (targetViewPosition == firstViewPosition) {
                View secondView = layoutManager.getChildAt(1);
                if (secondView != null) {
                    out[1] = -(layoutManager.getHeight() - secondView.getTop() - ((StackLayoutManager) layoutManager).getBottomOffset());
                }
            } else {
                out[1] = targetView.getTop();
            }
        }
        return out;
    }

    @Nullable
    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        View firstView = layoutManager.getChildAt(0);
        View secondView = layoutManager.getChildAt(1);
        if (firstView != null && secondView != null) {
            if (secondView.getTop() > layoutManager.getHeight() / 2) {
                return firstView;
            } else {
                return secondView;
            }
        }
        return null;
    }

    @Override
    public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
        boolean forwardDirection = velocityY > 0;
        final View firstView = layoutManager.getChildAt(0);
        final View secondView = layoutManager.getChildAt(1);
        if (firstView != null && secondView != null) {
            View targetView = forwardDirection ? secondView : firstView;
            return layoutManager.getPosition(targetView);
        }
        return RecyclerView.NO_POSITION;
    }

    @Nullable
    @Override
    protected RecyclerView.SmoothScroller createScroller(final RecyclerView.LayoutManager layoutManager) {
        return new LinearSmoothScroller(recyclerView.getContext()) {

            @Override
            protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
                final int[] out = calculateDistanceToFinalSnap(layoutManager, targetView);
                final int dy = out[1];

                final int time = calculateTimeForDeceleration(Math.abs(dy));
                if (time > 0) {
                    action.update(0, dy, time, mDecelerateInterpolator);
                }
            }

            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
            }

            @Override
            protected int calculateTimeForScrolling(int dx) {
                return Math.min(MAX_SCROLLING_TIME, super.calculateTimeForScrolling(dx));
            }
        };
    }

    @Override
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) throws IllegalStateException {
        super.attachToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        if (recyclerView != null && !(recyclerView.getLayoutManager() instanceof StackLayoutManager)) {
            throw new IllegalStateException("StackPagerSnapHelper can only be used with StackLayoutManager.");
        }
    }
}
