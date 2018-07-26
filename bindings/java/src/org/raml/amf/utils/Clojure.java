package org.raml.amf.utils;

import clojure.lang.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Clojure interop utilties.
 * Many of this utilities and this particular approach to interop is because of a bug related to core.async preventing
 * us to use aot compilation and the class that could have been generated in api-modeling-framework.core
 */
public class Clojure {

    public static final String CLOJURE_CORE = "clojure.core";
    public static final String API_MODELING_FRAMEWORK_CORE = "api-modeling-framework.core";
    public static final String API_MODELING_FRAMEWORK_MODEL_DOCUMENT = "api-modeling-framework.model.document";
    public static final String API_MODELING_FRAMEWORK_MODEL_DOMAIN = "api-modeling-framework.model.domain";
    public static final String CHESHIRE_CORE = "cheshire.core";
    public static final Var REQUIRE= RT.var("clojure.core", "require");


    public static Object require(String nsName) {
        return REQUIRE.invoke(Symbol.intern(nsName));
    }

    /**
     * Looks up a var by name in the given namespace.
     *
     * The var can subsequently be invoked if it is a function.
     * @param nsName
     * @param varName
     * @return
     */
    public static Var var(String nsName, String varName) {
        return RT.var(nsName,varName);
    }

    public static Keyword kw(String name) {
        return Keyword.find(name);
    }

    public static Object getKw(Object target, String name) {
        IFn getFn= var(CLOJURE_CORE, "get");
        return getFn.invoke(target, kw(name));
    }

    public static Object setKw(Object target, String name, Object value) {
        IFn setFn= var(CLOJURE_CORE, "assoc");
        return setFn.invoke(target, kw(name), value);
    }

    public static IPersistentVector list(List list) {
        IPersistentVector tmp = PersistentVector.EMPTY;
        for(Object e : list) {
            tmp = tmp.cons(e);
        }

        return tmp;
    }

    public static <T> List<T> toJavaList(List xs) {
        ArrayList<T> tmp = new ArrayList<>();
        IFn firstFn= var(CLOJURE_CORE, "first");
        IFn restFn= var(CLOJURE_CORE, "rest");
        IFn emptyFn= var(CLOJURE_CORE, "empty?");

        while(! (Boolean) emptyFn.invoke(xs)) {
            tmp.add((T) firstFn.invoke(xs));
            xs = (List) restFn.invoke(xs);
        }

        return tmp;
    }

    public static IPersistentMap map() {
        return PersistentHashMap.EMPTY;
    }
}
