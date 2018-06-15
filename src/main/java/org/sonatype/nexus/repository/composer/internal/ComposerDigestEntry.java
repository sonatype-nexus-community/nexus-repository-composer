package org.sonatype.nexus.repository.composer.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON entry representing a sha256 key contained in a Composer metadata file.
 */
public class ComposerDigestEntry
{
  @JsonProperty("sha256")
  private String sha256;

  public String getSha256() {
    return sha256;
  }

  public void setSha256(final String sha256) {
    this.sha256 = sha256;
  }
}
