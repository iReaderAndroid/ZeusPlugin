package zeus.plugin;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * 插件管理类，管理插件的初始化、安装、卸载、加载等等
 * 所有的方法和对象都为静态。
 * <p/>
 * Created by huangjian on 2016/6/21.
 */
public class PluginManager {

    private static final String PLUGIN_INSTALLED_LIST_FILE_NAME = "plugin.installedlist"; //已安装插件列表的文件地址

    /*start---这些对象的强引用系统也会一直持有，所以这里不会有内存泄漏---start*/
    public static volatile ClassLoader mNowClassLoader = null;          //正在使用的ClassLoader
    public static volatile ClassLoader mBaseClassLoader = null;         //系统原始的ClassLoader
    public static volatile Resources mNowResources;                     //正在使用的Resources
    public static volatile Resources mBaseResources;                    //原始的resources
    public static volatile Context mBaseContext;                        //原始的application中的BaseContext，不能是其他的，否则会内存泄漏
    private static Object mPackageInfo = null;                          //ContextImpl中的LoadedAPK对象mPackageInfo
    /*end---这些对象的强引用系统也会一直持有，所以这里不会有内存泄漏---end*/

    private static HashMap<String, PluginManifest> mInstalledPluginList = null; //已安装的插件列表
    private static HashMap<String, PluginManifest> mLoadedPluginList = null;    //已加载的插件列表
    private static HashMap<String, String> mLoadedDiffPluginPathinfoList = null;
    private static ArrayList<String> mOrgAssetPaths = new ArrayList<>();

    private static final Object mInstalledPluginListLock = new Object(); //插件安装的锁，支持多线程安装
    private static final Object mLoadedPluginListLock = new Object();    //修改已加载的插件对象的锁
    private static final Object mLoadLock = new Object();                //插件加载的锁，支持多线程加载

    private static boolean isIniteInstallPlugins = false;                //插件是否已经初始化

    private static HashMap<String, Integer> mDefaultList;
    /**
     * 缓存插件的对象
     */
    private static HashMap<String, ZeusPlugin> mPluginMap = new HashMap<>();

    /**
     * 得在插件相关的方法调用之前调用
     *
     * @param application application
     */
    public static void init(Application application, HashMap<String, Integer> defaultList) {
        if(defaultList == null){
            mDefaultList = new HashMap<>();
        }else{
            mDefaultList = defaultList;
        }
        //兼容7.0创建webview的时候会重新设置Resouces
        try{
            new WebView(application);
        }catch (Throwable e){
        }
        //初始化一些成员变量和加载已安装的插件
        mPackageInfo = PluginUtil.getField(application.getBaseContext(), "mPackageInfo");
        mBaseContext = application.getBaseContext();
        mNowClassLoader = mBaseContext.getClassLoader();
        mBaseClassLoader = mBaseContext.getClassLoader();
        mNowResources = mBaseContext.getResources();
        mBaseResources = mNowResources;
        initOrgAssetPaths(mBaseContext);
        //更改系统的Instrumentation对象，以便创建插件的activity
        Object mMainThread = PluginUtil.getField(mBaseContext, "mMainThread");
        PluginUtil.setField(mMainThread, "mInstrumentation", new ZeusInstrumentation());
        //创建插件的相关文件夹目录
        createPath();
        //加载已安装过的插件
        loadInstalledPlugins();
        //清除老版本的插件，最好放到软件退出时调用，防止让启动速度变慢
        clearOldPlugin();
        //安装内置插件
        Thread initPluginThread = new Thread(new Runnable() {
            @Override
            public void run() {
                installInitPlugins();
            }
        });
        initPluginThread.setName("initPluginThread");
        initPluginThread.start();
    }

    private static void createPath() {
        PluginUtil.createDir(PluginUtil.getInsidePluginPath());
    }

