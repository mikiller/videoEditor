package com.uilib.videoeditor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.StateSet;
import android.view.MotionEvent;
import android.view.View;

import com.uilib.R;

import java.text.DecimalFormat;

public class RangeSeekBar extends View {
    private static final String TAG = RangeSeekBar.class.getSimpleName();
    public static final int MIN = 0, MAX = 1, NONE = -1;
    private double normalizedMinValue = 0d;//点坐标占总长度的比例值，范围从0-1
    private double normalizedMaxValue = 1d;//点坐标占总长度的比例值，范围从0-1
    private double normalizedMinValueTime = 0d;
    private double normalizedMaxValueTime = 1d;// normalized：规格化的--点坐标占总长度的比例值，范围从0-1

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

//    public RangeSeekBar(Context context, long maxDuration) {
//        super(context);
//        if(maxDuration < 0)
//            throw new IllegalArgumentException("maxDuration is less than zero!");
//        this.maxTrimMs = maxDuration;
//        if(maxTrimMs < minTrimMs)
//            minTrimMs = maxTrimMs;
//        setFocusable(true);
//        setFocusableInTouchMode(true);
//
//    }

    private void initView(Context context, AttributeSet attrs){
        setFocusable(true);
        setFocusableInTouchMode(true);
        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setColor(Color.parseColor("#00befa"));
        if(attrs == null)
            return;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
        minTrimMs = ta.getInteger(R.styleable.RangeSeekBar_minTrimMs, 3000);
        thumbWidth = ta.getDimension(R.styleable.RangeSeekBar_thumbWidth, 64.0f);
        leftThumb = ta.getDrawable(R.styleable.RangeSeekBar_leftThumb);
        rightThumb = ta.getDrawable(R.styleable.RangeSeekBar_rightThumb);
        ta.recycle();
    }

