package audiosummer.github.me.audiosummer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

import audiosummer.github.me.audiosummer.audio.AudioEncoder;
import audiosummer.github.me.audiosummer.audio.PlayBackMusic;
import audiosummer.github.me.audiosummer.permission.PermissionsManager;
import audiosummer.github.me.audiosummer.permission.PermissionsResultAction;
import audiosummer.github.me.audiosummer.util.BytesTransUtil;
import audiosummer.github.me.audiosummer.util.FileUtils;
import audiosummer.github.me.audiosummer.util.Song;

public class MainActivity extends AppCompatActivity {

    private String mp3FilePath;
    private String mp3FilePath2;
    private File medicCodecFile = null;
    private Button mediaCodecBtn, recodeMixBtn;

    private TextView title;

    private RecordMixTask mRecordMixTask;
    private File mAudioFile = null;
    private boolean mIsRecording = false;
    private int mFrequence = 44100;
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;//单音轨 保证能在所有设备上工作
    private int mChannelStereo = AudioFormat.CHANNEL_IN_STEREO;
    private int mPlayChannelConfig = AudioFormat.CHANNEL_OUT_STEREO;
    private int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;//一个采样点16比特-2个字节

    private AudioEncoder mAudioEncoder;


    private PlayBackMusic mPlayBackMusic;
    private PlayBackMusic mPlayBackMusic2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initPermission();

        title = (TextView) findViewById(R.id.title);
        mediaCodecBtn = (Button) findViewById(R.id.recode_audio_mediacodec);
        recodeMixBtn = (Button) findViewById(R.id.recode_mix_audio);

        medicCodecFile = new File(FileUtils.getMusicDir(), "test_media_audio.mp3"); // m4a");

        List<Song> songs = FileUtils.musicFiles(FileUtils.getMusicDir());
        if (songs.size() > 0) {
            mp3FilePath = songs.get(0).url;
            title.setText(songs.get(0).title);
            mp3FilePath2 = songs.get(1).url;
        }

