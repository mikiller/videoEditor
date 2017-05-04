package com.uilib;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import com.uilib.videoeditor.OnTrimVideoListener;
import com.uilib.videoeditor.RangeSeekBar;
import com.uilib.videoeditor.TrimVideoUtils;
import com.uilib.videoeditor.VideoEditor;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ================================================
 * 作    者：顾修忠-guxiuzhong@youku.com/gfj19900401@163.com
 * 版    本：
 * 创建日期：2017/4/8-下午3:48
 * 描    述：
 * 修订历史：
 * ================================================
 */
public class VideoEditActivity extends AppCompatActivity {
    private static final String TAG = VideoEditActivity.class.getSimpleName();

    private VideoView mVideoView;
    private VideoEditor videoEditor;
    private Button btn_save;

    private String path;
    private boolean isSeeking;

    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_edit);
        initData();
        initView();
        initPlay();

        PermissionUtils.checkPermissions(this);
    }

    private void initData() {
        path = Environment.getExternalStorageDirectory() + "/Movies/sr-new.mp4";
//        path = Environment.getExternalStorageDirectory() + "/MP4_20170502_155322.mp4";

        //for video check
        if (!new File(path).exists()) {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_LONG).show();
            finish();
        }

    }

    private void initView() {
        mVideoView = (VideoView) findViewById(R.id.uVideoView);
        videoEditor = (VideoEditor) findViewById(R.id.videoeditor);
        videoEditor.setFilePath(path);
        videoEditor.setSeekListener(mOnRangeSeekBarChangeListener);

        btn_save = (Button) findViewById(R.id.btn_save);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TrimVideoUtils.startTrim(new File(path), Environment.getExternalStorageDirectory().toString().concat(File.separator), videoEditor.getLeftProgress(), videoEditor.getRightProgress(), new OnTrimVideoListener() {
                                @Override
                                public void onTrimStarted() {

                                }

                                @Override
                                public void getResult(Uri uri) {

                                }

                                @Override
                                public void cancelAction() {

                                }

                                @Override
                                public void onError(String message) {

                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }

    private void initPlay() {
        mVideoView.setVideoPath(path);
        //设置videoview的OnPrepared监听
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //设置MediaPlayer的OnSeekComplete监听
                Log.e(TAG, "prepared");
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(!isSeeking){
                            restartVideo();
                        }
                    }
                }, 0l, 1000l);
                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        if (!isSeeking) {
                            videoStart();
                        }
                    }
                });
            }
        });
        //first
        videoStart();
    }

    private final RangeSeekBar.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBar.OnRangeSeekBarChangeListener() {
        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBar bar, long minValue, long maxValue, int action, boolean isMin, int pressedThumb) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isSeeking = false;
                    videoPause();
                    break;
                case MotionEvent.ACTION_MOVE:
                    isSeeking = true;
                    mVideoView.seekTo((int) (pressedThumb == RangeSeekBar.MIN ?
                            minValue : maxValue));
                    break;
                case MotionEvent.ACTION_UP:
                    isSeeking = false;
                    //从minValue开始播
                    mVideoView.seekTo((int) minValue);
//                    videoStart();
                    break;
                default:
                    break;
            }
        }
    };

    private void videoStart() {
        mVideoView.start();
        videoEditor.startVideo();
    }

    private void restartVideo() {
        long currentPosition = mVideoView.getCurrentPosition();
        if (currentPosition >= (videoEditor.getRightProgress()) || (!mVideoView.isPlaying() && !isSeeking)) {
            VideoEditActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
            mVideoView.seekTo((int) videoEditor.getLeftProgress());
                    videoEditor.restartVideo();
                }
            });
        }

    }

    private void videoPause() {
        isSeeking = false;
        if (mVideoView != null && mVideoView.isPlaying()) {
            mVideoView.pause();
        }
        videoEditor.pauseVideo();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.seekTo((int) videoEditor.getLeftProgress());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null && mVideoView.isPlaying()) {
            videoPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(videoEditor != null)
            videoEditor.release();
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }

        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
