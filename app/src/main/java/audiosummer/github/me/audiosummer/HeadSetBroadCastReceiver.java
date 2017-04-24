package audiosummer.github.me.audiosummer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by hongen-android003 on 21/04/2017.
 */

public class HeadSetBroadCastReceiver extends BroadcastReceiver {

    private Context mContext;
    private static final String TAG = "HeadSetBroadCastReceiver";

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (intent.hasExtra("state")) {
            if (intent.getIntExtra("state", 2) == 0) {
                Log.d(TAG, "拔出");
                show("拔出");
            } else if (intent.getIntExtra("state", 2) == 1) {
                show("插入");
            }
        }

        if (intent.hasExtra("name")) {
            Log.d(TAG, "onReceive: " + intent.getStringExtra("name"));
            show("name" + intent.getStringExtra("name"));
        }
        if (intent.hasExtra("microphone")) {
            if (intent.getIntExtra("microphone", 2) == 0) {
                show("no microphone");
            } else if (intent.getIntExtra("microphone", 2) == 1) {
                show("has microphone");
            }
        }
    }

    private void show(String string) {
        Toast.makeText(mContext, string, Toast.LENGTH_SHORT).show();
    }
}
