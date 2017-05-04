package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.ParsedResponse;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

/**
 * Created by antoniogarrote on 04/05/2017.
 */


/**
 * Information about the response returned by an operation, associated to a particular status
 */
public class Response extends GenericOperationUnit {
    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public Response(ParsedResponse rawModel) {
        super(rawModel);
    }

    public Response(String id) {
        super(Clojure.var(
                Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN,
                "api-modeling-framework.model.domain/map->ParsedResponse"
                ).invoke(Clojure.map())
        );
        this.setId(id);
    }

    /**
     * Status code for the response
     * @return
     */
    public String getStatusCode() {
        String code = (String) this.wrapped().status_code();
        if (code != null)
            return code;
        else
            return null;
    }

    public void setStatusCode(String status) {
        this.rawModel = Clojure.setKw(this.wrapped(), "status-code", status);
    }

    private ParsedResponse wrapped() {
        return (ParsedResponse) this.rawModel;
    }
}
