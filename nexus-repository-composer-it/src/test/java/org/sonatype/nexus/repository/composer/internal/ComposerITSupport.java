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

import java.net.URL;

import javax.annotation.Nonnull;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.internal.fixtures.RepositoryRuleComposer;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;

import org.junit.Rule;

import static com.google.common.base.Preconditions.checkNotNull;

public class ComposerITSupport
    extends RepositoryITSupport
{
  @Rule
  public RepositoryRuleComposer repos = new RepositoryRuleComposer(() -> repositoryManager);

  @Override
  protected RepositoryRuleComposer createRepositoryRule() {
    return new RepositoryRuleComposer(() -> repositoryManager);
  }

  public ComposerITSupport() {
    testData.addDirectory(NexusPaxExamSupport.resolveBaseFile("target/it-resources/composer"));
  }

  @Nonnull
  protected ComposerClient composerClient(final Repository repository) throws Exception {
    checkNotNull(repository);
    return composerClient(repositoryBaseUrl(repository));
  }

  protected ComposerClient composerClient(final URL repositoryUrl) throws Exception {
    return new ComposerClient(
        clientBuilder(repositoryUrl).build(),
        clientContext(),
        repositoryUrl.toURI()
    );
  }

  public static Asset findAsset(Repository repository, String path) {
    try (StorageTx tx = getStorageTx(repository)) {
      tx.begin();
      return tx.findAssetWithProperty(MetadataNodeEntityAdapter.P_NAME, path, tx.findBucket(repository));
    }
  }

  protected static StorageTx getStorageTx(final Repository repository) {
    return repository.facet(StorageFacet.class).txSupplier().get();
  }
}
