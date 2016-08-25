package zeus.plugin;

import java.util.HashMap;

/**
 * Created by huangjian on 2016/6/21.
 * 插件的一些配置项
 */
public class PluginConfig {
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
    public static final String EXP_PLUG_NO_SO_PREFIX = EXP_PLUG_PREFIX + "_no_so";          //如果插件id是以zeusplugin_no_so开头，则认为是插件
    public static final String EXP_PLUG_HOT_FIX_PREFIX = "zeushotfix";                      //如果插件id是以zeushotfix开头，则认为是热修复补丁
    public static final String EXP_PLUG_HOT_FIX_NO_RES_PREFIX =
            EXP_PLUG_HOT_FIX_PREFIX + "_no_res";                                            //如果插件id是以zeushotfix_no_res开头，则认为是不带资源的热修复补丁
    public static final String EXP_PLUG_HOT_FIX_NO_RES_SO_PREFIX =
            EXP_PLUG_HOT_FIX_NO_RES_PREFIX + "_so";                                         //如果插件id是以zeushotfix_no_so_res开头，则认为是不带so和资源文件的热修复补丁
    public static final String EXP_PLUG_HOT_FIX_NO_SO_PREFIX =
            EXP_PLUG_HOT_FIX_PREFIX + "_no_so";                                             //如果插件id是以zeushotfix_no_so开头，则认为是不带so文件的热修复补丁
    public static HashMap<String, Integer> mDefaultList = new HashMap<>();

    public static final String PLUGIN_TEST = "zeusplugin_test";                             //插件测试demo
    public static final String HOTFIX_TEST = "zeushotfix_test";                             //热修复补丁测试demo

    /**
     * apk自带的插件的列表，每次添加内置插件的时候需要添加到这里，格式(pluginName,pluginVersion)
     * pluginVersion一定要与插件中PLUGINWEB_MAINIFEST_FILE文件里的version保持一致。
     * 对于插件还可以使用diff补丁的形式下载增量包，这样可以降低文件下载的大小。
     */
    static {
        //插件必须以PluginUtil.EXP_PLUG_PREFIX开头，否则不会识别为插件
        mDefaultList.put(PLUGIN_TEST, 1);
        //补丁必须以EXP_PLUG_HOT_FIX_PREFIX开头
        //这里是演示如何使用，一般都不会内置热修复bug fix插件，热修复插件都是网上下载，下载完成后执行安装方法
        //然后启动的时候就自动会加载热修复的补丁，热修复补丁不支持运行时动态更新，必须软件重启后才能生效
        //否则会出现未知异常，反正没试过，感兴趣可以试试
//        mDefaultList.put(HOTFIX_TEST, 1);
    }
}
