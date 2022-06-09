
package org.sonatype.nexus.repository.composer.internal.rest;

import io.swagger.annotations.*;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.sonatype.nexus.repository.composer.internal.api.ComposerHostedApiRepository;
import org.sonatype.nexus.repository.rest.api.AbstractHostedRepositoriesApiResource;
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
public abstract class ComposerHostedRepositoriesApiResource
    extends AbstractHostedRepositoriesApiResource<ComposerHostedRepositoryApiRequest>
{

  @ApiOperation("Create Composer hosted repository")
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = REPOSITORY_CREATED),
      @ApiResponse(code = 401, message = AUTHENTICATION_REQUIRED),
      @ApiResponse(code = 403, message = INSUFFICIENT_PERMISSIONS)
  })
  @POST
  @RequiresAuthentication
  @Validate
  @Override
  public Response createRepository(final ComposerHostedRepositoryApiRequest request) {
    return super.createRepository(request);
  }

  @ApiOperation("Update Composer hosted repository")
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
      final ComposerHostedRepositoryApiRequest request,
      @ApiParam(value = "Name of the repository to update") @PathParam("repositoryName") final String repositoryName)
  {
    return super.updateRepository(request, repositoryName);
  }

  @GET
  @Path("/{repositoryName}")
  @RequiresAuthentication
  @Validate
  @Override
  @ApiOperation(value = "Get repository", response = ComposerHostedApiRepository.class)
  public AbstractApiRepository getRepository(@ApiParam(hidden = true) @BeanParam final FormatAndType formatAndType,
                                             @PathParam("repositoryName") final String repositoryName) {
    return super.getRepository(formatAndType, repositoryName);
  }
}
