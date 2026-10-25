package com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch;

import com.mastercard.commons.mango4j.crypto.caas.delegate.config.DoNotMutate;

import java.util.Objects;

@DoNotMutate
public class BatchResponseItem {
	private String batchRequestId;
	private String path;
	private int statusCode;
	private PayloadResponse payload;

	public String getBatchRequestId() {
		return batchRequestId;
	}

	public BatchResponseItem setBatchRequestId(String batchRequestId) {
		this.batchRequestId = batchRequestId;
		return this;
	}

	public String getPath() {
		return path;
	}

	public BatchResponseItem setPath(String path) {
		this.path = path;
		return this;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public BatchResponseItem setStatusCode(int statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	public PayloadResponse getPayload() {
		return payload;
	}

	public BatchResponseItem setPayload(PayloadResponse payload) {
		this.payload = payload;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BatchResponseItem that = (BatchResponseItem) o;
		return statusCode == that.statusCode && Objects.equals(batchRequestId, that.batchRequestId) && Objects.equals(path, that.path) && Objects.equals(payload, that.payload);
	}

	@Override
	public int hashCode() {
		return Objects.hash(batchRequestId, path, statusCode, payload);
	}
}