package com.dueeeke.dkplayer.widget.controller;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dueeeke.dkplayer.R;
import com.dueeeke.videoplayer.controller.GestureVideoController;
import com.dueeeke.videoplayer.player.IjkVideoView;
import com.dueeeke.videoplayer.util.L;
import com.dueeeke.videoplayer.util.PlayerUtils;

/**
 * 全屏控制器
 * Created by Devlin_n on 2017/4/7.
 */

public class FullScreenController extends GestureVideoController implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    protected TextView totalTime, currTime;
    protected LinearLayout bottomContainer, topContainer;
    protected SeekBar videoProgress;
    protected ImageView backButton;
    protected ImageView lock;
    protected TextView title;
    private boolean isLive;
    private boolean isDragging;

    private ProgressBar bottomProgress;
    private ImageView playButton;
    private ProgressBar loadingProgress;
    private LinearLayout completeContainer;
    private Animation showAnim = AnimationUtils.loadAnimation(getContext(), R.anim.dkplayer_anim_alpha_in);
    private Animation hideAnim = AnimationUtils.loadAnimation(getContext(), R.anim.dkplayer_anim_alpha_out);


    public FullScreenController(@NonNull Context context) {
        this(context, null);
    }

    public FullScreenController(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FullScreenController(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.layout_fullscreen_controller;
    }

    @Override
    protected void initView() {
        super.initView();
        bottomContainer = mControllerView.findViewById(R.id.bottom_container);
        topContainer = mControllerView.findViewById(R.id.top_container);
        videoProgress = mControllerView.findViewById(R.id.seekBar);
        videoProgress.setOnSeekBarChangeListener(this);
        totalTime = mControllerView.findViewById(R.id.total_time);
        currTime = mControllerView.findViewById(R.id.curr_time);
        backButton = mControllerView.findViewById(R.id.back);
        backButton.setOnClickListener(this);
        lock = mControllerView.findViewById(R.id.lock);
        lock.setOnClickListener(this);
        playButton = mControllerView.findViewById(R.id.iv_play);
        playButton.setOnClickListener(this);
        loadingProgress = mControllerView.findViewById(R.id.loading);
        bottomProgress = mControllerView.findViewById(R.id.bottom_progress);
        ImageView rePlayButton = mControllerView.findViewById(R.id.iv_replay);
        rePlayButton.setOnClickListener(this);
        completeContainer = mControllerView.findViewById(R.id.complete_container);
        completeContainer.setOnClickListener(this);
        title = mControllerView.findViewById(R.id.title);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.lock) {
            doLockUnlock();
        } else if (i == R.id.iv_play || i == R.id.iv_replay) {
            doPauseResume();
        } else if (i == R.id.back) {
            PlayerUtils.scanForActivity(getContext()).finish();
        }
    }

    public void showTitle() {
        title.setVisibility(VISIBLE);
    }


    @Override
    public void setPlayerState(int playerState) {
        super.setPlayerState(playerState);
        switch (playerState) {
            case IjkVideoView.PLAYER_FULL_SCREEN:
                mIsGestureEnabled = true;
                break;
        }
    }

    @Override
    public void setPlayState(int playState) {
        super.setPlayState(playState);
        switch (playState) {
            case IjkVideoView.STATE_IDLE:
                L.e("STATE_IDLE");
                hide();
                mIsLocked = false;
                lock.setSelected(false);
                mMediaPlayer.setLock(false);
                completeContainer.setVisibility(GONE);
                bottomProgress.setVisibility(GONE);
                loadingProgress.setVisibility(GONE);
                break;
            case IjkVideoView.STATE_PLAYING:
                L.e("STATE_PLAYING");
                post(mShowProgress);
                playButton.setSelected(true);
                completeContainer.setVisibility(GONE);
                break;
            case IjkVideoView.STATE_PAUSED:
                L.e("STATE_PAUSED");
                playButton.setSelected(false);
                break;
            case IjkVideoView.STATE_PREPARING:
                L.e("STATE_PREPARING");
                completeContainer.setVisibility(GONE);
                loadingProgress.setVisibility(VISIBLE);
                break;
            case IjkVideoView.STATE_PREPARED:
                L.e("STATE_PREPARED");
                if (!isLive) bottomProgress.setVisibility(VISIBLE);
                loadingProgress.setVisibility(GONE);
                break;
            case IjkVideoView.STATE_ERROR:
                L.e("STATE_ERROR");
                break;
            case IjkVideoView.STATE_BUFFERING:
                L.e("STATE_BUFFERING");
                loadingProgress.setVisibility(VISIBLE);
                break;
            case IjkVideoView.STATE_BUFFERED:
                loadingProgress.setVisibility(GONE);
                L.e("STATE_BUFFERED");
                break;
            case IjkVideoView.STATE_PLAYBACK_COMPLETED:
                L.e("STATE_PLAYBACK_COMPLETED");
                hide();
                removeCallbacks(mShowProgress);
                completeContainer.setVisibility(VISIBLE);
                bottomProgress.setProgress(0);
                bottomProgress.setSecondaryProgress(0);
                mIsLocked = false;
                mMediaPlayer.setLock(false);
                break;
        }
    }

    private void doLockUnlock() {
        if (mIsLocked) {
            mIsLocked = false;
            mShowing = false;
            mIsGestureEnabled = true;
            show();
            lock.setSelected(false);
            Toast.makeText(getContext(), R.string.unlocked, Toast.LENGTH_SHORT).show();
        } else {
            hide();
            mIsLocked = true;
            mIsGestureEnabled = false;
            lock.setSelected(true);
            Toast.makeText(getContext(), R.string.locked, Toast.LENGTH_SHORT).show();
        }
        mMediaPlayer.setLock(mIsLocked);
    }

    /**
     * 设置是否为直播视频
     */
    public void setLive() {
        isLive = true;
        bottomProgress.setVisibility(GONE);
        videoProgress.setVisibility(INVISIBLE);
        totalTime.setVisibility(INVISIBLE);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        isDragging = true;
        removeCallbacks(mShowProgress);
        removeCallbacks(mFadeOut);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        long duration = mMediaPlayer.getDuration();
        long newPosition = (duration * seekBar.getProgress()) / videoProgress.getMax();
        mMediaPlayer.seekTo((int) newPosition);
        isDragging = false;
        post(mShowProgress);
        show();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }

        long duration = mMediaPlayer.getDuration();
        long newPosition = (duration * progress) / videoProgress.getMax();
        if (currTime != null)
            currTime.setText(stringForTime((int) newPosition));
    }

    @Override
    public void hide() {
        if (mShowing) {
            if (mMediaPlayer.isFullScreen()) {
                lock.setVisibility(GONE);
                if (!mIsLocked) {
                    hideAllViews();
                }
            } else {
                hideAllViews();
            }
            if (!isLive && !mIsLocked) {
                bottomProgress.setVisibility(VISIBLE);
                bottomProgress.startAnimation(showAnim);
            }
            mShowing = false;
        }
    }

    private void hideAllViews() {
        topContainer.setVisibility(GONE);
        topContainer.startAnimation(hideAnim);
        bottomContainer.setVisibility(GONE);
        bottomContainer.startAnimation(hideAnim);
    }

    private void show(int timeout) {
        if (!mShowing) {
            if (mMediaPlayer.isFullScreen()) {
                lock.setVisibility(VISIBLE);
                if (!mIsLocked) {
                    showAllViews();
                }
            } else {
                showAllViews();
            }
            if (!mIsLocked && !isLive) {
                bottomProgress.setVisibility(GONE);
                bottomProgress.startAnimation(hideAnim);
            }
            mShowing = true;
        }
        removeCallbacks(mFadeOut);
        if (timeout != 0) {
            postDelayed(mFadeOut, timeout);
        }
    }

    private void showAllViews() {
        bottomContainer.setVisibility(VISIBLE);
        bottomContainer.startAnimation(showAnim);
        topContainer.setVisibility(VISIBLE);
        topContainer.startAnimation(showAnim);
    }

    @Override
    public void show() {
        show(mDefaultTimeout);
    }

    @Override
    protected int setProgress() {
        if (mMediaPlayer == null || isDragging) {
            return 0;
        }
        int position = (int) mMediaPlayer.getCurrentPosition();
        int duration = (int) mMediaPlayer.getDuration();
        if (videoProgress != null) {
            if (duration > 0) {
                videoProgress.setEnabled(true);
                int pos = (int) (position * 1.0 / duration * videoProgress.getMax());
                videoProgress.setProgress(pos);
                bottomProgress.setProgress(pos);
            } else {
                videoProgress.setEnabled(false);
            }
            int percent = mMediaPlayer.getBufferedPercentage();
            if (percent >= 95) { //修复第二进度不能100%问题
                videoProgress.setSecondaryProgress(videoProgress.getMax());
                bottomProgress.setSecondaryProgress(bottomProgress.getMax());
            } else {
                videoProgress.setSecondaryProgress(percent * 10);
                bottomProgress.setSecondaryProgress(percent * 10);
            }
        }

        if (totalTime != null)
            totalTime.setText(stringForTime(duration));
        if (currTime != null)
            currTime.setText(stringForTime(position));
        if (title != null)
            title.setText(mMediaPlayer.getTitle());
        return position;
    }


    @Override
    protected void slideToChangePosition(float deltaX) {
        if (isLive) {
            mNeedSeek = false;
        } else {
            super.slideToChangePosition(deltaX);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (mIsLocked) {
            show();
            Toast.makeText(getContext(), R.string.lock_tip, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onBackPressed();
    }
}
