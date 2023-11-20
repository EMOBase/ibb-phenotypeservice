package ibb.api.phenotypeservice.models;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public class DsRNA {
    public String name;

    @NotBlank
    public String sequence;
}
