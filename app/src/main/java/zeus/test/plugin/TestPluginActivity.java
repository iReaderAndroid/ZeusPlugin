package zeus.test.plugin;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;

import zeus.plugin.PluginManager;
import zeus.plugin.PluginUtil;
import zeus.plugin.ZeusBaseActivity;
import zeus.plugin.ZeusPlugin;
import zeus.test.MyApplication;
import zeus.test.R;

/**
 * 插件测试页面
 *
 * @author adison
 * @date 16/8/21
 * @time 上午1:09
 */
public class TestPluginActivity extends ZeusBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);
        setTitle("插件测试");
        findViewById(R.id.plugin_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlugin();
            }
        });

        findViewById(R.id.plugin_install).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installPlugin();
            }
        });
    }

    /**
     * 启动插件
     *
     */
    public void startPlugin() {
        PluginManager.loadLastVersionPlugin(MyApplication.PLUGIN_TEST);
        try {
            Class cl = PluginManager.mNowClassLoader.loadClass(PluginManager.getPlugin(MyApplication.PLUGIN_TEST).getPluginMeta().mainClass);
            Intent intent = new Intent(this, cl);
            //这种方式为通过在宿主AndroidManifest.xml中预埋activity实现
//            startActivity(intent);
            //这种方式为通过欺骗android系统的activity存在性校验的方式实现
            PluginManager.startActivity(this,intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 安装assets中高版本插件plugin_test_version2.apk
     * 先拷贝到PluginUtil.getZipPath(PluginConfig.PLUGIN_TEST)
     * 然后调用install()安装。
     *
     */
    public void installPlugin() {
        ZeusPlugin zeusPlugin = PluginManager.getPlugin(MyApplication.PLUGIN_TEST);
        FileOutputStream out = null;
        InputStream in = null;
        try {
            AssetManager am = PluginManager.mBaseResources.getAssets();
            in = am.open("zeusplugin_test_version2.apk");
            PluginUtil.createDirWithFile(PluginUtil.getZipPath(MyApplication.PLUGIN_TEST));
            out = new FileOutputStream(PluginUtil.getZipPath(MyApplication.PLUGIN_TEST), false);
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
            Toast.makeText(PluginManager.mBaseContext,"高版本插件安装成功",Toast.LENGTH_SHORT).show();
        }
    }


}
