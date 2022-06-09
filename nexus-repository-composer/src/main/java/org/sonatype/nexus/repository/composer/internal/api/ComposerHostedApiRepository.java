
package org.sonatype.nexus.repository.composer.internal.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.ComponentAttributes;
import org.sonatype.nexus.repository.rest.api.model.HostedStorageAttributes;
import org.sonatype.nexus.repository.rest.api.model.SimpleApiHostedRepository;

import javax.validation.constraints.NotNull;

/**
 * REST API model for a composer hosted repository.
 *
 * @since 3.20
 */
@JsonIgnoreProperties(value = {"format", "type", "url"}, allowGetters = true)
public class ComposerHostedApiRepository
    extends SimpleApiHostedRepository
{
  @NotNull
  protected final ComposerAttributes composer;

  @JsonCreator
  public ComposerHostedApiRepository(
      @JsonProperty("name") final String name,
      @JsonProperty("url") final String url,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final HostedStorageAttributes storage,
      @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
      @JsonProperty("composer") final ComposerAttributes composer,
      @JsonProperty("component") final ComponentAttributes component)
  {
    super(name, ComposerFormat.NAME, url, online, storage, cleanup, component);
    this.composer = composer;
  }

  public ComposerAttributes getComposer() {
    return composer;
  }
}
