/**
 * Created by antoniogarrote on 05/05/2017.
 */

import {DomainModel} from "../domain/DomainModel";

export interface DeclaresDomainModel {
    /**
     * Declared DomainElements that can be re-used from other documents.
     * @return List of domain elements.
     */
    declares(): DomainModel[];
}