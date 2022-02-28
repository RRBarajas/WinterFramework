package encora.winterframework.server;

import java.io.IOException;
import java.io.InvalidClassException;
import java.lang.invoke.MethodHandles;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;

import encora.winterframework.annotation.WinterBootApplication;
import encora.winterframework.context.ApplicationContext;
import encora.winterframework.server.handler.RESTControllerHandler;

public class WinterServer {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    public static void run(Class<?> mainClass, String[] args) throws IOException {
        if (!mainClass.isAnnotationPresent(WinterBootApplication.class)) {
            throw new InvalidClassException("This is not a main WinterBoot application");
        }
        int port = 9000;
        log.info("Server starting on port " + port);

        WinterBootApplication mainApp = mainClass.getAnnotation(WinterBootApplication.class);
        String[] packages = mainApp.packages();
        if (Objects.isNull(packages) || packages.length == 0) {
            ApplicationContext.init(mainClass.getPackageName());
        } else {
            ApplicationContext.init(packages);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ApplicationContext.getBean(RESTControllerHandler.class));
        server.setExecutor(null);
        server.start();
    }
}
