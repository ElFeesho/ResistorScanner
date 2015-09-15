package ca.parth.resistordecoder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class CameraTargetLayout extends FrameLayout {

    private final Paint paint = new Paint();

    public CameraTargetLayout(Context context) {
        this(context, null, 0);
    }

    public CameraTargetLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraTargetLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint.setColor(0x99ff0000);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        canvas.drawRect(getMeasuredWidth() / 2 - 50, getMeasuredHeight() / 2, getMeasuredWidth() / 2 + 50, getMeasuredHeight() / 2 + 30, paint);
    }

}
