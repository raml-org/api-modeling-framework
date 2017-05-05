package org.raml.amf.parsers;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

import clojure.lang.IFn;
import org.raml.amf.core.document.DocumentModel;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

import java.net.URL;

/**
 * Basic interface for all AMF parsers. It allows to parse syntax files and syntax text and generate the AMF Model out
 * of it.
 */
public abstract class BaseParser {

    /**
     * Generates a model parsing the file referenced by the provided URL.
     * @param url Local or remote URL
     * @return The parsed model
     * @throws ParsingException
     */
    public DocumentModel parseFile(URL url) throws ParsingException, InvalidModelException {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_CORE);
        IFn parserFn = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, parserConstructor());
        Object parser = parserFn.invoke();
        IFn parseFileSync = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, "parse-file-sync");
        try {
            Object rawModel = parseFileSync.invoke(parser, url.toString().replace("file:",""));
            if (rawModel instanceof Exception) {
                throw new ParsingException((Exception) rawModel);
            }
            return DocumentModel.fromRawModel(rawModel);
        } catch (RuntimeException e) {
            throw new ParsingException(e);
        }
    }

    /**
     * Generates a model parsing the file referenced by the provided URL and parsing options.
     * @param url Local or remote URL
     * @param options Parsing options
     * @return The parsed model
     * @throws ParsingException
     */
    public DocumentModel parseFile(URL url, ParsingOptions options) throws ParsingException, InvalidModelException {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_CORE);
        IFn parserFn = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, parserConstructor());
        Object parser = parserFn.invoke();
        IFn parseFileSync = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, "parse-file-sync");
        try {
            Object rawModel = parseFileSync.invoke(parser, url.toString().replace("file:",""), options.build());
            if (rawModel instanceof Exception) {
                throw new ParsingException((Exception) rawModel);
            }
            return DocumentModel.fromRawModel(rawModel);
        } catch (RuntimeException e) {
            throw new ParsingException(e);
        }
    }

    protected abstract String parserConstructor();

    /**
     * Generates a model parsing the provided textual input syntax.
     * @param text Input syntax to parse
     * @param url Base URL for the document being parsed. It will be the base URL for the inclusions in this file
     * @return The parsed Model
     * @throws ParsingException
     */
    public DocumentModel parseString(String text, URL url) throws ParsingException {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_CORE);
        IFn parserFn = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, parserConstructor());
        Object parser = parserFn.invoke();
        IFn parseFileSync = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, "parse-string-sync");
        try {
            Object rawModel = parseFileSync.invoke(parser, url.toString().replace("file:",""), text);
            if (rawModel instanceof Exception) {
                throw new ParsingException((Exception) rawModel);
            }
            return DocumentModel.fromRawModel(rawModel);
        } catch (RuntimeException e) {
            throw new ParsingException(e);
        }
    }

    /**
     * Generates a model parsing the provided textual input syntax and parsing options.
     * @param text Input syntax to parse
     * @param url Base URL for the document being parsed. It will be the base URL for the inclusions in this file
     * @param options Parsing options
     * @return The parsed Model
     * @throws ParsingException
     */
    public DocumentModel parseString(String text, URL url, ParsingOptions options) throws ParsingException {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_CORE);
        IFn parserFn = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, parserConstructor());
        Object parser = parserFn.invoke();
        IFn parseFileSync = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, "parse-string-sync");
        try {
            Object rawModel = parseFileSync.invoke(parser, url.toString().replace("file:",""), text, options.build());
            if (rawModel instanceof Exception) {
                throw new ParsingException((Exception) rawModel);
            }
            return DocumentModel.fromRawModel(rawModel);
        } catch (RuntimeException e) {
            throw new ParsingException(e);
        }
    }
}