    private static void initOrgAssetPaths(Context baseContext) {
        try {
            mOrgAssetPaths.clear();
            AssetManager assetManager = baseContext.getAssets();
            Method getStringBlockCountMethod = PluginUtil.getMethod(assetManager.getClass(), "getStringBlockCount");
            Method getCookieNameMethod = PluginUtil.getMethod(assetManager.getClass(), "getCookieName", Integer.TYPE);
            int num = (Integer) getStringBlockCountMethod.invoke(assetManager, new Object[0]);
            String str;
            for (int i = 1; i <= num; i++) {
                str = (String) getCookieNameMethod.invoke(assetManager, i);
                if (!mOrgAssetPaths.contains(str)) {
                    mOrgAssetPaths.add(str);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    /**
     * 安装初始的内置插件
     */
    private static void installInitPlugins() {
        HashMap<String, PluginManifest> installedList = getInstalledPlugin();
        HashMap<String, Integer> defaultList = getDefaultPlugin();
        for (String key : defaultList.keySet()) {
            int installVersion = -1;
            int defaultVersion = defaultList.get(key);

            if (installedList.containsKey(key)) {
                installVersion = installedList.get(key).getVersion();
            }

            ZeusPlugin plugin = getPlugin(key);
            if (defaultVersion > installVersion) {
                boolean ret = plugin.installAssetPlugin();
                //提前将dex文件优化为odex或者opt文件
                if (ret) {
                    try {
                        new DexClassLoader(PluginUtil.getAPKPath(key), PluginUtil.getDexCacheParentDirectPath(key), null, mBaseClassLoader.getParent());
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 获取已安装的插件列表
     *
     * @return 已安装的插件列表
     */
    public static HashMap<String, PluginManifest> getInstalledPlugin() {
        if (mInstalledPluginList != null) {
            return mInstalledPluginList;
        } else {
            return mInstalledPluginList = readInstalledPlugin();
        }
    }

    /**
     * 获取某插件是否安装
     *
     * @param pluginId 插件ID
     * @return 某插件是否安装
     */
    public static boolean isInstall(String pluginId) {
        return isInstall(pluginId, -1);
    }

    /**
     * 获取某插件是否安装
     *
     * @param pluginId 插件ID
     * @return 某插件是否安装
     */
    public static boolean isInstall(String pluginId, int version) {
        getInstalledPlugin();
        return mInstalledPluginList != null && mInstalledPluginList.containsKey(pluginId) && mInstalledPluginList.get(pluginId).getVersion() >= version;
    }

    /**
     * 获取已经被加载到系统中的插件列表
     *
     * @return 已经被加载到系统中的插件列表
     */
    public static HashMap<String, PluginManifest> getLoadedPlugin() {
        return mLoadedPluginList;
    }

    /**
     * 判断某个插件是否已经被加载
     *
     * @param pluginId 插件ID
     * @return 某个插件是否已经被加载
     */
    public static boolean isLoaded(String pluginId) {
        return mLoadedPluginList != null && mLoadedPluginList.containsKey(pluginId);
    }

    private static void putLoadedPlugin(String pluginId, PluginManifest pluginManifest, String pathinfo) {
        synchronized (mLoadedPluginListLock) {
            if (mLoadedPluginList == null) {
                mLoadedPluginList = new HashMap<>();
            }
            if(mLoadedDiffPluginPathinfoList == null){
                mLoadedDiffPluginPathinfoList = new HashMap<>();
            }
            mLoadedPluginList.put(pluginId, pluginManifest);
            mLoadedDiffPluginPathinfoList.put(pluginId, pathinfo);
        }
    }

    /**
     * 获取内置插件列表
     *
     * @return 内置插件列表
     */
    public static HashMap<String, Integer> getDefaultPlugin() {
        return mDefaultList;
    }


    private static HashMap<String, PluginManifest> readInstalledPlugin() {
        synchronized (mInstalledPluginListLock) {
            HashMap<String, PluginManifest> pluginList = new HashMap<>();

            String path = getInstalledPluginListFilePath();
            File file = new File(path);
            if (file.exists()) {
                try {
                    InputStream inputStream = new FileInputStream(file);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();

                    byte[] buffer = new byte[512];
                    int length;
                    int count = 0;
                    while ((length = inputStream.read(buffer, 0, 512)) > 0) {
                        bos.write(buffer, 0, length);
                        count += length;
                    }
                    if (count > 0) {
                        String result = new String(bos.toByteArray(), "UTF-8");
                        PluginUtil.close(inputStream);
                        PluginUtil.close(bos);
                        JSONArray jObject = new JSONArray(result);
                        int jLength = jObject.length();
                        for (int i = 0; i < jLength; i++) {
                            JSONObject f = jObject.getJSONObject(i);
                            String id = f.optString("id", "");
                            //兼容老版本，新版本中version字段已不存在
                            int version = f.optInt("version", 1);
                            String meta = f.optString("meta","");
                            PluginManifest pluginManifest;
                            if(!TextUtils.isEmpty(meta)){
                                pluginManifest = new PluginManifest(meta);
                            }else{
                                pluginManifest = new PluginManifest();
                                pluginManifest.version = String.valueOf(version);
                                pluginManifest.name = id;
                            }
                            if (!TextUtils.isEmpty(id)) {
                                pluginList.put(id, pluginManifest);
                            }
                        }
                    }
                    PluginUtil.close(inputStream);
                    PluginUtil.close(bos);
                } catch (Exception e) {
                    e.printStackTrace();
                    return pluginList;
                }
            }
            return pluginList;
        }
    }

    protected static boolean addInstalledPlugin(String pluginId, PluginManifest pluginManifest) {
        if (getInstalledPlugin() != null) {
            synchronized (mInstalledPluginListLock) {
                mInstalledPluginList.put(pluginId, pluginManifest);
            }
            save();
            return true;
        }
        return false;
    }

    private static boolean save() {
        synchronized (mInstalledPluginListLock) {
            if (mInstalledPluginList == null) return true;
            FileOutputStream out = null;
            try {
                JSONArray array = new JSONArray();
                for (String key : mInstalledPluginList.keySet()) {
                    //新版本中不再使用此字段
//                    int version = mInstalledPluginList.get(key).getVersion();
                    JSONObject obj = new JSONObject();
                    obj.put("id", key);
//                    obj.put("version", version);
                    obj.put("meta",mInstalledPluginList.get(key).toString());
                    array.put(obj);
                }
                String result = array.toString();

                String path = getInstalledPluginListFilePath();
                File file = new File(path);

                if (!file.exists()) {
                    file.createNewFile();
                }
                out = new FileOutputStream(file);
                out.write(result.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                PluginUtil.close(out);
            }
        }
        return true;
    }

    private static String getInstalledPluginListFilePath() {
        return PluginUtil.getInsidePluginPath() + PLUGIN_INSTALLED_LIST_FILE_NAME;
    }

    /**
     * 加载某个插件，如果已经加载了直接返回
     * 如果已经加载了老版本插件，而新版本插件也安装了，此方法也不会加载最新版本的插件
     *
     * @param pluingId 插件的ID
     * @return 是否加载成功
     */
    public static boolean loadPlugin(String pluingId) {
        return loadLastVersionPlugin(pluingId);
    }

    /**
     * 加载最新版本的插件，如果老版本的该插件已经被加载过，则清除之前的版本后再加载最新版本。
     * 此方法不可在插件的使用过程中调用，只能在插件运行之前，或插件退出之后。
     * 如果运行过程中可能会出现class不一致的情况，但也不是一定的，有的插件还是可以运行时调用的，
     * 但通常生效都得下次进入插件时才生效，但不必退出软件再进才生效
     *
     * @param pluingId 插件的ID
     * @return 是否加载成功
     */
    public static boolean loadLastVersionPlugin(String pluingId) {
        ZeusPlugin plugin = getPlugin(pluingId);
        PluginManifest meta = plugin.getPluginMeta();
        int version = -1;
        if (meta != null) {
            version = Integer.valueOf(meta.version);
        }
        return loadPlugin(pluingId, version);
    }

    /**
     * 加载指定版本的插件
     *
     * @param pluginId 插件ID
     * @param version  要加载插件的版本号
     * @return 是否加载成功
     */
    public static boolean loadPlugin(String pluginId, int version) {
        synchronized (mLoadLock) {
            if (getLoadedPlugin() != null && getLoadedPlugin().get(pluginId) != null && getLoadedPlugin().get(pluginId).getVersion() >= version) {
                return true;
            }
            String pathInfo = PluginUtil.getInstalledPathInfo(pluginId);
            String pluginApkPath = PluginUtil.getAPKPath(pluginId, pathInfo);
            ZeusPlugin plugin = getPlugin(pluginId);
            if (!PluginUtil.exists(pluginApkPath)) {
                if (getDefaultPlugin().containsKey(pluginId)) {
                    if (!plugin.installAssetPlugin()) {
                        return false;
                    } else {
                        pluginApkPath = PluginUtil.getAPKPath(pluginId);
                    }
                } else {
                    return false;
                }
            }

            PluginManifest meta = plugin.getPluginMeta();
            if (meta == null || Integer.valueOf(meta.version) < version || !isSupport(pluginId)) return false;

            ClassLoader cl = mNowClassLoader;
            if (PluginUtil.isHotFix(pluginId)) {
                loadHotfixPluginClassLoader(pluginId);
            } else {
                //如果一个老版本的插件已经被加载了，则需要先移除
                if (getLoadedPlugin() != null && getLoadedPlugin().containsKey(pluginId)) {
                    if (cl instanceof ZeusClassLoader) {
                        ZeusClassLoader classLoader = (ZeusClassLoader) cl;
                        //移除老版本的插件
                        classLoader.removePlugin(pluginId);
                        //添加新版本的插件
                        classLoader.addAPKPath(pluginId, pluginApkPath, PluginUtil.getLibFileInside(pluginId));
                    }
                } else {
                    if (cl instanceof ZeusClassLoader) {
                        ZeusClassLoader classLoader = (ZeusClassLoader) cl;
                        classLoader.addAPKPath(pluginId, pluginApkPath, PluginUtil.getLibFileInside(pluginId));
                    } else {
                        ZeusClassLoader classLoader = new ZeusClassLoader(cl);
                        classLoader.addAPKPath(pluginId, pluginApkPath, PluginUtil.getLibFileInside(pluginId));
                        PluginUtil.setField(mPackageInfo, "mClassLoader", classLoader);
                        Thread.currentThread().setContextClassLoader(classLoader);
                        mNowClassLoader = classLoader;
                    }
                }
                putLoadedPlugin(pluginId, meta, pathInfo);
            }
            clearViewConstructorCache();
            if ((meta.getFlag() & PluginManifest.FLAG_WITHOUT_RESOURCES) != PluginManifest.FLAG_WITHOUT_RESOURCES) {
                reloadInstalledPluginResources();
            }
        }
        return true;
    }

    private static boolean isSupport(String pluginId){
        int versionCode = -1;
        try {
            versionCode = mBaseContext.getPackageManager().getPackageInfo(mBaseContext.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        PluginManifest manifest = getPlugin(pluginId).getPluginMeta();
        int maxVersion = TextUtils.isEmpty(manifest.maxVersion) ? Integer.MAX_VALUE : Integer.valueOf(manifest.maxVersion);
        int minVersion = TextUtils.isEmpty(manifest.minVersion) ? -1 : Integer.valueOf(manifest.minVersion);
        //如果宿主不支持该插件或补丁则不加载
        return !(versionCode != -1 &&
                (versionCode > maxVersion ||
                        versionCode < minVersion));
    }
    /**
     * 系统会记录当前已经加载过的view的所有的构造函数，避免反射，提高性能
     * 但是我们插件一旦更新后，view的构造函数就已经无效了，所以要清理这些缓存
     */
    private static void clearViewConstructorCache() {
        try {
            Field field = LayoutInflater.class.getDeclaredField("sConstructorMap");
            field.setAccessible(true);
            Map map = (Map) field.get(null);
            map.clear();
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException e1) {
            e1.printStackTrace();
        }
    }

    private static void clearCacheObject(Object drawableCache) {
        if (drawableCache == null) return;
        Method clearMethod = PluginUtil.getMethod(drawableCache.getClass(), "clear");
        if (clearMethod != null) {
            try {
                clearMethod.invoke(drawableCache);
            } catch (Exception ignored) {

            }
        }
        //高android版本走这里
        Object mThemedEntries = PluginUtil.getField(drawableCache, "mThemedEntries");
        if (mThemedEntries != null) {
            clearMethod = PluginUtil.getMethod(mThemedEntries.getClass(), "clear");
            if (clearMethod != null) {
                try {
                    clearMethod.invoke(mThemedEntries);
                } catch (Exception ignored) {

                }
            }
        }
    }

    /**
     * 清除resources中的图片缓存，降低内存消耗的一个办法
     * 不调用也不会产生严重问题
     *
     * @param resouces resouces
     */
    static private void clearResoucesDrawableCache(Object resouces) {
        if (resouces == null) return;
        Method flushLayoutCacheMethod = PluginUtil.getMethod(resouces.getClass(), "flushLayoutCache");
        if (flushLayoutCacheMethod != null) {
            try {
                flushLayoutCacheMethod.invoke(resouces);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        clearCacheObject(PluginUtil.getField(resouces, "mDrawableCache"));
        clearCacheObject(PluginUtil.getField(resouces, "mColorDrawableCache"));
        clearCacheObject(PluginUtil.getField(resouces, "mColorStateListCache"));
        clearCacheObject(PluginUtil.getField(resouces, "mAnimatorCache"));
        clearCacheObject(PluginUtil.getField(resouces, "mStateListAnimatorCache"));

        //清除对象池中的缓存
        Object typedArrayPool = PluginUtil.getField(resouces, "mTypedArrayPool");
        if (typedArrayPool != null) {
            Method acquirePath = PluginUtil.getMethod(typedArrayPool.getClass(), "acquire");
            try {
                while (acquirePath.invoke(typedArrayPool) != null) ;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 加载所有已安装的插件的资源，并清除资源中的缓存
     */
    private static void reloadInstalledPluginResources() {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            if (mOrgAssetPaths.size() != 0) {
                for (String orgAssetPath : mOrgAssetPaths) {
                    addAssetPath.invoke(assetManager, orgAssetPath);
                }
            } else {
                addAssetPath.invoke(assetManager, mBaseContext.getPackageResourcePath());
            }
            if (mLoadedPluginList != null && mLoadedPluginList.size() != 0) {
                //每个插件的packageID都不能一样
                for (String id : mLoadedPluginList.keySet()) {
                    //只有带有资源的补丁才会执行添加到assetManager中
                    PluginManifest manifest = mLoadedPluginList.get(id);
                    if (manifest.hasResoures()) {
                        addAssetPath.invoke(assetManager, PluginUtil.getAPKPath(id, mLoadedDiffPluginPathinfoList.get(id)));
                    }
                }
            }
            //这里提前创建一个resource是因为Resources的构造函数会对AssetManager进行一些变量的初始化
            //还不能创建系统的Resources类，否则中兴系统会出现崩溃问题
            Resources newResources = new Resources(assetManager,
                    mBaseContext.getResources().getDisplayMetrics(),
                    mBaseContext.getResources().getConfiguration());

            PluginUtil.setField(mBaseContext, "mResources", newResources);
            PluginUtil.setField(mPackageInfo, "mResources", newResources);

            //清除一下之前的resource的数据，释放一些内存
            //因为这个resource有可能还被系统持有着，内存都没被释放
            clearResoucesDrawableCache(mNowResources);

            mNowResources = newResources;
            //需要清理mtheme对象，否则通过inflate方式加载资源会报错
            PluginUtil.setField(mBaseContext, "mTheme", null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    /**
     * 清除已经加载过的插件
     *
     * @param pluginId 插件id
     */
    private static void removeLoadedPlugin(String pluginId) {
        synchronized (mLoadLock) {
            if (!isLoaded(pluginId)) return;
            if (mLoadedPluginList == null) return;
            mLoadedPluginList.remove(pluginId);
            clearViewConstructorCache();
            if (mLoadedPluginList.size() == 0) {
                PluginUtil.setField(mPackageInfo, "mClassLoader", mBaseClassLoader);
                Thread.currentThread().setContextClassLoader(mBaseClassLoader);
                mNowClassLoader = mBaseClassLoader;
                Thread.currentThread().setContextClassLoader(mBaseClassLoader);
            } else {
                ClassLoader cl = mNowClassLoader;
                if (!(cl instanceof ZeusClassLoader)) return;
                ZeusClassLoader classLoader = (ZeusClassLoader) cl;
                classLoader.removePlugin(pluginId);
            }
            reloadInstalledPluginResources();
        }
    }

    /**
     * 从安装列表中删除一个插件，并从内存中清除加载的插件
     *
     * @param pluginId 插件ID
     * @return true表明删除成功，false表明出现了异常
     */
    protected static boolean unInstalledPlugin(String pluginId) {
        removeLoadedPlugin(pluginId);
        if (getInstalledPlugin() != null) {
            synchronized (mInstalledPluginListLock) {
                mInstalledPluginList.remove(pluginId);
            }
            save();
            return true;
        }
        return false;
    }

    /**
     * 清除老版本的插件，软件一启动或者软件退出时调用最佳
     */
    public static void clearOldPlugin() {
        HashMap<String, PluginManifest> map = getInstalledPlugin();
        if (map != null) {
            for (String key : map.keySet()) {
                ZeusPlugin plugin = getPlugin(key);
                plugin.clearOldPlugin();
            }
        }
    }

    private synchronized static void loadHotfixPluginClassLoader(String pluginId) {
        //如果不是补丁，或者该补丁已经加载过，则不做处理
        if (!PluginUtil.isHotFix(pluginId) ||
                (getLoadedPlugin() != null && getLoadedPlugin().containsKey(pluginId))) {
            return;
        }
        HashMap<String, PluginManifest> installedPluginMaps = getInstalledPlugin();
        //如果开启了instant run，则需要更改为mBaseClassLoader.getParent();
        ClassLoader orgClassLoader = mBaseClassLoader;
        //以下这段是补丁框架为了兼容android studio 2.0版本以上在debug时的instant run功能，在打正式包的时候请删除这段无用代码
        //---start----
        if (mBaseClassLoader.getParent().getClass().getSimpleName().equals("IncrementalClassLoader")) {
            orgClassLoader = mBaseClassLoader.getParent();
        }
        //---end----
        ClassLoader classLoader = orgClassLoader.getParent();
        ZeusHotfixClassLoader hotfixClassLoader;
        String pathInfo = PluginUtil.getInstalledPathInfo(pluginId);
        if (classLoader instanceof ZeusHotfixClassLoader) {
            hotfixClassLoader = (ZeusHotfixClassLoader) classLoader;
            hotfixClassLoader.addAPKPath(PluginUtil.getAPKPath(pluginId, pathInfo),
                    PluginUtil.getLibFileInside(pluginId));
        } else {
            hotfixClassLoader = new ZeusHotfixClassLoader(PluginUtil.getAPKPath(pluginId, pathInfo),
                    PluginUtil.getDexCacheParentDirectPath(pluginId),
                    PluginUtil.getLibFileInside(pluginId),
                    classLoader);
            hotfixClassLoader.setOrgAPKClassLoader(orgClassLoader);
            PluginUtil.setField(orgClassLoader, "parent", hotfixClassLoader);
        }
        putLoadedPlugin(pluginId, installedPluginMaps.get(pluginId), pathInfo);
    }

    private static void loadInstalledPlugins() {
        synchronized (mLoadLock) {
            if (isIniteInstallPlugins) {
                return;
            }
            HashMap<String, PluginManifest> installedPluginMaps = getInstalledPlugin();
            if (installedPluginMaps.isEmpty()) {
                isIniteInstallPlugins = true;
                return;
            }
            //获取classloader设置classloader
            ZeusClassLoader classLoader = null;
            boolean isNeedLoadResource = false;

            for (String pluginId : installedPluginMaps.keySet()) {
                if(!isSupport(pluginId))continue;
                if (PluginUtil.isPlugin(pluginId)) {
                    if (classLoader == null) {
                        classLoader = new ZeusClassLoader(mBaseContext.getClassLoader());
                    }
                    String pathInfo = PluginUtil.getInstalledPathInfo(pluginId);
                    classLoader.addAPKPath(pluginId, PluginUtil.getAPKPath(pluginId, pathInfo), PluginUtil.getLibFileInside(pluginId));
                    putLoadedPlugin(pluginId, installedPluginMaps.get(pluginId), pathInfo);
                    isNeedLoadResource = true;
                }
                if (PluginUtil.isHotFix(pluginId)) {
                    loadHotfixPluginClassLoader(pluginId);
                    isNeedLoadResource = true;
                }
            }
            clearViewConstructorCache();
            if (!isNeedLoadResource) {
                isIniteInstallPlugins = true;
                return;
            }
            //设置原始APK所使用的ClassLoader
            if (classLoader != null) {
                PluginUtil.setField(mPackageInfo, "mClassLoader", classLoader);
                Thread.currentThread().setContextClassLoader(classLoader);
                mNowClassLoader = classLoader;
            }
            reloadInstalledPluginResources();
            isIniteInstallPlugins = true;
        }
    }

    /**
     * Application中以及activity中的getResources()方法都应该调用这个方法
     *
     * @return 返回正在使用的resources
     */
    public static Resources getResources() {
        return mNowResources;
    }

    /**
     * 获取最新插件对象，不存在就生成一个
     *
     * @param pluginId 插件的名称
     * @return 获取某个插件对象
     */
    public static ZeusPlugin getPlugin(String pluginId) {
        ZeusPlugin plugin = null;
        try {
            plugin = mPluginMap.get(pluginId);
            if (plugin != null) {
                return plugin;
            }
            if (PluginUtil.iszeusPlugin(pluginId)) {
                plugin = new ZeusPlugin(pluginId);
            }
            mPluginMap.put(pluginId, plugin);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return plugin;
    }


    public static void startActivity(Activity activity, Intent intent) {
        ComponentName componentName = intent.getComponent();
        intent.setClassName(componentName.getPackageName(), PluginConstant.PLUGIN_ACTIVITY_FOR_STANDARD);
        intent.putExtra(PluginConstant.PLUGIN_REAL_ACTIVITY, componentName.getClassName());
        activity.startActivity(intent);
    }

    public static void startActivity(Intent intent) {
        ComponentName componentName = intent.getComponent();
        intent.setClassName(componentName.getPackageName(), PluginConstant.PLUGIN_ACTIVITY_FOR_STANDARD);
        intent.putExtra(PluginConstant.PLUGIN_REAL_ACTIVITY, componentName.getClassName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mBaseContext.startActivity(intent);
    }
}
