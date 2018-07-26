/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DomainModel} from "./DomainModel";
import {Clojure} from "../../Clojure";
import {Type} from "./Type";

/**
 * Schema information for a Payload associated to a particular media-type
 */
export class Payload  extends DomainModel {

    public getMediaType(): string | undefined {
        return Clojure.amf_domain.media_type(this.rawModel);
    }

    public setMediaType(mediaType: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("media-type"), mediaType);
    }

    /**
     * Schema information for the payload
     * @return
     */
    public getSchema(): Type | undefined {
        const type = Clojure.amf_domain.schema(this.rawModel);
        if (type != null)
            return new Type(type);
    }

    public setSchema(schema: Type | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("media-type"), schema  != null ? schema.clojureModel() : null);
    }

    public static build(id: string): Payload {
        return new Payload(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_domain.map__GT_ParsedPayload,
            id));
    }
}