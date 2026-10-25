package com.mastercard.commons.mango.jpa;

import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@EqualsAndHashCode
public class OffsetLimitPageable implements Pageable {
	private final long offset;
	private final int limit;
	private final Sort sort;

	public static Pageable of(long offset, int limit) {
		return new OffsetLimitPageable(offset, limit, Sort.unsorted());
	}

	public static Pageable of(long offset, int limit, Sort sort) {
		return new OffsetLimitPageable(offset, limit, sort == null ? Sort.unsorted() : sort);
	}

	private OffsetLimitPageable(long offset, int limit, Sort sort) {
		if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");
		if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");
		this.offset = offset;
		this.limit = limit;
		this.sort = sort;
	}

	public int getLimit() {
		return limit;
	}

	@Override
	public int getPageNumber() {
		return (int) Math.floorDiv(offset, limit);
	}

	@Override
	public int getPageSize() {
		return limit;
	}

	@Override
	public long getOffset() {
		return offset;
	}

	@Override
	public Sort getSort() {
		return sort;
	}

	@Override
	public Pageable next() {
		return of(offset + limit, limit, sort);
	}

	@Override
	public Pageable previousOrFirst() {
		return of(Math.max(offset - limit, 0), limit, sort);
	}

	@Override
	public Pageable first() {
		return of(0, limit, sort);
	}

	@Override
	public Pageable withPage(int pageNumber) {
		return of((long) pageNumber * limit, limit, sort);
	}

	@Override
	public boolean hasPrevious() {
		return offset > 0;
	}
}
