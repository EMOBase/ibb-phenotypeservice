package ibb.api.phenotypeservice.models;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public class OntologyTerm {

    public String termId;
    
    @NotBlank
    public String termName;
}
