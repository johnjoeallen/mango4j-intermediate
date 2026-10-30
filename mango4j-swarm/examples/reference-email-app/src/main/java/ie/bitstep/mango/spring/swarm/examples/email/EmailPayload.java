package ie.bitstep.mango.spring.swarm.examples.email;

public record EmailPayload(
        String customerId,
        String to,
        String subject,
        String body
) {
}
