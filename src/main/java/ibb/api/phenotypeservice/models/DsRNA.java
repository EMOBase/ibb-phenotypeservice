package ibb.api.phenotypeservice.models;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public class DsRNA extends PanacheEntity {
    public String name;

    @NotBlank
    public String sequence;
}
