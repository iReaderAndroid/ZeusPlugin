package zeus.plugin;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;

import static java.lang.System.arraycopy;

/***
 * 加载任意后缀的zip格式的classLoader，主要用来加载热修复补丁
 * 热修复补丁与插件的区别是热修复补丁只能软件启动的时候加载一次，插件可以随时加载、卸载。
 * <p>
 * Created by huangjian on 2016/6/21.
 */
class ZeusHotfixClassLoader extends ZeusPluginClassLoader {
    private ClassLoader mChild = null;
    private Method findClassMethod = null;
    private Method findLoadedClassMethod = null;

    public ZeusHotfixClassLoader(String dexPath, String dexOutputDir, String libPath,
                                    ClassLoader parent) {
        super(null, dexPath, dexOutputDir, libPath,parent);
    }

    protected void setOrgAPKClassLoader(ClassLoader child) {
        mChild = child;
        findLoadedClassMethod = PluginUtil.getMethod(mChild.getClass(), "findLoadedClass", String.class);
        findClassMethod = PluginUtil.getMethod(mChild.getClass(), "findClass", String.class);
    }

    protected void addAPKPath(String dexPath, String libPath) {
        if(mDexs == null){
            ensureInit();
        }
        int oldLength = mDexs.length;
        int index = oldLength + 1;

        Object[] old = mDexs;
        mDexs = new DexFile[index];
        arraycopy(old, 0, mDexs, 0, index - 1);

        old = mFiles;
        mFiles = new File[index];
        arraycopy(old, 0, mFiles, 0, index - 1);

        old = mZips;
        mZips = new ZipFile[index];
        arraycopy(old, 0, mZips, 0, index - 1);

        if (!TextUtils.isEmpty(libPath)) {
            String pathSep = System.getProperty("path.separator", ":");
            if (mRawLibPath.endsWith(pathSep)) {
                mRawLibPath = mRawLibPath + libPath;
            } else {
                mRawLibPath = mRawLibPath + pathSep + libPath;
            }
            generateLibPath();

        }

        File pathFile = new File(dexPath);
        mFiles[oldLength] = pathFile;
        if (pathFile.isFile()) {
            try {
                mZips[oldLength] = new ZipFile(pathFile);
            } catch (IOException ioex) {
                System.out.println("Failed opening '" + pathFile
                        + "': " + ioex);
            }
        }

        try {
            String outputName =
                    generateOutputName(dexPath, mDexOutputPath);
            mDexs[oldLength] = DexFile.loadDex(dexPath, outputName, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        //先查找补丁自己已经加载过的有没有
        Class<?> clazz = findLoadedClass(className);

        if (clazz == null) {
            try {
                //查查parent中有没有，也就是android系统中的
                clazz = getParent().loadClass(className);
            } catch (ClassNotFoundException ignored) {

            }

            if (clazz == null) {
                try {
                    //查查自己有没有，就是补丁中有没有
                    clazz = findClass(className);
                } catch (ClassNotFoundException ignored) {

                }
            }
        }
        //查查child中有没有，child是设置进来的，实际就是宿主apk中有没有
        if (clazz == null && mChild != null) {
            try {
                if (findLoadedClassMethod != null) {
                    clazz = (Class<?>) findLoadedClassMethod.invoke(mChild, className);
                }
                if (clazz != null) return clazz;
                if (findClassMethod != null) {
                    clazz = (Class<?>) findClassMethod.invoke(mChild, className);
                    return clazz;
                }
            } catch (Exception ignored) {

            }
        }
        if (clazz == null) {
            throw new ClassNotFoundException(className + " in loader " + this);
        }
        return clazz;
    }
}

