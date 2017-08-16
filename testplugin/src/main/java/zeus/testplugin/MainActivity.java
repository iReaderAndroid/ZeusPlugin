package zeus.testplugin;

import android.os.Bundle;

import zeus.plugin.ZeusBaseActivity;
import zeus.test.StringConstant;
import zeus.test.plugin.TestView;

/**
 * Created by huangjian on 2016/7/8.
 */
public class MainActivity extends ZeusBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        String version = "1";
        String version = "2";

        TestView textView = (TestView) findViewById(R.id.action0);
        //内置插件zeusplugin_test, 记得assets下的zeusplugin.meta里的version也得改为对应的版本
        textView.setText("这是插件,版本为" + version + " \n" + getResources().getString(StringConstant.string1));
        setTitle("插件,版本为" + version);
    }
}
