
package org.sonatype.nexus.repository.composer.internal.rest;

import io.swagger.annotations.Api;
import org.sonatype.nexus.repository.rest.api.RepositoriesApiResourceBeta;
import org.sonatype.nexus.rest.APIConstants;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

/**
 * @since 3.26
 * @deprecated the 'beta' prefix is being phased out, prefer starting new APIs with {@link APIConstants#V1_API_PREFIX}
 * instead. Support backward compatibility.
 */
@Api(hidden = true)
@Named
@Singleton
@Path(ComposerHostedRepositoriesApiResourceBeta.RESOURCE_URI)
@Deprecated
public class ComposerHostedRepositoriesApiResourceBeta
    extends ComposerHostedRepositoriesApiResource
{
  static final String RESOURCE_URI = RepositoriesApiResourceBeta.RESOURCE_URI + "/composer/hosted";
}
