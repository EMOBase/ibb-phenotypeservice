package ibb.api.phenotypeservice.models;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public class Reference {
    public static enum Type {
        PMID, DOI, LAB
    }

    @Enumerated(EnumType.STRING)
    public Type type;

    @NotBlank
    public String value;
}
