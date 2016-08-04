package zeus.plugin;

/**
 * 插件的配置，对应文件存放在插件apk下的assets/plugin.meta,这也是可配置的
 * 之所以没有放在androidManifest.xml中是因为有些手机使用无法获取插件中对应的meta data数据。
 * 其实还有一套完整强安全校验方案，想到绝大部分人用不到，就删掉了，想要的可以在单独联系。
 */
public class PluginManifest {
    public static final String PLUG_NAME = "name";
    public static final String PLUG_MIN_VERSION = "minVersion";
    public static final String PLUG_MAX_VERSION = "maxVersion";
    public static final String PLUG_VERSION = "version";
    public static final String PLUG_MAINCLASS = "mainClass";
    public static final String PLUG_OTHER_INFO = "otherInfo";
    /**
     * 插件显示的名称
     */
    public String name = "";
    /**
     * 插件依赖的最低平台版本号
     */
    public String minVersion = "";
    /**
     * 插件依赖的最大平台版本号
     */
    public String maxVersion = "";
    /**
     * 插件版本号
     */
    public String version = "";
    /**
     * 插件入口的类名
     */
    public String mainClass = "";
    /**
     * 插件的其他信息，可扩展，可是其他json格式字符串
     */
    public String otherInfo = "";
}
