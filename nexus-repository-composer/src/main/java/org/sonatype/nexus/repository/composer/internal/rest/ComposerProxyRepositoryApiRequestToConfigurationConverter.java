
package org.sonatype.nexus.repository.composer.internal.rest;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.ProxyRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Objects;

/**
 * @since 3.20
 */
@Named
public class ComposerProxyRepositoryApiRequestToConfigurationConverter
    extends ProxyRepositoryApiRequestToConfigurationConverter<ComposerProxyRepositoryApiRequest>
{
  private static final String COMPOSER = "composer";

  @Inject
  public ComposerProxyRepositoryApiRequestToConfigurationConverter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  @Override
  public Configuration convert(final ComposerProxyRepositoryApiRequest request) {
    Configuration configuration = super.convert(request);
    configuration.attributes(COMPOSER).set("versionPolicy", request.getComposer().getVersionPolicy());
    configuration.attributes(COMPOSER).set("layoutPolicy", request.getComposer().getLayoutPolicy());
    configuration.attributes(COMPOSER).set("contentDisposition", request.getComposer().getContentDisposition());
    NestedAttributesMap httpclient = configuration.attributes("httpclient");
    if (Objects.nonNull(httpclient.get("authentication"))) {
      httpclient.child("authentication").set("preemptive", request.getHttpClient().getAuthentication().isPreemptive());
    }
    return configuration;
  }
}
