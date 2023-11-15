package ibb.api.phenotypeservice.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public class Reference extends PanacheEntity {
    public static enum Type {
        PMID, DOI, LAB
    }

    @Enumerated(EnumType.STRING)
    public Type type;

    @NotBlank
    public String value;
}
