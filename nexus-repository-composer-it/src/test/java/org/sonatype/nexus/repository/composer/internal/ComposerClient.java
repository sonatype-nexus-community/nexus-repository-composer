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

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.http.entity.ContentType;
import org.sonatype.nexus.testsuite.testsupport.FormatClientSupport;

import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;

import static com.google.common.base.Preconditions.checkNotNull;

public class ComposerClient
    extends FormatClientSupport
{ public ComposerClient(
    final CloseableHttpClient httpClient,
    final HttpClientContext httpClientContext,
    final URI repositoryBaseUri)
  {
    super(httpClient, httpClientContext, repositoryBaseUri);
  }

  public int put(final String path, final File file) throws Exception {
    checkNotNull(path);
    checkNotNull(file);

    HttpPut put = new HttpPut(repositoryBaseUri.resolve(path));
    put.setEntity(EntityBuilder.create().setContentType(ContentType.parse("application/zip")).setFile(file).build());
    return status(execute(put));
  }

  public int put(String path, String string) throws IOException {
    checkNotNull(path);
    checkNotNull(string);
    HttpPut put = new HttpPut(repositoryBaseUri.resolve(path));
    put.setEntity(EntityBuilder.create().setContentType(ContentType.parse("application/json")).setText(string).build());
    return status(execute(put));
  }
}
