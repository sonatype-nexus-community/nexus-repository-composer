/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.composer.internal.hosted;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.ComposerContentFacet;
import org.sonatype.nexus.repository.composer.internal.ComposerJsonProcessor;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.content.fluent.internal.FluentComponentQueryImpl;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class ComposerHostedFacetImplTest
    extends TestSupport
{
  private static final String VENDOR = "vendor";

  private static final String PROJECT = "project";

  private static final String VERSION = "version";

  private static final String SRC_TYPE = "src-type";

  private static final String SRC_URL = "src-url";

  private static final String SRC_REF = "src-ref";

  private static final String ZIPBALL_PATH = "/vendor/project/version/vendor-project-version.zip";

  private static final String PROVIDER_PATH = "/p/vendor/project.json";

  @Mock
  private Repository repository;

  @Mock
  private ComposerContentFacet composerContentFacet;

  @Mock
  private ComposerJsonProcessor composerJsonProcessor;

  @Mock
  private Payload payload;

  @Mock
  private Content content;

  @Mock
  private FluentComponents components;

  private ComposerHostedFacetImpl underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.facet(ComposerContentFacet.class)).thenReturn(composerContentFacet);
    when(composerContentFacet.components()).thenReturn(components);

    underTest = spy(new ComposerHostedFacetImpl(composerJsonProcessor));
    underTest.attach(repository);
  }

  @Test
  public void testUpload() throws Exception {
    underTest.upload(VENDOR, PROJECT, VERSION, SRC_TYPE, SRC_URL, SRC_REF, payload);
    verify(composerContentFacet).put(ZIPBALL_PATH, payload, SRC_TYPE, SRC_URL, SRC_REF);
  }

  @Test
  public void testGetZipball() throws Exception {
    when(composerContentFacet.get(ZIPBALL_PATH)).thenReturn(Optional.of(content));
    assertThat(underTest.getZipball(ZIPBALL_PATH), is(content));
  }

  @Test
  public void testGetPackagesJson() throws Exception {
    when(composerJsonProcessor.generatePackagesFromComponents(repository, components)).thenReturn(content);
    assertThat(underTest.getPackagesJson(), is(content));
  }

  @Test
  public void testGetListJson() throws Exception {
    // Without filter
    when(composerJsonProcessor.generateListFromComponents(components)).thenReturn(content);
    assertThat(underTest.getListJson(null), is(content));

    // With filter
    FluentQuery<FluentComponent> query = mock(FluentComponentQueryImpl.class);
    when(components.byFilter("namespace LIKE #{filterParams.vendor} AND name LIKE #{filterParams.project}",
        ImmutableMap.of("vendor", "test", "project", "%"))).thenReturn(query);
    when(composerJsonProcessor.generateListFromComponents(query)).thenReturn(content);
    assertThat(underTest.getListJson("test/*"), is(content));

    when(components.byFilter("namespace LIKE #{filterParams.vendor} AND name LIKE #{filterParams.project}",
        ImmutableMap.of("vendor", "%abc%", "project", "pr0_j3cT"))).thenReturn(query);
    when(composerJsonProcessor.generateListFromComponents(query)).thenReturn(content);
    assertThat(underTest.getListJson("*abc**/pr0_j3cT"), is(content));

    // Invalid filter
    when(composerJsonProcessor.generateListFromComponents(null)).thenReturn(content);
    assertThat(underTest.getListJson("In\\al1d"), is(content));
  }

  @Test
  public void testGetProviderJson() throws Exception {
    when(composerContentFacet.get(PROVIDER_PATH)).thenReturn(Optional.of(content));
    assertThat(underTest.getProviderJson(VENDOR, PROJECT), is(content));
  }

  @Test
  public void testBuildProviderJson() throws Exception {
    ArgumentCaptor<String> filter = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map<String, Object>> filterArgs = ArgumentCaptor.forClass(Map.class);

    FluentQuery<FluentComponent> query = mock(FluentQuery.class);
    when(components.byFilter(filter.capture(), filterArgs.capture())).thenReturn(query);
    when(composerJsonProcessor.buildProviderJson(repository, composerContentFacet, query))
        .thenReturn(Optional.of(content));

    Optional<Content> res = underTest.rebuildProviderJson(VENDOR, PROJECT);
    assertThat(res.isPresent(), is(true));
    assertThat(res.get(), is(content));
    assertThat(filter.getValue(), is("namespace = #{filterParams.vendor} AND name = #{filterParams.project}"));
    assertThat(filterArgs.getValue(), is(ImmutableMap.of("vendor", VENDOR, "project", PROJECT)));
  }
}
