package com.humanify.expertconnect.sample.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A TextView that is checkable, making the text bold when it's checked.
 */
public class BoldActivatedTextView extends TextView {
    public BoldActivatedTextView(Context context) {
        super(context);
    }

    public BoldActivatedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BoldActivatedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setActivated(boolean activated) {
        super.setActivated(activated);
        if (activated) {
            setTypeface(null, Typeface.BOLD);
        } else {
            setTypeface(null, Typeface.NORMAL);
        }
    }
}
