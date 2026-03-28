package io.github.zekerzhayard.forgewrapper.installer.util;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

public class ModuleUtil {
    public static void addModules(String modulePath) {
        // nothing to do with Java 8
    }

    public static void addExports(List<String> exports) {
        // nothing to do with Java 8
    }

    public static void addOpens(List<String> opens) {
        // nothing to do with Java 8
    }

    public static Class<?> setupBootstrapLauncher(Class<?> mainClass) {
        // nothing to do with Java 8
        return mainClass;
    }

    public static ClassLoader getPlatformClassLoader() {
        // PlatformClassLoader does not exist in Java 8
        return null;
    }
}
