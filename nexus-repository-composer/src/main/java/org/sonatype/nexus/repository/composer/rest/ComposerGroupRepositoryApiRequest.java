package org.sonatype.nexus.repository.composer.rest;

import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.rest.api.model.GroupAttributes;
import org.sonatype.nexus.repository.rest.api.model.GroupRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"format", "type"})
public class ComposerGroupRepositoryApiRequest extends GroupRepositoryApiRequest {
  @JsonCreator
  public ComposerGroupRepositoryApiRequest(
      @JsonProperty("name") final String name,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final StorageAttributes storage,
      @JsonProperty("group") final GroupAttributes group) {
    super(name, ComposerFormat.NAME, online, storage, group);
  }

}
