package zeus.test;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by huangjian on 2016/7/8.
 */
public class HotfixTest {
    public static void showText(Context context) {
        Toast.makeText(context, "这是宿主调用" + MainActivity.testPluginCallHost(), Toast.LENGTH_SHORT).show();
    }
}
