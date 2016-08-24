package zeus.test;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;

import zeus.plugin.PluginConfig;
import zeus.plugin.PluginManager;
import zeus.plugin.PluginUtil;
import zeus.plugin.ZeusBaseAppCompactActivity;
import zeus.plugin.ZeusPlugin;
import zeus.test.hotfix.TestHotFixActivity;
import zeus.test.plugin.TestPluginActivity;


/**
 * Created by huangjian on 2016/6/21.
 */
public class MainActivity extends ZeusBaseAppCompactActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 插件测试
     *
     * @param view
     */
    public void testPlugin(View view) {
        Intent intent = new Intent(MainActivity.this, TestPluginActivity.class);
        startActivity(intent);
    }

    /**
     * 补丁测试
     *
     * @param view
     */
    public void testHotfix(View view) {
        //第一次启动是宿主，应用补丁后就是补丁。
        Intent intent = new Intent(MainActivity.this, TestHotFixActivity.class);
        startActivity(intent);
    }

    /**
     * 应用补丁
     *
     * @param view
     */
    public void applyHotfix(View view) {
        if(PluginManager.isInstall(PluginConfig.HOTFIX_TEST)){
            Toast.makeText(this, "补丁"+PluginConfig.HOTFIX_TEST+"已经被安装,不用再次安装", Toast.LENGTH_SHORT).show();
            return;
        }
        ZeusPlugin zeusPlugin = PluginManager.getPlugin(PluginConfig.HOTFIX_TEST);
        FileOutputStream out = null;
        InputStream in = null;
        try {
            AssetManager am = PluginManager.mBaseResources.getAssets();
            in = am.open("zeushotfix_test.apk");
            PluginUtil.createDirWithFile(PluginUtil.getZipPath(PluginConfig.HOTFIX_TEST));
            out = new FileOutputStream(PluginUtil.getZipPath(PluginConfig.HOTFIX_TEST), false);
            byte[] temp = new byte[2048];
            int len;
            while ((len = in.read(temp)) > 0) {
                out.write(temp, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            PluginUtil.close(in);
            PluginUtil.close(out);
        }
        boolean result= zeusPlugin.install();
        if (result) {
            Toast.makeText(this, "补丁"+PluginConfig.HOTFIX_TEST+"安装成功,下次启动生效", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

}
