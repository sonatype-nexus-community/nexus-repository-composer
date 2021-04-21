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
# Nexus Repository Composer Format

[![Maven Central](https://img.shields.io/maven-central/v/org.sonatype.nexus.plugins/nexus-repository-composer.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.sonatype.nexus.plugins%22%20AND%20a:%22nexus-repository-composer%22) [![CircleCI](https://circleci.com/gh/sonatype-nexus-community/nexus-repository-composer.svg?style=shield)](https://circleci.com/gh/sonatype-nexus-community/nexus-repository-composer) [![Join the chat at https://gitter.im/sonatype/nexus-developers](https://badges.gitter.im/sonatype/nexus-developers.svg)](https://gitter.im/sonatype/nexus-developers?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![DepShield Badge](https://depshield.sonatype.org/badges/sonatype-nexus-community/nexus-repository-composer/depshield.svg)](https://depshield.github.io)

# Table Of Contents
* [Developing](#developing)
   * [Requirements](#requirements)
   * [Building](#building)
* [Using Composer with Nexus Repository Manger 3](#using-composer-with-nexus-repository-manager-3)
* [Installing the plugin](#installing-the-plugin)
   * [Easiest Install](#easiest-install)
   * [Temporary Install](#temporary-install)
   * [(more) Permanent Install](#more-permanent-install)
   * [(most) Permament Install](#most-permanent-install)
* [The Fine Print](#the-fine-print)
* [Getting Help](#getting-help)
* [Composer Plugin](#composer-plugin)

## Developing

### Requirements

* [Apache Maven 3.3.3+](https://maven.apache.org/install.html)
* [Java 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* Network access to https://repository.sonatype.org/content/groups/sonatype-public-grid

Also, there is a good amount of information available at [Bundle Development](https://help.sonatype.com/display/NXRM3/Bundle+Development)

### Building

To build the project and generate the bundle use Maven

    mvn clean package

If everything checks out, the bundle for Composer should be available in the `target` folder

#### Build with Docker

    docker build -t nexus-repository-composer .

#### Run as a Docker container

    docker run -d -p 8081:8081 --name nexus-repository-composer nexus-repository-composer 

For further information like how to persist volumes check out [the GitHub repo for our official image](https://github.com/sonatype/docker-nexus3).

The application will now be available from your browser at http://localhost:8081

* As of Nexus Repository Manager Version 3.17, the default admin password is randomly generated.
  If running in a Docker container, you will need to view the generated password file 
  (/nexus-data/admin.password) in order to login to Nexus. The command below will open a bash shell 
  in the container named `nexus-repository-composer`:

      docker exec -it nexus-repository-composer /bin/bash
      $ cat /nexus-data/admin.password 
      
  Once logged into the application UI as `admin` using the generated password, you should also 
  turn on "Enable anonymous access" when prompted by the setup wizard.     

## Using Composer With Nexus Repository Manager 3

[We have detailed instructions on how to get started here!](docs/COMPOSER_USER_DOCUMENTATION.md)

## Installing the plugin

There are a range of options for installing the Composer plugin. You'll need to build it first, and
then install the plugin with the options shown below:

### Easiest Install

Thanks to some upstream work in Nexus Repository (versions newer than 3.15), it's become a LOT easier to install a plugin. To install this format plugin, you can either build locally or download from The Central Repository:

#### Option 1: Build a *.kar file locally from the GitHub Repo
* Clone this repo and `cd` to the cloned directory location
* Build the plugin with `mvn clean package -PbuildKar`
* There should now be a `nexus-repository-composer-<version>-bundle.kar` file in your `<cloned_repo>/target` directory 

#### Option 2: Download a *.kar file from The Central Repository 
* Download `nexus-repository-composer-<version>-bundle.kar` from [The Central Repository](https://search.maven.org/artifact/org.sonatype.nexus.plugins/nexus-repository-composer)

Once you've completed Option 1 or 2, copy the `nexus-repository-composer-<version>-bundle.kar` file into the `<nexus_dir>/deploy` folder for your Nexus Repository installation.

Restart Nexus Repo, or go ahead and start it if it wasn't running to begin with.

You should see the new repository types (e.g. `composer (hosted, proxy, group)`) in the available Repository Recipes to use, if all has gone according to plan :)

### Temporary Install

Installations done via the Karaf console will be wiped out with every restart of Nexus Repository. This is a
good installation path if you are just testing or doing development on the plugin.

* Enable the NXRM console: edit `<nexus_dir>/bin/nexus.vmoptions` and change `karaf.startLocalConsole`  to `true`.

  More details here: [Bundle Development](https://help.sonatype.com/display/NXRM3/Bundle+Development+Overview)

* Run NXRM's console:
  ```
  # sudo su - nexus
  $ cd <nexus_dir>/bin
  $ ./nexus run
  > bundle:install file:///tmp/nexus-repository-composer-0.0.8.jar
  > bundle:list
  ```
  (look for org.sonatype.nexus.plugins:nexus-repository-composer ID, should be the last one)
  ```
  > bundle:start <org.sonatype.nexus.plugins:nexus-repository-composer ID>
  ```

### (more) Permanent Install

For more permanent installs of the nexus-repository-composer plugin, follow these instructions:

* Copy the bundle (nexus-repository-composer-0.0.8.jar) into <nexus_dir>/deploy

This will cause the plugin to be loaded with each restart of Nexus Repository. As well, this folder is monitored
by Nexus Repository and the plugin should load within 60 seconds of being copied there if Nexus Repository
is running. You will still need to start the bundle using the karaf commands mentioned in the temporary install.

### (most) Permanent Install

If you are trying to use the Composer plugin permanently, it likely makes more sense to do the following:

* Copy the bundle into `<nexus_dir>/system/org/sonatype/nexus/plugins/nexus-repository-composer/0.0.8/nexus-repository-composer-0.0.8.jar`
* Make the following additions marked with + to `<nexus_dir>/system/org/sonatype/nexus/assemblies/nexus-core-feature/3.x.y/nexus-core-feature-3.x.y-features.xml`

   ```
         <feature prerequisite="false" dependency="false">wrap</feature>
   +     <feature prerequisite="false" dependency="false">nexus-repository-composer</feature>
   ```
   to the `<feature name="nexus-core-feature" description="org.sonatype.nexus.assemblies:nexus-core-feature" version="3.x.y.xy">` section below the last <feature prerequisite...> (above is an example, the exact last one may vary).

   And    
   ```
   + <feature name="nexus-repository-composer" description="org.sonatype.nexus.plugins:nexus-repository-composer" version="0.0.8">
   +     <details>org.sonatype.nexus.plugins:nexus-repository-composer</details>
   +     <bundle>mvn:org.sonatype.nexus.plugins/nexus-repository-composer/0.0.8</bundle>
   + </feature>
    </features>
   ```
   as the last feature.

This will cause the plugin to be loaded and started with each startup of Nexus Repository.

## The Fine Print

It is worth noting that this is **NOT SUPPORTED** by Sonatype, and is a contribution of ours
to the open source community (read: you!)

Remember:

* Use this contribution at the risk tolerance that you have
* Do NOT file Sonatype support tickets related to Composer support in regard to this plugin
* DO file issues here on GitHub, so that the community can pitch in

Phew, that was easier than I thought. Last but not least of all:

Have fun creating and using this plugin and the Nexus platform, we are glad to have you here!

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information:

* Chat with us on [Gitter](https://gitter.im/sonatype/nexus-developers)
* Check out the [Nexus3](http://stackoverflow.com/questions/tagged/nexus3) tag on Stack Overflow
* Check out the [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)

## Composer Plugin
The composer plugin `elendev/nexus-composer-push` (https://github.com/Elendev/nexus-composer-push) provide a composer command to push to a Nexus Repository using this plugin.
