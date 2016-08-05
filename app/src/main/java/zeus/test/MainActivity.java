package zeus.test;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.android.internal.util.Predicate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import zeus.plugin.BaseActivity;
import zeus.plugin.PluginConfig;
import zeus.plugin.PluginManager;
import zeus.plugin.PluginUtil;


/**
 * Created by huangjian on 2016/6/21.
 */
public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.test_plugin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PluginManager.loadLastVersionPlugin(PluginConfig.PLUGIN_TEST);
                try {
                    Class cl = PluginManager.mNowClassLoader.loadClass(PluginManager.getPlugin(PluginConfig.PLUGIN_TEST).getPluginMeta().mainClass);
                    Intent intent = new Intent(MainActivity.this, cl);
                    //这种方式为通过在宿主AndroidManifest.xml中预埋activity实现
//                    startActivity(intent);
                    //这种方式为通过欺骗android系统的activity存在性校验的方式实现
                    PluginManager.startActivity(MainActivity.this,intent);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.test_hotfix).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //第一次启动是宿主，后面就是补丁，这里的测试程序即使插件，也是补丁。
                HotfixTest.showText(MainActivity.this);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static String testPluginCallHost() {
        return "宿主";
    }
}
