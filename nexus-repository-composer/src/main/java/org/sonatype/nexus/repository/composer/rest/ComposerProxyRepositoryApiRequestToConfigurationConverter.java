package org.sonatype.nexus.repository.composer.rest;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.rest.api.ProxyRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

@Named
public class ComposerProxyRepositoryApiRequestToConfigurationConverter extends ProxyRepositoryApiRequestToConfigurationConverter<ComposerProxyRepositoryApiRequest>
{
  @Inject
  public ComposerProxyRepositoryApiRequestToConfigurationConverter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }
}
