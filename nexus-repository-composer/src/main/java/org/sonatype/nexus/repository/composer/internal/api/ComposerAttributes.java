
package org.sonatype.nexus.repository.composer.internal.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;

/**
 * REST API model for describing composer specific repository properties.
 *
 * @since 3.20
 */
public class ComposerAttributes
{
  @ApiModelProperty(value = "What type of artifacts does this repository store?",
      allowableValues = "RELEASE,SNAPSHOT,MIXED",
      example = "MIXED")
  @NotEmpty
  protected final String versionPolicy;

  @ApiModelProperty(value = "Validate that all paths are composer artifact or metadata paths",
      allowableValues = "STRICT,PERMISSIVE",
      example = "STRICT")
  @NotEmpty
  protected final String layoutPolicy;

  @ApiModelProperty(value = "Content Disposition",
      allowableValues = "INLINE,ATTACHMENT", example = "ATTACHMENT")
  @NotEmpty
  private final String contentDisposition;

  @JsonCreator
  public ComposerAttributes(
      @JsonProperty("versionPolicy") final String versionPolicy,
      @JsonProperty("layoutPolicy") final String layoutPolicy,
      @JsonProperty("contentDisposition") final String contentDisposition)
  {
    this.versionPolicy = versionPolicy;
    this.layoutPolicy = layoutPolicy;
    this.contentDisposition = contentDisposition;
  }

  public String getVersionPolicy() {
    return versionPolicy;
  }

  public String getLayoutPolicy() {
    return layoutPolicy;
  }

  public String getContentDisposition() {
    return contentDisposition;
  }
}
