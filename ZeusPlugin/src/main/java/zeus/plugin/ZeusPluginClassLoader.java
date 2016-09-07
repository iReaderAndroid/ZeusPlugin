package zeus.plugin;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;

/***
 * 加载任意后缀的zip格式的classLoader，主要用来加载apk文件
 * 如果插件是放在SD卡目录下，最好的格式是jar，有些手机的DexFile只能识别apk或者jar后缀的，因为apk格式会被清理软件当作未安装apk文件清理掉
 * <p>
 * Created by huangjian on 2016/6/21.
 */
class ZeusPluginClassLoader extends ClassLoader {

    protected String mRawLibPath;
    protected final String mDexOutputPath;
    protected File[] mFiles;
    protected ZipFile[] mZips;
    protected DexFile[] mDexs;
    protected String[] mLibPaths;

    private boolean mInitialized;
    public final String mRawDexPath;
    final private String mPluginId;

    public ZeusPluginClassLoader(String pluginId, String dexPath, String dexOutputDir, String libPath,
                                    ClassLoader parent) {

        super(parent);
        if (dexPath == null || dexOutputDir == null)
            throw new NullPointerException();
        mPluginId = pluginId;
        mRawDexPath = dexPath;
        mDexOutputPath = dexOutputDir;
        mRawLibPath = libPath;
    }

    public String getPluginId() {
        return mPluginId;
    }

    /***
     * 初始化
     */
    protected synchronized void ensureInit() {
        if (mInitialized) {
            return;
        }

        String[] dexPathList;

        mInitialized = true;

        dexPathList = mRawDexPath.split(":");
        int length = dexPathList.length;

        mFiles = new File[length];
        mZips = new ZipFile[length];
        mDexs = new DexFile[length];

        for (int i = 0; i < length; i++) {
            File pathFile = new File(dexPathList[i]);
            mFiles[i] = pathFile;

            if (pathFile.isFile()) {
                try {
                    mZips[i] = new ZipFile(pathFile);
                } catch (IOException ioex) {
                    System.out.println("Failed opening '" + pathFile
                            + "': " + ioex);
                }
                try {
                    String outputName =
                            generateOutputName(dexPathList[i], mDexOutputPath);
                    mDexs[i] = DexFile.loadDex(dexPathList[i], outputName, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        generateLibPath();
    }

    protected void generateLibPath() {
        //优先查找本地添加的lib库，然后在去系统级别的去找，防止自己的插件中so跟系统存在的so重名了
        String pathList;
        String systemPathList = System.getProperty("java.library.path", ".");
        String pathSep = System.getProperty("path.separator", ":");
        String fileSep = System.getProperty("file.separator", "/");

        if (mRawLibPath != null) {
            if (systemPathList.length() > 0) {
                if (mRawLibPath.endsWith(pathSep)) {
                    pathList = mRawLibPath + systemPathList;
                } else {
                    pathList = mRawLibPath + pathSep + systemPathList;
                }
            } else {
                pathList = mRawLibPath;
            }
        } else {
            pathList = systemPathList;
        }

        mLibPaths = pathList.split(pathSep);
        int length = mLibPaths.length;

        for (int i = 0; i < length; i++) {
            if (!mLibPaths[i].endsWith(fileSep))
                mLibPaths[i] += fileSep;
        }
    }

    protected static String generateOutputName(String sourcePathName,
                                               String outputDir) {
        StringBuilder newStr = new StringBuilder(80);

        newStr.append(outputDir);
        if (!outputDir.endsWith("/"))
            newStr.append("/");

        String sourceFileName;
        int lastSlash = sourcePathName.lastIndexOf("/");
        if (lastSlash < 0)
            sourceFileName = sourcePathName;
        else
            sourceFileName = sourcePathName.substring(lastSlash + 1);

        int lastDot = sourceFileName.lastIndexOf(".");
        if (lastDot < 0)
            newStr.append(sourceFileName);
        else
            newStr.append(sourceFileName, 0, lastDot);
        newStr.append(".dex");

        return newStr.toString();
    }

    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(className);

        if (clazz == null) {
            try {
                clazz = getParent().loadClass(className);
            } catch (ClassNotFoundException e) {
            }

            if (clazz == null) {
                try {
                    clazz = findClass(className);
                } catch (ClassNotFoundException e) {
                    throw e;
                }
            }
        }

        return clazz;
    }

    /***
     * 每个插件里也可能有多个dex文件，挨个的查找dex文件
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        ensureInit();
        Class clazz;
        int length = mFiles.length;
        for (int i = 0; i < length; i++) {

            if (mDexs[i] != null) {
                String slashName = name.replace('.', '/');
                clazz = mDexs[i].loadClass(slashName, this);
                if (clazz != null) {
                    return clazz;
                }
            }
        }
        throw new ClassNotFoundException(name + " in loader " + this);
    }


    /**
     * 只查找插件自己是否存在该class
     *
     * @param className 类名字
     * @return 类对象
     * @throws ClassNotFoundException
     */
    public Class<?> loadClassByself(String className) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(className);

        if (clazz == null) {
            clazz = findClass(className);
        }

        return clazz;
    }

    @Override
    protected String findLibrary(String libname) {//根据插件的pathInfo查找对应的so
        ensureInit();

        String fileName = System.mapLibraryName(libname);
        for (String libPath : mLibPaths) {
            String pathName = libPath + fileName;
            File test = new File(pathName);

            if (test.exists()) {
                return pathName;
            }
        }
        return null;
    }

}


