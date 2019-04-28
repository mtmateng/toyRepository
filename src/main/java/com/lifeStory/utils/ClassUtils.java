package com.lifeStory.utils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

class ClassUtils {

    /**
     * 获取接口或抽象类的实现类，要求所有的实现类都与接口或抽象类在同一目录，或其子目录下
     * @param fatherInterface 接口或抽象类
     * @return 接口或抽象类所在包内的所有实现类
     */
    static <T> List<Class<T>> getAllClassByInterface(Class<T> fatherInterface, String packageName) {
        List<Class<T>> returnList = new ArrayList<>();
        if (fatherInterface.isInterface() || Modifier.isAbstract(fatherInterface.getModifiers())) {
            try {
                packageName = packageName == null ? fatherInterface.getPackage().getName() : packageName;
                List<Class> classes = findClassInPackage(packageName);
                for (Class child : classes) {
                    if (fatherInterface.isAssignableFrom(child) && !fatherInterface.equals(child)) {
                        returnList.add(child);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Class not found error in getAllClassByInterface");
            }
        }
        return returnList;
    }

    static List<Class> getAllClassByAnnotation(Class<? extends Annotation> annotation, String packageName) {

        List<Class> returnList = new ArrayList<>();
        try {
            List<Class> classes = findClassInPackage(packageName);
            for (Class child : classes) {
                if (child.isAnnotationPresent(annotation)) {
                    returnList.add(child);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Class not found error in getAllClassByInterface");
        }
        return returnList;
    }

    private static List<Class> findClassInPackage(String packageName) throws IOException, ClassNotFoundException {

        List<Class> classes = new ArrayList<>();

        String path = packageName.replace('.', '/');
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classloader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String newPath = resource.getFile().replace("%20", " ");
            dirs.add(new File(newPath));
        }
        for (File directory : dirs) {
            classes.addAll(findClass(directory, packageName));
        }
        return classes;

    }

    private static List<Class> findClass(File directory, String packageName)
            throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files != null ? files : new File[0]) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClass(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + "." + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

}
