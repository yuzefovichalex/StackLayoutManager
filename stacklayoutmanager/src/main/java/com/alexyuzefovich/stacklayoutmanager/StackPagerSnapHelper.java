package com.alexyuzefovich.stacklayoutmanager;

import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

public class StackPagerSnapHelper extends SnapHelper {

    private RecyclerView recyclerView;

    @Nullable
    @Override
    public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View targetView) {
        int[] out = new int[2];
        View firstView = layoutManager.getChildAt(0);
        int firstViewPosition = layoutManager.getPosition(firstView);
        int targetViewPosition = layoutManager.getPosition(targetView);
        if (targetViewPosition == firstViewPosition) {
            out[1] = -(layoutManager.getHeight() - layoutManager.getChildAt(1).getTop() - 50);
        } else {
            out[1] = targetView.getTop();
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
        View targetView = forwardDirection ? secondView : firstView;
        return layoutManager.getPosition(targetView);
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
                return 100f / displayMetrics.densityDpi;
            }

            @Override
            protected int calculateTimeForScrolling(int dx) {
                return Math.min(100, super.calculateTimeForScrolling(dx));
            }
        };
    }

    @Override
    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) throws IllegalStateException {
        super.attachToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }
}
