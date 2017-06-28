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
        if(className.equals("com.zeus.ZeusActivityForStandard") && intent != null){
            Bundle bundle = intent.getExtras();
            if(bundle != null){
                String realActivity = bundle.getString(PluginConstant.PLUGIN_REAL_ACTIVITY);
                if(!TextUtils.isEmpty(realActivity)){
                    return super.newActivity(cl, realActivity, intent);
                }
            }
        }
        return super.newActivity(cl, className, intent);
    }
}
