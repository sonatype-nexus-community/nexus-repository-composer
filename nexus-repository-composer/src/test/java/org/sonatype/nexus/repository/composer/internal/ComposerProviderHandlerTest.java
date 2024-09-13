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
import org.sonatype.nexus.repository.composer.internal.proxy.ComposerProviderHandler;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.composer.internal.proxy.ComposerProviderHandler.DO_NOT_REWRITE;
import static org.sonatype.nexus.repository.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;

public class ComposerProviderHandlerTest
    extends TestSupport
{
  @Mock
  private ComposerJsonProcessor composerJsonProcessor;

  @Mock
  private Context context;

  @Mock
  private Request request;

  @Mock
  private Response response;

  @Mock
  private Repository repository;

  @Mock
  private AttributesMap requestAttributes;

  @Mock
  private Payload payload;

  @Mock
  private Payload rewrittenPayload;

  @Mock
  private Status status;

  private ComposerProviderHandler underTest;

  @Before
  public void setUp() throws Exception {
    when(composerJsonProcessor.rewriteProviderJson(repository, payload)).thenReturn(rewrittenPayload);

    when(context.getRequest()).thenReturn(request);
    when(context.getRepository()).thenReturn(repository);
    when(context.proceed()).thenReturn(response);

    when(request.getAttributes()).thenReturn(requestAttributes);
    when(requestAttributes.get(DO_NOT_REWRITE, String.class)).thenReturn("false");

    when(response.getStatus()).thenReturn(status);
    when(response.getPayload()).thenReturn(payload);
    when(status.getCode()).thenReturn(OK);

    underTest = new ComposerProviderHandler(composerJsonProcessor);
  }

  @Test
  public void handleWithRewriteDisabled() throws Exception {
    when(requestAttributes.get(DO_NOT_REWRITE, String.class)).thenReturn("true");

    Response output = underTest.handle(context);

    assertThat(output, is(response));
    verifyNoMoreInteractions(composerJsonProcessor);
  }

  @Test
  public void handleWithRewriteEnabled() throws Exception {
    Response output = underTest.handle(context);

    assertThat(output, is(not(response)));
    assertThat(output.getPayload(), is(rewrittenPayload));

    verify(composerJsonProcessor).rewriteProviderJson(repository, payload);
  }

  @Test
  public void handleWithRewriteEnabledNotOK() throws Exception {
    when(status.getCode()).thenReturn(INTERNAL_SERVER_ERROR);

    Response output = underTest.handle(context);

    assertThat(output, is(response));
    verifyNoMoreInteractions(composerJsonProcessor);
  }

  @Test
  public void handleWithRewriteEnabledNoPayload() throws Exception {
    when(response.getPayload()).thenReturn(null);

    Response output = underTest.handle(context);

    assertThat(output, is(response));
    verifyNoMoreInteractions(composerJsonProcessor);
  }
}