    public void setMaxTrimMs(long duration){
        if(duration < 0)
            throw new IllegalArgumentException("maxDuration is less than zero!");
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

    public void setLeftThumb(Drawable drawable){
        leftThumb = drawable;
    }

    public void setRightThumb(Drawable drawable){
        rightThumb = drawable;
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
        int[] drawableState = getDrawableState();
        if(matrix == null)
            return;

        if(pressedThumb != NONE){
            if(leftThumb != null) {
                leftThumb.setState(drawableState);
                pressedThumbLeft = ((BitmapDrawable) leftThumb.getCurrent()).getBitmap();
                pressedThumbLeft = Bitmap.createBitmap(pressedThumbLeft, 0, 0, pressedThumbLeft.getWidth(), pressedThumbLeft.getHeight(), matrix, true);
                leftThumb = null;
            }
            if(rightThumb != null) {
                rightThumb.setState(drawableState);
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
        float rangeL = getThumbPosX(normalizedMinValue);
        float rangeR = getThumbPosX(normalizedMaxValue);
        try {
            //画上下边框
            canvas.drawRect(rangeL, 0, rangeR, dip2px(2), rectPaint);
            canvas.drawRect(rangeL, getHeight() - dip2px(2), rangeR, getHeight(), rectPaint);
            //画左右thumb
            drawThumb(rangeL, canvas, true);
            drawThumb(rangeR, canvas, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawThumb(float screenCoord, Canvas canvas, boolean isLeft) {
        canvas.drawBitmap(isLeft ?
                        (pressedThumb == MIN ? pressedThumbLeft : thumbImageLeft)
                        : (pressedThumb == MAX ? pressedThumbRight : thumbImageRight),
                (float) (screenCoord - (isLeft ? 0 : thumbWidth)), 0, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1 /*|| absoluteMaxValuePrim <= minTrimMs*/) {
            return super.onTouchEvent(event);
        }

        if (!isEnabled())
            return false;
//        int pointerIndex;// 记录点击点的index
        switch (event.getAction()/* & MotionEvent.ACTION_MASK*/) {
            case MotionEvent.ACTION_DOWN:
                //记住最后一个手指点击屏幕的点的坐标x，mDownMotionX
//                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
//                pointerIndex = event.findPointerIndex(mActivePointerId);
//                mDownMotionX = event.getX(pointerIndex);
                // 判断touch到的是最大值thumb还是最小值thumb
                if ((pressedThumb = evalPressedThumb(event.getX())) == NONE)
                    return false;
                setPressed(true);// 设置该控件被按下了
                trackTouchEvent(event, pressedThumb);
                attemptClaimDrag();
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this,
                            normalizedToValue(normalizedMinValueTime),
                            normalizedToValue(normalizedMaxValueTime),
                            MotionEvent.ACTION_DOWN, isMin, pressedThumb);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                trackTouchEvent(event, pressedThumb);
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this,
                            normalizedToValue(normalizedMinValueTime),
                            normalizedToValue(normalizedMaxValueTime),
                            MotionEvent.ACTION_MOVE, isMin, pressedThumb);
                }
                break;
            case MotionEvent.ACTION_UP:
                trackTouchEvent(event, pressedThumb);
                refreshDrawableState();
                invalidate();
                if (listener != null) {
                    listener.onRangeSeekBarValuesChanged(this,
                            normalizedToValue(normalizedMinValueTime),
                            normalizedToValue(normalizedMaxValueTime),
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

    private void trackTouchEvent(MotionEvent event, int pressedThumb) {
        setThumbPosValue(pressedThumb, screenToNormalized(event.getX(), pressedThumb));
    }

    private double screenToNormalized(double screenCoord, int position) {
        isMin = false;
        double min = getValueLength() * minTrimMs / maxTrimMs;
        if (maxTrimMs > 5 * 60 * 1000) {//大于5分钟的精确小数四位
            DecimalFormat df = new DecimalFormat("0.0000");
            min = Double.parseDouble(df.format(min));
        } else {
            min = Math.round(min + 0.5d);
        }

        double minLength = 2 * thumbWidth + min;
        double maxLength = position == MIN ? Math.abs(minLength - getThumbPosX(normalizedMaxValue)) : Math.abs(minLength + getThumbPosX(normalizedMinValue));
        if (position == MIN ? (screenCoord > maxLength) : (screenCoord < maxLength)) {
            isMin = true;
            screenCoord = maxLength;
        }
        if (position == MIN) {
            //解决快速滑动时无法滑到0点
            if (screenCoord < thumbWidth * 2 / 3) {
                screenCoord = 0;
            }
            normalizedMinValueTime = Math.min(1d, Math.max(0d, (screenCoord) / getValueLength()));
        } else if (position == MAX) {
            if (getWidth() - screenCoord < (thumbWidth * 2 / 3)) {
                screenCoord = getWidth();
            }
            normalizedMaxValueTime = Math.min(1d, Math.max(0d, (screenCoord - 2 * thumbWidth) / getValueLength()));
        }
        return Math.min(1d, Math.max(0d, screenCoord / getWidth()));
    }

    public double getValueLength() {
        return (getMeasuredWidth()*1.0 - 2.0 * thumbWidth);
    }

    /**
     * 计算位于哪个Thumb内
     *
     * @param touchX touchX
     * @return 被touch的是空还是最大值或最小值
     */
    private int evalPressedThumb(float touchX) {
        int result = NONE;
        boolean minThumbPressed = isInThumbRange(touchX, normalizedMinValue, 1.5);// 触摸点是否在最小值图片范围内
        boolean maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue, 1.5);
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
        // 当前触摸点X坐标-最小值图片中心点在屏幕的X坐标之差<=滑块的宽度*scale
        // 即判断触摸点是否在以最小值图片中心为原点，半径为滑块的宽度*scale的圆内。
        return Math.abs(touchX - getThumbPosX(normalizedThumbValue)) <= thumbWidth * scale;
    }

    /**
     * 试图告诉父view不要拦截子控件的drag
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    private float getThumbPosX(double normalizedCoord) {
        return (float) (getPaddingLeft() + normalizedCoord * (getWidth() - getPaddingLeft() - getPaddingRight()));
    }

    public void setSelectValue(int thumb, long value) {
        setThumbPosValue(thumb, valueToNormalized(value));
    }

    private void setThumbPosValue(int thumb, double value) {
        if (thumb == MIN) {
            normalizedMinValue = Math.max(0d, Math.min(1d, Math.min(value, normalizedMaxValue)));
        } else if (thumb == MAX) {
            normalizedMaxValue = Math.max(0d, Math.min(1d, Math.max(value, normalizedMinValue)));
        }
        invalidate();
    }

    private double valueToNormalized(long value) {
//        return (value - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim);
        return 1.0 * value / maxTrimMs;
    }

    public long getSelectedMinValue() {
        return normalizedToValue(normalizedMinValueTime);
    }

    public long getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValueTime);
    }

    private long normalizedToValue(double normalized) {
//        return (long) (absoluteMinValuePrim + normalized * (absoluteMaxValuePrim - absoluteMinValuePrim));
        return (long) (normalized * maxTrimMs);
    }

    public int getThumbWidth(){
        return (int) thumbWidth;
    }

    public int dip2px(int dip) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) ((float) dip * scale + 0.5F);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", normalizedMinValue);
        bundle.putDouble("MAX", normalizedMaxValue);
        bundle.putDouble("MIN_TIME", normalizedMinValueTime);
        bundle.putDouble("MAX_TIME", normalizedMaxValueTime);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        normalizedMinValue = bundle.getDouble("MIN");
        normalizedMaxValue = bundle.getDouble("MAX");
        normalizedMinValueTime = bundle.getDouble("MIN_TIME");
        normalizedMaxValueTime = bundle.getDouble("MAX_TIME");
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
