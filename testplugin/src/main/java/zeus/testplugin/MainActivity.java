package zeus.testplugin;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import zeus.plugin.ZeusBaseAppCompactActivity;

/**
 * Created by huangjian on 2016/7/8.
 */
public class MainActivity extends ZeusBaseAppCompactActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        String version = "1";
        String version = "2";

        TextView textView= (TextView) findViewById(R.id.action0);
        //内置插件zeusplugin_test, 记得assets下的zeusplugin.meta里的version也得改为对应的版本
        textView.setText("这是插件,版本为" + version);
        getSupportActionBar().setTitle("插件,版本为" + version);
    }
}
