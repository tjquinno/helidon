#!/bin/bash
#
# Copyright (c) 2018, 2025 Oracle and/or its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Path to this script
[ -h "${0}" ] && readonly SCRIPT_PATH="$(readlink "${0}")" || readonly SCRIPT_PATH="${0}"

# Load pipeline environment setup and define WS_DIR
. $(dirname -- "${SCRIPT_PATH}")/includes/pipeline-env.sh "${SCRIPT_PATH}" '../..'

# Setup error handling using default settings (defined in includes/error_handlers.sh)
error_trap_setup

usage(){
    cat <<EOF

DESCRIPTION: Helidon Release Script

USAGE:

$(basename ${0}) [ --build-number=N ] CMD

  --version=V
        Override the version to use.
        This trumps --build-number=N

  --help
        Prints the usage and exits.

  CMD:

    update_version
        Update the version in the workspace

    release_build
        Perform a release build
        This will create a local branch, deploy artifacts and push a tag

    deploy_snapshot
        Perform a snapshot build and deploy to snapshot repository

EOF
}

# parse command line args
ARGS=( "${@}" )
for ((i=0;i<${#ARGS[@]};i++))
{
    ARG=${ARGS[${i}]}
    case ${ARG} in
    "--version="*)
        VERSION=${ARG#*=}
        ;;
    "--help")
        usage
        exit 0
        ;;
    *)
        if [ "${ARG}" = "update_version" ] || [ "${ARG}" = "release_build" ] || [ "${ARG}" = "deploy_snapshot" ] ; then
            readonly COMMAND="${ARG}"
        else
            echo "ERROR: unknown argument: ${ARG}"
            exit 1
        fi
        ;;
    esac
}

if [ -z "${COMMAND}" ] ; then
    echo "ERROR: no command provided"
    usage
    exit 1
fi

# Resolve FULL_VERSION
if [ -z "${VERSION+x}" ]; then

    # get maven version
    MVN_VERSION=$(mvn ${MAVEN_ARGS} \
        -q \
        -f ${WS_DIR}/pom.xml \
        -Dexec.executable="echo" \
        -Dexec.args="\${project.version}" \
        --non-recursive \
        org.codehaus.mojo:exec-maven-plugin:1.3.1:exec)

    # strip qualifier
    readonly VERSION="${MVN_VERSION%-*}"
    readonly FULL_VERSION="${VERSION}"
else
    readonly FULL_VERSION="${VERSION}"
fi

export FULL_VERSION
printf "\n%s: FULL_VERSION=%s\n\n" "$(basename ${0})" "${FULL_VERSION}"

update_version(){
    # Update version
    mvn ${MAVEN_ARGS} -f ${WS_DIR}/parent/pom.xml versions:set versions:set-property \
        -DgenerateBackupPoms=false \
        -DnewVersion="${FULL_VERSION}" \
        -Dproperty=helidon.version \
        -DprocessAllModules=true

    # Hack to update helidon.version
    for pom in `egrep "<helidon.version>.*</helidon.version>" -r . --include pom.xml | cut -d ':' -f 1 | sort | uniq `
    do
        echo "Updating helidon.version property in ${pom} to ${FULL_VERSION}"
        cat ${pom} | \
            sed -e s@'<helidon.version>.*</helidon.version>'@"<helidon.version>${FULL_VERSION}</helidon.version>"@g \
            > ${pom}.tmp
        mv ${pom}.tmp ${pom}
    done

    # Hack to update helidon-version in doc files
    for dfile in `egrep ":helidon-version: .*" -r . --include attributes.adoc | cut -d ':' -f 1 | sort | uniq `
    do
        echo "Updating helidon-version property in ${dfile} to ${FULL_VERSION}"
        cat ${dfile} | \
            sed -e s@':helidon-version: .*'@":helidon-version: ${FULL_VERSION}"@g \
            > ${dfile}.tmp
        mv ${dfile}.tmp ${dfile}
    done

    # Hack to update helidon-version-is-release in doc files
    # We are a released version if we are not a SNAPSHOT version
    if [[ ${FULL_VERSION} == *-SNAPSHOT ]]; then
        readonly IS_RELEASED="false"
    else
        readonly IS_RELEASED="true"
    fi
    for dfile in `egrep ":helidon-version-is-release: .*" -r . --include attributes.adoc | cut -d ':' -f 1 | sort | uniq `
    do
        echo "Updating helidon-version-is-release property in ${dfile} to ${IS_RELEASED}"
        cat ${dfile} | \
            sed -e s@':helidon-version-is-release: .*'@":helidon-version-is-release: ${IS_RELEASED}"@g \
            > ${dfile}.tmp
        mv "${dfile}.tmp" "${dfile}"
    done
}

stage_site(){
    # Generate site
    mvn ${MAVEN_ARGS} site

    # Sign site jar
    gpg -ab ${WS_DIR}/target/helidon-project-${FULL_VERSION}-site.jar

    # Deploy site.jar and signature file explicitly using deploy-file
    mvn ${MAVEN_ARGS} deploy:deploy-file \
        -Dfile="${WS_DIR}/target/helidon-project-${FULL_VERSION}-site.jar" \
        -Dfiles="${WS_DIR}/target/helidon-project-${FULL_VERSION}-site.jar.asc" \
        -Dclassifier="site" \
        -Dclassifiers="site" \
        -Dtypes="jar.asc" \
        -DgeneratePom="false" \
        -DgroupId="io.helidon" \
        -DartifactId="helidon-project" \
        -Dversion="${FULL_VERSION}" \
        -Durl="file://${PWD}/staging"
}

release_build(){
    # Do the release work in a branch
    local GIT_BRANCH="release/${FULL_VERSION}"
    git branch -D "${GIT_BRANCH}" > /dev/null 2>&1 || true
    git checkout -b "${GIT_BRANCH}"

    # Invoke update_version
    update_version

    # Update scm/tag entry in the parent pom
    cat parent/pom.xml | \
        sed -e s@'<tag>HEAD</tag>'@"<tag>${FULL_VERSION}</tag>"@g \
        > parent/pom.xml.tmp
    mv parent/pom.xml.tmp parent/pom.xml

    # Git user info
    git config user.email || git config --global user.email "info@helidon.io"
    git config user.name || git config --global user.name "Helidon Robot"

    # Commit version changes
    git commit -a -m "Release ${FULL_VERSION} [ci skip]"

    # Perform deployment to local file system
    mvn ${MAVEN_ARGS} clean deploy \
      -Prelease,archetypes \
      -DskipTests \
      -DaltDeploymentRepository=":::file://${PWD}/staging"

    # Stage site (documentation, javadocs) to local filesystem
    stage_site

    git tag -f "${FULL_VERSION}"
    git push --force origin refs/tags/"${FULL_VERSION}":refs/tags/"${FULL_VERSION}"

    "${WS_DIR}/etc/scripts/upload.sh" upload_release \
                --dir="staging" \
                --description="Helidon v%{FULL_VERSION}"
}

deploy_snapshot(){
    # Make sure version ends in -SNAPSHOT
    if [[ ${MVN_VERSION} != *-SNAPSHOT ]]; then
        echo "Helidon version ${MVN_VERSION} is not a SNAPSHOT version. Failing snapshot release."
        exit 1
    fi

    mvn ${MAVEN_ARGS} -e clean deploy \
      -Parchetypes \
      -DskipTests \
      -DaltDeploymentRepository=":::file://${PWD}/staging"

    "${WS_DIR}/etc/scripts/upload.sh" upload_snapshots \
            --dir="staging"
}

# Invoke command
${COMMAND}
