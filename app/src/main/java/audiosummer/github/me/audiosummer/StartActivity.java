package audiosummer.github.me.audiosummer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class StartActivity extends AppCompatActivity implements View.OnClickListener {


    private BroadcastReceiver receiver;

    private static final String TAG = "DEMO";

    private TextView mTvMusic, mTvProgress;

    private String mAccompanyPath, mTitle, mArtist, mDuration, mRecordPath;

    private MediaPlayer mMediaPlayer;
    private AudioRecord mAudioRecord;

    // 用于更新播放进度
    Handler mUpdateProgressHandler = new Handler();
    Runnable mUpdateProgressRunnable = new Runnable() {

        @Override
        public void run() {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                String curDuration = DateFormat.format("mm:ss",
                        mMediaPlayer.getCurrentPosition()).toString();
                mTvProgress.setText(curDuration + "/" + mDuration);
            }
            mUpdateProgressHandler.postDelayed(mUpdateProgressRunnable, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        receiver = new HeadSetBroadCastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(receiver, filter);

        initView();

        initEvent();

    }

    private void initView() {
        // 获取控件
        mTvMusic = (TextView) findViewById(R.id.tv_music);
        mTvProgress = (TextView) findViewById(R.id.tv_progress);

    }

    private void initEvent(){
        findViewById(R.id.btn_save).setOnClickListener(this);
        findViewById(R.id.btn_playback).setOnClickListener(this);
        findViewById(R.id.btn_media_extractor).setOnClickListener(this);
    }

    private void show(String string) {
        Toast.makeText(this, string, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        Log.i(TAG, "onDestroy");
        mUpdateProgressHandler.removeCallbacks(mUpdateProgressRunnable);
        mMediaPlayer.release();
        mMediaPlayer = null;
        mAudioRecord.release();
        mAudioRecord = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_save:
                startActivity(new Intent(this,MainActivity.class));
                break;
            case R.id.btn_playback:
                startActivity(new Intent(this,AudioTrackPlayActivity.class));
                break;
            case R.id.btn_media_extractor:
                startActivity(new Intent(this,MediaExtractorActivity.class));
                break;
            default:
                break;
        }
    }
}
