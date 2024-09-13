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
package org.sonatype.nexus.repository.composer;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.repository.cache.CacheControllerHolder.CONTENT;
import static org.sonatype.nexus.repository.cache.CacheControllerHolder.METADATA;
import static org.sonatype.nexus.repository.composer.AssetKind.LIST;
import static org.sonatype.nexus.repository.composer.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.composer.AssetKind.PROVIDER;
import static org.sonatype.nexus.repository.composer.AssetKind.ZIPBALL;

public class AssetKindTest
    extends TestSupport
{
  @Test
  public void cacheTypes() {
    assertThat(LIST.getCacheType(), is(METADATA));
    assertThat(PROVIDER.getCacheType(), is(METADATA));
    assertThat(PACKAGES.getCacheType(), is(METADATA));
    assertThat(ZIPBALL.getCacheType(), is(CONTENT));
  }
}
