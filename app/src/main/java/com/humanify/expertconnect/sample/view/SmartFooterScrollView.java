package com.humanify.expertconnect.sample.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

import com.humanify.expertconnect.sample.R;

public class SmartFooterScrollView extends ScrollView {
    public static final int CONTENT_GRAVITY_TOP = 0;
    public static final int CONTENT_GRAVITY_CENTER = 1;
    public static final int CONTENT_GRAVITY_BOTTOM = 2;

    private int mContentId;
    private int mFooterId;
    private int mContentGravity;

    private View mContent;
    private View mFooter;

    public SmartFooterScrollView(Context context) {
        this(context, null);
    }

    public SmartFooterScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartFooterScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SmartFooterScrollView);
            mContentId = a.getResourceId(R.styleable.SmartFooterScrollView_content, 0);
            mFooterId = a.getResourceId(R.styleable.SmartFooterScrollView_footer, 0);
            mContentGravity = a.getInt(R.styleable.SmartFooterScrollView_content_gravity, CONTENT_GRAVITY_TOP);
            a.recycle();
        }
    }

    public void setContentView(View view) {
        mContent = view;
        requestLayout();
    }

    public void setFooterView(View view) {
        mFooter = view;
        requestLayout();
    }

    public void setContentGravity(int gravity) {
        mContentGravity = gravity;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mContent = findViewById(mContentId);
        mFooter = findViewById(mFooterId);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mContent.getHeight() + mFooter.getHeight() < getHeight()) {
            int newTop = getHeight() - mFooter.getHeight();

            View main = getChildAt(0);
            main.layout(
                    main.getLeft(),
                    main.getTop(),
                    main.getRight(),
                    getBottom()
            );

            int contentTop = 0;
            if (mContentGravity == CONTENT_GRAVITY_TOP) {
                contentTop = mContent.getTop();
            } else if (mContentGravity == CONTENT_GRAVITY_CENTER) {
                contentTop = (newTop - mContent.getHeight()) / 2;
            } else if (mContentGravity == CONTENT_GRAVITY_BOTTOM) {
                contentTop = newTop - mContent.getHeight();
            }

            mContent.layout(
                    mContent.getLeft(),
                    contentTop,
                    mContent.getRight(),
                    contentTop + mContent.getHeight()
            );

            mFooter.layout(
                    mFooter.getLeft(),
                    newTop,
                    mFooter.getRight(),
                    newTop + mFooter.getHeight()
            );
        }
    }
}