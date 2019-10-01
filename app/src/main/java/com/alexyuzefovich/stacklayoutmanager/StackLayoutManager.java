package com.alexyuzefovich.stacklayoutmanager;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StackLayoutManager extends RecyclerView.LayoutManager {

    private static final int DEFAULT_BOTTOM_OFFSET = 0;
    private static final float DEFAULT_SCALE_FACTOR = 1;

    private float currentScrollOffset = 0;

    private int bottomOffset = DEFAULT_BOTTOM_OFFSET;
    private float scaleFactor = DEFAULT_SCALE_FACTOR;

    public void setBottomOffset(int bottomOffset) {
        this.bottomOffset = bottomOffset;
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor =
                scaleFactor >= 0f && scaleFactor <= 1f
                        ? scaleFactor
                        : DEFAULT_SCALE_FACTOR;
    }

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
        int viewHeight = getHeight() - bottomOffset;

        while (fillDown && position < itemCount){
            View view = addViewFromRecycler(recycler, position, false);
            layoutDecorated(view, 0, viewTop, getWidth(), viewTop + viewHeight);
            viewTop = getDecoratedBottom(view);
            fillDown = viewTop <= height;
            position++;
        }
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        // if two items or more we can scroll it
        if (getItemCount() > 1) {
            // view fixed in the top
            View firstView = getChildAt(0);
            // view scrolled and overlapped first view
            View secondView = getChildAt(1);
            if (firstView != null && secondView != null) {
                // view scrolled after second view
                View thirdView = getChildAt(2);
                int secondViewItemPosition = getPosition(secondView);
                if (thirdView == null && secondViewItemPosition != getItemCount() - 1) {
                    // we take third view from recycler where position is next after second view item
                    thirdView = addViewFromRecycler(recycler, secondViewItemPosition + 1, false);
                    int viewTop = getDecoratedBottom(secondView);
                    int viewRight = getWidth();
                    int viewBottom = viewTop + getHeight() - bottomOffset;
                    layoutDecorated(thirdView, 0, viewTop, viewRight, viewBottom);
                }

                // count offset
                int delta = -dy;
                int secondViewTop = secondView.getTop() + delta;

                // value in range [-viewHeight; viewHeight]
                currentScrollOffset = (currentScrollOffset + dy) % firstView.getHeight();

                float scaleValue = 1f;
                // scroll down
                if (dy > 0) {
                    // if this case is true than second view scrolls up to max top position
                    // that's why we need to scroll it to 0 (max top position), because top value can be less than 0
                    if (secondViewTop < 0) {
                        delta = -secondView.getTop();
                        // this check is needed for correct animation of penult item
                        // every time when second view reaches top of recycler we reset to zero current offset
                        if (getPosition(firstView) != getItemCount() - 2) {
                            currentScrollOffset = 0;
                        } else {
                            // but if this is the penult item - current offset is max possible value (viewHeight),
                            // because in case of penult item we don't delete first view from recycler
                            currentScrollOffset = firstView.getHeight();
                        }
                    }
                    scaleValue = 1 - Math.abs(currentScrollOffset * scaleFactor) / firstView.getHeight();
                } else if (dy < 0) { // scroll up
                    if (secondViewTop > getDecoratedBottom(firstView)) {
                        // if first view is not first in item list we add view under current scrolling view (it will be new first view)
                        if (getPosition(firstView) != 0) {
                            View view = addViewFromRecycler(recycler, getPosition(firstView) - 1, true);
                            int viewRight = getWidth();
                            int viewBottom = getHeight() - bottomOffset;
                            layoutDecorated(view, 0, 0, viewRight, viewBottom);
                            view.setScaleX(0f);
                            view.setScaleY(0f);
                            currentScrollOffset = firstView.getHeight();
                        } else {
                            currentScrollOffset = 0;
                        }
                        // for scroll to max bottom position without overscroll
                        delta = getDecoratedBottom(firstView) - getDecoratedTop(secondView);
                        // remove redundant not visible fourth item
                        if (getChildCount() > 3) {
                            removeAndRecycleViewAt(3, recycler);
                        }
                    } else {
                        scaleValue = 1 - Math.abs(currentScrollOffset * scaleFactor) / firstView.getHeight();
                    }
                }

                // if we can scroll
                if (delta != 0) {
                    // scroll all view except first
                    for (int i = 1; i < getChildCount(); i++) {
                        View view = getChildAt(i);
                        if (view != null) {
                            int viewTop = view.getTop() + delta;
                            int viewRight = getWidth();
                            int viewBottom = view.getBottom() + delta;
                            layoutDecorated(view, 0, viewTop, viewRight, viewBottom);
                        }
                    }
                    // if second view completely overlaps first view (first view is not visible now) we remove first view
                    if (secondView.getTop() == 0 && getPosition(firstView) != getItemCount() - 2) {
                        removeAndRecycleViewAt(0, recycler);
                    }

                    // scale-on-scroll
                    firstView.setScaleX(scaleValue);
                    firstView.setScaleY(scaleValue);
                    return dy;
                }
            }
        }
        return 0;
    }

    @NonNull
    private View addViewFromRecycler(RecyclerView.Recycler recycler, int position, boolean addToStart) {
        View view = recycler.getViewForPosition(position);
        if (addToStart) {
            addView(view, 0);
        } else {
            addView(view);
        }
        measureChildWithMargins(view, 0, 0);
        return view;
    }


}