        mPlayBackMusic = new PlayBackMusic(mp3FilePath);
        mPlayBackMusic2 = new PlayBackMusic(mp3FilePath2);
        title.setText(title.getText().toString());

    }

    private void initPermission() {
        PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                Toast.makeText(MainActivity.this, "All permissions have been granted.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDenied(String permission) {
                String message = "Permission " + permission + " has been denied.";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * AudioRecord 录制音频 use MediaCodec & MediaMuxer write data
     */
    public void mediaCodec(View view) {
        if (mediaCodecBtn.getTag() == null) {
            mediaCodecBtn.setText("stop");
            mediaCodecBtn.setTag(this);
            mAudioEncoder = new AudioEncoder(medicCodecFile.getAbsolutePath());
            mAudioEncoder.prepareEncoder();
            RecordMediaCodecByteBufferTask mRecordMediaCodecByteBufferTask = new RecordMediaCodecByteBufferTask();
            mRecordMediaCodecByteBufferTask.execute();
        } else {
            mediaCodecBtn.setText("recode");
            mediaCodecBtn.setTag(null);
            mIsRecording = false;
            mAudioEncoder.stop();
        }
    }

    /**
     * 混合音频
     */
    public void recodeMixAudio(View view) {
        if (recodeMixBtn.getTag() == null) {
            recodeMixBtn.setText("stop");
            recodeMixBtn.setTag(this);
            mPlayBackMusic.startPlayBackMusic();
            mPlayBackMusic2.startPlayBackMusic();

            mAudioEncoder = new AudioEncoder(medicCodecFile.getAbsolutePath());
            mAudioEncoder.prepareEncoder();
            mRecordMixTask = new RecordMixTask();
            mRecordMixTask.execute();
            if (mPlayBackMusic != null) {
                mPlayBackMusic.setNeedRecodeDataEnable(true);
            }
            if (mPlayBackMusic2 != null) {
                mPlayBackMusic2.setNeedRecodeDataEnable(true);
            }
        } else {
            recodeMixBtn.setText("recode");
            recodeMixBtn.setTag(null);
            mIsRecording = false;
            mAudioEncoder.stop();
            mRecordMixTask.cancel(true);

            mPlayBackMusic.release();
            if (mPlayBackMusic != null) {
                mPlayBackMusic.setNeedRecodeDataEnable(false);
            }
            mPlayBackMusic2.release();
            if (mPlayBackMusic2 != null) {
                mPlayBackMusic2.setNeedRecodeDataEnable(false);
            }
        }
    }

    /**
     * use ByteBuffer
     */
    class RecordMediaCodecByteBufferTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            mIsRecording = true;
            int samples_per_frame = 2048;// SAMPLES_PER_FRAME
            int bufferReadResult = 0;
            long audioPresentationTimeNs; //音频时间戳 pts
            try {
                // 根据定义好的几个配置，来获取合适的缓冲大小
                int bufferSize = AudioRecord.getMinBufferSize(mFrequence,
                        mChannelConfig, mAudioEncoding);
                // 实例化AudioRecord
                AudioRecord record = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, mFrequence,
                        mChannelConfig, mAudioEncoding, bufferSize);
                // 定义缓冲
                ByteBuffer buf = ByteBuffer.allocateDirect(samples_per_frame);

                // 开始录制
                record.startRecording();

                while (mIsRecording) {
                    // 从bufferSize中读取字节，返回读取的short个数
                    audioPresentationTimeNs = System.nanoTime();
                    //从缓冲区中读取数据，存入到buffer字节数组数组中
                    // read audio data from internal mic
                    buf.clear();
                    bufferReadResult = record.read(buf, samples_per_frame);
                    //判断是否读取成功
                    if (bufferReadResult == AudioRecord.ERROR || bufferReadResult == AudioRecord.ERROR_BAD_VALUE ||
                            bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION)
                        Log.e("slack", "Read error");
                    if (mAudioEncoder != null) {
                        //将音频数据发送给AudioEncoder类进行编码
                        buf.position(bufferReadResult).flip();
                        mAudioEncoder.offerAudioEncoder(buf, audioPresentationTimeNs, bufferReadResult);
                    }

                }
                // 录制结束
                if (record != null) {
                    record.setRecordPositionUpdateListener(null);
                    record.stop();
                    record.release();
                }

            } catch (Exception e) {
                // TODO: handle exception
                Log.e("slack", "::" + e.getMessage());
            }
            return null;
        }

        // 当在上面方法中调用publishProgress时，该方法触发,该方法在UI线程中被执行
        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Void result) {
        }

    }

    class RecordMixTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            mIsRecording = true;
            int bufferReadResult = 0;
            long audioPresentationTimeNs; //音频时间戳 pts
            try {
                // 根据定义好的几个配置，来获取合适的缓冲大小
                int bufferSize = AudioRecord.getMinBufferSize(mFrequence, mChannelConfig, mAudioEncoding);
                // 实例化AudioRecord
                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, mFrequence, mChannelConfig, mAudioEncoding, bufferSize * 4);

                // 开始录制
                record.startRecording();

                while (mIsRecording) {

                    audioPresentationTimeNs = System.nanoTime();

                    int samples_per_frame = mPlayBackMusic.getBufferSize(); // 这里需要与 背景音乐读取出来的数据长度 一样
                    byte[] buffer = new byte[samples_per_frame];
                    //从缓冲区中读取数据，存入到buffer字节数组数组中
                    bufferReadResult = record.read(buffer, 0, buffer.length);
                    //判断是否读取成功
                    if (bufferReadResult == AudioRecord.ERROR_BAD_VALUE || bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e("slack", "Read error");
                    }
                    if (mAudioEncoder != null) {
//                        Log.i("slack","buffer length: " + buffer.length + " " + bufferReadResult + " " + bufferSize);
                        buffer = mixBuffer(buffer);
                        //将音频数据发送给AudioEncoder类进行编码
                        mAudioEncoder.offerAudioEncoder(buffer, audioPresentationTimeNs);
                    }
                    Log.d("recording", "isRecording");

                }
                // 录制结束
                record.setRecordPositionUpdateListener(null);
                record.stop();
                record.release();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        // 当在上面方法中调用publishProgress时，该方法触发,该方法在UI线程中被执行
        protected void onProgressUpdate(Integer... progress) {
            //
        }

        protected void onPostExecute(Void result) {

        }

    }


    /**
     * 混合 音频
     */
    private byte[] mixBuffer(byte[] buffer) {
        if (mPlayBackMusic.hasFrameBytes()) {
//            return mPlayBackMusic.getBackGroundBytes(); // 直接写入背景音乐数据
            return BytesTransUtil.INSTANCE.averageMix(mPlayBackMusic2.getBackGroundBytes(), mPlayBackMusic.getBackGroundBytes());
        }
        return buffer;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRecording = false;
    }

    @Override
    public void onBackPressed() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
