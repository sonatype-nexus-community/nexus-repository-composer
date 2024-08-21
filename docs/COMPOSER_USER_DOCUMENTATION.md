<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2018-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at https://www.sonatype.com/usage/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at https://www.eclipse.org/legal/epl/epl-v10.html.

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

To proxy a Composer repository, you simply create a new 'composer (proxy)' as documented in [Repository Management](https://help.sonatype.com/en/nexus-repository-administration.html#Configuration-RepositoryManagement)
in detail. Minimal configuration steps are:

- Define 'Name'
- Define URL for 'Remote storage' e.g. [https://packagist.org/](https://packagist.org/)
- Select a 'Blob store' for 'Storage'

### Hosting Composer Repositories

Creating a Composer hosted repository allows you to register packages in the repository manager. The hosted
repository acts as an authoritative location for these components.

To add a hosted Composer repository, create a new repository with the recipe 'composer (hosted)' as
documented in [Repository Management](https://help.sonatype.com/display/NXRM3/Configuration#Configuration-RepositoryManagement).

Minimal configuration steps are:

- Define 'Name' - e.g. `composer-hosted`
- Select 'Blob store' for 'Storage'

### Configuring Composer 

The least-invasive way of configuring the Composer client to communicate with Nexus is to update the `repositories`
section in the `composer.json` for your particular project. We also recommend [disabling](https://getcomposer.org/doc/05-repositories.md#disabling-packagist-org) `packagist.org` access so
that all requests are directed to your Nexus repository manager. The following settings assumes the URL proviced by Nexus is `https://localhost:8081/repository/packagist/`. If you named your Composer repository another name substitute the URL provided to you by Nexus. Note that the trailing slash at the end of the URL is required by Nexus to operate correctly.

Composer [supplies commands](https://getcomposer.org/doc/03-cli.md#modifying-repositories) to alter your `composer.json`. To add your Nexus repo issue the following command in your project
`composer config repo.foo '{"type": "composer", "url": "https://localhost:8081/repository/packagist/"}'` or `composer config repo.foo composer https://localhost:8081/repository/packagist/` where `foo` is just an indexed name. Numbers may also be used for the index.

If you want do disable Packagist for your project issue this command `composer config repo.packagist false`.

If you want to disable Packagist globally (say you are running in a Docker container) issue this command `composer config -g repo.packagist false`.

If you would like to edit the composer.json manually the entry would look as follows:

```
{
  "repositories": [
    {
      "type": "composer",
      "url": "https://localhost:8081/repository/packagist/"
    },
    {
      "packagist.org": false
    }
  ]
}
```

By default, Composer will only allow HTTPS URLs. To allow non-SSL URLs you will have to set [secure-http](https://getcomposer.org/doc/06-config.md#secure-http) to `false`.

### Browsing Composer Repository Packages

You can browse Composer repositories in the user interface inspecting the components and assets and their details, as
described in [Browsing Repositories and Repository Groups](https://help.sonatype.com/en/browsing-repositories-and-repository-groups.html).

### Publishing Composer Packages

If you are authoring your own packages and want to distribute them to other users in your organization, you have
to upload them to a hosted repository on the repository manager. The consumers can then download it via the
repository.

A Composer package should consist of a zipped archive of the sources containing a `composer.json` file. You should be
able to determine the vendor and project of the package from the `composer.json` file. The version can be determined
based on your own particular development process (for example, the version is not required in `composer.json` files and
may instead be something like a Git tag for a release depending on your local arrangements and preferences).

With this information known, the package can be uploaded to your hosted Composer repository (replacing the credentials,
source filename, and vendor, project, and version path segments to match your particular distributable):

`curl -v --user 'user:pass' --upload-file example.zip http://localhost:8081/repository/composer-hosted/packages/upload/vendor/project/version`

*Note that the relevant information for vendor, project, and version is obtained from the URL you use to upload the zip,
not the composer.json file contained in the archive.* This flexible upload mechanism allows you to avoid changing the
contents of your `composer.json` in order to upload to Nexus. For example, you could write a script to check out new
tags from your Git repo, construct the appropriate upload URL, then push the tagged releases from your Git repo to your
Nexus hosted repository.
