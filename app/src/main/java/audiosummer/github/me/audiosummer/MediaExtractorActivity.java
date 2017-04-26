package audiosummer.github.me.audiosummer;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import audiosummer.github.me.audiosummer.util.FileUtils;
import audiosummer.github.me.audiosummer.util.Song;


public class MediaExtractorActivity extends AppCompatActivity {


    private TextView tvTitle , tvTrackNum ,tvMine;


    private Song mSong;

    private MediaExtractor mMediaExtractor;

    private boolean selectThisTrack = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meida_extrator);


        initData();

        initView();

        initEvent();

    }

    private void initData() {
        List<Song> songs = FileUtils.musicFiles(FileUtils.getMusicDir());
        mSong = songs.get(0);
    }

    private void initView() {
        tvTitle = (TextView) findViewById(R.id.title);
        tvTitle.setText(mSong.title);
        tvTrackNum = (TextView)findViewById(R.id.tv_track_num);
        tvMine = (TextView)findViewById(R.id.tv_mine);
    }

    private void initEvent() {
        try {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(mSong.url);

            int numTracks = mMediaExtractor.getTrackCount();
            tvTrackNum.setText("track num is : " + numTracks);

            StringBuffer mineBuffer = new StringBuffer();
            for (int i = 0 ; i<numTracks;i++){
                MediaFormat format = mMediaExtractor.getTrackFormat(i);
                String mine = format.getString(MediaFormat.KEY_MIME);
                mineBuffer.append(mine);
                mineBuffer.append("\n");
                if (selectThisTrack){
                    mMediaExtractor.selectTrack(i);
                }
            }
            tvMine.setText(mineBuffer.toString());


            ByteBuffer inputBuffer = ByteBuffer.allocate(0);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            mMediaExtractor.release();
            mMediaExtractor = null;
        }
    }


}
