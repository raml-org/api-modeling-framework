/**
 * Created by antoniogarrote on 05/05/2017.
 */

/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DocumentModel} from "./DocumentModel";
import {DeclaresDomainModel} from "./DeclaresDomainModel";
import {DomainModel} from "../domain/DomainModel";

/**
 * AMF Modules contains collections of DomainElements that can be re-used and referenced from other documents in the
 * Documentmodel.
 */
export class Module extends DocumentModel implements DeclaresDomainModel{
    constructor(protected rawModel: any) {
        super(rawModel);
    }

    encodes(): DomainModel {
        throw new Error("Modules don't encode domain elements");
    }
}