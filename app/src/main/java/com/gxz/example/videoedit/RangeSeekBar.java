package com.gxz.example.videoedit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.text.DecimalFormat;


/**
 * ================================================
 * 作    者：顾修忠-guxiuzhong@youku.com/gfj19900401@163.com
 * 版    本：
 * 创建日期：2017/4/4-下午1:22
 * 描    述：
 * 修订历史：
 * ================================================
 */

public class RangeSeekBar extends View {
    private static final String TAG = RangeSeekBar.class.getSimpleName();
    public static final int MIN = 0, MAX = 1, NONE = -1;
    private double absoluteMinValuePrim, absoluteMaxValuePrim;
    private double normalizedMinValue = 0d;//点坐标占总长度的比例值，范围从0-1
    private double normalizedMaxValue = 1d;//点坐标占总长度的比例值，范围从0-1
    private double normalizedMinValueTime = 0d;
    private double normalizedMaxValueTime = 1d;// normalized：规格化的--点坐标占总长度的比例值，范围从0-1

    private float CURSOR_MOVE = 0;
    private long min_cut_time = 3000;
    private double min_width = 1;//最小裁剪距离
    private int pressedThumb = NONE;
    private boolean isActive = false;

    private Bitmap thumbImageLeft;
    private Bitmap thumbImageRight;
    private Bitmap pressedThumbLeft;
    private Bitmap pressedThumbRight;
    private Paint thumbPaint;
    private Paint rectPaint;
    private Paint cursorPaint;
    private int thumbWidth = 64;
    private boolean isMin;


    public RangeSeekBar(Context context) {
        super(context);
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RangeSeekBar(Context context, long absoluteMinValuePrim, long absoluteMaxValuePrim) {
        super(context);
        this.absoluteMinValuePrim = absoluteMinValuePrim;
        this.absoluteMaxValuePrim = absoluteMaxValuePrim;
        if (checkAbsoluteValue() && (absoluteMaxValuePrim - absoluteMinValuePrim < min_cut_time))
            min_cut_time = absoluteMaxValuePrim - absoluteMinValuePrim;
        setFocusable(true);
        setFocusableInTouchMode(true);

    }

    private boolean checkAbsoluteValue() {
        if (absoluteMaxValuePrim - absoluteMinValuePrim <= 0) {
            absoluteMinValuePrim = 0l;
            absoluteMaxValuePrim = 1l;
            return false;
        } else
            return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        init();
    }

    private void init() {
        //等比例缩放图片
        thumbImageLeft = BitmapFactory.decodeResource(getResources(), R.drawable.lseekbar);
        thumbImageRight = BitmapFactory.decodeResource(getResources(), R.drawable.rseekbar);
        pressedThumbLeft = BitmapFactory.decodeResource(getResources(), R.drawable.lseekbar_selected);
        pressedThumbRight = BitmapFactory.decodeResource(getResources(), R.drawable.rseekbar_selected);
        int height = thumbImageLeft.getHeight();
        int newHeight = getMeasuredHeight();
        float scaleHeight = newHeight * 1.0f / height;
        Matrix matrix = new Matrix();
        matrix.postScale(1, scaleHeight);
        thumbImageLeft = Bitmap.createBitmap(thumbImageLeft, 0, 0, thumbImageLeft.getWidth(), thumbImageLeft.getHeight(), matrix, true);
        thumbImageRight = Bitmap.createBitmap(thumbImageRight, 0, 0, thumbImageRight.getWidth(), thumbImageRight.getHeight(), matrix, true);
        pressedThumbLeft = Bitmap.createBitmap(pressedThumbLeft, 0, 0, pressedThumbLeft.getWidth(), pressedThumbLeft.getHeight(), matrix, true);
        pressedThumbRight = Bitmap.createBitmap(pressedThumbRight, 0, 0, pressedThumbRight.getWidth(), pressedThumbRight.getHeight(), matrix, true);
        thumbWidth = thumbImageRight.getWidth();

        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setColor(Color.parseColor("#00befa"));
        cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cursorPaint.setStyle(Paint.Style.FILL);
        cursorPaint.setShader(new LinearGradient(0, 0, 0, getMeasuredHeight(),
                new int[]{Color.parseColor("#75ffd855"), Color.parseColor("#ffd855"), Color.parseColor("#75ffd855")},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
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
                screenCoord - (isLeft ? 0 : thumbWidth), 0, thumbPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1 || absoluteMaxValuePrim <= min_cut_time) {
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
                setPressed(false);
                invalidate();
                if (listener != null) {
                    Log.e(TAG, "min: " + normalizedMinValue + ", max: " + normalizedMaxValue);
                    Log.e(TAG, "duration: " + absoluteMaxValuePrim + ", max: " + normalizedToValue(normalizedMaxValueTime));
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
        double min = min_cut_time / (absoluteMaxValuePrim - absoluteMinValuePrim) * getValueLength();
        if (absoluteMaxValuePrim > 5 * 60 * 1000) {//大于5分钟的精确小数四位
            DecimalFormat df = new DecimalFormat("0.0000");
            min_width = Double.parseDouble(df.format(min));
        } else {
            min_width = Math.round(min + 0.5d);
        }

        double minLength = 2 * thumbWidth + min_width;
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

    public int getValueLength() {
        return (getWidth() - 2 * thumbWidth);
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

    public void setMin_cut_time(long min_cut_time) {
        this.min_cut_time = min_cut_time;
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
        return (value - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim);
    }

    public long getSelectedMinValue() {
        return normalizedToValue(normalizedMinValueTime);
    }

    public long getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValueTime);
    }

    private long normalizedToValue(double normalized) {
        return (long) (absoluteMinValuePrim + normalized
                * (absoluteMaxValuePrim - absoluteMinValuePrim));
    }

    public int getThumbWidth(){
        return thumbWidth;
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
}
