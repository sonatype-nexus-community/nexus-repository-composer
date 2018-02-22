#!/bin/bash
dirname=`dirname $0`
dirname=`cd "$dirname" && pwd`
cd "$dirname"

op=$1; shift
case "$op" in
    'check' | 'format')
        ;;
     *)
        echo "usage: `basename $0` { check | format } [mvn-options]"
        exit 1
esac

# still depends on profiles defined in https://github.com/sonatype/buildsupport/blob/master/pom.xml
mvn -f ./pom.xml -N -P license-${op} "$@"

