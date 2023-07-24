package org.sonatype.nexus.repository.composer.rest;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.sonatype.nexus.repository.rest.api.RepositoriesApiResourceV1;

@Named
@Singleton
@Path(ComposerGroupRepositoriesApiResourceV1.RESOURCE_URI)
public class ComposerGroupRepositoriesApiResourceV1 extends ComposerGroupRepositoriesApiResource
{
  static final String RESOURCE_URI = RepositoriesApiResourceV1.RESOURCE_URI + "/composer/group";
}
