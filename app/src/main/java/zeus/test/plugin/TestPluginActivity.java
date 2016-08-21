package zeus.test.plugin;

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;

import zeus.plugin.PluginConfig;
import zeus.plugin.PluginManager;
import zeus.plugin.PluginUtil;
import zeus.plugin.ZeusBaseAppCompactActivity;
import zeus.plugin.ZeusPlugin;
import zeus.test.R;

/**
 * 插件测试页面
 *
 * @author adison
 * @date 16/8/21
 * @time 上午1:09
 */
public class TestPluginActivity extends ZeusBaseAppCompactActivity {
    private static final String PLUGIN_ID = "zeusplugin_test";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);
        getSupportActionBar().setTitle("插件测试");
    }

    /**
     * 插件安装
     *
     * @param view
     */
    public void pluginInstall(View view) {
        ZeusPlugin zeusPlugin = new ZeusPlugin(PLUGIN_ID);
        FileOutputStream out = null;
        InputStream in = null;
        try {
            AssetManager am = PluginManager.mBaseResources.getAssets();
            in = am.open("plugin_test.apk");
            PluginUtil.createDirWithFile(PluginUtil.getZipPath(PLUGIN_ID));
            out = new FileOutputStream(PluginUtil.getZipPath(PLUGIN_ID), false);
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

        boolean installed=zeusPlugin.install();
        if(installed){
            Toast.makeText(PluginManager.mBaseContext,"插件安装成功",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 内置插件
     *
     * @param view
     */
    public void pluginAsset(View view) {
        PluginManager.loadLastVersionPlugin(PluginConfig.PLUGIN_TEST);
        try {
            Class cl = PluginManager.mNowClassLoader.loadClass(PluginManager.getLastPlugin(PluginConfig.PLUGIN_TEST).getPluginMeta().mainClass);
            Intent intent = new Intent(this, cl);
            //这种方式为通过在宿主AndroidManifest.xml中预埋activity实现
            startActivity(intent);
            //这种方式为通过欺骗android系统的activity存在性校验的方式实现
//            PluginManager.startActivity(this,intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 插件启动
     *
     * @param view
     */
    public void pluginLaunch(View view) {
        PluginManager.loadLastVersionPlugin(PLUGIN_ID);
        Class cl = null;
        try {
            cl = PluginManager.mNowClassLoader.loadClass(PluginManager.getLastPlugin(PLUGIN_ID).getPluginMeta().mainClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (cl != null) {
            Intent intent = new Intent(TestPluginActivity.this, cl);
            startActivity(intent);
        }else{
            Toast.makeText(PluginManager.mBaseContext,"请先安装插件",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 插件升级
     *
     * @param view
     */
    public void pluginUpdate(View view) {
        ZeusPlugin zeusPlugin = new ZeusPlugin(PLUGIN_ID);
        FileOutputStream out = null;
        InputStream in = null;
        try {
            AssetManager am = PluginManager.mBaseResources.getAssets();
            in = am.open("plugin_test_update.apk");
            PluginUtil.createDirWithFile(PluginUtil.getZipPath(PLUGIN_ID));
            out = new FileOutputStream(PluginUtil.getZipPath(PLUGIN_ID), false);
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

       boolean installed= zeusPlugin.install();
        if(installed){
            Toast.makeText(PluginManager.mBaseContext,"插件升级成功",Toast.LENGTH_SHORT).show();
        }
    }
}
