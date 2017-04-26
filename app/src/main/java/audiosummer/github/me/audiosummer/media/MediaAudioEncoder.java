package audiosummer.github.me.audiosummer.media;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

/**
 * @author slack
 * @time 17/2/13 下午4:08
 *  audio MediaCodec
 */
public class MediaAudioEncoder extends MediaEncoder {

    private static final String TAG = "MediaAudioEncoder";

    public MediaAudioEncoder(MediaMuxerMixAudioAndVideo mediaMuxer) {
        super(mediaMuxer);
    }

    @Override
    void prepare(MediaInfo info) throws IOException {
        MediaFormat mformat = info.createAudioFormat(); // only audio is ok
        mMediaCodec = null;
        mMediaCodec = MediaCodec.createEncoderByType(info.mAudioMime);// only audio is ok
        mMediaCodec.configure(mformat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
    }

}
