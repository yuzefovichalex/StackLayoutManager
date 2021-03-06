package com.alexyuzefovich.stacklayoutmanager;

import android.graphics.PointF;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class StackLayoutManager extends RecyclerView.LayoutManager implements RecyclerView.SmoothScroller.ScrollVectorProvider {

    private static final float DEFAULT_SCALE_FACTOR = 1;

    // value stored item position of first child view
    private int firstPosition = 0;

    private float currentScrollOffset = 0;

    private float scaleFactor = DEFAULT_SCALE_FACTOR;

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
        fill(recycler);
    }

    /**This method is used for fill initial pack of view or when one (or more) child view
     * calls its measure() and layout() methods on resizing event**/
    private void fill(RecyclerView.Recycler recycler) {
        final int itemCount = getItemCount();
        if (itemCount > 0) {
            final int viewHeight = getHeight() - getPaddingBottom();
            // if child count == 0 -> initial set or data update after removing all views (ex. scrollToPosition)
            if (getChildCount() == 0) {
                currentScrollOffset = 0;
            } else { // children have updates (ex. resizing)
                // detach all views before adding and re-measure
                detachAndScrapAttachedViews(recycler);
            }

            int viewTop = 0;
            final int startPosition = itemCount > 1 ? firstPosition : 0;
            final int positionOffset = startPosition + 2 < itemCount
                    ? 2
                    : itemCount - 1 - startPosition;
            final int endPosition = itemCount > 1
                    ? (startPosition + positionOffset)
                    : 0;
            // add child views taking into account the currentScrollOffset
            for (int i = startPosition; i <= endPosition; i++) {
                final View view = addViewFromRecycler(recycler, i, false);
                measureMatchParentChild(view);
                final int viewRight = getWidth();
                final int viewBottom = viewTop + viewHeight;
                layoutDecorated(view, 0, viewTop, viewRight, viewBottom);
                int offset = i != startPosition + 1 ? (int) currentScrollOffset : 0;
                viewTop = getDecoratedBottom(view) - offset;
            }
        }
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scrollBy(dy, recycler);
    }

    /**This method is used for apply dy offset for child views and add/remove needed children
     * on scroll**/
    private int scrollBy(int dy, RecyclerView.Recycler recycler) {
        final int itemCount = getItemCount();
        if (itemCount < 2) {
            return 0;
        }

        // view fixed in the top
        final View firstView = getChildAt(0);
        if (firstView == null) {
            return 0;
        }
        // count offset
        int delta = -dy;
        // view scrolled and overlapped first view
        final View secondView = getChildAt(1);
        // if secondView == null -> firstView is last item
        if (secondView != null) {
            // view scrolled after second view
            View thirdView = getChildAt(2);
            int secondViewItemPosition = getPosition(secondView);
            if (thirdView == null && secondViewItemPosition != itemCount - 1) {
                // we take third view from recycler where position is next after second view item
                thirdView = addViewFromRecycler(recycler, secondViewItemPosition + 1, false);
                measureMatchParentChild(thirdView);
                final int viewTop = getDecoratedBottom(secondView);
                final int viewRight = getWidth();
                final int viewBottom = viewTop + getDecoratedMeasuredHeight(thirdView);
                layoutDecorated(thirdView, 0, viewTop, viewRight, viewBottom);
            }

            final int secondViewTop = secondView.getTop() + delta;

            // value in range [-viewHeight; viewHeight]
            currentScrollOffset = (currentScrollOffset + dy) % firstView.getHeight();

            float scaleValue = 1f;
            // scroll down
            if (dy > 0) {
                // if this case is true than second view scrolls up to max top position
                // that's why we need to scroll it to 0 (max top position), because top value can be less than 0
                if (secondViewTop <= 0) {
                    delta = -secondView.getTop();
                    currentScrollOffset = 0;
                }
                scaleValue = 1 - Math.abs(currentScrollOffset * scaleFactor) / firstView.getHeight();
            } else if (dy < 0) { // scroll up
                if (secondViewTop > getDecoratedBottom(firstView)) {
                    // for scroll to max bottom position without overscroll
                    delta = getDecoratedBottom(firstView) - getDecoratedTop(secondView);
                    // if first view is not first in item list we add view under current scrolling view (it will be new first view)
                    if (firstPosition != 0) {
                        final View view = insertFirstView(recycler);
                        currentScrollOffset = view.getMeasuredHeight() - delta;
                    } else {
                        currentScrollOffset = 0;
                    }
                    // remove redundant not visible fourth item
                    if (getChildCount() > 3) {
                        removeAndRecycleViewAt(3, recycler);
                    }
                } else {
                    scaleValue = 1 - Math.abs(currentScrollOffset * scaleFactor) / firstView.getHeight();
                }
            }

            offsetChildren(delta);

            // scale-on-scroll
            firstView.setScaleX(scaleValue);
            firstView.setScaleY(scaleValue);

            // if second view completely overlaps first view (first view is not visible now) we remove first view
            if (secondView.getTop() == 0) {
                removeAndRecycleViewAt(0, recycler);
                firstPosition++;
            }
        } else {
            if (firstPosition == itemCount - 1 && dy < 0) {
                final View view = insertFirstView(recycler);
                currentScrollOffset = view.getMeasuredHeight() - delta;
                offsetChildren(delta);
            } else {
                delta = 0;
            }
        }
        return delta != 0 ? dy : 0;
    }

    @NonNull
    private View insertFirstView(RecyclerView.Recycler recycler) {
        final View view = addViewFromRecycler(recycler, firstPosition - 1, true);
        firstPosition--;
        measureMatchParentChild(view);
        final int viewRight = getWidth();
        final int viewBottom = getDecoratedMeasuredHeight(view);
        layoutDecorated(view, 0, 0, viewRight, viewBottom);
        view.setScaleX(scaleFactor);
        view.setScaleY(scaleFactor);
        return view;
    }

    private void offsetChildren(int delta) {
        // scroll all view except first
        for (int i = 1; i < getChildCount(); i++) {
            final View view = getChildAt(i);
            if (view != null) {
                view.offsetTopAndBottom(delta);
            }
        }
    }

    @NonNull
    private View addViewFromRecycler(RecyclerView.Recycler recycler, int position, boolean addToStart) {
        final View view = recycler.getViewForPosition(position);
        if (addToStart) {
            addView(view, 0);
        } else {
            addView(view);
        }
        return view;
    }

    private void measureMatchParentChild(View view) {
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.EXACTLY);
        measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec) {
        Rect decorRect = new Rect();
        calculateItemDecorationsForChild(child, decorRect);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        widthSpec = updateSpecWithExtra(widthSpec, lp.leftMargin + decorRect.left,
                lp.rightMargin + decorRect.right);
        heightSpec = updateSpecWithExtra(heightSpec, lp.topMargin + decorRect.top,
                lp.bottomMargin + decorRect.bottom + getPaddingBottom());
        child.measure(widthSpec, heightSpec);
    }

    private int updateSpecWithExtra(int spec, int startInset, int endInset) {
        if (startInset == 0 && endInset == 0) {
            return spec;
        }
        final int mode = View.MeasureSpec.getMode(spec);
        if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.EXACTLY) {
            return View.MeasureSpec.makeMeasureSpec(
                    View.MeasureSpec.getSize(spec) - startInset - endInset, mode);
        }
        return spec;
    }

    @Override
    public void scrollToPosition(int position) {
        requestLayout(position);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        final LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
                View firstView = getChildAt(0);
                if (firstView != null) {
                    int firstChildPosition = getPosition(firstView);
                    int targetViewPosition = getPosition(targetView);
                    if (targetViewPosition == firstChildPosition) {
                        View secondView = getChildAt(1);
                        if (secondView != null) {
                            int dy = -(getHeight() - secondView.getTop());
                            int time = calculateTimeForDeceleration(Math.abs(dy));
                            action.update(0, dy, time, mDecelerateInterpolator);
                        }
                    } else {
                        super.onTargetFound(targetView, state, action);
                    }
                }
            }
        };
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    @Nullable
    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        final View firstChild = getChildAt(0);
        if (getChildCount() != 0 && firstChild != null) {
            final int firstChildPos = getPosition(firstChild);
            final float direction = targetPosition < firstChildPos ? -1f : 1f;
            return new PointF(0, direction);
        }
        return null;
    }

    @Override
    public void onItemsChanged(@NonNull RecyclerView recyclerView) {
        super.onItemsChanged(recyclerView);
        requestLayout(0);
    }

    private void requestLayout(int firstPosition) {
        this.firstPosition = firstPosition;
        removeAllViews();
        requestLayout();
    }
}
