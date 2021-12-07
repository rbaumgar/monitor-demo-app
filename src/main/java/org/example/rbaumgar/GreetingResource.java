package org.example.rbaumgar;

import org.eclipse.microprofile.metrics.annotation.Counted;

import javax.ws.rs.Consumes;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

    @GET
    @Path("2xx")
    @Produces(MediaType.TEXT_PLAIN)
    @Counted(name = "orders.2xx")
    public String simulate2xxResponse() {
        //meterRegistry.counter("orders.2xx","status","OK").increment();
        return "Got 2xx Response";
    }

	@GET
    @Path("5xx")
    @Produces(MediaType.TEXT_PLAIN)
    @Counted(name = "orders.5xx")
	public String simulate5xxResponse() {
        //meterRegistry.counter("orders.5xx","status","NOTOK").increment();
        return "Got 5xx Response";
    }

    @POST
    @Path("alert-hook")
    @Consumes(MediaType.APPLICATION_JSON)
	public String receiveAlertHook(String request) {
		System.out.println("Alert received: " + request);
        return "OK";
    }
}