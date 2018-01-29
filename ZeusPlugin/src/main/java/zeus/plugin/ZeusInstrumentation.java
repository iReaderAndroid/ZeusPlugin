package zeus.plugin;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * Created by huangjian on 2016/7/28.
 */
public class ZeusInstrumentation extends Instrumentation{

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if(intent != null){
            Bundle bundle = intent.getExtras();
            if(bundle != null){
                //给Bundle设置classLoader以使Bundle中序列化对象可以直接转化为插件中的对象
                //类似于在宿主中这么使用:TestInPlugin testInPlugin = (TestInPlugin)bundle.get("TestInPlugin");
                //TestInPlugin是在插件中定义的,如果不这么设置则会找不到TestInPlugin类
                bundle.setClassLoader(PluginManager.mNowClassLoader);
                if(className.equals("com.zeus.ZeusActivityForStandard")) {
                    String realActivity = bundle.getString(PluginConstant.PLUGIN_REAL_ACTIVITY);
                    if (!TextUtils.isEmpty(realActivity)) {
                        return super.newActivity(cl, realActivity, intent);
                    }
                }
            }
        }
        return super.newActivity(cl, className, intent);
    }
}
