package com.mastercard.commons.mango.logging.enums;

/**
 * @see TRACK
 * @see DONT_TRACK
 * @see SPLIT
 * @see TRAIL
 */
public enum TrackingType {
	/**
	 * Track the flow by keeping the current tracking cookie (correlation id)
	 */
	TRACK,
	/**
	 * Don't track the flow (clear the correlation id)
	 */
	DONT_TRACK,
	/**
	 * Split from the flow (new correlation id)
	 */
	SPLIT,
	/**
	 * Create a trail from the flow to a new flow (previous correlation id + new correlation id)
	 */
	TRAIL
}