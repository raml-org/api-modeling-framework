/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DomainModel} from "./DomainModel";
import {Clojure} from "../../Clojure";

/**
 * Tag included in a SourceMap
 * Tags are tuples with an identifier, describing the kind of information in the mapping and an arbitrary value.
 * Tags are also elements of the model, so they also have an associated URI.
 */
export class GenericTag extends DomainModel {

    public getTagId(): string | undefined {
        return Clojure.amf_domain.tagId(this.rawModel);
    }

    public setTagId(tagId: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("tag-id"), tagId);
    }

    public getValue(): string | undefined {
        return Clojure.amf_domain.value(this.rawModel);
    }

    public setValue(value: string | undefined) {
        this.rawModel = Clojure.amf.update(DomainModel.domain_builder, this.rawModel, Clojure.kw("value"), value);
    }


    public static build(id: string, value: string, tagId?: string): GenericTag {
        let tag = new GenericTag(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_document.map__GT_APITagTag,
            id));
        tag.setValue(value);
        if (tagId != null)
            tag.setTagId(tagId);
        return tag;
    }
}