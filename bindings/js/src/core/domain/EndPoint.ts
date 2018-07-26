/**
 * Created by antoniogarrote on 05/05/2017.
 */
import {Clojure} from "../../Clojure";
import {DomainModel} from "./DomainModel";
import {Operation} from "./Operation";

/**
 * EndPoints contains information about a HTTP remote location where a number of API operations have been bound
 */
export class EndPoint extends DomainModel {

    /**
     * Path for the URL where the operations of the EndPoint are bound
     * @return
     */
    public getPath(): string | undefined{
        return Clojure.amf_domain.path(this.rawModel);
    }

    public setPath(basePath: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("path"), basePath);
    }

    /**
     * List of API Operations bound to this EndPoint
     * @return
     */
    public getSupportedOperations(): Operation[] {
        const endpointsCljs = Clojure.amf_domain.supported_operations(this.rawModel);
        return Clojure.cljsMap(endpointsCljs, (e) => new Operation(e));
    }

    public setSupportedOperations(operations: Operation[]) {
        const newOperations = Clojure.toCljsSeq(operations.map(x => x.clojureModel()));
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("supported-operations"), newOperations);
    }

    /**
     * Builds a new EndPoint for the provided URI
     * @param id
     */
    public static build(id: string): EndPoint {
        return new EndPoint(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_domain.map__GT_ParsedEndPoint,
            id));
    }
}