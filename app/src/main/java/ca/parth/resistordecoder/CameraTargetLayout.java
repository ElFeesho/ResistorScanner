package ca.parth.resistordecoder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class CameraTargetLayout extends FrameLayout {

    private final Paint paint = new Paint();
    private double phase = 0.0;
    public CameraTargetLayout(Context context) {
        this(context, null, 0);
    }

    public CameraTargetLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraTargetLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        paint.setColor(Color.argb(128 + ((int) (Math.sin(phase) * 64)), 255, 0, 0));
        phase += 0.05;
        canvas.drawRect(getMeasuredWidth() / 2 - 50, getMeasuredHeight() / 2, getMeasuredWidth() / 2 + 50, getMeasuredHeight() / 2 + 30, paint);
        postInvalidate();
    }

}
