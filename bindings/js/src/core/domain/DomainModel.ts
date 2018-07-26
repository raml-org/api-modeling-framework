/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {Clojure} from "../../Clojure";
import {Model} from "../Model";

/**
 * Base class for all Domain Model elements
 */
export class DomainModel extends Model {

    constructor(protected rawModel: any) {
        super(rawModel);
    }

    /**
     * Factory method building the right wrapper Java DomainModel subclass for the provided Domain Model Clojure data structure
     * @param rawModel native Clojure encoded model
     * @return The right DocumentModel
     */
    public static fromRawModel(rawModel: any): DomainModel {
        const type  = Clojure.core.type(rawModel);
        if (type === Clojure.amf_domain.ParsedAPIDocumentation) {
            return new APIDocumentation(rawModel);
        } else if (type === Clojure.amf_domain.ParsedEndPoint) {
                return new EndPoint(rawModel);
        } else if (type === Clojure.amf_domain.ParsedOperation) {
            return new Operation(rawModel);
        } else if (type === Clojure.amf_domain.ParsedRequest) {
            return new Request(rawModel);
        } else if (type === Clojure.amf_domain.ParsedResponse) {
            return new Response(rawModel);
        } else if (type === Clojure.amf_domain.ParsedPayload) {
            return new Payload(rawModel);
        } else if (type === Clojure.amf_domain.ParsedType) {
            return new Type(rawModel);
        } else {
            throw new Error(`Unknown model ${type}`)
        }
    }

    public clojureModel(): any {
        return this.rawModel;
    }

    /**
     * Returns the unique URI identifier the Domain Element in the AMF graph
     * @return
     */
    public getId(): string {
        return Clojure.amf_document.id(this.rawModel);
    }

    /**
     * Returns a human readable string identifying the Domain Element
     * @return
     */
    public getName(): string | undefined {
        return Clojure.amf_domain.name(this.rawModel);
    }

    public setName(name: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("name"), name);
    }

    /**
     * If true, the Domain Element is abstract and should be used to be extended by other Domain Elements
     * @return
     */
    public getAbstract(): boolean | undefined {
        return Clojure.amf_domain.abstract(this.rawModel);
    }

    public setAbstract(abstract: boolean | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("abstract"), abstract);
    }

    /**
     * List of Domain Elements this element extends
     * @return
     */
    public getExtends(): DomainModel[] {
        let extensions = Clojure.amf_domain.extends(this.rawModel);
        return Clojure.cljsMap(extensions, (e) => DomainModel.fromRawModel(e));
    }

    public setExtensions(extensions: DomainModel[]) {
        const newExtensions = extensions.map(e => e.clojureModel());
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("extends"), Clojure.toCljsSeq(newExtensions));
    }

    /**
     * Optional list of source maps with lexical information associated to the Domain Element
     * @return
     */
    public getSourceMaps(): SourceMap[] {
        let sourceMaps = Clojure.amf_domain.sources(this.rawModel);
        return Clojure.cljsMap(sourceMaps, (e) => new SourceMap(e));
    }

    public setSourceMaps(sourceMaps: SourceMap[]) {
        const newSourceMaps = sourceMaps.map(e => e.clojureModel());
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("sources"), Clojure.toCljsSeq(newSourceMaps));
    }

    public static domain_builder = Clojure.amf.map__GT_JSDomainBuilder()
}

import {APIDocumentation} from "./APIDocumentation";
import {EndPoint} from "./EndPoint";
import {Operation} from "./Operation";import { SourceMap } from "./SourceMap";
import {Payload} from "./Payload";
import {Parameter} from "./Parameter";
import {Type} from "./Type";
import {Request} from "./Request";
import {Response} from "./Response";
