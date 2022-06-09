/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.composer.internal.api;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.composer.internal.ContentDisposition;
import org.sonatype.nexus.repository.composer.internal.LayoutPolicy;
import org.sonatype.nexus.repository.composer.internal.VersionPolicy;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.rest.api.model.SimpleApiGroupRepository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import java.util.Arrays;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.AdditionalMatchers.not;

public class ComposerApiRepositoryAdapterTest
    extends TestSupport
{
  private ComposerApiRepositoryAdapter underTest;

  @Mock
  private RoutingRuleStore routingRuleStore;

  @Before
  public void setup() {
    underTest = new ComposerApiRepositoryAdapter(routingRuleStore);
    BaseUrlHolder.set("http://nexus-url", "");
  }

  @Test
  public void testAdapt_groupRepository() throws Exception {
    // No Composer specific props so simple smoke test
    Repository repository = createRepository(new GroupType());
    Configuration configuration = repository.getConfiguration();
    configuration.attributes("group").set("memberNames", Arrays.asList("a", "b"));
    repository.update(configuration);

    SimpleApiGroupRepository groupRepository = (SimpleApiGroupRepository) underTest.adapt(repository);
    assertRepository(groupRepository, "group", true);
  }

  @Test
  public void testAdapt_hostedRepository() throws Exception {
    Repository repository = createRepository(new HostedType(), LayoutPolicy.STRICT, VersionPolicy.MIXED, ContentDisposition.INLINE);

    ComposerHostedApiRepository hostedRepository = (ComposerHostedApiRepository) underTest.adapt(repository);
    assertRepository(hostedRepository, "hosted", true);
    assertThat(hostedRepository.getComposer().getLayoutPolicy(), is("STRICT"));
    assertThat(hostedRepository.getComposer().getVersionPolicy(), is("MIXED"));
    assertThat(hostedRepository.getComposer().getContentDisposition(), is("INLINE"));
    // Check fields are populated, actual values validated with SimpleApiRepositoryAdapterTest
    assertThat(hostedRepository.getCleanup(), nullValue());
    assertThat(hostedRepository.getStorage(), notNullValue());
  }

  @Test
  public void testAdapt_proxyRepository() throws Exception {
    Repository repository = createRepository(new ProxyType(), LayoutPolicy.STRICT, VersionPolicy.MIXED, ContentDisposition.INLINE);

    ComposerProxyApiRepository proxyRepository = (ComposerProxyApiRepository) underTest.adapt(repository);
    assertRepository(proxyRepository, "proxy", true);
    assertThat(proxyRepository.getComposer().getLayoutPolicy(), is("STRICT"));
    assertThat(proxyRepository.getComposer().getVersionPolicy(), is("MIXED"));
    assertThat(proxyRepository.getComposer().getContentDisposition(), is("INLINE"));
    // Check fields are populated, actual values validated with SimpleApiRepositoryAdapterTest
    assertThat(proxyRepository.getCleanup(), nullValue());
    assertThat(proxyRepository.getHttpClient(), notNullValue());
    assertThat(proxyRepository.getNegativeCache(), notNullValue());
    assertThat(proxyRepository.getProxy(), notNullValue());
    assertThat(proxyRepository.getStorage(), notNullValue());
  }

  private static void assertRepository(
      final AbstractApiRepository repository,
      final String type,
      final Boolean online)
  {
    assertThat(repository.getFormat(), is("composer"));
    assertThat(repository.getName(), is("my-repo"));
    assertThat(repository.getOnline(), is(online));
    assertThat(repository.getType(), is(type));
    assertThat(repository.getUrl(), is(BaseUrlHolder.get() + "/repository/my-repo"));
  }

  private static Configuration config(final String repositoryName) {
    Configuration configuration = mock(Configuration.class);
    when(configuration.isOnline()).thenReturn(true);
    when(configuration.getRepositoryName()).thenReturn(repositoryName);
    when(configuration.attributes(not(eq("composer")))).thenReturn(new NestedAttributesMap("dummy", newHashMap()));

    return configuration;
  }

  private static Repository createRepository(final Type type) throws Exception {
    Repository repository = new RepositoryImpl(Mockito.mock(EventManager.class), type, new ComposerFormat());
    repository.init(config("my-repo"));
    return repository;
  }

  private static Repository createRepository(
      final Type type,
      final LayoutPolicy layoutPolicy,
      final VersionPolicy versionPolicy,
      final ContentDisposition contentDisposition) throws Exception
  {
    Repository repository = new RepositoryImpl(Mockito.mock(EventManager.class), type, new ComposerFormat());

    Configuration configuration = config("my-repo");
    NestedAttributesMap composer = new NestedAttributesMap("composer", newHashMap());
    composer.set("layoutPolicy", layoutPolicy.toString());
    composer.set("versionPolicy", versionPolicy.toString());
    composer.set("contentDisposition", contentDisposition.toString());
    when(configuration.attributes("composer")).thenReturn(composer);
    repository.init(configuration);
    return repository;
  }
}
