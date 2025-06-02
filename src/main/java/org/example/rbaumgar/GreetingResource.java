package org.example.rbaumgar;

import org.eclipse.microprofile.metrics.annotation.Counted;

import io.quarkus.logging.Log;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


@Path("/hello")
public class GreetingResource {

    private String HOSTNAME = System.getenv().getOrDefault("HOSTNAME", "unknown");


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Counted(name = "greetings", description = "How many greetings we've given.", absolute = true)
        public String hello() {
        return "Hello from monitor-demo-app " + HOSTNAME;
    }

    @GET
    @Path("2xx")
    @Produces(MediaType.TEXT_PLAIN)
    @Counted(name = "greetings.2xx", description = "How many 2xx we've given.", absolute = true)
    public String simulate2xxResponse() {
        Log.info("2xx received");
        return "Got 2xx Response";
    }

    @GET
    @Path("5xx")
    @Produces(MediaType.TEXT_PLAIN)
	@Counted(name = "greetings.5xx", description = "How many 5xx we've given.", absolute = true)
	public String simulate5xxResponse() {
        Log.info("5xx received");
        return "Got 5xx Response";
    }

    @Path("alert-hook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Counted(name = "alerts", description = "How many alters we've received.", absolute = true)@POST
	public String receiveAlertHook(String request) {
		Log.info("Alert received: " + request);
        return "OK";
    }

}