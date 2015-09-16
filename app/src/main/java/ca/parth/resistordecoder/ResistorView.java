package ca.parth.resistordecoder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class ResistorView extends View {

    public static final int TOLERANCE_GOLD = 0xffcb9931;
    public static final int RESISTANCE_BLACK = 0xff000000;
    public static final int RESISTANCE_RED = 0xffff0000;
    public static final int RESISTANCE_BROWN = 0xff663236;
    public static final int RESISTANCE_ORANGE = 0xffff6500;
    public static final int RESISTANCE_YELLOW = 0xfffdfe02;
    public static final int RESISTANCE_GREEN = 0xff32ce2f;
    public static final int RESISTANCE_BLUE = 0xff6368fa;
    public static final int RESISTANCE_VIOLET = 0xffcc65fe;
    public static final int RESISTANCE_GREY = 0xff979191;
    public static final int RESISTANCE_WHITE = 0xffefefef;

    public static int[] COLOUR_TABLE = {RESISTANCE_BLACK, RESISTANCE_BROWN, RESISTANCE_RED, RESISTANCE_ORANGE, RESISTANCE_YELLOW, RESISTANCE_GREEN, RESISTANCE_BLUE, RESISTANCE_VIOLET, RESISTANCE_GREY, RESISTANCE_WHITE};

    private Path resistorBody = new Path();
    private Paint bodyPaint = new Paint();
    private Paint strokePaint = new Paint();
    private Paint bandPaint = new Paint();
    private Paint textPaint = new Paint();
    private int resistance = 407;
    private int bandWidth;
    private int toleranceOffset;

    private int[] bandOffsets = new int[3];
    private int[] bandColours = new int[3];
    private String resistanceLabel = "0 Ω";

    public ResistorView(Context context) {
        this(context, null, 0);
    }

    public ResistorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ResistorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        bodyPaint.setColor(0xffffcc66);
        bodyPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStrokeWidth(16);
        strokePaint.setAntiAlias(true);
        strokePaint.setColor(0xff000000);
        strokePaint.setStyle(Paint.Style.STROKE);
        bandPaint.setStyle(Paint.Style.FILL);

        textPaint.setTextSize(50);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setColor(0xffdfdfdf);

        setResistance(resistance);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = (int) ((MeasureSpec.getSize(widthMeasureSpec) / 16.0f) * 7);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resistorBody.reset();

        // Thank you view-source:https://upload.wikimedia.org/wikipedia/commons/6/6e/4-Band_Resistor.svg !!!
        resistorBody.moveTo(0, 25);
        resistorBody.cubicTo(0.0f, 0.9f, 25.5f, 0.9f, 35.5f, 0.9f);

        resistorBody.cubicTo(45f, 0f, 50f, 5f, 75f, 5f);
        resistorBody.cubicTo(100f, 5.f, 105f, 0f, 115f, 0f);
        resistorBody.cubicTo(125f, 0.f, 150f, 0f, 150f, 25f);
        resistorBody.cubicTo(150f, 50.f, 125f, 50f, 115f, 50f);
        resistorBody.cubicTo(105f, 50.f, 100f, 45f, 75f, 45f);
        resistorBody.cubicTo(50.f, 45.f, 45f, 50f, 35f, 50f);
        resistorBody.cubicTo(25.f, 50.f, 0f, 51f, 0f, 25f);
        resistorBody.close();
        Matrix transformMatrix = new Matrix();
        transformMatrix.preTranslate(15, 10);
        transformMatrix.postScale(w / 180.f, h / 71.f);
        resistorBody.transform(transformMatrix);

        bandWidth = w / 18;

        bandOffsets[0] = w / 5;
        int bandStride = w / 8;
        bandOffsets[1] = bandOffsets[0] + bandStride;
        bandOffsets[2] = bandOffsets[1] + bandStride;

        toleranceOffset = w / 4 * 3;
    }

    public void setResistance(int resistance) {
        if (resistance <= 0 || resistance > 99_000_000) {
            this.resistance = 0;
            bandColours[0] = RESISTANCE_BLACK;
            bandColours[1] = RESISTANCE_BLACK;
            bandColours[2] = RESISTANCE_BLACK;
        } else {
            int powersOfTen = (int) Math.floor(Math.log10(resistance)) - 1;
            if (powersOfTen < 0) {
                powersOfTen = 0;
            }
            this.resistance = resistance;
            String resistanceValue = resistance + "";
            bandColours[0] = COLOUR_TABLE[resistanceValue.charAt(0) - '0'];
            bandColours[1] = COLOUR_TABLE[resistanceValue.charAt(0) - '0'];
            bandColours[2] = COLOUR_TABLE[powersOfTen];
        }


        if (resistance >= 1e3 && resistance < 1e6) {
            resistanceLabel = String.valueOf(resistance / 1e3) + " kΩ";
        } else if (resistance >= 1e6) {
            resistanceLabel = String.valueOf(resistance / 1e6) + " MΩ";
        } else {
            resistanceLabel = String.valueOf(resistance) + " Ω";
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(resistorBody, bodyPaint);
        canvas.drawPath(resistorBody, strokePaint);

        canvas.clipPath(resistorBody);

        for (int i = 0; i < bandOffsets.length; i++) {
            drawBand(canvas, bandOffsets[i], bandColours[i]);
        }
        drawBand(canvas, toleranceOffset, TOLERANCE_GOLD);

        textPaint.setColor(0x99888888);
        float shadowOffset = getResources().getDisplayMetrics().scaledDensity * 2;
        int xPos = (int) ((getMeasuredWidth() - textPaint.measureText(resistanceLabel)) / 2);
        int yPos = (int) ((getMeasuredHeight()) / 2 + textPaint.getFontMetrics().bottom);

        canvas.drawText(resistanceLabel, xPos + shadowOffset, yPos + shadowOffset, textPaint);
        textPaint.setColor(0xffdfdfdf);
        canvas.drawText(resistanceLabel, xPos, yPos, textPaint);
    }

    private void drawBand(Canvas canvas, int offset, int bandColour) {

        bandPaint.setColor(bandColour);
        canvas.drawRect(offset, 0, offset + bandWidth, getMeasuredHeight(), bandPaint);
    }
}
