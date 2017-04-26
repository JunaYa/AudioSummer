package audiosummer.github.me.audiosummer.media;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.io.IOException;

/**
 * @author slack
 * @time 17/2/13 下午4:09
 * video MediaCodec
 */

public class MediaVideoEncoder extends MediaEncoder {

    private static final String TAG = "MediaVideoEncoder";

    public MediaVideoEncoder(MediaMuxerMixAudioAndVideo mediaMuxer) {
        super(mediaMuxer);
    }

    //这里需要传进来一个编码时的 MediaInfo
    public void prepare(MediaInfo info) throws IOException {

        MediaFormat mformat = info.createVideoFormatCopy();
        mMediaCodec = null;
        mMediaCodec = MediaCodec.createEncoderByType(info.mVideoMime);
        mMediaCodec.configure(mformat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();

    }

}
