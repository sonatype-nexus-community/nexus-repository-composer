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
package org.sonatype.nexus.repository.composer.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.composer.internal.ComposerProxyFacetImpl.NonResolvableProviderJsonException;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.LIST;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.PROVIDER;
import static org.sonatype.nexus.repository.composer.internal.AssetKind.ZIPBALL;

public class ComposerProxyFacetImplTest
    extends TestSupport
{
  private static final String LIST_PATH = "packages/list.json";

  private static final String PACKAGES_PATH = "packages.json";

  private static final String PROVIDER_PATH = "p/vendor/project.json";

  private static final String ZIPBALL_PATH = "vendor/project/version/project-version.zip";

  @Mock
  private Repository repository;

  @Mock
  private Context context;

  @Mock
  private AttributesMap contextAttributes;

  @Mock
  private ComposerContentFacet composerContentFacet;

  @Mock
  private ViewFacet viewFacet;

  @Mock
  private ComposerJsonProcessor composerJsonProcessor;

  @Mock
  private Content content;

  @Mock
  private TokenMatcher.State state;

  @Mock
  private CacheInfo cacheInfo;

  @Mock
  private Request request;

  @Mock
  private Response response;

  @Mock
  private Payload payload;

  private ComposerProxyFacetImpl underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new ComposerProxyFacetImpl(composerJsonProcessor);
    underTest.attach(repository);

    when(repository.facet(ComposerContentFacet.class)).thenReturn(composerContentFacet);
    when(repository.facet(ViewFacet.class)).thenReturn(viewFacet);

    when(context.getAttributes()).thenReturn(contextAttributes);
    when(context.getRequest()).thenReturn(request);
    when(context.getRepository()).thenReturn(repository);

    when(response.getPayload()).thenReturn(payload);
  }

  @Test
  public void getCachedContentPackages() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(PACKAGES);
    when(composerContentFacet.get(PACKAGES_PATH)).thenReturn(content);

    assertThat(underTest.getCachedContent(context), is(content));
  }

  @Test
  public void getCachedContentList() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(LIST);
    when(composerContentFacet.get(LIST_PATH)).thenReturn(content);

    assertThat(underTest.getCachedContent(context), is(content));
  }

  @Test
  public void getCachedContentProvider() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(PROVIDER);
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);

    when(composerContentFacet.get(PROVIDER_PATH)).thenReturn(content);

    when(state.getTokens()).thenReturn(new ImmutableMap.Builder<String, String>()
        .put("vendor", "vendor")
        .put("project", "project")
        .build());

    assertThat(underTest.getCachedContent(context), is(content));
  }

  @Test
  public void getCachedContentZipball() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(ZIPBALL);
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);

    when(composerContentFacet.get(ZIPBALL_PATH)).thenReturn(content);

    when(state.getTokens()).thenReturn(new ImmutableMap.Builder<String, String>()
        .put("vendor", "vendor")
        .put("project", "project")
        .put("version", "version")
        .put("name", "project-version")
        .build());

    assertThat(underTest.getCachedContent(context), is(content));
  }

  @Test
  public void indicateVerifiedPackages() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(PACKAGES);

    underTest.indicateVerified(context, content, cacheInfo);

    verify(composerContentFacet).setCacheInfo(PACKAGES_PATH, content, cacheInfo);
  }

  @Test
  public void indicateVerifiedList() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(LIST);

    underTest.indicateVerified(context, content, cacheInfo);

    verify(composerContentFacet).setCacheInfo(LIST_PATH, content, cacheInfo);
  }

  @Test
  public void indicateVerifiedProvider() throws Exception {
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);
    when(contextAttributes.require(AssetKind.class)).thenReturn(PROVIDER);

    when(state.getTokens()).thenReturn(new ImmutableMap.Builder<String, String>()
        .put("vendor", "vendor")
        .put("project", "project")
        .build());

    underTest.indicateVerified(context, content, cacheInfo);

    verify(composerContentFacet).setCacheInfo(PROVIDER_PATH, content, cacheInfo);
  }

  @Test
  public void indicateVerifiedZipball() throws Exception {
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);
    when(contextAttributes.require(AssetKind.class)).thenReturn(ZIPBALL);

    when(state.getTokens()).thenReturn(new ImmutableMap.Builder<String, String>()
        .put("vendor", "vendor")
        .put("project", "project")
        .put("version", "version")
        .put("name", "project-version")
        .build());

    underTest.indicateVerified(context, content, cacheInfo);

    verify(composerContentFacet).setCacheInfo(ZIPBALL_PATH, content, cacheInfo);
  }

  @Test
  public void storePackages() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(PACKAGES);
    when(composerContentFacet.put(PACKAGES_PATH, content, PACKAGES)).thenReturn(content);

    when(viewFacet.dispatch(any(Request.class), eq(context))).thenReturn(response);
    when(composerJsonProcessor.generatePackagesFromList(repository, payload)).thenReturn(content);

    assertThat(underTest.store(context, content), is(content));

    verify(composerContentFacet).put(PACKAGES_PATH, content, PACKAGES);
  }

  @Test
  public void storeList() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(LIST);
    when(composerContentFacet.put(LIST_PATH, content, LIST)).thenReturn(content);

    assertThat(underTest.store(context, content), is(content));

    verify(composerContentFacet).put(LIST_PATH, content, LIST);
  }

  @Test
  public void storeProvider() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(PROVIDER);
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);

    when(composerContentFacet.put(PROVIDER_PATH, content, PROVIDER)).thenReturn(content);

    when(state.getTokens()).thenReturn(new ImmutableMap.Builder<String, String>()
        .put("vendor", "vendor")
        .put("project", "project")
        .build());

    assertThat(underTest.store(context, content), is(content));

    verify(composerContentFacet).put(PROVIDER_PATH, content, PROVIDER);
  }

  @Test
  public void storeZipball() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(ZIPBALL);
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);

    when(composerContentFacet.put(ZIPBALL_PATH, content, ZIPBALL)).thenReturn(content);

    when(state.getTokens()).thenReturn(new ImmutableMap.Builder<String, String>()
        .put("vendor", "vendor")
        .put("project", "project")
        .put("version", "version")
        .put("name", "project-version")
        .build());

    assertThat(underTest.store(context, content), is(content));

    verify(composerContentFacet).put(ZIPBALL_PATH, content, ZIPBALL);
  }

  @Test
  public void getUrlPackages() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(LIST);
    when(request.getPath()).thenReturn("/" + PACKAGES_PATH);

    assertThat(underTest.getUrl(context), is(PACKAGES_PATH));
  }

  @Test
  public void getUrlList() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(LIST);
    when(request.getPath()).thenReturn("/" + LIST_PATH);

    assertThat(underTest.getUrl(context), is(LIST_PATH));
  }

  @Test
  public void getUrlProvider() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(LIST);
    when(request.getPath()).thenReturn("/" + PROVIDER_PATH);

    assertThat(underTest.getUrl(context), is(PROVIDER_PATH));
  }

  @Test
  public void getUrlZipball() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(ZIPBALL);
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);

    when(viewFacet.dispatch(any(Request.class), eq(context))).thenReturn(response);
    when(composerJsonProcessor.getDistUrl("vendor", "project", "version", payload)).thenReturn("distUrl");

    when(state.getTokens()).thenReturn(new ImmutableMap.Builder<String, String>()
        .put("vendor", "vendor")
        .put("project", "project")
        .put("version", "version")
        .put("name", "project-version")
        .build());

    assertThat(underTest.getUrl(context), is("distUrl"));
  }

  @Test(expected = NonResolvableProviderJsonException.class)
  public void getUrlZipballMissingProviderJson() throws Exception {
    when(contextAttributes.require(AssetKind.class)).thenReturn(ZIPBALL);
    when(contextAttributes.require(TokenMatcher.State.class)).thenReturn(state);

    when(viewFacet.dispatch(any(Request.class), eq(context))).thenReturn(response);
    when(response.getPayload()).thenReturn(null);

    when(state.getTokens()).thenReturn(new ImmutableMap.Builder<String, String>()
        .put("vendor", "vendor")
        .put("project", "project")
        .put("version", "version")
        .put("name", "project-version")
        .build());

    underTest.getUrl(context);
  }
}
