package audiosummer.github.me.audiosummer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import audiosummer.github.me.audiosummer.util.FileUtils;
import audiosummer.github.me.audiosummer.util.Song;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

public class AudioTrackPlayActivity extends AppCompatActivity {

    private boolean mIsRecording = false, mIsPlaying = false;
    private int mFrequence = 44100;
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;//单音轨 保证能在所有设备上工作
    private int mChannelStereo = AudioFormat.CHANNEL_IN_STEREO;
    private int mPlayChannelConfig = AudioFormat.CHANNEL_OUT_STEREO;
    private int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;//一个采样点16比特-2个字节

    private MediaPlayer mMediaPlayer;

    private File mAudioFile;

    private TextView title;
    private Song mSong;

    private PlayTask mPlayTask;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atudio_track_play);

        initData();

        initView();

        initEvent();

    }

    private void initData() {
        List<Song> songs = FileUtils.musicFiles(FileUtils.getMusicDir());
        if (songs.size() > 0) {
            mSong = songs.get(0);
        }

        mAudioFile = new File(FileUtils.getMusicDir().getAbsolutePath(), System.nanoTime() + "audio.mp3");
    }

    private void initView() {
        title = (TextView) findViewById(R.id.title);
        title.setText(mSong.title);
    }

    private void initMediaPlayer() {
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
            mMediaPlayer.setDataSource(mSong.url);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initEvent() {

        mPlayTask = new PlayTask();

        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayTask.execute();
            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsPlaying = false;
            }
        });

        findViewById(R.id.media_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initMediaPlayer();
            }
        });

        findViewById(R.id.media_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
            }
        });
    }

    class PlayTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            mIsPlaying = true;
            Decoder mDecoder = new Decoder();
            try {
                int bufferSize = AudioTrack.getMinBufferSize(mFrequence,
                        mPlayChannelConfig, mAudioEncoding);
                short[] buffer = new short[bufferSize];
                // 定义输入流，将音频写入到AudioTrack类中，实现播放
                FileInputStream fin = new FileInputStream(mSong.url);
                Bitstream bitstream = new Bitstream(fin);
                // 实例AudioTrack
                AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
                        mFrequence,
                        mPlayChannelConfig, mAudioEncoding, bufferSize,
                        AudioTrack.MODE_STREAM);
                // 开始播放
                track.play();
                // 由于AudioTrack播放的是流，所以，我们需要一边播放一边读取
                Header header;
                while (mIsPlaying && (header = bitstream.readFrame()) != null) {
                    SampleBuffer sampleBuffer = (SampleBuffer) mDecoder.decodeFrame(header, bitstream);
                    buffer = sampleBuffer.getBuffer();
                    track.write(buffer, 0, buffer.length);
                    bitstream.closeFrame();
                }

                // 播放结束
                track.stop();
                track.release();
                fin.close();
            } catch (Exception e) {
                // TODO: handle exception
                Log.e("slack", "error:" + e.getMessage());
            }
            return null;
        }


        protected void onPostExecute(Void result) {

        }


        protected void onPreExecute() {

        }
    }

    class PlayPCMTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            mIsPlaying = true;
            int bufferSize = AudioTrack.getMinBufferSize(mFrequence,
                    mPlayChannelConfig, mAudioEncoding);
            short[] buffer = new short[bufferSize];
            try {
                // 定义输入流，将音频写入到AudioTrack类中，实现播放
                DataInputStream dis = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(mAudioFile)));
                // 实例AudioTrack
                // AudioTrack AudioFormat.CHANNEL_IN_STEREO here may some problem
                AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
                        mFrequence,
                        AudioFormat.CHANNEL_IN_STEREO, mAudioEncoding, bufferSize,
                        AudioTrack.MODE_STREAM);
                track.setStereoVolume(1.0f, 1.0f);//设置当前音量大小
                // 开始播放
                track.play();
                // 由于AudioTrack播放的是流，所以，我们需要一边播放一边读取
                while (mIsPlaying && dis.available() > 0) {
                    int i = 0;
                    while (dis.available() > 0 && i < buffer.length) {
                        buffer[i] = dis.readShort();
                        i++;
                    }

                    // 然后将数据写入到AudioTrack中
                    track.write(buffer, 0, buffer.length);
                }


                // 播放结束
                track.stop();
                dis.close();
            } catch (Exception e) {
                // TODO: handle exception
                Log.e("slack", "error:" + e.getMessage());
            }
            return null;
        }


        protected void onPostExecute(Void result) {

        }


        protected void onPreExecute() {

        }
    }
}
