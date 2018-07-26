/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {GenericParameter} from "./GenericParameter";
import {Clojure} from "../../Clojure";
import {DomainModel} from "./DomainModel";

/**
 * Parameter representing a HTTP header
 */
export class Header extends GenericParameter {

    public constructor(rawModel: any) {
        super(rawModel);
        this.setParameterKindInternal("header");
    }

    public static build(id: string): Header {
        return new Header(Clojure.amf.build(
            DomainModel.domain_builder,
            Clojure.amf_domain.map__GT_ParsedParameter,
            id));
    }
}