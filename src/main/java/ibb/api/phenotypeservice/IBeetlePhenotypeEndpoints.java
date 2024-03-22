package ibb.api.phenotypeservice;

import java.util.List;

import org.jboss.resteasy.reactive.RestPath;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/ibeetle-phenotypes")
public class IBeetlePhenotypeEndpoints {

    @Inject
    IBeetlePhenotypeRepository repository;

    @GET
    @Path("/screens/by-ib/{ib}")
    public List<Phenotype> findByIB(@RestPath String ib) {
        return repository.findByIB(ib);
    }
}
