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

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.HostedRepositoryApiRequestToConfigurationConverter;

import javax.inject.Named;

/**
 * @since 3.20
 */
@Named
public class ComposerHostedRepositoryApiRequestToConfigurationConverter
    extends HostedRepositoryApiRequestToConfigurationConverter<ComposerHostedRepositoryApiRequest>
{
  private static final String COMPOSER = "composer";

  @Override
  public Configuration convert(final ComposerHostedRepositoryApiRequest request) {
    Configuration configuration = super.convert(request);
    configuration.attributes(COMPOSER).set("versionPolicy", request.getComposer().getVersionPolicy());
    configuration.attributes(COMPOSER).set("layoutPolicy", request.getComposer().getLayoutPolicy());
    configuration.attributes(COMPOSER).set("contentDisposition", request.getComposer().getContentDisposition());
    return configuration;
  }
}
