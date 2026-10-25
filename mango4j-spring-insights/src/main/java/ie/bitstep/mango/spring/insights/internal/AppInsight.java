package ie.bitstep.mango.spring.insights.internal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ie.bitstep.mango.spring.insights.annotations.Insight;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Tolerate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@FieldNameConstants
@Data
@Accessors(chain = true)
public class AppInsight {

	private String id = UUID.randomUUID().toString();
	private String type;
	private String correlationId;
	private String serviceId;
	private String action;
	private String method;
	private Instant timestamp;
	private long executionTimeMillis;
	private String status;
	private String user;
	private List<ExceptionDetail> exception = new ArrayList<>();
	private Map<String, Object> context = new LinkedHashMap<>();

	@JsonIgnore // don't serialise tags, only used for generic metrics publishing (micrometer, OTEL, etc...)
	private Map<String, Object> tags = new LinkedHashMap<>();

	public AppInsight() {
	}

	public AppInsight(String type, String action) {
		this.timestamp = Instant.now();
		this.type = type;
		this.action = action;
	}

	public AppInsight(Insight insight) {
		this(insight.type(), insight.action());
	}

	public AppInsight(String insightType) {
		this(insightType, "");
	}

	public AppInsight setContextValue(String key, Object value) {
		context.put(key, value);
		return this;
	}

	public void setStatus(String status) {
		this.status = status;
		setTag("status", status);
	}

	@Tolerate
	@JsonIgnore
	public void setException(Exception exception) {
		this.exception = ExceptionUtils.getCauses(exception);
		setTag("exception", this.exception.get(0).getName());
	}

	public AppInsight setTag(String key, Object value) {
		context.put(key, value);
		tags.put(key, value);
		return this;
	}
}
