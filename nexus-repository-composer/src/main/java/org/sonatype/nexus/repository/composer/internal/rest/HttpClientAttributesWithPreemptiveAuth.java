
package org.sonatype.nexus.repository.composer.internal.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAttributes;

import javax.validation.Valid;

/**
 * REST API model for describing HTTP connection properties for proxy repositories supporting preemptive
 * authentication.
 *
 * @since 3.30
 */
public class HttpClientAttributesWithPreemptiveAuth
    extends HttpClientAttributes
{
  @Valid
  protected final HttpClientConnectionAuthenticationAttributesWithPreemptive authenticationWithPreemptive;

  @JsonCreator
  public HttpClientAttributesWithPreemptiveAuth(
      @JsonProperty("blocked") final Boolean blocked,
      @JsonProperty("autoBlock") final Boolean autoBlock,
      @JsonProperty("connection") final HttpClientConnectionAttributes connection,
      @JsonProperty("authentication") final HttpClientConnectionAuthenticationAttributesWithPreemptive authentication)
  {
    super(blocked, autoBlock, connection, null);
    this.authenticationWithPreemptive = authentication;
  }

  public HttpClientAttributesWithPreemptiveAuth(
      final HttpClientAttributes httpClientAttributes,
      final HttpClientConnectionAuthenticationAttributesWithPreemptive authentication)
  {
    super(httpClientAttributes.getBlocked(), httpClientAttributes.getAutoBlock(), httpClientAttributes.getConnection(),
        null);
    this.authenticationWithPreemptive = authentication;
  }

  @Override
  public HttpClientConnectionAuthenticationAttributesWithPreemptive getAuthentication() {
    return authenticationWithPreemptive;
  }
}
