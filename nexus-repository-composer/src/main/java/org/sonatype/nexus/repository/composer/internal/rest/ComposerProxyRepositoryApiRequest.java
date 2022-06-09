
package org.sonatype.nexus.repository.composer.internal.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.composer.internal.api.ComposerAttributes;
import org.sonatype.nexus.repository.rest.api.model.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @since 3.20
 */
@JsonIgnoreProperties({"format", "type"})
public class ComposerProxyRepositoryApiRequest
    extends ProxyRepositoryApiRequest
{
  @NotNull
  protected final ComposerAttributes composer;

  @NotNull
  @Valid
  protected final HttpClientAttributesWithPreemptiveAuth httpClient;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  @SuppressWarnings("squid:S00107") // suppress constructor parameter count
  public ComposerProxyRepositoryApiRequest(
      @JsonProperty("name") final String name,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final StorageAttributes storage,
      @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
      @JsonProperty("proxy") final ProxyAttributes proxy,
      @JsonProperty("negativeCache") final NegativeCacheAttributes negativeCache,
      @JsonProperty("httpClient") final HttpClientAttributesWithPreemptiveAuth httpClient,
      @JsonProperty("routingRule") final String routingRule,
      @JsonProperty("composer") final ComposerAttributes composer)
  {
    super(name, ComposerFormat.NAME, online, storage, cleanup, proxy, negativeCache, httpClient, routingRule);
    this.composer = composer;
    this.httpClient = httpClient;
  }

  public ComposerAttributes getComposer() {
    return composer;
  }

  @Override
  public HttpClientAttributesWithPreemptiveAuth getHttpClient() {
    return httpClient;
  }
}
