package zeus.testplugin;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import zeus.plugin.ZeusBaseAppCompactActivity;

/**
 * Created by adison on 16/8/21.
 */
public class AssetPluginActiviy extends ZeusBaseAppCompactActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //内置插件
        TextView textView=new TextView(this);
        textView.setTextColor(getResources().getColor(android.R.color.black));
        textView.setTextSize(18);
        textView.setGravity(Gravity.CENTER);
        textView.setText("这是内置插件");
        setContentView(textView);
        getSupportActionBar().setTitle("内置插件");

    }
}
