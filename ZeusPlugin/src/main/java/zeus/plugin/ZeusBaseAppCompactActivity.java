package zeus.plugin;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 *  AppCompat基类
 * @author adison
 * @date 16/8/21
 * @time 上午12:47
 */
public class ZeusBaseAppCompactActivity extends AppCompatActivity{

    //---------------------插件相关的代码-----------------------start
    ZeusHelper helper = new ZeusHelper();

    @Override
    public Object getSystemService(String name) {
        return helper.getSystemService(this, super.getSystemService(name), name);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        ZeusHelper.attachBaseContext(newBase,this);
    }

    @Override
    public Resources getResources() {
        return PluginManager.getResources();
    }

    /**
     * 解决有时插件通过inflate找不到资源的问题
     * @return Resources.Theme
     */
    public Resources.Theme getTheme() {
        return helper.getTheme(super.getTheme());
    }
    //---------------------------插件相关代码-------------------------end

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
