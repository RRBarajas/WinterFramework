package encora.winterframework.context.loader;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class AnnotationScanner {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @SuppressWarnings("unchecked")
    public static List<Class<?>> scanAnnotatedClasses(String packageName, Class<? extends Annotation> targetAnnotation) {
        List<Class<?>> classList = new ArrayList<>();
        try {
            // We need the path as directory to get the URL of the classes
            String packagePath = packageName.replace('.', '/');
            URL pathURL = ClassLoader.getSystemResource(packagePath);

            // We get all the class files in the specified URL
            if (Objects.isNull(pathURL)) {
                log.warning("Package '" + packageName + "' does not exist.");
                return classList;
            }
            File file = new File(pathURL.getPath());

            // Iterate through the classes and keep the ones using the annotation
            List<String> classNames = new ArrayList<>();
            getFileClasses(Objects.requireNonNull(file), classNames, packageName.substring(0, packageName.lastIndexOf('.')));
            for (String clazz : classNames) {
                Class<?> actualClass = Class.forName(clazz);
                if (actualClass.isAnnotationPresent(targetAnnotation)) {
                    if (actualClass.isAnnotation()) {
                        classList.addAll(scanAnnotatedClasses(packageName, (Class<? extends Annotation>) actualClass));
                    } else {
                        classList.add(actualClass);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            log.warning("Error loading classes from package '" + packageName + "' - " + e.getMessage());
            e.printStackTrace();
        }
        return classList;
    }

    public static void getFileClasses(File file, List<String> classNames, String parentPackage) {
        String fileName = parentPackage + "." + file.getName();
        if (file.isFile()) {
            classNames.add(fileName.substring(0, fileName.lastIndexOf('.')));
        } else {
            for (File f : Objects.requireNonNull(file.listFiles())) {
                getFileClasses(f, classNames, fileName);
            }
        }
    }
}