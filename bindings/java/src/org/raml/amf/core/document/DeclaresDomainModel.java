package org.raml.amf.core.document;

import org.raml.amf.core.domain.DomainModel;

import java.util.List;

/**
 * Created by antoniogarrote on 04/05/2017.
 */

public interface DeclaresDomainModel {
    /**
     * Declared DomainElements that can be re-used from other documents.
     * @return List of domain elements.
     */
    public List<DomainModel> declares();
}
