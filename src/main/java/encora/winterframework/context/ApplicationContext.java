package encora.winterframework.context;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import encora.winterframework.annotation.Autowired;
import encora.winterframework.annotation.Component;
import encora.winterframework.annotation.RESTController;
import encora.winterframework.annotation.RequestMapping;
import encora.winterframework.annotation.Service;
import encora.winterframework.context.loader.AnnotationScanner;

public final class ApplicationContext {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private static volatile ApplicationContext instance;

    private static final Map<Class<?>, Object> componentInstances = new HashMap<>();

    private static Map<Object, List<Object>> instanceDependencies;

    private static Map<String, Method> requestHandlers;

    private ApplicationContext() {}

    public static ApplicationContext getInstance() {
        // Does not support multithreading (efficiently)
        if (Objects.isNull(instance)) {
            synchronized (ApplicationContext.class) {
                if (Objects.isNull(instance)) {
                    instance = new ApplicationContext();
                }
            }
        }
        return instance;
    }

    // TODO: Check how to overload the init for parameters
    public static void init(String... packages) {
        log.info("Initializing everything ... :turtle: ");
        for (String pkg : packages) {
            log.info("Scanning for package: " + pkg);
            componentInstances.putAll(initializeComponentInstances(pkg));
        }

        requestHandlers = initializeControllerMappings();
        initializedAutowiredDependencies();
        log.info("I did my best while initializing");
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> theClass) {
        log.info("Getting bean for class: " + theClass);
        Object o = componentInstances.get(theClass);
        if (Objects.isNull(o)) {
            try {
                o = theClass.getDeclaredConstructor().newInstance();
                componentInstances.put(theClass, o);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalArgumentException("Cannot create instance of " + Component.class + " : " + theClass.getName());
            }
        }
        return (T) o;
    }

    public static Method getRequestHandlerMethod(String requestPath) {
        return requestHandlers.get(requestPath);
    }

    private static Map<Class<?>, Object> initializeComponentInstances(String rootPackage) {
        List<Class<?>> classes = AnnotationScanner.scanAnnotatedClasses(rootPackage, Component.class);
        classes.addAll(AnnotationScanner.scanAnnotatedClasses(rootPackage, RESTController.class));
        classes.addAll(AnnotationScanner.scanAnnotatedClasses(rootPackage, Service.class));
        Map<Class<?>, Object> instances = new HashMap<>(classes.size());

        for (Class<?> clazz : classes) {
            try {
                log.info("Initializing class: " + clazz);
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                Object inst = constructor.newInstance();
                instances.put(clazz, inst);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalArgumentException("Cannot create instance of " + Component.class + " : " + clazz.getName());
            }
        }
        return instances;
    }

    private static void initializedAutowiredDependencies() {
        for (Map.Entry<Class<?>, Object> bean : componentInstances.entrySet()) {
            Class<?> beanClazz = bean.getKey();
            for (Field field : beanClazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    try {
                        field.setAccessible(true);
                        field.set(bean.getValue(), ApplicationContext.getBean(field.getType()));
                    } catch (IllegalAccessException e) {
                        // TODO: Maneja esto mejor
                    }
                }
            }
        }
    }

    private static Map<String, Method> initializeControllerMappings() {
        Map<String, Method> methodList = new HashMap<>();
        for (Class<?> clazz : componentInstances.keySet()) {
            if (clazz.isAnnotationPresent(RESTController.class)) {
                RESTController kAnnotation = clazz.getAnnotation(RESTController.class);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping mAnnotation = method.getAnnotation(RequestMapping.class);
                        String endpointUrl = mAnnotation.method() + "/" + kAnnotation.value() + mAnnotation.path();
                        methodList.put(endpointUrl, method);
                    }
                }
            }
        }
        return methodList;
    }
}
