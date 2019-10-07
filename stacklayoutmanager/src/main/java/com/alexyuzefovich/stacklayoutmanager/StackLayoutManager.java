package com.alexyuzefovich.stacklayoutmanager;

import android.graphics.PointF;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class StackLayoutManager extends RecyclerView.LayoutManager implements RecyclerView.SmoothScroller.ScrollVectorProvider {

    private static final int DEFAULT_BOTTOM_OFFSET = 0;
    private static final float DEFAULT_SCALE_FACTOR = 1;

    // value stored item position of first child view
    private int firstPosition = 0;

    private float currentScrollOffset = 0;

    private int bottomOffset = DEFAULT_BOTTOM_OFFSET;
    private float scaleFactor = DEFAULT_SCALE_FACTOR;

    public int getBottomOffset() {
        return bottomOffset;
    }

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
        fill(recycler);
    }

    /**This method is used for fill initial pack of view or when one (or more) child view
     * calls its measure() and layout() methods on resizing event**/
    private void fill(RecyclerView.Recycler recycler) {
        int itemCount = getItemCount();
        if (itemCount > 0) {
            int viewHeight = getHeight() - bottomOffset;
            boolean isFirstIsLastItem = firstPosition == itemCount - 1;
            // if child count == 0 -> initial set or data update after removing all views (ex. scrollToPosition)
            if (getChildCount() == 0) {
                currentScrollOffset = isFirstIsLastItem ? viewHeight : 0;
            } else { // children have updates (ex. resizing)
                // remove all view before adding and re-measure
                removeAndRecycleAllViews(recycler);
            }

            int viewTop = 0;
            int startPosition = itemCount > 1
                    ? (isFirstIsLastItem ? itemCount - 2 : firstPosition)
                    : 0;
            int endPosition = itemCount > 1
                    ? (startPosition + 1)
                    : 0;
            // add child views taking into account the currentScrollOffset
            for (int i = startPosition; i <= endPosition; i++) {
                View view = addViewFromRecycler(recycler, i, false);
                int viewRight = getWidth();
                int viewBottom = viewTop + viewHeight;
                layoutDecorated(view, 0, viewTop, viewRight, viewBottom);
                if (!isFirstIsLastItem) {
                    viewTop = getDecoratedBottom(view) - (int) currentScrollOffset;
                }
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
                    int firstViewPosition = getPosition(firstView);
                    if (firstViewPosition != 0) {
                        View view = addViewFromRecycler(recycler, getPosition(firstView) - 1, true);
                        firstPosition = firstViewPosition - 1;
                        int viewRight = getWidth();
                        int viewBottom = getHeight() - bottomOffset;
                        layoutDecorated(view, 0, 0, viewRight, viewBottom);
                        view.setScaleX(scaleFactor);
                        view.setScaleY(scaleFactor);
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

                // scale-on-scroll
                firstView.setScaleX(scaleValue);
                firstView.setScaleY(scaleValue);

                // if second view completely overlaps first view (first view is not visible now) we remove first view
                if (secondView.getTop() == 0 && getPosition(firstView) != getItemCount() - 2) {
                    removeAndRecycleViewAt(0, recycler);
                    firstPosition = getPosition(firstView) + 1;
                }
                return dy;
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
        int widthSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.EXACTLY);
        measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
        return view;
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec) {
        Rect decorRect = new Rect();
        calculateItemDecorationsForChild(child, decorRect);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        widthSpec = updateSpecWithExtra(widthSpec, lp.leftMargin + decorRect.left,
                lp.rightMargin + decorRect.right);
        heightSpec = updateSpecWithExtra(heightSpec, lp.topMargin + decorRect.top,
                lp.bottomMargin + decorRect.bottom + bottomOffset);
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
        LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }
        };
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        View firstChild = getChildAt(0);
        if (getChildCount() != 0 && firstChild != null) {
            int firstChildPos = getPosition(firstChild);
            int direction = targetPosition < firstChildPos ? -1 : 1;
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
