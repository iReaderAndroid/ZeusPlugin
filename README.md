# 欢迎使用ZeusPlugin

本项目为`ZeusPlugin`插件框架项目，`ZeusPlugin`为插件框架代码，`app`为测试插件与补丁的项目demo，`testplugin`为插件demo, `testhotfix`为补丁`demo`。绝大部分核心代码都在`PluginManger.java`中。`PluginManager`也是入口类，核心方法是`inite`初始化、`loadLastVersionPlugin`加载插件、`reloadInstalledPluginResources`加载插件与补丁的资源、`loadHotfixPluginClassLoader`加载补丁的类。插件与补丁更新的最小单位是`java`类(不局限四大组件)。

## 插件的定位

提高某些功能的升级率，使功能可以不通过安装新apk版本进行更新，可以实现wifi/移动环境下用户无感知的更新功能。如果电商类的网页，可以通过url告知客户端使用哪个插件并可以指定最低版本，然后客户端发现存在符合的插件就加载使用。如:`http//www.baidu.com/a.php?p=zeusplugin_test&pversion=2`,表示该页面使用`zeusplugin_test`插件，插件的最低版本号为`2`。宿主发现存在符合的插件则加载并将该url传给插件，不存在则进入下载流程，下载完成后即可加载。插件的下发可以通过`push`或者轮训的方式预下载增量更新包，或者进入loading页面同步下载，下载完成后即可加载使用。这时插件就可以是`Fragment`、`View`、`Activity`等等。

## 补丁使用的定位

解决某个版本发布后的bug，或者是更新某个功能。由于代码会进行混淆，一个补丁通常只能对应一个版本。

## 插件补丁框架的核心思想

- 替换系统使用的`ClassLoader`，通过阅读源码发现系统反射四大组件都是用的`ContextImpl`中的`mPackageInfo`中的`mClassLoader`成员变量。所以为了能让系统生成四大组件，我们通过反射修改了`mClassLoader`成员变量。补丁原理则是`ClassLoader`优先查找补丁中的类，如果存在则返回，然后再查找原宿主中的类，我们是通过反射的方式设置宿主`ClassLoader`的`parent`成员来完成的。
- 替换系统获取资源用到的`Resources`对象并使该对象可以访问到所有插件和补丁的资源,Resources是通过AssetManager来访问资源的。系统的`Resources`对象是`ContextImpl`的`mPackageInfo`中的`mResources`成员。因此我们生成了一个`PluginResources`对象并创建一个可以访问所有插件的`AssetManager`，反射调用`addAssetPath`将插件/补丁的路径都添加上。 通过反射修改了`mResources`成员变量。但是部分手机是通过获取调用`Activity/Application`中的`getResources`来访问，我们重写该方法，并返回生成的`PluginResources`。  

> 以上就是我们的核心，**修改生成类和资源的成员变量**。

## 支持运行时更新的原理

- 插件更新后，插件在访问资源应该都是新插件中的资源。因此我们重新创建一个`Resources`，这个新的`Resources`只能访问宿主跟新插件中的资源。这解决了资源更新的问题。
- 插件更新后，插件在生成类的时候应该生成新插件中的类。因此我们重新创建了一个插件ClassLoader来替换原来的`ClassLoader`，在方案中，一个插件对应一个`ClassLoader`，
   `ZeusClassLoader`是个空壳，它内部有一个数组保存了所有的插件`ClassLoader`，`zeusClassLoader`首先查找原宿主`apk`的`ClassLoader`，因为宿主`apk`的`ClassLoader`的`parent`为补丁`ClassLoader`，所以先在补丁中查找类，然后在宿主apk中查找，最后依次在插件中查找。我们只要替换了插件`ClassLoader`，那么新的插件`ClassLoader`自然只能访问新插件中的类了。
