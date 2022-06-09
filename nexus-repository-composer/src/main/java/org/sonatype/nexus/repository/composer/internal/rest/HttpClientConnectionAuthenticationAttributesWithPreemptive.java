
package org.sonatype.nexus.repository.composer.internal.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import io.swagger.annotations.ApiModelProperty;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAuthenticationAttributes;

/**
 * REST API model for describing authentication for HTTP connections used by a proxy repository supporting preemptive
 * authentication.
 *
 * @since 3.30
 */
public class HttpClientConnectionAuthenticationAttributesWithPreemptive
    extends HttpClientConnectionAuthenticationAttributes
{
  @ApiModelProperty(value = "Whether to use pre-emptive authentication. Use with caution. Defaults to false.",
      example = "false")
  protected final Boolean preemptive;

  @JsonCreator
  public HttpClientConnectionAuthenticationAttributesWithPreemptive(
      @JsonProperty("type") final String type,
      @JsonProperty("preemptive") final Boolean preemptive,
      @JsonProperty("username") final String username,
      @JsonProperty(value = "password", access = Access.WRITE_ONLY) final String password,
      @JsonProperty("ntlmHost") final String ntlmHost,
      @JsonProperty("ntlmDomain") final String ntlmDomain)
  {
    super(type, username, password, ntlmHost, ntlmDomain);
    this.preemptive = preemptive;
  }

  public HttpClientConnectionAuthenticationAttributesWithPreemptive(
      final HttpClientConnectionAuthenticationAttributes auth,
      final Boolean preemptive)
  {
    super(auth.getType(), auth.getUsername(), auth.getPassword(), auth.getNtlmHost(), auth.getNtlmDomain());
    this.preemptive = preemptive;
  }

  public Boolean isPreemptive() {
    return preemptive;
  }
}
