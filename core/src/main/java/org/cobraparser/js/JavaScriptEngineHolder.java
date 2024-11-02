package org.cobraparser.js;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 2024-11-02
 */
class JavaScriptEngineHolder {
     static JavaScriptEngine ourInstance = calcEngine();

    static JavaScriptEngine calcEngine() {
        ServiceLoader<JavaScriptEngine> loader = ServiceLoader.load(JavaScriptEngine.class, JavaScriptEngine.class.getClassLoader());

        Optional<JavaScriptEngine> first = loader.findFirst();
        if (first.isPresent()) {
            return first.get();
        }

        return new DummyJavaScriptEngine();
    }
}
