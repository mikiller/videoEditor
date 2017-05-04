package com.uilib.videoeditor;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.uilib.R;

import java.io.File;

/**
 * Created by Mikiller on 2017/5/3.
 */

public class VideoEditor extends FrameLayout {
    private Context mContext;
    private RecyclerView rcv_thumb;
    private ImageView iv_cursor;
    private RangeSeekBar seekBar;
    private VideoEditAdapter videoEditAdapter;

    private int maxThumbCount = 10;
    private double mMaxWidth;
    private double thumbWidth;
    private double averagePxMs;//每毫秒所占的px
    private long leftProgress, rightProgress;
    private long duration;
    private String filePath;
    private String OutPutFileDirPath;

    private ValueAnimator animator;
    private ExtractFrameWorkThread mExtractFrameWorkThread;
    private ExtractVideoInfoUtil extractVideoInfoUtil;

    public VideoEditor(Context context) {
        super(context);
        initView(context, null);
    }

    public VideoEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.layout_video_seekbar, this);
        rcv_thumb = (RecyclerView) findViewById(R.id.rcv_thumb);
        iv_cursor = (ImageView) findViewById(R.id.iv_cursor);
        seekBar = (RangeSeekBar) findViewById(R.id.seekBar);
        rcv_thumb.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

        if (attrs == null)
            return;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.VideoEditor);
        setMinTrimMs(ta.getInteger(R.styleable.VideoEditor_minTrimMs, 3000));
        if (ta.getDrawable(R.styleable.VideoEditor_leftThumb) != null) {
            setLeftThumb(ta.getDrawable(R.styleable.VideoEditor_leftThumb));
        }
        if (ta.getDrawable(R.styleable.VideoEditor_rightThumb) != null) {
            setRightThumb(ta.getDrawable(R.styleable.VideoEditor_rightThumb));
        }
        thumbWidth = ta.getDimension(R.styleable.VideoEditor_thumbWidth, 64.0f);
        setThumbWidth(thumbWidth);
        setBorderColor(ta.getColor(R.styleable.VideoEditor_borderColor, Color.parseColor("#00befa")));
        OutPutFileDirPath = PictureUtils.getSaveEditThumbnailDir(context);
    }

    public void setMaxTrimMs(long duration) {
        seekBar.setMaxTrimMs(duration);
    }

    public void setMinTrimMs(long min) {
        seekBar.setMinTrimMs(min);
    }

    public void setThumbWidth(double thumbWidth) {
        seekBar.setThumbWidth(thumbWidth);
    }

    public void setLeftThumb(Drawable drawable) {
        seekBar.setLeftThumb(drawable);
    }

    public void setRightThumb(Drawable drawable) {
        seekBar.setRightThumb(drawable);
    }

    public void setBorderColor(int color){
        seekBar.setBorderColor(color);
    }

    public void setFilePath(String path) {
        filePath = path;
        extractVideoInfoUtil = new ExtractVideoInfoUtil(path);
        duration = extractVideoInfoUtil.getVideoLength();
        leftProgress = 0;
        rightProgress = duration;
        seekBar.setMaxTrimMs(duration);
        seekBar.setThubmPos(RangeSeekBar.MIN, 0);
        seekBar.setThubmPos(RangeSeekBar.MAX, duration);
    }

    public void setSeekListener(final RangeSeekBar.OnRangeSeekBarChangeListener listener) {
        seekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, long minValue, long maxValue, int action, boolean isMin, int pressedThumb) {
                leftProgress = minValue;
                rightProgress = maxValue;
                listener.onRangeSeekBarValuesChanged(bar, minValue, maxValue, action, isMin, pressedThumb);
            }
        });
    }

    public void addItemInfo(VideoEditInfo info) {
        videoEditAdapter.addItemVideoInfo(info);
    }

    public long getLeftProgress() {
        return leftProgress;
    }

    public long getRightProgress() {
        return rightProgress;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //rcv_thumb.addItemDecoration(new EditSpacingItemDecoration(seekBar.getThumbWidth(), maxThumbCount));
        mMaxWidth = seekBar.getValueLength();
        averagePxMs = (mMaxWidth * 1.0f / duration);
        videoEditAdapter = new VideoEditAdapter(mContext,
                (int) (mMaxWidth / maxThumbCount));
        rcv_thumb.setAdapter(videoEditAdapter);
        int extractW = (int) (mMaxWidth / maxThumbCount);
        int extractH = getMeasuredHeight();

        if (mExtractFrameWorkThread == null) {
            mExtractFrameWorkThread = new ExtractFrameWorkThread(extractW, extractH, new MainHandler(), filePath, OutPutFileDirPath, 0, duration, maxThumbCount);
            mExtractFrameWorkThread.start();
        }
    }

    private class MainHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ExtractFrameWorkThread.MSG_SAVE_SUCCESS) {
                addItemInfo((VideoEditInfo) msg.obj);
            }
        }
    }

    public void startVideo() {
        iv_cursor.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        anim();
    }

    public void pauseVideo() {
        if (iv_cursor.getVisibility() == View.VISIBLE) {
            iv_cursor.setVisibility(View.GONE);
        }
        iv_cursor.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    public void restartVideo() {
        iv_cursor.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        anim();
    }

    private void anim() {
        if (iv_cursor.getVisibility() == View.GONE) {
            iv_cursor.setVisibility(View.VISIBLE);
        }
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) iv_cursor.getLayoutParams();
        int start = (int) (leftProgress * averagePxMs + seekBar.getThumbWidth());
        int end = (int) (rightProgress * averagePxMs + seekBar.getThumbWidth());
        animator = ValueAnimator
                .ofInt(start, end)
                .setDuration(rightProgress - leftProgress);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.leftMargin = (int) animation.getAnimatedValue();
                iv_cursor.setLayoutParams(params);
            }
        });
        animator.start();
    }

    public void release() {
        extractVideoInfoUtil.release();
        extractVideoInfoUtil = null;
        if (animator != null) {
            animator.cancel();
            iv_cursor.clearAnimation();
        }
        seekBar.release();
        if (!TextUtils.isEmpty(OutPutFileDirPath)) {
            PictureUtils.deleteFile(new File(OutPutFileDirPath));
        }
    }

}
