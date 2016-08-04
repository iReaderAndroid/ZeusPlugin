package com.android.internal.util;

/**
 * 该类从API8开始系统就存在，这个类只是为了让当前APK中存在一个类与系统的ClassLoader同名，
 * 在从dex文件优化为odex文件时，让每个类都标记为可以从其他ClassLoader加载
 * 如果你的项目是只支持android 4.4以上(默认为art虚拟机)，则不需要这个类。
 */
public interface Predicate<T> {

    boolean apply(T t);
}