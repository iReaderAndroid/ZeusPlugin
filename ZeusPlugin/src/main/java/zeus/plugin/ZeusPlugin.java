package zeus.plugin;

import android.content.res.AssetManager;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import dalvik.system.DexClassLoader;

/**
 * 插件类，包括插件的安装、卸载、清除
 * Created by huangjian on 2016/6/21.
 */
public class ZeusPlugin {
    private String mPluginId;            //使用插件的安装目录作为插件id
    private String mInstalledPathInfo = "";            //安装插件的随机路径信息

    private boolean isInstalling = false;
    private boolean isAssetInstalling = false;

    protected ZeusPlugin(String pluginId) {
        mPluginId = pluginId;
    }

    public synchronized boolean install() {
        isInstalling = true;
        //创建插件安装目录
        PluginUtil.createDir(PluginUtil.getPlugDir(mPluginId));

        //将当前时间记录为插件的随机数，等效于android系统后面~1、~2等
        mInstalledPathInfo = String.valueOf(System.nanoTime());

        //获取插件apk文件
        String path = PluginUtil.getZipPath(mPluginId);

        //插件文件不存在，则安装asset中的默认插件。
        if (!PluginUtil.exists(path)) {
            if (PluginUtil.iszeusPlugin(mPluginId)) {
                isInstalling = false;
                mInstalledPathInfo = getInstalledPathInfoNoCache();
                return false;
            }
            return installAssetPlugin();
        }
        //把下载路径下的插件文件，直接重命名到安装目录，不需要耗时的拷贝过程。
        boolean ret = PluginUtil.rename(PluginUtil.getZipPath(mPluginId), getAPKPath(mPluginId));
        if (!ret) {
            isInstalling = false;
            mInstalledPathInfo = getInstalledPathInfoNoCache();
            return false;
        }

        //校验是否下载的是正确文件，如果插件下载错误则获取这个配置文件就会失败。
        PluginManifest meta = getPluginMeta();
        if (meta == null) {
            PluginUtil.deleteFile(new File(getAPKPath(mPluginId)));
            mInstalledPathInfo = getInstalledPathInfoNoCache();
            isInstalling = false;
            return false;
        }

        //拷贝so文件，一些插件是没有so文件，而这个方法耗时还稍微高点，所以对于没有so的插件和补丁是不会拷贝的。
        if (((meta.getFlag() & PluginManifest.FLAG_WITHOUT_SO_FILE) != PluginManifest.FLAG_WITHOUT_SO_FILE )&&
                !copySoFile(mInstalledPathInfo, PluginUtil.getCpuArchitecture())) {
            isInstalling = false;
            mInstalledPathInfo = getInstalledPathInfoNoCache();
            return false;
        }

        if (!PluginUtil.writePathInfo(mPluginId, mInstalledPathInfo)) {
            isInstalling = false;
            mInstalledPathInfo = getInstalledPathInfoNoCache();
            return false;
        }
        try {
            //预优化补丁dex的加载
            new DexClassLoader(getAPKPath(mPluginId), PluginUtil.getDexCacheParentDirectPath(mPluginId), "", PluginManager.mBaseClassLoader);
        }catch (Throwable e){
            e.printStackTrace();
        }
        PluginManager.addInstalledPlugin(mPluginId, meta);
        isInstalling = false;
        return true;
    }

