package zeus.plugin;

/**
 * Created by huangjian on 2016/6/21.
 * 插件的一些配置项
 */
public class PluginConstant {
    /**
     * 插件配置文件存放的路径，没有放到manifest中的meta data是因为有些手机取不到这个值
     */
    public static final String PLUGINWEB_MAINIFEST_FILE = "assets/zeusplugin.meta";
    /**
     * 有些手机中DexFile只能接受.apk和.jar后缀，所以这里二选一，如果是sd卡上最好用jar，因为apk会被当成未安装apk被清理软件清理。
     */
    public static final String PLUGIN_SUFF = ".apk";
    /**
     * 真实的activity
     */
    public static final String PLUGIN_REAL_ACTIVITY = "realActivity";
    /**
     * 校验的标准的activity，仅为了通过android的activity校验，需要在AndroidManifest.xml中添加
     */
    public static final String PLUGIN_ACTIVITY_FOR_STANDARD = "com.zeus.ZeusActivityForStandard";

    /**
     * 如果插件路径是在SD卡上，则插件后缀应为jar
     */
    public static final String PLUGIN_JAR_SUFF = ".jar";
    public static final String PLUGIN_INSTALLED_INFO_PATH = "zeusplugin_installinfo";       //插件的安装路径信息，每次安装后的apk的文件名都是随机的，为了实现动态实时加载
    public static final String EXP_PLUG_PREFIX = "zeusplugin";                              //如果插件id是以zeusplugin开头，则认为是插件
    public static final String EXP_PLUG_HOT_FIX_PREFIX = "zeushotfix";                      //如果插件id是以zeushotfix开头，则认为是热修复补丁

}