- 系统在解析`xml`生成`View`的时候都是通过反射来生成`View`的，系统为了加快速度会把所有已经反射过的`View`的构造函数都保存在`LayoutInflater`的静态成员变量`sConstructorMap`中，所以更新了插件后，我们会清除该`map`中的所有对象。
- 在使用旧版本插件时，可以安装新版本插件，每个插件的安装地址都是随机的，有个`pathInfo`文件来保存最新插件的随机安装路径。一旦加载最新插件时，`ClassLoader`和`AssetManager`都指向最新版本的插件，当然`so`路径也是随机的。当软件退出或者再次启动的时候会清理掉老版本插件，因为`pathInfo`只保留最新插件的安装地址，这些老版本插件就已经不可访问了，只能加载最新版本的插件。

## 使用步骤

1. 使原应用程序的`Application`继承`BaseApplication`。或者将`BaseApplication`代码拷贝至自己的Application中。具体参考`app`中的`MyApplication`
2. 使原应用程序的Activity都继承`BaseActivity`。或者将`BaseActivity`代码拷贝至自己的Activity中。具体参考app中的`MainActivity`
3. 内置的插件应放入assets目录中。插件的命名以`PluginConfig.EXP_PLUG_PREFIX`为前缀，以`PluginConfig.PLUGINWEB_APK_SUFF`为结尾。
4. 内置的插件必须在插件项目的assets中添加`PluginConfig.PLUGINWEB_MAINIFEST_FILE(即plugin.meta)`文件，该文件为插件的配置文件。配置如插件名称(name)，插件版本(version)，插件支持的宿主最低版本(minVersion)，插件的入口类名(mainClass,可不写)。具体参考`testplugin`例子。
5. `PluginConfig`中是一些可以配置的信息，建议除了内置插件目录以外都不要修改。
6. 请将当前aapt目录下`aapt.exe`拷贝到sdk目录下的`build-tools`中的正在使用的打包工具，替换原有的aapt.exe，
    该`aapt.exe`是基于6.0源码编译，包含windows、mac和linux(64位)版本。测试替换23.0.2、23.0.3都没有问题。该`aapt.exe`还集成了资源混淆功能。需要在`build.gradle`中进行配置。
    如下：`aaptOptions.additionalParameters '--PLUG-resoure-proguard', '--PLUG-resoure-id', '0x7d'`
    `'--PLUG-resoure-proguard'`表明开启资源混淆，可以不写，不写表示不开启资源混淆，`'--PLUG-resoure-id'`表示设置资源的packageID，`'0x7d'`表示资源`packageID`为`0x7d`开头。
7. 插件或补丁的资源`packageID`不能与其他插件或者是宿主相同。具体如何设置请参考testplugin中的`build.gradle`。
8. 将`app`模块中的`buil.gradle`中的`buildJar`方法拷贝到你的`build.gradle`中，这个方法是用来生成插件的sdk，生成sdk的jar文件在`build/libs`路径下，
    把生成的jar放到插件项目的`sdk-jars`，要把什么类放到sdk中，请对该方法进行修改。具体参考`testplugin`。
9. 请将插件的`AndroidManifest.xml`中有关的配置添加到宿主中，包括四大组件、权限申请、meta-data等，为了插件的扩展也可以在宿主中预定义一些组件。

> 以下是补丁相关的。补丁与插件类似，只不过补丁把实时加载的功能去掉了。如果项目只运行在android 4.4及以上(art虚拟机，部分低于4.4的手机也可以去掉)，则1忽略，以上就已经支持补丁了，可以直接运行app模块，不需要以下的额外操作。

10. 如果要支持bug fix的补丁功能，请把gradleplugin拷贝到工程里，并将project的`build.gradle`中的带有“`//-----补丁相关-------`”的相关配置移植到你的项目里。如果你的项目只支持android 4.4及以上时(比如内置应用)，不需要按照第当前移植即可支持bug fix补丁功能。具体请查看`testhotfix`模块。
20. bug fix的补丁需要以`PluginConfig.EXP_PLUG_HOT_FIX_PREFIX`为开头，补丁apk中res文件夹中必须有实际的资源，实在没有就随便写个
    `com.android.internal.util.Predicate`要保留，故意让所有的类中都包含这个类，这个类系统也定义了，所以dalvik虚拟机生成dex的时候会给当前记一个标记，这样补丁才能实现。
