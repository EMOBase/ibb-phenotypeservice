package ibb.api.phenotypeservice.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public class OntologyTerm extends PanacheEntity {

    public String termId;
    
    @NotBlank
    public String termName;
}
