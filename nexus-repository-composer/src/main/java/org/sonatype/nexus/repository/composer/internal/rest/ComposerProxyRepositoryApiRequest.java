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
package org.sonatype.nexus.repository.composer.internal.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.composer.internal.api.ComposerAttributes;
import org.sonatype.nexus.repository.rest.api.model.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @since 3.20
 */
@JsonIgnoreProperties({"format", "type"})
public class ComposerProxyRepositoryApiRequest
    extends ProxyRepositoryApiRequest
{
  @NotNull
  protected final ComposerAttributes composer;

  @NotNull
  @Valid
  protected final HttpClientAttributesWithPreemptiveAuth httpClient;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  @SuppressWarnings("squid:S00107") // suppress constructor parameter count
  public ComposerProxyRepositoryApiRequest(
      @JsonProperty("name") final String name,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final StorageAttributes storage,
      @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
      @JsonProperty("proxy") final ProxyAttributes proxy,
      @JsonProperty("negativeCache") final NegativeCacheAttributes negativeCache,
      @JsonProperty("httpClient") final HttpClientAttributesWithPreemptiveAuth httpClient,
      @JsonProperty("routingRule") final String routingRule,
      @JsonProperty("composer") final ComposerAttributes composer)
  {
    super(name, ComposerFormat.NAME, online, storage, cleanup, proxy, negativeCache, httpClient, routingRule);
    this.composer = composer;
    this.httpClient = httpClient;
  }

  public ComposerAttributes getComposer() {
    return composer;
  }

  @Override
  public HttpClientAttributesWithPreemptiveAuth getHttpClient() {
    return httpClient;
  }
}