30. 插件与补丁都需要先安装，插件可以任意时刻进行加载，补丁则下次启动由框架加载，不提供实时加载，实时加载之后你的内存中的对象可能就会乱套了。插件与补丁的安装代码如下：`PluginManager.getPlugin(pluginName).install();`
40. 如果宿主被混淆请打补丁包时使用-applymapping,具体请搜索，在此不做详细介绍。
50. 补丁只在宿主的release包才生效，请在release包中进行测试。
60. 补丁在`android studio`中开启`instant run`时调试是无效的，如果要生效就得更改代码，具体如何更改请查看`PluginManager.java`。

## 支持特性

1. 支持插件的安装、升级、卸载、版本管理
2. 支持插件调用宿主的类与资源。要在插件中使用宿主的资源ID，需要使用public.xml将资源ID固定，public.xml如何使用请自行搜索，并将该ID添加到sdk-jar中，如果只是插件调用宿主中的某个类，然后这个使用了宿主资源则不需处理。
3. 支持插件的动态实时升级。只需要调用`PluginManager.loadLastVersionPlugin(pluginName)`即可使用最新版本的插件。
4. 插件与宿主的关系和apk与android系统的关系接近。
    **如果插件中有与宿主重名的类，这个插件中的类只能被插件使用，宿主是不会使用插件中的类的。宿主只能通过显式loadClass的方式才能访问插件。**
5. 当插件版本过多又怕新插件在早期apk中不支持，应编写一个类CTS测试(google强制厂商执行的兼容性测试)的小插件，该插件中会调用所有之前插件用到的宿主中的所有方法和成员等等。如果该小程序跑过了则说明新版本apk兼容所有插件。
6. 支持so以及so的动态实时升级。
7. 插件与补丁支持加固方案，单dex或者多个dex文件情况，已对android 1.5以上版本(已适配最新的android N)和厂商定制的android系统进行了适配，适配了各种机型和厂商自己的系统(包括yunOS等)。测试无资源加载找不到的问题，存在极个别的第一次加载后类找不到的情况，尝试几次就可以了。(概率极低，<0.0001%)
8. 对性能无明显影响。经过在android 2.2及以上进行高强度测试，对性能无明显影响。
9. 支持bug fix的补丁功能，补丁修复最小单位是java中的class，补丁中可以有资源，也可以使用宿主的资源，它其实跟插件是一样的，只不过补丁的class与宿主的class重名了，发现重名就替换，支持单dex、多dex(方法数超了的情况)。补丁对性能有微弱影响(个人认为可以忽略)，android 4.4及以上完全无影响。
10. 如果你的apk没有进行代码混淆，补丁也可以产生与插件相同的作用来进行功能的更新。

## 不支持的特性

1. 不支持插件中使用activity动画。如果要使用activity动画请将activity动画用到的xml文件放到宿主中，否则卡死。
2. 不支持插件有自己的Application，插件获取的是宿主的application。
3. 不支持动态升级插件的AndroidManifest.xml文件，所有试图修改AndroidManifest.xml的功能都需要升级宿主。不过这种情况很少，目前我们还没遇到过。
4. 不支持补丁实时加载，下次启动才能加载，否则内存中的对象会乱掉，如之前保存了A类的实例，现在A类已经被实时替换为B类了，那么之前的A类实例就不能转为B类了。
5. 不支持插件在xml使用宿主的自定义属性。(支持这个性价比太低，请使用其他替代方法)
6. 其他还不清楚，还请大家进行测试。

## 欢迎加群交流讨论

> QQ群：`558449447`，添加请注明来自`ZeusPlugin`
>
> <a target="_blank" href="http://shang.qq.com/wpa/qunwpa?idkey=4464e9ee4fc8b05ee3c4eeb4f4be97469c1cfe46cded6b00f4a887ebebb60916"><img border="0" src="http://pub.idqqimg.com/wpa/images/group.png" alt="Android技术交流分享" title="Android技术交流分享"></a>

# LICENSE

```
MIT LICENSE 
Copyright (c) 2016 huangjian

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```