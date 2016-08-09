package zeus.plugin;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/**
 * 可以访问插件的资源resoureces
 * 如果插件里查找资源不是调用这个类的话，说明资源resources设置的有问题
 * <p>
 * Created by huangjian on 2016/6/21.
 */
class PluginResources extends Resources {

    public PluginResources(AssetManager assets, DisplayMetrics metrics,
                           Configuration config) {
        super(assets, metrics, config);
    }

    @Override
    public void getValue(int id, TypedValue outValue, boolean resolveRefs)
            throws NotFoundException {
        super.getValue(id, outValue, resolveRefs);
    }

    @Override
    public void getValue(String name, TypedValue outValue, boolean resolveRefs)
            throws NotFoundException {
        super.getValue(name, outValue, resolveRefs);
    }

    @Override
    public CharSequence getText(int id) throws NotFoundException {
        return super.getText(id);
    }

    @Override
    public String getString(int id) throws NotFoundException {
        return super.getString(id);
    }

    @Override
    public XmlResourceParser getLayout(int id) throws NotFoundException {
        return super.getLayout(id);
    }

    @Override
    public String getResourceName(int resid) throws NotFoundException {
        return super.getResourceName(resid);
    }

}
