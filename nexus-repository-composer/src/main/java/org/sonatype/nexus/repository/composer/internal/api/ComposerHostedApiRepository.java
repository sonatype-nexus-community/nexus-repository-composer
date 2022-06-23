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
package org.sonatype.nexus.repository.composer.internal.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.ComponentAttributes;
import org.sonatype.nexus.repository.rest.api.model.HostedStorageAttributes;
import org.sonatype.nexus.repository.rest.api.model.SimpleApiHostedRepository;

import javax.validation.constraints.NotNull;

/**
 * REST API model for a composer hosted repository.
 *
 * @since 3.20
 */
@JsonIgnoreProperties(value = {"format", "type", "url"}, allowGetters = true)
public class ComposerHostedApiRepository
    extends SimpleApiHostedRepository
{
  @NotNull
  protected final ComposerAttributes composer;

  @JsonCreator
  public ComposerHostedApiRepository(
      @JsonProperty("name") final String name,
      @JsonProperty("url") final String url,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final HostedStorageAttributes storage,
      @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
      @JsonProperty("composer") final ComposerAttributes composer,
      @JsonProperty("component") final ComponentAttributes component)
  {
    super(name, ComposerFormat.NAME, url, online, storage, cleanup, component);
    this.composer = composer;
  }

  public ComposerAttributes getComposer() {
    return composer;
  }
}