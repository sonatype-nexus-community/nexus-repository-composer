CI Debug Notes
================
To validate some circleci stuff, I was able to run a “build locally” using the steps below.
The local build runs in a docker container.

  * (Once) Install circleci client (`brew install circleci`)

  * Convert the “real” config.yml into a self-contained (non-workspace) config via:

        circleci config process .circleci/config.yml > .circleci/local-config.yml

  * Run a local build with the following command:
          
        circleci local execute -c .circleci/local-config.yml 'build_and_test'

    With the above command, those operations what cannot occur locally will show an error (like `Error: FAILED with error not supported`), but the build will proceed and can complete “successfully”, which allows you to verify scripts in your config, etc.
