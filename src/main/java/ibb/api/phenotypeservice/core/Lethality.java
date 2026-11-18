package ibb.api.phenotypeservice.core;

import io.quarkus.runtime.annotations.RegisterForReflection;

// The Elasticsearch client maps documents to this class by reflection, which
// a native executable only allows for registered classes.
@RegisterForReflection
public class Lethality {
    public float dali11 = -1;
    public float dali22 = -1;
    public float dapi11 = -1;
}
