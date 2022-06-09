
package org.sonatype.nexus.repository.composer.internal.rest;

import io.swagger.annotations.*;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.sonatype.nexus.repository.composer.internal.api.ComposerProxyApiRepository;
import org.sonatype.nexus.repository.rest.api.AbstractProxyRepositoriesApiResource;
import org.sonatype.nexus.repository.rest.api.FormatAndType;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.validation.Validate;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static org.sonatype.nexus.rest.ApiDocConstants.*;

/**
 * @since 3.20
 */
@Api(value = API_REPOSITORY_MANAGEMENT)
public abstract class ComposerProxyRepositoriesApiResource
    extends AbstractProxyRepositoriesApiResource<ComposerProxyRepositoryApiRequest>
{

  @ApiOperation("Create Composer proxy repository")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = REPOSITORY_CREATED),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
  })
  @POST
  @RequiresAuthentication
  @Validate
  @Override
  public Response createRepository(final ComposerProxyRepositoryApiRequest request) {
    return super.createRepository(request);
  }

  @ApiOperation("Update Composer proxy repository")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = REPOSITORY_UPDATED),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
  })
  @PUT
  @Path("/{repositoryName}")
  @RequiresAuthentication
  @Validate
  @Override
  public Response updateRepository(
      final ComposerProxyRepositoryApiRequest request,
      @ApiParam(value = "Name of the repository to update") @PathParam("repositoryName") final String repositoryName)
  {
    return super.updateRepository(request, repositoryName);
  }

  @GET
  @Path("/{repositoryName}")
  @RequiresAuthentication
  @Validate
  @ApiOperation(value = "Get repository", response = ComposerProxyApiRepository.class)
  @Override
  public AbstractApiRepository getRepository(@ApiParam(hidden = true) @BeanParam final FormatAndType formatAndType,
                                             @PathParam("repositoryName") final String repositoryName) {
    return super.getRepository(formatAndType, repositoryName);
  }
}
