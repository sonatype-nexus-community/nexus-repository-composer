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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.*;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.junit.Test;
import org.mockito.Mock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport.*;

public class ComposerHostedUploadHandlerTest
    extends TestSupport
{
  private ComposerHostedUploadHandler underTest = new ComposerHostedUploadHandler();

  @Mock
  private Map<String, String> tokens;

  @Mock
  private TokenMatcher.State state;

  @Mock
  private ComposerHostedFacet composerHostedFacet;

  @Mock
  private Repository repository;

  @Mock
  private Context context;

  @Mock
  private Request request;

  @Mock
  private Payload payload;

  @Mock
  private AttributesMap attributes;

  @Test
  public void testHandleClassic() throws Exception {
    when(repository.facet(ComposerHostedFacet.class)).thenReturn(composerHostedFacet);
    when(request.getPayload()).thenReturn(payload);
    when(context.getRepository()).thenReturn(repository);
    when(context.getAttributes()).thenReturn(attributes);
    when(context.getRequest()).thenReturn(request);

    when(attributes.require(TokenMatcher.State.class)).thenReturn(state);
    when(state.getTokens()).thenReturn(tokens);
    when(tokens.get(VENDOR_TOKEN)).thenReturn("testvendor");
    when(tokens.get(PROJECT_TOKEN)).thenReturn("testproject");
    when(tokens.get(VERSION_TOKEN)).thenReturn("testversion");

    Response response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(200));
    assertThat(response.getPayload(), is(nullValue()));

    verify(composerHostedFacet).upload("testvendor", "testproject", "testversion", null, null, null, payload);
  }

  @Test
  public void testHandleMultipartWithSource() throws Exception {
    when(repository.facet(ComposerHostedFacet.class)).thenReturn(composerHostedFacet);
    List<PartPayload> parts = new ArrayList<>();
    PartPayload sourceTypeField = mock(PartPayload.class);
    when(sourceTypeField.getFieldName()).thenReturn(SOURCE_TYPE_FIELD_NAME);
    when(sourceTypeField.openInputStream()).thenReturn(new ByteArrayInputStream("srcType".getBytes(UTF_8)));
    PartPayload sourceUrlField = mock(PartPayload.class);
    when(sourceUrlField.getFieldName()).thenReturn(SOURCE_URL_FIELD_NAME);
    when(sourceUrlField.openInputStream()).thenReturn(new ByteArrayInputStream("srcUrl".getBytes(UTF_8)));
    PartPayload sourceRefField = mock(PartPayload.class);
    when(sourceRefField.getFieldName()).thenReturn(SOURCE_REFERENCE_FIELD_NAME);
    when(sourceRefField.openInputStream()).thenReturn(new ByteArrayInputStream("srcRef".getBytes(UTF_8)));
    PartPayload packageField = mock(PartPayload.class);
    when(packageField.getFieldName()).thenReturn(PACKAGE_FIELD_NAME);
    parts.add(packageField);
    parts.add(sourceUrlField);
    parts.add(sourceTypeField);
    parts.add(sourceRefField);
    when(request.getMultiparts()).thenReturn(parts);
    when(request.isMultipart()).thenReturn(true);
    when(context.getRepository()).thenReturn(repository);
    when(context.getAttributes()).thenReturn(attributes);
    when(context.getRequest()).thenReturn(request);

    when(attributes.require(TokenMatcher.State.class)).thenReturn(state);
    when(state.getTokens()).thenReturn(tokens);
    when(tokens.get(VENDOR_TOKEN)).thenReturn("testvendor");
    when(tokens.get(PROJECT_TOKEN)).thenReturn("testproject");
    when(tokens.get(VERSION_TOKEN)).thenReturn("testversion");

    Response response = underTest.handle(context);
    assertThat(response.getStatus().getCode(), is(200));
    assertThat(response.getPayload(), is(nullValue()));

    verify(composerHostedFacet).upload("testvendor", "testproject", "testversion", "srcType", "srcUrl", "srcRef", packageField);
  }
}
