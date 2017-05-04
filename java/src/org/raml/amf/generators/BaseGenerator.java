package org.raml.amf.generators;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

import clojure.lang.IFn;
import org.raml.amf.core.document.DocumentModel;
import org.raml.amf.utils.Clojure;

import java.io.File;
import java.net.MalformedURLException;

/**
 * Basic interface for all AMF generators. It allows to generate syntax files out of AMF Document Models.
 */
public abstract class BaseGenerator {
    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_CORE);
    }

    /**
     * Serialises the model and stores it in the provided file path and options
     * @param path Path where the model will be serialised
     * @param model DocumentModel to be serialised
     */
    public void generateFile(File path, DocumentModel model, GenerationOptions options) throws GenerationException {
        String location = path.getAbsolutePath().toString();
        IFn parserFn = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, generatorConstructor());
        Object generator = parserFn.invoke();
        IFn parseFileSync = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, "generate-file-sync");
        try {
            Object result = parseFileSync.invoke(generator, location, model.clojureModel(), options.build());
            if (result instanceof Exception) {
                throw new GenerationException((Exception) result);
            }

        } catch (RuntimeException e) {
            throw new GenerationException(e);
        }
    }

    /**
     * Serialises the model and stores it in the provided file path.
     * @param path
     * @param model
     * @throws GenerationException
     */
    public void generateFile(File path, DocumentModel model) throws GenerationException {
        generateFile(path, model, new GenerationOptions());
    }

    /**
     * Serialises the model and uses the provided file path as the default model location, applying the provided options
     * @param path Path where the model will be serialised
     * @param model DocumentModel to be serialised
     */
    public String generateString(File path, DocumentModel model, GenerationOptions options) throws GenerationException {
        return generateStringInternal(path.getAbsolutePath().toString(), model, options);
    }

    /**
     * Serialises the model and stores it in the using the privded file path as the model location
     * @param path
     * @param model
     * @throws GenerationException
     */
    public String generateString(File path, DocumentModel model) throws GenerationException {
        return generateString(path, model, new GenerationOptions());
    }

    /**
     * Serialises the model using the default location stored in the model
     * @param model
     * @throws GenerationException
     */
    public String generateString(DocumentModel model) throws GenerationException {
        try {
            return generateStringInternal(model.location().toString(), model, new GenerationOptions());
        } catch (MalformedURLException ex) {
            throw new GenerationException(ex);
        }
    }

    protected String generateStringInternal(String location, DocumentModel model, GenerationOptions options) throws GenerationException {
        IFn parserFn = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, generatorConstructor());
        Object generator = parserFn.invoke();
        IFn parseFileSync = Clojure.var(Clojure.API_MODELING_FRAMEWORK_CORE, "generate-string-sync");
        try {
            Object result = parseFileSync.invoke(generator, location, model.clojureModel(), options.build());
            if (result instanceof Exception) {
                throw new GenerationException((Exception) result);
            } else {
                return (String) result;
            }

        } catch (RuntimeException e) {
            throw new GenerationException(e);
        }
    }
    protected abstract String generatorConstructor();
}
