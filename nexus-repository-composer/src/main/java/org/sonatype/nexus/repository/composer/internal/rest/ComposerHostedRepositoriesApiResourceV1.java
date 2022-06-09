
package org.sonatype.nexus.repository.composer.internal.rest;

import org.sonatype.nexus.repository.rest.api.RepositoriesApiResourceV1;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

/**
 * @since 3.26
 */
@Named
@Singleton
@Path(ComposerHostedRepositoriesApiResourceV1.RESOURCE_URI)
public class ComposerHostedRepositoriesApiResourceV1
    extends ComposerHostedRepositoriesApiResource
{
  static final String RESOURCE_URI = RepositoriesApiResourceV1.RESOURCE_URI + "/composer/hosted";
}
