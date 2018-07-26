/**
 * Created by antoniogarrote on 05/05/2017.
 */

/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DocumentModel} from "./DocumentModel";
import {EncodesDomainModel} from "./EncodesDomainModel";
import {DomainModel} from "../domain/DomainModel";

/**
 * AMF Fragments encode a single DomainElement that can be referenced and re-used in other documents.
 */
export class Fragment extends DocumentModel implements EncodesDomainModel {
    constructor(protected rawModel: any) {
        super(rawModel);
    }

    declares(): DomainModel[] {
        throw new Error("Fragments don't declare domain elements");
    }
}