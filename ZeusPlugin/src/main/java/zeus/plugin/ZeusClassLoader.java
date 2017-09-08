package zeus.plugin;

import android.text.TextUtils;

import static java.lang.System.arraycopy;

/***
 * 这是一个空ClassLoader，主要是个容器
 * <p>
 * Created by huangjian on 2016/6/21.
 */
class ZeusClassLoader extends ClassLoader {
    //这里每个插件对应着一个ClassLoader，一旦插件更新了，则classLoader也会使用新的。
    //这样java的class就会从新的classLoader中查找，而不会去使用旧的classLoader的缓存
    private ZeusPluginClassLoader[] mClassLoader = null;

    public ZeusClassLoader(ClassLoader parent) {
        super(parent);
    }

    public ZeusPluginClassLoader[] getClassLoaders() {
        return mClassLoader;
    }

    /**
     * 添加一个插件到当前的classLoader中
     *
     * @param pluginId 插件名称
     * @param dexPath dex文件路径
     * @param libPath so文件夹路径
     */
    protected void addAPKPath(String pluginId, String dexPath, String libPath) {
        if (mClassLoader == null) {
            mClassLoader = new ZeusPluginClassLoader[1];
        } else {
            int oldLenght = mClassLoader.length;
            Object[] old = mClassLoader;
            mClassLoader = new ZeusPluginClassLoader[oldLenght + 1];
            arraycopy(old, 0, mClassLoader, 0, oldLenght);
        }
        mClassLoader[mClassLoader.length - 1] = new ZeusPluginClassLoader(pluginId, dexPath,
                PluginUtil.getDexCacheParentDirectPath(pluginId),
                libPath,
                getParent());
    }

    /**
     * 移除一个插件classLoader
     *
     * @param pluginId 插件id
     */
    protected void removePlugin(String pluginId) {
        if (mClassLoader == null || TextUtils.isEmpty(pluginId)) return;
        for (int i = 0; i < mClassLoader.length; i++) {
            ZeusPluginClassLoader cl = mClassLoader[i];
            if (pluginId.equals(cl.getPluginId())) {
                if (mClassLoader.length == 1) {
                    mClassLoader = null;
                    return;
                }
                int oldLength = mClassLoader.length;
                Object[] old = mClassLoader;
                mClassLoader = new ZeusPluginClassLoader[oldLength - 1];
                if (i != 0) {
                    arraycopy(old, 0, mClassLoader, 0, i);
                }
                if (i != oldLength - 1) {
                    arraycopy(old, i + 1, mClassLoader, i, oldLength - i - 1);
                }
                return;
            }
        }
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = null;
        try {
            //先查找parent classLoader，这里实际就是系统帮我们创建的classLoader，目标对应为宿主apk
            clazz = getParent().loadClass(className);
        } catch (ClassNotFoundException ignored) {

        }

        if (clazz != null) {
            return clazz;
        }

        //挨个的到插件里进行查找
        if (mClassLoader != null) {
            for (ZeusPluginClassLoader classLoader : mClassLoader) {
                if (classLoader == null) continue;
                try {
                    //这里只查找插件它自己的apk，不需要查parent，避免多次无用查询，提高性能
                    clazz = classLoader.loadClassByself(className);
                    if (clazz != null) {
                        return clazz;
                    }
                } catch (ClassNotFoundException ignored) {

                }
            }
        }
        throw new ClassNotFoundException(className + " in loader " + this);
    }
}

