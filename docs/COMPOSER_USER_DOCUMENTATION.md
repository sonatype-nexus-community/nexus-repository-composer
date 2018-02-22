<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2018-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
## Composer Repositories

### Introduction

[Composer](https://getcomposer.org/) is a dependency manager for the PHP programming language. Composer allows developers
to install packages from a variety of repositories, particularly the main Composer-format repository hosted at
[https://packagist.org](https://packagist.org/).

The Formats Team has been investigating approaches to add Composer support to the Nexus 3 Repository Manager in response
to customer inquiries. Our current efforts have involved the creation of various spike approaches for proxying the main
Packagist repository, and as a result of community interest in developing an open-source Composer format plugin, we have
elected to release our current work to the community.

_This code is not feature-complete or production-ready. It is provided as a potential starting point to the community
based on external interest. Many important features are absent, but we do have some ideas on how to approach these tasks
if anyone is interested in collaborating._

### Proxying Composer Repositories

The current spike implementation makes certain assumptions about the layout of the upstream repository and is only
intended for use with the main Composer repository at [https://packagist.org](https://packagist.org/).

To proxy a Composer repository, you simply create a new 'composer (proxy)' as documented in [Repository Management](https://help.sonatype.com/display/NXRM3/Configuration#Configuration-RepositoryManagement)
in detail. Minimal configuration steps are:

- Define 'Name'
- Define URL for 'Remote storage' e.g. [https://packagist.org/](https://packagist.org/)
- Select a 'Blob store' for 'Storage'

### Configuring Composer 

The least-invasive way of configuring the Composer client to communicate with Nexus is to update the `repositories`
section in the `composer.json` for your particular project. We also recommend turning off `packagist.org` access so
that all requests are directed to your Nexus repository manager.

```
{
  "repositories": [
    {
      "type": "composer",
      "url": "http://localhost:8081/repository/packagist"
    },
    {
      "packagist.org": false
    }
  ]
}
```

### Browsing Composer Repository Packages

You can browse Composer repositories in the user interface inspecting the components and assets and their details, as
described in [Browsing Repositories and Repository Groups](https://help.sonatype.com/display/NXRM3/Browsing+Repositories+and+Repository+Groups).
