package com.uilib.videoeditor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.uilib.R;

public class RangeSeekBar extends View {
    private static final String TAG = RangeSeekBar.class.getSimpleName();
    public static final int MIN = 0, MAX = 1, NONE = -1;
    private double minThumbRate = 0d;//左滑块位置占总长度的比例值，范围从0-1
    private double maxThumbRate = 1d;//右滑块位置占总长度的比例值，范围从0-1
    private double cursorMinRate = 0d;// 指针在滑块之间最小位置比例值，范围从0-1
    private double cursorMaxRate = 1d;// 指针在滑块之间最大位置比例值，范围从0-1

    private long maxTrimMs;
    private long minTrimMs = 3000;
    private double thumbWidth = 64.0;
    private int pressedThumb = NONE;
    private int[] state = new int[]{android.R.attr.state_pressed};
    private Matrix matrix = null;

    private Drawable leftThumb, rightThumb;
    private Bitmap thumbImageLeft;
    private Bitmap thumbImageRight;
    private Bitmap pressedThumbLeft;
    private Bitmap pressedThumbRight;
    private Paint thumbPaint;
    private Paint rectPaint;
    private Paint maskPaint;
    private boolean isMin;


    public RangeSeekBar(Context context) {
        super(context);
        initView(context, null);
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs){
        setFocusable(true);
        setFocusableInTouchMode(true);
        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setStyle(Paint.Style.FILL);
        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setStyle(Paint.Style.FILL);
        maskPaint.setColor(Color.parseColor("#66000000"));
        if(attrs == null)
            return;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
        minTrimMs = ta.getInteger(R.styleable.RangeSeekBar_minTrimMs, 3000);
        thumbWidth = ta.getDimension(R.styleable.RangeSeekBar_thumbWidth, 64.0f);
        leftThumb = ta.getDrawable(R.styleable.RangeSeekBar_leftThumb);
        rightThumb = ta.getDrawable(R.styleable.RangeSeekBar_rightThumb);
        setBorderColor(ta.getColor(R.styleable.RangeSeekBar_borderColor, Color.parseColor("#00befa")));
        ta.recycle();
    }

    public void setMaxTrimMs(long duration){
        if(duration < 0)
            throw new IllegalArgumentException("maxTrimMs is less than zero!");
        maxTrimMs = duration;
        if(maxTrimMs < minTrimMs)
            minTrimMs = maxTrimMs;
    }

    public void setMinTrimMs(long minTrim){
        minTrimMs = minTrim;
    }

    public void setThumbWidth(double thumbWidth){
        this.thumbWidth = thumbWidth;
    }

    public int getThumbWidth(){
        return (int) thumbWidth;
    }

    public void setLeftThumb(Drawable drawable){
        leftThumb = drawable;
    }

    public void setRightThumb(Drawable drawable){
        rightThumb = drawable;
    }

    public void setBorderColor(int color){
        rectPaint.setColor(color);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        init();
    }

