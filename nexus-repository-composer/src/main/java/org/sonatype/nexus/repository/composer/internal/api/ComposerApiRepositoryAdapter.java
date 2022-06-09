
package org.sonatype.nexus.repository.composer.internal.api;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.composer.internal.rest.HttpClientAttributesWithPreemptiveAuth;
import org.sonatype.nexus.repository.composer.internal.rest.HttpClientConnectionAuthenticationAttributesWithPreemptive;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Adapter to expose composer specific repository configuration for the repositories REST API.
 *
 * @since 3.20
 */
@Named(ComposerFormat.NAME)
public class ComposerApiRepositoryAdapter
    extends SimpleApiRepositoryAdapter
{
  private static final String COMPOSER = "composer";

  @Inject
  public ComposerApiRepositoryAdapter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  @Override
  public AbstractApiRepository adapt(final Repository repository) {
    boolean online = repository.getConfiguration().isOnline();
    String name = repository.getName();
    String url = repository.getUrl();

    switch (repository.getType().toString()) {
      case HostedType.NAME:
        return new ComposerHostedApiRepository(
            name,
            url,
            online,
            getHostedStorageAttributes(repository),
            getCleanupPolicyAttributes(repository),
            createComposerAttributes(repository),
            getComponentAttributes(repository));
      case ProxyType.NAME:
        return new ComposerProxyApiRepository(name, url, online,
            getHostedStorageAttributes(repository),
            getCleanupPolicyAttributes(repository),
            getProxyAttributes(repository),
            getNegativeCacheAttributes(repository),
            getHttpClientAttributes(repository),
            getRoutingRuleName(repository),
            createComposerAttributes(repository));
      default:
        return super.adapt(repository);
    }
  }

  private ComposerAttributes createComposerAttributes(final Repository repository) {
    String versionPolicy = repository.getConfiguration().attributes(COMPOSER).get("versionPolicy", String.class);
    String layoutPolicy = repository.getConfiguration().attributes(COMPOSER).get("layoutPolicy", String.class);
    String contentDisposition = repository.getConfiguration().attributes(COMPOSER).get("contentDisposition", String.class);
    return new ComposerAttributes(versionPolicy, layoutPolicy, contentDisposition);
  }

  @Override
  protected HttpClientAttributesWithPreemptiveAuth getHttpClientAttributes(final Repository repository) {
    HttpClientAttributes httpClientAttributes = super.getHttpClientAttributes(repository);
    HttpClientConnectionAuthenticationAttributesWithPreemptive authentication = null;

    Configuration configuration = repository.getConfiguration();
    NestedAttributesMap httpclient = configuration.attributes("httpclient");
    if (httpclient.contains("authentication")) {
      NestedAttributesMap authenticationMap = httpclient.child("authentication");
      Boolean preemptive = authenticationMap.get("preemptive", Boolean.class);

      authentication = new HttpClientConnectionAuthenticationAttributesWithPreemptive(httpClientAttributes.getAuthentication(),
          preemptive);
    }

    return new HttpClientAttributesWithPreemptiveAuth(httpClientAttributes, authentication);
  }
}
