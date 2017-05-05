package org.raml.amf.core.domain;

import api_modeling_framework.model.domain.HeadersHolder;
import api_modeling_framework.model.domain.PayloadHolder;
import org.raml.amf.core.exceptions.InvalidModelException;
import org.raml.amf.utils.Clojure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

/**
 * Base class for the Request and Response of an API
 */
public abstract class GenericOperationUnit extends DomainModel {

    static {
        Clojure.require(Clojure.API_MODELING_FRAMEWORK_MODEL_DOMAIN);
    }

    public GenericOperationUnit(Object rawModel) {
        super(rawModel);
    }

    /**
     * List of HTTP headers in this unit
     * @return
     */
    public List<Header> getHeaders() {
        List headers =  (List) this.headersHolder().headers();
        List<api_modeling_framework.model.domain.ParsedParameter> tmp = Clojure.toJavaList(headers);
        ArrayList<Header> eps = new ArrayList<>();
        for(api_modeling_framework.model.domain.ParsedParameter x : tmp) {
            Header parsed = new Header(x);
            eps.add(parsed);
        }

        return eps;
    }

    public void setHeaders(List<Header> headers) {
        ArrayList<Object> raws = new ArrayList<>();
        for(Header x : headers) {
            raws.add(x.clojureModel());
        }

        this.rawModel = Clojure.setKw(this.rawModel, "headers", Clojure.list(raws));
    }

    /**
     * List of Payloads in the unit
     * @return
     */
    public List<Payload> getPayloads() {
        List payloads =  (List) this.payloadHolder().payloads();
        List<api_modeling_framework.model.domain.ParsedPayload> tmp = Clojure.toJavaList(payloads);
        ArrayList<Payload> eps = new ArrayList<>();
        for(api_modeling_framework.model.domain.ParsedPayload x : tmp) {
            Payload parsed = new Payload(x);
            eps.add(parsed);
        }

        return eps;
    }

    public void setPayloads(List<Payload> payloads) {
        ArrayList<Object> raws = new ArrayList<>();
        for(Payload x : payloads) {
            raws.add(x.clojureModel());
        }

        this.rawModel = Clojure.setKw(this.rawModel, "payloads", Clojure.list(raws));
    }


    private HeadersHolder headersHolder() {
        return (HeadersHolder) this.rawModel;
    }

    private PayloadHolder payloadHolder() {
        return (PayloadHolder) this.rawModel;
    }
}