    private void init() {
        //等比例缩放图片
        if(leftThumb != null) {
            thumbImageLeft = ((BitmapDrawable) leftThumb.getCurrent()).getBitmap();
        }
        if(rightThumb != null){
            thumbImageRight = ((BitmapDrawable)rightThumb.getCurrent()).getBitmap();
        }
        int height = thumbImageLeft != null ? thumbImageLeft.getHeight() : (thumbImageRight != null ? thumbImageRight.getHeight() : getMeasuredHeight());
        int width = thumbImageLeft != null ? thumbImageLeft.getWidth() : (thumbImageRight != null ? thumbImageRight.getWidth() : (int)thumbWidth);
        int newHeight = getMeasuredHeight();
        float scaleHeight = newHeight * 1.0f / height;
        float scaleWidth = (float) (thumbWidth / width);
        matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        if(thumbImageLeft != null)
            thumbImageLeft = Bitmap.createBitmap(thumbImageLeft, 0, 0, thumbImageLeft.getWidth(), thumbImageLeft.getHeight(), matrix, true);
        if(thumbImageRight != null)
            thumbImageRight = Bitmap.createBitmap(thumbImageRight, 0, 0, thumbImageRight.getWidth(), thumbImageRight.getHeight(), matrix, true);
    }


    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] drawState = super.onCreateDrawableState(extraSpace + 1);
        if(isPressed()){
            mergeDrawableStates(drawState, state);
        }
        return drawState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if(matrix == null)
            return;

        if(pressedThumb != NONE){
            if(leftThumb != null) {
                leftThumb.setState(getDrawableState());
                pressedThumbLeft = ((BitmapDrawable) leftThumb.getCurrent()).getBitmap();
                pressedThumbLeft = Bitmap.createBitmap(pressedThumbLeft, 0, 0, pressedThumbLeft.getWidth(), pressedThumbLeft.getHeight(), matrix, true);
                leftThumb = null;
            }
            if(rightThumb != null) {
                rightThumb.setState(getDrawableState());
                pressedThumbRight = ((BitmapDrawable) rightThumb.getCurrent()).getBitmap();
                pressedThumbRight = Bitmap.createBitmap(pressedThumbRight, 0, 0, pressedThumbRight.getWidth(), pressedThumbRight.getHeight(), matrix, true);
                rightThumb = null;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 300;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height = 120;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float rangeL = getThumbPosX(minThumbRate);
        float rangeR = getThumbPosX(maxThumbRate);
        try {
            //画上下边框
            canvas.drawRect(rangeL, 0, rangeR, dip2px(2), rectPaint);
            canvas.drawRect(rangeL, getHeight() - dip2px(2), rangeR, getHeight(), rectPaint);
            //画左右滑块
            drawThumb(rangeL, canvas, true);
            drawThumb(rangeR, canvas, false);
            //画左右蒙版
            drawMask(rangeL, canvas, true);
            drawMask(rangeR, canvas , false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float getThumbPosX(double thumbRate) {
        return (float) (getPaddingLeft() + thumbRate * (getWidth() - getPaddingLeft() - getPaddingRight()));
    }

    private void drawThumb(float posX, Canvas canvas, boolean isLeft) {
        canvas.drawBitmap(isLeft ?
                        (pressedThumb == MIN ? pressedThumbLeft : thumbImageLeft)
                        : (pressedThumb == MAX ? pressedThumbRight : thumbImageRight),
                (float) (posX - (isLeft ? 0 : thumbWidth)), 0, thumbPaint);
    }

    private void drawMask(float range, Canvas canvas, boolean isLeft){
        if(isLeft && range > 0){
            canvas.drawRect(new Rect(0, 0, (int)range, getHeight()), maskPaint);
        }else if(!isLeft && range < getWidth()){
            canvas.drawRect(new Rect((int)range, 0, getWidth(), getHeight()), maskPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) {
            return super.onTouchEvent(event);
        }

        if (!isEnabled())
            return false;
        switch (event.getAction()/* & MotionEvent.ACTION_MASK*/) {
            case MotionEvent.ACTION_DOWN:
                // 判断touch到的是最大值thumb还是最小值thumb
                if ((pressedThumb = evalPressedThumb(event.getX())) == NONE)
                    return false;
                setPressed(true);// 设置该控件被按下了
                trackTouchEvent(event, pressedThumb);
                attemptClaimDrag();
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this,
                            RateToTimeValue(cursorMinRate),
                            RateToTimeValue(cursorMaxRate),
                            MotionEvent.ACTION_DOWN, isMin, pressedThumb);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                trackTouchEvent(event, pressedThumb);
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this,
                            RateToTimeValue(cursorMinRate),
                            RateToTimeValue(cursorMaxRate),
                            MotionEvent.ACTION_MOVE, isMin, pressedThumb);
                }
                break;
            case MotionEvent.ACTION_UP:
                trackTouchEvent(event, pressedThumb);
                refreshDrawableState();
                invalidate();
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this,
                            RateToTimeValue(cursorMinRate),
                            RateToTimeValue(cursorMaxRate),
                            MotionEvent.ACTION_UP, isMin, pressedThumb);
                }
                pressedThumb = NONE;// 手指抬起，则置被touch到的thumb为空
                break;
            case MotionEvent.ACTION_CANCEL:
                Log.e(TAG, "cancel");
                setPressed(false);
                invalidate(); // see above explanation
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 计算位于哪个Thumb内
     *
     * @param touchX touchX
     * @return 被touch的是空还是最大值或最小值
     */
    private int evalPressedThumb(float touchX) {
        int result = NONE;
        boolean minThumbPressed = isInThumbRange(touchX, minThumbRate, 1.5);// 触摸点是否在最小值图片范围内
        boolean maxThumbPressed = isInThumbRange(touchX, maxThumbRate, 1.5);
        if (minThumbPressed && maxThumbPressed) {
            // 如果两个thumbs重叠在一起，无法判断拖动哪个，做以下处理
            // 触摸点在屏幕右侧，则判断为touch到了最小值thumb，反之判断为touch到了最大值thumb
            result = (touchX / getWidth() > 0.5f) ? MIN : MAX;
        } else if (minThumbPressed) {
            result = MIN;
        } else if (maxThumbPressed) {
            result = MAX;
        }
        return result;
    }

    private boolean isInThumbRange(float touchX, double normalizedThumbValue, double scale) {
        // 当前触摸点X坐标-滑块图片中心点在屏幕的X坐标之差<=滑块的宽度*scale
        // 即判断触摸点是否在以最小值图片中心为原点，半径为滑块的宽度*scale的圆内。
        return Math.abs(touchX - getThumbPosX(normalizedThumbValue)) <= thumbWidth * scale;
    }

    private void trackTouchEvent(MotionEvent event, int pressedThumb) {
        double posX = computeNewCoord(pressedThumb, event.getX());
        setCursorPosRate(pressedThumb, posX);
        setThumbPosRate(pressedThumb, CoordValueToRate(posX));
    }

    private double computeNewCoord(int thumb, double screenCoord){
        double minRange = 2 * thumbWidth + getValueLength() * timeValueToRate(minTrimMs);
        double maxRange = thumb == MIN ? Math.abs(minRange - getThumbPosX(maxThumbRate)) : Math.abs(minRange + getThumbPosX(minThumbRate));
        if(thumb == MIN){
            if(screenCoord > maxRange)
                screenCoord = maxRange;
            else if(screenCoord < thumbWidth * 2 /3)
                screenCoord = 0;
        }else if(thumb == MAX){
            if(screenCoord < maxRange)
                screenCoord = maxRange;
            else if(screenCoord > getWidth() - thumbWidth * 2 / 3)
                screenCoord = getWidth();
        }
        return screenCoord;
    }

    private void setCursorPosRate(int thumb, double posX){
        if(thumb == MIN){
            cursorMinRate = Math.min(1d, Math.max(0d, posX / getValueLength()));
        }else if(thumb == MAX){
            cursorMaxRate = Math.min(1d, Math.max(0d, (posX - 2 * thumbWidth) / getValueLength()));
        }
    }

    private void setThumbPosRate(int thumb, double thumbRate) {
        if (thumb == MIN) {
            minThumbRate = Math.max(0d, Math.min(thumbRate, maxThumbRate));
        } else if (thumb == MAX) {
            maxThumbRate = Math.min(1d, Math.max(thumbRate, minThumbRate));
        }
        invalidate();
    }

    /**
     * 根据毫秒值设置左右滑块的初始位置
     * @param thumb MIN 左滑块， MAX 右滑块
     * @param timeValue 毫秒值
     * */
    public void setThubmPos(int thumb, long timeValue) {
        setThumbPosRate(thumb, timeValueToRate(timeValue));
    }

    private double timeValueToRate(long timeValue) {
        return 1.0 * timeValue / maxTrimMs;
    }

    private long RateToTimeValue(double thumbRate) {
        return (long) (thumbRate * maxTrimMs);
    }

    private double CoordValueToRate(double pos){
        return 1.0 * pos / getWidth();
    }

    public double getValueLength() {
        return (getMeasuredWidth()*1.0 - 2.0 * thumbWidth);
    }

    /**
     * 试图告诉父view不要拦截子控件的drag
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    private int dip2px(int dip) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) ((float) dip * scale + 0.5F);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", minThumbRate);
        bundle.putDouble("MAX", maxThumbRate);
        bundle.putDouble("MIN_TIME", cursorMinRate);
        bundle.putDouble("MAX_TIME", cursorMaxRate);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        minThumbRate = bundle.getDouble("MIN");
        maxThumbRate = bundle.getDouble("MAX");
        cursorMinRate = bundle.getDouble("MIN_TIME");
        cursorMaxRate = bundle.getDouble("MAX_TIME");
    }

    private OnRangeSeekBarChangeListener listener;

    public interface OnRangeSeekBarChangeListener {
        void onRangeSeekBarValuesChanged(RangeSeekBar bar, long minValue, long maxValue, int action, boolean isMin, int pressedThumb);
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        this.listener = listener;
    }

    public void release(){
        if(!thumbImageLeft.isRecycled()) {
            thumbImageLeft.recycle();
            thumbImageLeft = null;
        }
        if(!thumbImageRight.isRecycled()){
            thumbImageRight.recycle();
            thumbImageRight = null;
        }
        if(!pressedThumbLeft.isRecycled()){
            pressedThumbLeft.recycle();
            pressedThumbLeft = null;
        }
        if(!pressedThumbRight.isRecycled()){
            pressedThumbRight.recycle();
            pressedThumbRight = null;
        }
        System.gc();
    }
}
