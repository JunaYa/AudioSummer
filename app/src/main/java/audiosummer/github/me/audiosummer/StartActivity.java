package audiosummer.github.me.audiosummer;

import android.content.BroadcastReceiver;
import android.content.Context;
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

public class StartActivity extends AppCompatActivity implements View.OnClickListener,
        MediaPlayer.OnCompletionListener {


    private AudioManager mAudioManager;
    private BroadcastReceiver receiver;

    private static final String TAG = "DEMO";

    private TextView mTvMusic, mTvProgress;
    private Button mBtnSave, mBtnPlayback;

    private String mAccompanyPath, mTitle, mArtist, mDuration, mRecordPath;

    private MediaPlayer mMediaPlayer;
    private AudioRecord mAudioRecord;
    private FileInputStream mAccompany;
    private FileOutputStream mRecord;
    private boolean isRunning = true;
    private byte[] accompanyBuf;
    private byte[] recordBuf;
    private byte[] header = new byte[44];

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    return true;
                case R.id.navigation_dashboard:
                    return true;
                case R.id.navigation_notifications:
                    return true;
            }
            return false;
        }

    };

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

    private static class RecorderParameter {
        // 音频获取源
        private static final int audioSource = MediaRecorder.AudioSource.MIC;
        // 设置音频采样率
        private static final int sampleRateInHz = 44100;
        // 设置音频的录制的声道
        private static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        // 音频数据格式
        private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        // 缓冲区字节大小
        private static int bufferSizeInBytes;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        receiver = new HeadSetBroadCastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(receiver, filter);

        initView();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        boolean isHeadsetOn = mAudioManager.isWiredHeadsetOn();
        if (isHeadsetOn) {
            show("isHeadsetOn");
        } else {
            show("isHeadsetOff");
        }

        boolean isMicrophoneMute = mAudioManager.isMicrophoneMute();
        if (isMicrophoneMute) {
            show("isMicrophoneMuteOn");
        } else {
            show("isMicrophoneMuteOff");
        }

        boolean isMusicActive = mAudioManager.isMusicActive();
        mAudioManager.setMicrophoneMute(true);
        if (isMusicActive) {
            show("isMusicActiveOn");
        } else {
            show("isMusicActiveOff");
        }

        boolean isSpeakerphoneOn = mAudioManager.isSpeakerphoneOn();
        if (isSpeakerphoneOn) {
            show("isSpeakerphoneOn");
        } else {
            show("isSpeakerphoneOff");
        }

        // 创建 MediaPlayer
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(mAccompanyPath);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.setOnCompletionListener(this);

        // 创建 AudioRecord
        RecorderParameter.bufferSizeInBytes = AudioRecord.getMinBufferSize(
                RecorderParameter.sampleRateInHz,
                RecorderParameter.channelConfig, RecorderParameter.audioFormat);
        mAudioRecord = new AudioRecord(RecorderParameter.audioSource,
                RecorderParameter.sampleRateInHz,
                RecorderParameter.channelConfig, RecorderParameter.audioFormat,
                RecorderParameter.bufferSizeInBytes);

        accompanyBuf = new byte[RecorderParameter.bufferSizeInBytes];
        recordBuf = new byte[RecorderParameter.bufferSizeInBytes];

        new Thread(new Runnable() {

            @Override
            public void run() {
                // 播放和录音准备
                try {
                    mMediaPlayer.prepare();
                    mAccompany = new FileInputStream(mAccompanyPath);
                    mRecord = new FileOutputStream(mRecordPath);
                    mAccompany.read(header);
                    // mRecord.write(header);
                    mMediaPlayer.start();
                    mAudioRecord.startRecording();
                    mUpdateProgressHandler.post(mUpdateProgressRunnable);

                    // 边录音边混合
                    while (isRunning) {
                        Log.e("H3c",
                                "run==============================================");
                        int rSize = mAudioRecord.read(recordBuf, 0,
                                RecorderParameter.bufferSizeInBytes);
                        Log.e("H3c", "go:" + rSize);
                        int size = mAccompany.read(accompanyBuf, 0,
                                RecorderParameter.bufferSizeInBytes);
                        Log.e("H3c", "s:" + size);
                        if (size < 0) {
                            isRunning = false;
                            continue;
                        }
                        byte[] mixBuff = new byte[size];

                        for (int i = 0; i < size; i++) {
                            mixBuff[i] = (byte) Math
                                    .round((accompanyBuf[i] + recordBuf[i]) / 2);
                        }

                        mRecord.write(mixBuff);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (mMediaPlayer != null) {
                        mMediaPlayer.stop();
                    }
                    if (mAudioRecord != null) {
                        mAudioRecord.stop();
                    }
                    try {
                        mAccompany.close();
                        mRecord.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mUpdateProgressHandler
                            .removeCallbacks(mUpdateProgressRunnable);
                }
                copyWaveFile(mRecordPath, "/sdcard/h3c.wav");
                Log.e("H3c", "end");
            }
        }).start();

    }

    private void initView() {
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        // 获取控件
        mTvMusic = (TextView) findViewById(R.id.tv_music);
        mTvProgress = (TextView) findViewById(R.id.tv_progress);
        mBtnSave = (Button) findViewById(R.id.btn_save);
        mBtnPlayback = (Button) findViewById(R.id.btn_playback);

        mBtnSave.setOnClickListener(this);
        mBtnPlayback.setOnClickListener(this);
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
    public void onCompletion(MediaPlayer mp) {
        // isRunning = false;
        Log.e("H3c", "completion");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_save:
                isRunning = false;
                break;
            case R.id.btn_playback:
                playback();
                break;
            default:
                break;
        }
    }

    private void playback() {

    }

    // 这里得到可播放的音频文件
    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RecorderParameter.sampleRateInHz;
        int channels = 1;
        long byteRate = 16 * RecorderParameter.sampleRateInHz * channels / 8;
        byte[] data = new byte[RecorderParameter.bufferSizeInBytes];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            Log.e("H3c", "long:" + totalAudioLen);
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有 自己特有的头文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
