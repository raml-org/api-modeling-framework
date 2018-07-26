/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DomainModel} from "../domain/DomainModel";

export interface EncodesDomainModel {
    /**
     * Encoded domain element described in the document element.
     * @return DomainElement encoded in the document.
     * @throws InvalidModelException
     */
    encodes(): DomainModel;
}