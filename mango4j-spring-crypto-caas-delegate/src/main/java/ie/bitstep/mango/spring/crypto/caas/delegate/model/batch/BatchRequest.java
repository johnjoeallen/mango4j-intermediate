package ie.bitstep.mango.spring.crypto.caas.delegate.model.batch;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

import java.util.List;

@DoNotMutate
public record BatchRequest(List<BatchRequestItem> requests) {
}