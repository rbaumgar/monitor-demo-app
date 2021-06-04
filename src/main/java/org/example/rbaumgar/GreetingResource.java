package org.example.rbaumgar;

import org.eclipse.microprofile.metrics.annotation.Counted;

//import io.quarkus.vertx.http.runtime.security.TrustedAuthenticationRequest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    private String HOSTNAME =
    System.getenv().getOrDefault("HOSTNAME", "unknown");
 
    @Counted(name = "greetings", description = "How many greetings we've given.", absolute = true)
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello from monitor-demo-app " + HOSTNAME;
    }
}