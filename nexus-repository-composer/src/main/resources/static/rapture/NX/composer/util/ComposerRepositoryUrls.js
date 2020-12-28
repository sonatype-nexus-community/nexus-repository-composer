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
Ext.define('NX.composer.util.ComposerRepositoryUrls', {
  '@aggregate_priority': 90,

  singleton: true,
  requires: [
    'NX.coreui.util.RepositoryUrls',
    'NX.util.Url'
  ]
}, function(self) {
  NX.coreui.util.RepositoryUrls.addRepositoryUrlStrategy('composer', function(me, assetModel) {
    var repositoryName = assetModel.get('repositoryName'), assetName = assetModel.get('name');
    return NX.util.Url.asLink(
        NX.util.Url.baseUrl + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodeURI(assetName),
        assetName);
  });
});
