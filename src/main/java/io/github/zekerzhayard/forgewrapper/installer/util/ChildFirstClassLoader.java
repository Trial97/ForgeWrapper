package io.github.zekerzhayard.forgewrapper.installer.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A class loader that loads classes from its own URLs before delegating to the parent.
 * This allows for isolation of dependencies by prioritizing local classes over parent classes.
 */
public class ChildFirstClassLoader extends URLClassLoader {
    public ChildFirstClassLoader(URL[] urls) {
        super(urls);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        // Always delegate bootstrap classes first
        if (name.startsWith("java.") || name.startsWith("javax.")) {
            return super.loadClass(name, resolve);
        }

        // Check if already loaded
        Class<?> clazz = findLoadedClass(name);
        if (clazz != null) return clazz;

        // Try child first
        try {
            clazz = findClass(name);
        } catch (ClassNotFoundException ignored) {
        }

        // Fallback to parent
        if (clazz == null) {
            clazz = getParent().loadClass(name);
        }

        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

    @Override
    public URL getResource(String name) {
        // Try to find resource locally first
        URL resource = findResource(name);
        if (resource != null) {
            return resource;
        }
        // Delegate to parent
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        // Get local resources first
        Enumeration<URL> localResources = findResources(name);
        // Get parent resources
        Enumeration<URL> parentResources = super.getResources(name);

        // Combine: local resources first, then parent resources
        return new CombinedEnumeration<>(localResources, parentResources);
    }

    /**
     * Retrieves all classpath entries from the system classloader hierarchy.
     * Works on Java 8 through Java 21+ using multiple fallback strategies.
     *
     * @return List of classpath entries as absolute paths
     */
    public static List<String> getAllClassPaths() {
        Set<String> paths = new LinkedHashSet<>();

        // Strategy 1: Traverse classloader hierarchy (Java 8 compatible)
        collectFromClassLoader(ClassLoader.getSystemClassLoader(), paths);

        // Strategy 2: Use java.class.path system property as fallback
        String classPath = System.getProperty("java.class.path");
        if (classPath != null && !classPath.isEmpty()) {
            for (String entry : classPath.split(File.pathSeparator)) {
                if (!entry.isEmpty()) {
                    paths.add(new File(entry).getAbsolutePath());
                }
            }
        }

        return new ArrayList<>(paths);
    }

    /**
     * Collects URLs from URLClassLoader hierarchy.
     */
    private static void collectFromClassLoader(ClassLoader classLoader, Set<String> paths) {
        ClassLoader current = classLoader;
        while (current != null) {
            if (current instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader) current).getURLs();
                for (URL url : urls) {
                    String path = urlToPath(url);
                    if (path != null) {
                        paths.add(path);
                    }
                }
            }
            current = current.getParent();
        }
    }

    /**
     * Converts URL to normalized file path.
     */
    private static String urlToPath(URL url) {
        if (url == null) return null;
        try {
            return new File(url.toURI()).getAbsolutePath();
        } catch (Exception e) {
            // Fallback for URLs that aren't file://
            String path = url.getPath();
            if (path != null && !path.isEmpty()) {
                return new File(path).getAbsolutePath();
            }
            return null;
        }
    }

    /**
     * Factory method that creates a ChildFirstClassLoader with the specified classpath.
     * Only adds paths that are not already present in the system classpath.
     *
     * @param libraryDir Base directory for resolving relative paths
     * @param paths List of relative paths to resolve against libraryDir
     * @param parent Parent classloader to delegate to
     * @return Configured ChildFirstClassLoader with child-first isolation
     * @throws Throwable if URL conversion fails
     */
    public static ChildFirstClassLoader createWithClassPath(
            java.nio.file.Path libraryDir, List<String> paths) throws Throwable {
        List<URL> urls = new ArrayList<>();
        List<String> source = getAllClassPaths();
        for (String path : paths) {
            java.nio.file.Path resolved = libraryDir.resolve(path);
            String absolutePath = resolved.toAbsolutePath().toString();
            if (!source.contains(absolutePath)) {
                urls.add(resolved.toUri().toURL());
            }
        }
        return new ChildFirstClassLoader(urls.toArray(new URL[0]));
    }

    /**
     * Helper class to combine two enumerations.
     */
    private static class CombinedEnumeration<E> implements Enumeration<E> {
        private final Enumeration<E> first;
        private final Enumeration<E> second;

        CombinedEnumeration(Enumeration<E> first, Enumeration<E> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean hasMoreElements() {
            return first.hasMoreElements() || second.hasMoreElements();
        }

        @Override
        public E nextElement() {
            if (first.hasMoreElements()) {
                return first.nextElement();
            }
            return second.nextElement();
        }
    }
}
