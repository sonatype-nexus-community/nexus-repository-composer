
package org.sonatype.nexus.repository.composer.internal.rest;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.HostedRepositoryApiRequestToConfigurationConverter;

import javax.inject.Named;

/**
 * @since 3.20
 */
@Named
public class ComposerHostedRepositoryApiRequestToConfigurationConverter
    extends HostedRepositoryApiRequestToConfigurationConverter<ComposerHostedRepositoryApiRequest>
{
  private static final String COMPOSER = "composer";

  @Override
  public Configuration convert(final ComposerHostedRepositoryApiRequest request) {
    Configuration configuration = super.convert(request);
    configuration.attributes(COMPOSER).set("versionPolicy", request.getComposer().getVersionPolicy());
    configuration.attributes(COMPOSER).set("layoutPolicy", request.getComposer().getLayoutPolicy());
    configuration.attributes(COMPOSER).set("contentDisposition", request.getComposer().getContentDisposition());
    return configuration;
  }
}
