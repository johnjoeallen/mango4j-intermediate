package com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch;

import com.mastercard.commons.mango4j.crypto.caas.delegate.config.DoNotMutate;

import java.util.List;

@DoNotMutate
public record BatchRequest(List<BatchRequestItem> requests) {
}