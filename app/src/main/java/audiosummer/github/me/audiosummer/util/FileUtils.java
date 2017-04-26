package audiosummer.github.me.audiosummer.util;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileUtils {

    public static final String VOICE_EXTENSION = ".mp3";

    public static File getDiskCacheDir(Context context, String uniqueName) {
        boolean externalStorageAvailable = Environment
                .getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (externalStorageAvailable) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }

        return new File(cachePath + File.separator + uniqueName);
    }


    public static File getExternalCacheDir(Context context) {
        File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
        File appCacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
        if (!appCacheDir.exists()) {
            if (!appCacheDir.mkdirs()) {
                return null;
            }
            try {
                new File(appCacheDir, ".kids-box-data").createNewFile();
            } catch (IOException e) {
            }
        }
        return appCacheDir;
    }

    /**
     * create or get file save lyric
     *
     * @param context
     * @param url
     * @return
     */
    public static File getLyricFile(Context context, String url) {
        String fileName = url.substring(url.lastIndexOf("/"));
        String lyricFileStr = context.getFilesDir().getPath();
        File lyricFile = new File(lyricFileStr, fileName);
        return lyricFile;
    }

    public static File getMusicDir() {
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        if (!musicDir.exists()) {
            if (!musicDir.mkdir()) {
                return new File(Environment.getExternalStorageDirectory(), "KidsMusic");
            }
        }
        return musicDir;
    }


    public static File getDownloadSongFile(String songName) {
        File downloadSongFile = new File(getMusicDir().getAbsoluteFile(), songName + ".mp3");
        return downloadSongFile;
    }

    public static File getAPKFile(Context context, String url) {
        String fileName = url.substring(url.lastIndexOf("/")) + ".apk";
        String apkFileStr = context.getFilesDir().getPath();
        File apkFile = new File(apkFileStr, fileName);
        return apkFile;
    }


    public static boolean isApk(String url) {
        return url.toLowerCase().endsWith(".apk");
    }

    public static boolean isMusic(String url) {
        final String REGEX = "(.*/)*.+\\.(mp3|m4a|ogg|wav|aac)$";
        return url.matches(REGEX);
    }

    public static boolean isMusic(File file) {
        final String REGEX = "(.*/)*.+\\.(mp3|m4a|ogg|wav|aac)$";
        return file.getName().matches(REGEX);
    }

    /**
     * http://stackoverflow.com/a/5599842/2290191
     *
     * @param size Original file size in byte
     * @return Readable file size in formats
     */
    public static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"b", "kb", "M", "G", "T"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static List<Song> musicFiles(File dir) {
        List<Song> songs = new ArrayList<>();
        if (dir != null && dir.isDirectory()) {
            final File[] files = dir.listFiles(item -> item.isFile() && isMusic(item));
            for (File file : files) {
                Song song = fileToMusic(file);
                if (song != null) {
                    songs.add(song);
                }
            }
        }
        if (songs.size() > 1) {
            Collections.sort(songs, (left, right) -> left.title.compareTo(right.title));
        }
        return songs;
    }

    public static Song fileToMusic(File file) {
        if (file.length() == 0) return null;

        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(file.getAbsolutePath());
        final int duration;

        String keyDuration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        // ensure the duration is a digit, otherwise return null song
        if (keyDuration == null || !keyDuration.matches("\\d+")) return null;
        duration = Integer.parseInt(keyDuration);

        final String title = extractMetadata(metadataRetriever, MediaMetadataRetriever.METADATA_KEY_TITLE, file.getName());

        final Song song = new Song();
        song.title = title;
        song.url = file.getAbsolutePath();
        song.duration = duration;
        return song;
    }


    public static boolean deleteVoice(String name) {
        boolean delete = false;
        File dir = getMusicDir();
        if (dir != null && dir.isDirectory()) {
            final File[] files = dir.listFiles(item -> item.isFile() && isMusic(item));
            for (File file : files) {
                if (file.getName().replace(".mp3","").equals(name)) {
                    delete = file.delete();
                    break;
                }
            }
        }
        return delete;
    }

    public static File filterDownloadMusic(String name) {
        File dir = getMusicDir();
        if (dir != null && dir.isDirectory()) {
            final File[] files = dir.listFiles(item -> item.isFile() && isMusic(item));
            for (File file : files) {
                if (file.getName().replace(VOICE_EXTENSION,"").equals(name)) {
                    return file;
                }
            }
        }
        return null;
    }

    private static String extractMetadata(MediaMetadataRetriever retriever, int key, String defaultValue) {
        String value = retriever.extractMetadata(key);
        if (TextUtils.isEmpty(value)) {
            value = defaultValue;
        }
        return value;
    }
}
