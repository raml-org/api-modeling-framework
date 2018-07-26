/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DocumentModel} from "./DocumentModel";
import {EncodesDomainModel} from "./EncodesDomainModel";
import {DeclaresDomainModel} from "./DeclaresDomainModel";

/**
 * AMF Documents encode the main element of a description in a particular Domain Model
 * For example, in RAML/HTTP, the main domain element is an APIDescription.
 *
 * Since AMF Documents encode Domain elements they behave like Fragments
 * AMF Documents can also contains  declarations of domain elements to be used in the description of the domain.
 * From this point of view Documents also behave like Modules.
 */
export class Document extends DocumentModel implements EncodesDomainModel, DeclaresDomainModel {

    constructor(protected rawModel: any) {
        super(rawModel || {});
    }
}

