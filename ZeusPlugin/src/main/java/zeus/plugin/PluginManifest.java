package zeus.plugin;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

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
    public static final String PLUG_FLAG = "flag";
    public static final int FLAG_WITHOUT_RESOURCES = 0x1;
    public static final int FLAG_WITHOUT_SO_FILE = 0x2;
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

    public String flag = "";

    public PluginManifest() {
    }

    public PluginManifest(String manifest) {
        try {
            JSONObject jsonObject = new JSONObject(manifest);
            name = jsonObject.optString(PLUG_NAME);
            minVersion = jsonObject.optString(PLUG_MIN_VERSION);
            maxVersion = jsonObject.optString(PLUG_MAX_VERSION);
            version = jsonObject.optString(PLUG_VERSION);
            mainClass = jsonObject.optString(PLUG_MAINCLASS);
            otherInfo = jsonObject.optString(PLUG_OTHER_INFO);
            flag = jsonObject.optString(PLUG_FLAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getVersion() {
        if (!TextUtils.isEmpty(version)) {
            try {
                return Integer.valueOf(version);
            } catch (Throwable e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public int getFlag() {
        return TextUtils.isEmpty(flag) ? 0 : Integer.valueOf(flag);
    }

    public boolean hasResoures() {
        return (getFlag() & FLAG_WITHOUT_RESOURCES) != FLAG_WITHOUT_RESOURCES;
    }

    public boolean hasSoLibrary(){
        return (getFlag() & FLAG_WITHOUT_SO_FILE) != FLAG_WITHOUT_SO_FILE;
    }
    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(PLUG_NAME, name);
            jsonObject.put(PLUG_MIN_VERSION, minVersion);
            jsonObject.put(PLUG_MAX_VERSION, maxVersion);
            jsonObject.put(PLUG_VERSION, version);
            jsonObject.put(PLUG_MAINCLASS, mainClass);
            jsonObject.put(PLUG_OTHER_INFO, otherInfo);
            jsonObject.put(PLUG_OTHER_INFO, flag);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
