package com.alexyuzefovich.stacklayoutmanager;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class StackLayoutManager extends RecyclerView.LayoutManager {

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT
        );
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);
        int position = 0;
        boolean fillDown = true;
        int height = getHeight();
        int viewTop = 0;
        int itemCount = getItemCount();
        int viewHeight = getHeight() - 100;

        while (fillDown && position < itemCount){
            View view = recycler.getViewForPosition(position);
            addView(view);
            measureChildWithMargins(view, 0, 0);
            layoutDecorated(view, 0, viewTop, getWidth(), viewTop + viewHeight);
            viewTop = getDecoratedBottom(view);
            fillDown = viewTop <= height;
            position++;
        }
    }

    @Override
    public boolean canScrollVertically() {
        return getItemCount() > 1;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getItemCount() > 1) {
            View firstView = getChildAt(0);
            View secondView = getChildAt(1);
            if (firstView != null && secondView != null) {
                View thirdView = getChildAt(2);
                int secondViewItemPosition = getPosition(secondView);
                if (thirdView == null && secondViewItemPosition != getItemCount() - 1) {
                    thirdView = recycler.getViewForPosition(secondViewItemPosition + 1);
                    addView(thirdView);
                    measureChildWithMargins(thirdView, 0, 0);
                    layoutDecorated(thirdView, 0, getDecoratedBottom(secondView), getWidth(), getDecoratedBottom(secondView) + getHeight() - 100);
                }
                int top = secondView.getTop() - dy;
                int delta = -dy;
                if (dy > 0) {
                    if (top < 0) {
                        if (getPosition(firstView) != getItemCount() - 2) {
                            removeAndRecycleViewAt(0, recycler);
                        }
                        delta = 0;
                    }
                } else if (dy < 0) {
                    if (top > getDecoratedBottom(firstView)) {
                        if (getPosition(firstView) != 0) {
                            View view = recycler.getViewForPosition(getPosition(firstView) - 1);
                            addView(view, 0);
                            measureChildWithMargins(view, 0, 0);
                            layoutDecorated(view, 0, 0, getWidth(), getHeight() - 100);
                        } else { // for blocking scroll of first item
                            delta = 0;
                        }
                        if (getChildCount() > 3) {
                            removeAndRecycleViewAt(3, recycler);
                        }
                    }
                }
                if (delta != 0) {
                    for (int i = 1; i < getChildCount(); i++) {
                        View view = getChildAt(i);
                        if (view != null) {
                            layoutDecorated(view, 0, view.getTop() + delta, getWidth(), view.getBottom() + delta);
                        }
                    }
                    return dy;
                }
            }
        }
        return 0;
    }

}
