package zeus.plugin;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.view.LayoutInflater;

/**
 * 一些重复的方法放到这里。
 * Created by huangjian on 2016/7/14.
 */
public class ZeusHelper {

    //---------------------插件相关的代码-----------------------start
    /**
     * 一旦插件resources发生变化，这个resources就可以用来比较了
     */
    private Resources mMyResources = null;

    /**
     * 配置LAYOUT_INFLATER_SERVICE时的一些参数
     *
     * @param context       调用着的context
     * @param systemServcie systemServer对象
     * @param name          server的名字
     * @return systemServer对象
     */
    public static Object getSystemService(Context context, Object systemServcie, String name) {
        if (Context.LAYOUT_INFLATER_SERVICE.equals(name)) {
            LayoutInflater inflater = (LayoutInflater) systemServcie;
            inflater.cloneInContext(context);
            //使用某些加固之后该inflater里的mContext变量一直是系统的context，根本不是当前Context
            //所以这里手动设置一次
            PluginUtil.setField(inflater, "mContext", context);
            return inflater;
        }
        return systemServcie;
    }

    /**
     * 当系统调用attachBaseContext时，进行一些参数的设置
     *
     * @param newBase base的context即ContextImpl
     * @param context 调用者自己
     */
    public void attachBaseContext(Context newBase, ContextWrapper context) {
        //某些手机中的是mOuterContext作为context来用
        //这样写还可以防止某些手机的内存泄漏,有些手机会记录它启动当前界面的activity作为mOuterContext，
        //而如果之前的activity被finish，那么它也不能被GC回收
        PluginUtil.setField(newBase, "mOuterContext", context);
        //中兴手机是个奇葩，不知道它怎么实现的又重新生成了一个resources,这里得再次替换
        PluginUtil.setField(newBase, "mResources", PluginManager.mNowResources);
        mMyResources =  PluginManager.mNowResources;
    }

    /**
     * 解决有时插件通过inflate找不到资源的问题
     *
     * @return Resources.Theme 调用者自己生成的theme
     */
    public Resources.Theme getTheme(ZeusBaseActivity zeusBaseActivity) {
        Resources localResources = PluginManager.mNowResources;
        if ( localResources != null && mMyResources != localResources) {
            mMyResources = localResources;
            PluginUtil.setField(zeusBaseActivity.getBaseContext(), "mResources", localResources);
            //AppCompatActivity包含了一个Resouces，这里设置为null让其再次生成一遍
            PluginUtil.setField(zeusBaseActivity, "mResources", null);
            //原始的theme指向的Resources是老的Resources，无法访问新插件，这里设置为null，
            // 系统会再次使用新的Resouces来生成一次theme，新的theme才能访问新的插件资源
            PluginUtil.setField(zeusBaseActivity, "mTheme", null);
            PluginUtil.setField(zeusBaseActivity.getBaseContext(), "mTheme", null);
        }
        return zeusBaseActivity.getSuperTheme();
    }

    /**
     * 系统配置改变时的回调，是为了支持插件的语言、地区、字体、字号等的切换
     */
    public static void onConfigurationChanged() {
        if (PluginManager.mNowResources != null
                && PluginManager.mBaseResources != null
                && PluginManager.mNowResources != PluginManager.mBaseResources) {
            PluginManager.mNowResources.updateConfiguration(PluginManager.mBaseResources.getConfiguration(),
                    PluginManager.mBaseResources.getDisplayMetrics());
        }
    }
}