    /**
     * 安装assets中的插件，assets中插件的文件名要为 mPluginId +".apk"
     *
     * @return 是否成功
     */
    public boolean installAssetPlugin() {
        PluginManifest meta;
        synchronized (this) {
            isAssetInstalling = true;
            Integer version = PluginManager.getDefaultPlugin().get(mPluginId);
            if (version == null || PluginManager.isInstall(mPluginId, version)) {
                isInstalling = false;
                return true;
            }
            PluginUtil.createDir(PluginUtil.getPlugDir(mPluginId));
            mInstalledPathInfo = String.valueOf(System.nanoTime());

            FileOutputStream out = null;
            InputStream in = null;
            try {
                AssetManager am = PluginManager.mBaseResources.getAssets();
                in = am.open(mPluginId + PluginConstant.PLUGIN_SUFF);
                PluginUtil.createDirWithFile(getAPKPath(mPluginId));
                out = new FileOutputStream(getAPKPath(mPluginId), false);
                byte[] temp = new byte[2048];
                int len;
                while ((len = in.read(temp)) > 0) {
                    out.write(temp, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
                mInstalledPathInfo = getInstalledPathInfoNoCache();
                isInstalling = false;
                return false;
            } finally {
                PluginUtil.close(in);
                PluginUtil.close(out);
            }

            meta = getPluginMeta();
            if (meta == null) {
                PluginUtil.deleteFile(new File(getAPKPath(mPluginId)));
                isAssetInstalling = false;
                mInstalledPathInfo = getInstalledPathInfoNoCache();
                return false;
            }

            //拷贝so文件
            if (!copySoFile(mInstalledPathInfo, PluginUtil.getCpuArchitecture())) {
                isInstalling = false;
                mInstalledPathInfo = getInstalledPathInfoNoCache();
                return false;
            }

            if (!PluginUtil.writePathInfo(mPluginId, mInstalledPathInfo)) {
                isAssetInstalling = false;
                mInstalledPathInfo = getInstalledPathInfoNoCache();
                return false;
            }
            isAssetInstalling = false;
        }
        try {
            //预优化补丁dex的加载
            new DexClassLoader(getAPKPath(mPluginId), PluginUtil.getDexCacheParentDirectPath(mPluginId), "", PluginManager.mBaseClassLoader);
        }catch (Throwable e){
            e.printStackTrace();
        }
        PluginManager.addInstalledPlugin(mPluginId, meta);
        return true;
    }

    /**
     * 清除之前版本的旧数据
     */
    public synchronized void clearOldPlugin() {
        if (getInstalledPathInfo() == null || isAssetInstalling || isInstalling) return;
        File pluginDir = new File(PluginUtil.getPlugDir(mPluginId));
        String installedPathInfo = getInstalledPathInfoNoCache();
        if (TextUtils.isEmpty(installedPathInfo)) return;
        if (pluginDir.exists() && pluginDir.isDirectory()) {
            File[] list = pluginDir.listFiles();
            if (list == null) return;
            for (File f : list) {
                String fileFullName = f.getName();
                if (fileFullName.endsWith(PluginConstant.PLUGIN_JAR_SUFF) || fileFullName.endsWith(PluginConstant.PLUGIN_SUFF)) {
                    String fileName = fileFullName.substring(0, fileFullName.lastIndexOf("."));
                    if (!fileName.equalsIgnoreCase(installedPathInfo)) {
                        f.delete();
                        File dir = new File(f.getParent() + "/" + fileName);
                        PluginUtil.deleteDirectory(dir);

                        File cacheFile = new File(PluginUtil.getDexCacheFilePath(mPluginId, fileName));
                        if (cacheFile.exists()) {
                            cacheFile.delete();
                        }
                    }
                }
            }
        }
    }

    /**
     * 将插件中的lib库拷贝入手机内存中。
     * 根据当前手机cpu的类型拷贝合适的lib库入手机内存中
     *
     * @param installedPathInfo 安装的随机路径信息
     * @param cpuType           CPU_AMR:1 CPU_X86:2 CPU_MIPS:3 具体见{@link PluginUtil}中的getCpuArchitecture()和getLibFile()方法
     * @return 是否成功
     */
    protected boolean copySoFile(String installedPathInfo, int cpuType) {
        String insideLibPath = PluginUtil.getInsidePluginPath() + mPluginId + "/" + installedPathInfo + "/";
        PluginUtil.createDir(insideLibPath);
        String apkLibPath = PluginUtil.getLibFile(cpuType);
        //首先将apk中libs文件夹下的一级so文件拷贝
        return PluginUtil.unzipFile(getAPKPath(mPluginId), insideLibPath, apkLibPath);
    }

    /**
     * 获取插件已经安装的apk路径
     *
     * @param pluginName 插件id
     * @return 插件已经安装的apk路径
     */
    public String getAPKPath(String pluginName) {
        return PluginUtil.getPlugDir(pluginName) + getInstalledPathInfo() + PluginConstant.PLUGIN_SUFF;
    }

    /**
     * 获取当前安装的随机路径信息，不使用缓存，直接读取文件
     *
     * @return 当前安装的随机路径信息
     */
    public String getInstalledPathInfoNoCache() {
        return PluginUtil.getInstalledPathInfo(mPluginId);
    }

    /**
     * 获取插件清单文件信息，不使用缓存，读取速度很快
     *
     * @return 插件清单文件信息
     */
    public PluginManifest getPluginMeta() {
        PluginManifest meta = null;
        String result = readMeta();
        if (!TextUtils.isEmpty(result)) {
            meta = parserMeta(result);
        }
        return meta;
    }

    /**
     * 解析清单文件
     *
     * @param metaString meta字符串
     * @return PluginManifest对象
     */
    private PluginManifest parserMeta(String metaString) {
        PluginManifest meta = new PluginManifest();
        try {
            JSONObject jObject = new JSONObject(metaString.replaceAll("\r|\n", ""));
            meta.name = jObject.optString(PluginManifest.PLUG_NAME);
            meta.minVersion = jObject.optString(PluginManifest.PLUG_MIN_VERSION);
            meta.maxVersion = jObject.optString(PluginManifest.PLUG_MAX_VERSION);
            meta.version = jObject.optString(PluginManifest.PLUG_VERSION);
            meta.mainClass = jObject.optString(PluginManifest.PLUG_MAINCLASS);
            meta.otherInfo = jObject.optString(PluginManifest.PLUG_OTHER_INFO);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return meta;
    }

    /**
     * 卸载某个插件，通常情况下不需要卸载，除非需要显示调用
     *
     * @return 是否成功
     */
    public boolean uninstall() {
        try {
            PluginManager.unInstalledPlugin(mPluginId);
            //删除手机内存中/data/data/packageName/plugins/mPluginName下的文件
            File baseModulePathF = new File(PluginUtil.getPlugDir(mPluginId));
            PluginUtil.deleteDirectory(baseModulePathF);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 读取meta文件
     *
     * @return meta字符串
     */
    private String readMeta() {
        return PluginUtil.readZipFileString(getAPKPath(mPluginId), PluginConstant.PLUGINWEB_MAINIFEST_FILE);
    }

    /**
     * 获取当前安装的随机路径信息，有缓存则使用缓存
     *
     * @return 当前安装的随机路径信息
     */
    private String getInstalledPathInfo() {
        if (!TextUtils.isEmpty(mInstalledPathInfo)) {
            return mInstalledPathInfo;
        }
        mInstalledPathInfo = PluginUtil.getInstalledPathInfo(mPluginId);
        return mInstalledPathInfo;
    }
}
