package encora.winterframework.server.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Objects;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import encora.winterframework.context.ApplicationContext;
import encora.winterframework.util.JSONParser;

public class RESTControllerHandler implements HttpHandler {

    // Do not use uppercase since it's not a constant
    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    @Override
    public void handle(HttpExchange req) throws IOException {
        String reqResponseJSON;
        URI reqURI = req.getRequestURI();
        String reqPath = req.getRequestMethod() + reqURI.getPath();
        Method reqMethod = validateRequestedMethod(req, reqPath);
        try {
            Object instance = ApplicationContext.getBean(reqMethod.getDeclaringClass());
            Object reqResponse = reqMethod.invoke(instance);
            reqResponseJSON = JSONParser.toJSON(reqResponse);

            req.sendResponseHeaders(200, reqResponseJSON.length());
            OutputStream os = req.getResponseBody();
            os.write(reqResponseJSON.getBytes());
            os.close();
            req.close();
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.warning(String.format("Error calling method '%s'%n", reqMethod.getName()));
        }
        req.sendResponseHeaders(500, 0);
        req.close();
    }

    private Method validateRequestedMethod(HttpExchange req, String reqPath) throws IOException {
        Method reqMethod = ApplicationContext.getRequestHandlerMethod(reqPath);
        if (Objects.isNull(reqMethod)) {
            req.sendResponseHeaders(404, 0);
            OutputStream os = req.getResponseBody();
            os.write(String.format("Hey! There's no handler for '%s' registered", reqPath).getBytes());
            os.close();
            req.close();
        }
        reqMethod.setAccessible(true);
        return reqMethod;
    }
}
