
package org.sonatype.nexus.repository.composer.internal.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.composer.internal.api.ComposerAttributes;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.ComponentAttributes;
import org.sonatype.nexus.repository.rest.api.model.HostedRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.HostedStorageAttributes;

import javax.validation.constraints.NotNull;

/**
 * @since 3.20
 */
@JsonIgnoreProperties({"format", "type"})
public class ComposerHostedRepositoryApiRequest
    extends HostedRepositoryApiRequest
{
  @NotNull
  protected final ComposerAttributes composer;

  @JsonCreator
  public ComposerHostedRepositoryApiRequest(
      @JsonProperty("name") final String name,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final HostedStorageAttributes storage,
      @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
      @JsonProperty("composer") final ComposerAttributes composer,
      @JsonProperty("component") final ComponentAttributes componentAttributes
  )
  {
    super(name, ComposerFormat.NAME, online, storage, cleanup, componentAttributes);
    this.composer = composer;
  }

  public ComposerAttributes getComposer() {
    return composer;
  }
}
