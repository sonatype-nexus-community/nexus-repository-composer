
package org.sonatype.nexus.repository.composer.internal.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.composer.internal.rest.HttpClientAttributesWithPreemptiveAuth;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.NegativeCacheAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyAttributes;
import org.sonatype.nexus.repository.rest.api.model.SimpleApiProxyRepository;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;

import javax.validation.constraints.NotNull;

/**
 * REST API model for a composer proxy repository.
 *
 * @since 3.20
 */
@JsonIgnoreProperties(value = {"format", "type", "url"}, allowGetters = true)
public class ComposerProxyApiRepository
    extends SimpleApiProxyRepository
{
  @NotNull
  protected final ComposerAttributes composer;

  @SuppressWarnings("squid:S00107") // suppress constructor parameter count
  @JsonCreator
  public ComposerProxyApiRepository(
      @JsonProperty("name") final String name,
      @JsonProperty("url") final String url,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final StorageAttributes storage,
      @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
      @JsonProperty("proxy") final ProxyAttributes proxy,
      @JsonProperty("negativeCache") final NegativeCacheAttributes negativeCache,
      @JsonProperty("httpClient") final HttpClientAttributesWithPreemptiveAuth httpClient,
      @JsonProperty("routingRuleName") final String routingRuleName,
      @JsonProperty("composer") final ComposerAttributes composer)

  {
    super(name, ComposerFormat.NAME, url, online, storage, cleanup, proxy, negativeCache, httpClient, routingRuleName);
    this.composer = composer;
  }

  public ComposerAttributes getComposer() {
    return composer;
  }
}
