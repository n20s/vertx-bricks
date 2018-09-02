package com.github.n20s.vertxbricks.build

/**
 * Build information retrieval class. Provides the version of a packaged jar.
 * If no packaged version is available, the revision and information of a local git repository are tried to be retrieved.
 *
 * https://github.com/n20s/vertx-bricks
 * Open Source MIT License (X11), see LICENSE.txt
 *
 * @author Nils Mitoussis
 * @version 1.0
 */
class BuildInformation {

    private String buildVersion = ''

    BuildInformation() {
        this.buildVersion = analyseBuildVersion()
    }

    public String getBuildVersion() {
        return this.buildVersion
    }

    private String analyseBuildVersion() {

        def jarVersion = BuildInformation.class.getPackage().getImplementationVersion();

        if (jarVersion) {
            return jarVersion
        }

        // fallback when no jar version is available
        def devInfo = "Unpackaged Snapshot"
        def gitInfo = gitRevision()
        if (gitInfo) {
            devInfo += " ${gitInfo}"
        }
        return devInfo
    }

    private String gitRevision() {

        // Get the short description of revision.
        String versionInfo = executeCommand('git rev-parse --short HEAD')
        if (versionInfo == null) {
            // Return empty string if not successful. Accept fail, e.g. when the directory is not git managed at all.
            return ''
        }

        // Get dirty flag on managed files, does not reflect untracked files. Status code is 0 in both cases dirty or not.
        String diffStat = executeCommand('git diff --shortstat')
        // When the command before succeeded, we expect this to succeed too.
        if (diffStat == null) {
            throw new RuntimeException("Unexpected resulting status code in git command")
        }
        if (diffStat.length() > 0) {
            // Not empty, the source has been locally modified.
            versionInfo += '-mod'
        }
        return versionInfo
    }

    private String executeCommand(String cmd) {
        Process process = cmd.execute()
        // wait for execution to be finished and exitValue() is 0
        if (process.waitFor() != 0) {
            return null
        }
        return process.text.trim()
    }
}
