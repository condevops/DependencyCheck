/*
 * This file is part of dependency-check-ant.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2013 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.taskdefs;

import java.io.File;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.Resources;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.data.nvdcve.DatabaseException;
import org.owasp.dependencycheck.data.update.exception.UpdateException;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Identifier;
import org.owasp.dependencycheck.dependency.Vulnerability;
import org.owasp.dependencycheck.exception.ExceptionCollection;
import org.owasp.dependencycheck.exception.ReportException;
import org.owasp.dependencycheck.reporting.ReportGenerator.Format;
import org.owasp.dependencycheck.utils.Settings;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * An Ant task definition to execute dependency-check during an Ant build.
 *
 * @author Jeremy Long
 */
public class Check extends Update {

    /**
     * System specific new line character.
     */
    private static final String NEW_LINE = System.getProperty("line.separator", "\n").intern();
    /**
     * Whether the ruby gemspec analyzer should be enabled.
     */
    private Boolean rubygemsAnalyzerEnabled;
    /**
     * Whether or not the Node.js Analyzer is enabled.
     */
    private Boolean nodeAnalyzerEnabled;
    /**
     * Whether or not the Ruby Bundle Audit Analyzer is enabled.
     */
    private Boolean bundleAuditAnalyzerEnabled;
    /**
     * Whether the CMake analyzer should be enabled.
     */
    private Boolean cmakeAnalyzerEnabled;
    /**
     * Whether or not the Open SSL analyzer is enabled.
     */
    private Boolean opensslAnalyzerEnabled;
    /**
     * Whether the python package analyzer should be enabled.
     */
    private Boolean pyPackageAnalyzerEnabled;
    /**
     * Whether the python distribution analyzer should be enabled.
     */
    private Boolean pyDistributionAnalyzerEnabled;
    /**
     * Whether or not the central analyzer is enabled.
     */
    private Boolean centralAnalyzerEnabled;
    /**
     * Whether or not the nexus analyzer is enabled.
     */
    private Boolean nexusAnalyzerEnabled;
    /**
     * The URL of a Nexus server's REST API end point
     * (http://domain/nexus/service/local).
     */
    private String nexusUrl;
    /**
     * Whether or not the defined proxy should be used when connecting to Nexus.
     */
    private Boolean nexusUsesProxy;
    /**
     * Additional ZIP File extensions to add analyze. This should be a
     * comma-separated list of file extensions to treat like ZIP files.
     */
    private String zipExtensions;
    /**
     * The path to Mono for .NET assembly analysis on non-windows systems.
     */
    private String pathToMono;

    /**
     * The application name for the report.
     *
     * @deprecated use projectName instead.
     */
    @Deprecated
    private String applicationName = null;
    /**
     * The name of the project being analyzed.
     */
    private String projectName = "dependency-check";
    /**
     * Specifies the destination directory for the generated Dependency-Check
     * report.
     */
    private String reportOutputDirectory = ".";
    /**
     * Specifies if the build should be failed if a CVSS score above a specified
     * level is identified. The default is 11 which means since the CVSS scores
     * are 0-10, by default the build will never fail and the CVSS score is set
     * to 11. The valid range for the fail build on CVSS is 0 to 11, where
     * anything above 10 will not cause the build to fail.
     */
    private float failBuildOnCVSS = 11;
    /**
     * Sets whether auto-updating of the NVD CVE/CPE data is enabled. It is not
     * recommended that this be turned to false. Default is true.
     */
    private Boolean autoUpdate;
    /**
     * Whether only the update phase should be executed.
     *
     * @deprecated Use the update task instead
     */
    @Deprecated
    private boolean updateOnly = false;

    /**
     * The report format to be generated (HTML, XML, VULN, CSV, JSON, ALL).
     * Default is HTML.
     */
    private String reportFormat = "HTML";
    /**
     * The path to the suppression file.
     */
    private String suppressionFile;
    /**
     * The path to the suppression file.
     */
    private String hintsFile;
    /**
     * flag indicating whether or not to show a summary of findings.
     */
    private boolean showSummary = true;
    /**
     * Whether experimental analyzers are enabled.
     */
    private Boolean enableExperimental;
    /**
     * Whether or not the Jar Analyzer is enabled.
     */
    private Boolean jarAnalyzerEnabled;
    /**
     * Whether or not the Archive Analyzer is enabled.
     */
    private Boolean archiveAnalyzerEnabled;
    /**
     * Whether or not the .NET Nuspec Analyzer is enabled.
     */
    private Boolean nuspecAnalyzerEnabled;
    /**
     * Whether or not the PHP Composer Analyzer is enabled.
     */
    private Boolean composerAnalyzerEnabled;

    /**
     * Whether or not the .NET Assembly Analyzer is enabled.
     */
    private Boolean assemblyAnalyzerEnabled;
    /**
     * Whether the autoconf analyzer should be enabled.
     */
    private Boolean autoconfAnalyzerEnabled;
    /**
     * Sets the path for the bundle-audit binary.
     */
    private String bundleAuditPath;
    /**
     * Whether or not the CocoaPods Analyzer is enabled.
     */
    private Boolean cocoapodsAnalyzerEnabled;

    /**
     * Whether or not the Swift package Analyzer is enabled.
     */
    private Boolean swiftPackageManagerAnalyzerEnabled;
    //The following code was copied Apache Ant PathConvert
    //BEGIN COPY from org.apache.tools.ant.taskdefs.PathConvert
    /**
     * Path to be converted
     */
    private Resources path = null;
    /**
     * Reference to path/file set to convert
     */
    private Reference refId = null;

    /**
     * Add an arbitrary ResourceCollection.
     *
     * @param rc the ResourceCollection to add.
     * @since Ant 1.7
     */
    public void add(ResourceCollection rc) {
        if (isReference()) {
            throw new BuildException("Nested elements are not allowed when using the refId attribute.");
        }
        getPath().add(rc);
    }

    /**
     * Returns the path. If the path has not been initialized yet, this class is
     * synchronized, and will instantiate the path object.
     *
     * @return the path
     */
    private synchronized Resources getPath() {
        if (path == null) {
            path = new Resources(getProject());
            path.setCache(true);
        }
        return path;
    }

    /**
     * Learn whether the refId attribute of this element been set.
     *
     * @return true if refId is valid.
     */
    public boolean isReference() {
        return refId != null;
    }

    /**
     * Add a reference to a Path, FileSet, DirSet, or FileList defined
     * elsewhere.
     *
     * @param r the reference to a path, fileset, dirset or filelist.
     */
    public synchronized void setRefId(Reference r) {
        if (path != null) {
            throw new BuildException("Nested elements are not allowed when using the refId attribute.");
        }
        refId = r;
    }

    /**
     * If this is a reference, this method will add the referenced resource
     * collection to the collection of paths.
     *
     * @throws BuildException if the reference is not to a resource collection
     */
    private void dealWithReferences() throws BuildException {
        if (isReference()) {
            final Object o = refId.getReferencedObject(getProject());
            if (!(o instanceof ResourceCollection)) {
                throw new BuildException("refId '" + refId.getRefId()
                        + "' does not refer to a resource collection.");
            }
            getPath().add((ResourceCollection) o);
        }
    }
    // END COPY from org.apache.tools.ant.taskdefs

    /**
     * Construct a new DependencyCheckTask.
     */
    public Check() {
        super();
        // Call this before Dependency Check Core starts logging anything - this way, all SLF4J messages from
        // core end up coming through this tasks logger
        StaticLoggerBinder.getSingleton().setTask(this);
    }

    /**
     * Get the value of applicationName.
     *
     * @return the value of applicationName
     *
     * @deprecated use projectName instead.
     */
    @Deprecated
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Set the value of applicationName.
     *
     * @param applicationName new value of applicationName
     * @deprecated use projectName instead.
     */
    @Deprecated
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Get the value of projectName.
     *
     * @return the value of projectName
     */
    public String getProjectName() {
        if (applicationName != null) {
            log("Configuration 'applicationName' has been deprecated, please use 'projectName' instead", Project.MSG_WARN);
            if ("dependency-check".equals(projectName)) {
                projectName = applicationName;
            }
        }
        return projectName;
    }

    /**
     * Set the value of projectName.
     *
     * @param projectName new value of projectName
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Get the value of reportOutputDirectory.
     *
     * @return the value of reportOutputDirectory
     */
    public String getReportOutputDirectory() {
        return reportOutputDirectory;
    }

    /**
     * Set the value of reportOutputDirectory.
     *
     * @param reportOutputDirectory new value of reportOutputDirectory
     */
    public void setReportOutputDirectory(String reportOutputDirectory) {
        this.reportOutputDirectory = reportOutputDirectory;
    }

    /**
     * Get the value of failBuildOnCVSS.
     *
     * @return the value of failBuildOnCVSS
     */
    public float getFailBuildOnCVSS() {
        return failBuildOnCVSS;
    }

    /**
     * Set the value of failBuildOnCVSS.
     *
     * @param failBuildOnCVSS new value of failBuildOnCVSS
     */
    public void setFailBuildOnCVSS(float failBuildOnCVSS) {
        this.failBuildOnCVSS = failBuildOnCVSS;
    }

    /**
     * Get the value of autoUpdate.
     *
     * @return the value of autoUpdate
     */
    public Boolean isAutoUpdate() {
        return autoUpdate;
    }

    /**
     * Set the value of autoUpdate.
     *
     * @param autoUpdate new value of autoUpdate
     */
    public void setAutoUpdate(Boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    /**
     * Get the value of updateOnly.
     *
     * @return the value of updateOnly
     * @deprecated Use the update task instead
     */
    @Deprecated
    public boolean isUpdateOnly() {
        return updateOnly;
    }

    /**
     * Set the value of updateOnly.
     *
     * @param updateOnly new value of updateOnly
     * @deprecated Use the update task instead
     */
    @Deprecated
    public void setUpdateOnly(boolean updateOnly) {
        this.updateOnly = updateOnly;
    }

    /**
     * Get the value of reportFormat.
     *
     * @return the value of reportFormat
     */
    public String getReportFormat() {
        return reportFormat;
    }

    /**
     * Set the value of reportFormat.
     *
     * @param reportFormat new value of reportFormat
     */
    public void setReportFormat(ReportFormats reportFormat) {
        this.reportFormat = reportFormat.getValue();
    }

    /**
     * Get the value of suppressionFile.
     *
     * @return the value of suppressionFile
     */
    public String getSuppressionFile() {
        return suppressionFile;
    }

    /**
     * Set the value of suppressionFile.
     *
     * @param suppressionFile new value of suppressionFile
     */
    public void setSuppressionFile(String suppressionFile) {
        this.suppressionFile = suppressionFile;
    }

    /**
     * Get the value of hintsFile.
     *
     * @return the value of hintsFile
     */
    public String getHintsFile() {
        return hintsFile;
    }

    /**
     * Set the value of hintsFile.
     *
     * @param hintsFile new value of hintsFile
     */
    public void setHintsFile(String hintsFile) {
        this.hintsFile = hintsFile;
    }

    /**
     * Get the value of showSummary.
     *
     * @return the value of showSummary
     */
    public boolean isShowSummary() {
        return showSummary;
    }

    /**
     * Set the value of showSummary.
     *
     * @param showSummary new value of showSummary
     */
    public void setShowSummary(boolean showSummary) {
        this.showSummary = showSummary;
    }

    /**
     * Get the value of enableExperimental.
     *
     * @return the value of enableExperimental
     */
    public Boolean isEnableExperimental() {
        return enableExperimental;
    }

    /**
     * Set the value of enableExperimental.
     *
     * @param enableExperimental new value of enableExperimental
     */
    public void setEnableExperimental(Boolean enableExperimental) {
        this.enableExperimental = enableExperimental;
    }

    /**
     * Returns whether or not the analyzer is enabled.
     *
     * @return true if the analyzer is enabled
     */
    public Boolean isJarAnalyzerEnabled() {
        return jarAnalyzerEnabled;
    }

    /**
     * Sets whether or not the analyzer is enabled.
     *
     * @param jarAnalyzerEnabled the value of the new setting
     */
    public void setJarAnalyzerEnabled(Boolean jarAnalyzerEnabled) {
        this.jarAnalyzerEnabled = jarAnalyzerEnabled;
    }

    /**
     * Returns whether or not the analyzer is enabled.
     *
     * @return true if the analyzer is enabled
     */
    public Boolean isArchiveAnalyzerEnabled() {
        return archiveAnalyzerEnabled;
    }

    /**
     * Sets whether or not the analyzer is enabled.
     *
     * @param archiveAnalyzerEnabled the value of the new setting
     */
    public void setArchiveAnalyzerEnabled(Boolean archiveAnalyzerEnabled) {
        this.archiveAnalyzerEnabled = archiveAnalyzerEnabled;
    }

    /**
     * Returns whether or not the analyzer is enabled.
     *
     * @return true if the analyzer is enabled
     */
    public Boolean isAssemblyAnalyzerEnabled() {
        return assemblyAnalyzerEnabled;
    }

    /**
     * Sets whether or not the analyzer is enabled.
     *
     * @param assemblyAnalyzerEnabled the value of the new setting
     */
    public void setAssemblyAnalyzerEnabled(Boolean assemblyAnalyzerEnabled) {
        this.assemblyAnalyzerEnabled = assemblyAnalyzerEnabled;
    }

    /**
     * Returns whether or not the analyzer is enabled.
     *
     * @return true if the analyzer is enabled
     */
    public Boolean isNuspecAnalyzerEnabled() {
        return nuspecAnalyzerEnabled;
    }

    /**
     * Sets whether or not the analyzer is enabled.
     *
     * @param nuspecAnalyzerEnabled the value of the new setting
     */
    public void setNuspecAnalyzerEnabled(Boolean nuspecAnalyzerEnabled) {
        this.nuspecAnalyzerEnabled = nuspecAnalyzerEnabled;
    }

    /**
     * Get the value of composerAnalyzerEnabled.
     *
     * @return the value of composerAnalyzerEnabled
     */
    public Boolean isComposerAnalyzerEnabled() {
        return composerAnalyzerEnabled;
    }

    /**
     * Set the value of composerAnalyzerEnabled.
     *
     * @param composerAnalyzerEnabled new value of composerAnalyzerEnabled
     */
    public void setComposerAnalyzerEnabled(Boolean composerAnalyzerEnabled) {
        this.composerAnalyzerEnabled = composerAnalyzerEnabled;
    }

    /**
     * Get the value of autoconfAnalyzerEnabled.
     *
     * @return the value of autoconfAnalyzerEnabled
     */
    public Boolean isAutoconfAnalyzerEnabled() {
        return autoconfAnalyzerEnabled;
    }

    /**
     * Set the value of autoconfAnalyzerEnabled.
     *
     * @param autoconfAnalyzerEnabled new value of autoconfAnalyzerEnabled
     */
    public void setAutoconfAnalyzerEnabled(Boolean autoconfAnalyzerEnabled) {
        this.autoconfAnalyzerEnabled = autoconfAnalyzerEnabled;
    }

    /**
     * Get the value of cmakeAnalyzerEnabled.
     *
     * @return the value of cmakeAnalyzerEnabled
     */
    public Boolean isCMakeAnalyzerEnabled() {
        return cmakeAnalyzerEnabled;
    }

    /**
     * Set the value of cmakeAnalyzerEnabled.
     *
     * @param cmakeAnalyzerEnabled new value of cmakeAnalyzerEnabled
     */
    public void setCMakeAnalyzerEnabled(Boolean cmakeAnalyzerEnabled) {
        this.cmakeAnalyzerEnabled = cmakeAnalyzerEnabled;
    }

    /**
     * Returns if the Bundle Audit Analyzer is enabled.
     *
     * @return if the Bundle Audit Analyzer is enabled.
     */
    public Boolean isBundleAuditAnalyzerEnabled() {
        return bundleAuditAnalyzerEnabled;
    }

    /**
     * Sets if the Bundle Audit Analyzer is enabled.
     *
     * @param bundleAuditAnalyzerEnabled whether or not the analyzer should be
     * enabled
     */
    public void setBundleAuditAnalyzerEnabled(Boolean bundleAuditAnalyzerEnabled) {
        this.bundleAuditAnalyzerEnabled = bundleAuditAnalyzerEnabled;
    }

    /**
     * Returns the path to the bundle audit executable.
     *
     * @return the path to the bundle audit executable
     */
    public String getBundleAuditPath() {
        return bundleAuditPath;
    }

    /**
     * Sets the path to the bundle audit executable.
     *
     * @param bundleAuditPath the path to the bundle audit executable
     */
    public void setBundleAuditPath(String bundleAuditPath) {
        this.bundleAuditPath = bundleAuditPath;
    }

    /**
     * Returns if the cocoapods analyzer is enabled.
     *
     * @return if the cocoapods analyzer is enabled
     */
    public boolean isCocoapodsAnalyzerEnabled() {
        return cocoapodsAnalyzerEnabled;
    }

    /**
     * Sets whether or not the cocoapods analyzer is enabled.
     *
     * @param cocoapodsAnalyzerEnabled the state of the cocoapods analyzer
     */
    public void setCocoapodsAnalyzerEnabled(Boolean cocoapodsAnalyzerEnabled) {
        this.cocoapodsAnalyzerEnabled = cocoapodsAnalyzerEnabled;
    }

    /**
     * Returns whether or not the Swift package Analyzer is enabled.
     *
     * @return whether or not the Swift package Analyzer is enabled
     */
    public Boolean isSwiftPackageManagerAnalyzerEnabled() {
        return swiftPackageManagerAnalyzerEnabled;
    }

    /**
     * Sets the enabled state of the swift package manager analyzer.
     *
     * @param swiftPackageManagerAnalyzerEnabled the enabled state of the swift
     * package manager
     */
    public void setSwiftPackageManagerAnalyzerEnabled(Boolean swiftPackageManagerAnalyzerEnabled) {
        this.swiftPackageManagerAnalyzerEnabled = swiftPackageManagerAnalyzerEnabled;
    }

    /**
     * Get the value of opensslAnalyzerEnabled.
     *
     * @return the value of opensslAnalyzerEnabled
     */
    public Boolean isOpensslAnalyzerEnabled() {
        return opensslAnalyzerEnabled;
    }

    /**
     * Set the value of opensslAnalyzerEnabled.
     *
     * @param opensslAnalyzerEnabled new value of opensslAnalyzerEnabled
     */
    public void setOpensslAnalyzerEnabled(Boolean opensslAnalyzerEnabled) {
        this.opensslAnalyzerEnabled = opensslAnalyzerEnabled;
    }

    /**
     * Get the value of nodeAnalyzerEnabled.
     *
     * @return the value of nodeAnalyzerEnabled
     */
    public Boolean isNodeAnalyzerEnabled() {
        return nodeAnalyzerEnabled;
    }

    /**
     * Set the value of nodeAnalyzerEnabled.
     *
     * @param nodeAnalyzerEnabled new value of nodeAnalyzerEnabled
     */
    public void setNodeAnalyzerEnabled(Boolean nodeAnalyzerEnabled) {
        this.nodeAnalyzerEnabled = nodeAnalyzerEnabled;
    }

    /**
     * Get the value of rubygemsAnalyzerEnabled.
     *
     * @return the value of rubygemsAnalyzerEnabled
     */
    public Boolean isRubygemsAnalyzerEnabled() {
        return rubygemsAnalyzerEnabled;
    }

    /**
     * Set the value of rubygemsAnalyzerEnabled.
     *
     * @param rubygemsAnalyzerEnabled new value of rubygemsAnalyzerEnabled
     */
    public void setRubygemsAnalyzerEnabled(Boolean rubygemsAnalyzerEnabled) {
        this.rubygemsAnalyzerEnabled = rubygemsAnalyzerEnabled;
    }

    /**
     * Get the value of pyPackageAnalyzerEnabled.
     *
     * @return the value of pyPackageAnalyzerEnabled
     */
    public Boolean isPyPackageAnalyzerEnabled() {
        return pyPackageAnalyzerEnabled;
    }

    /**
     * Set the value of pyPackageAnalyzerEnabled.
     *
     * @param pyPackageAnalyzerEnabled new value of pyPackageAnalyzerEnabled
     */
    public void setPyPackageAnalyzerEnabled(Boolean pyPackageAnalyzerEnabled) {
        this.pyPackageAnalyzerEnabled = pyPackageAnalyzerEnabled;
    }

    /**
     * Get the value of pyDistributionAnalyzerEnabled.
     *
     * @return the value of pyDistributionAnalyzerEnabled
     */
    public Boolean isPyDistributionAnalyzerEnabled() {
        return pyDistributionAnalyzerEnabled;
    }

    /**
     * Set the value of pyDistributionAnalyzerEnabled.
     *
     * @param pyDistributionAnalyzerEnabled new value of
     * pyDistributionAnalyzerEnabled
     */
    public void setPyDistributionAnalyzerEnabled(Boolean pyDistributionAnalyzerEnabled) {
        this.pyDistributionAnalyzerEnabled = pyDistributionAnalyzerEnabled;
    }

    /**
     * Get the value of centralAnalyzerEnabled.
     *
     * @return the value of centralAnalyzerEnabled
     */
    public Boolean isCentralAnalyzerEnabled() {
        return centralAnalyzerEnabled;
    }

    /**
     * Set the value of centralAnalyzerEnabled.
     *
     * @param centralAnalyzerEnabled new value of centralAnalyzerEnabled
     */
    public void setCentralAnalyzerEnabled(Boolean centralAnalyzerEnabled) {
        this.centralAnalyzerEnabled = centralAnalyzerEnabled;
    }

    /**
     * Get the value of nexusAnalyzerEnabled.
     *
     * @return the value of nexusAnalyzerEnabled
     */
    public Boolean isNexusAnalyzerEnabled() {
        return nexusAnalyzerEnabled;
    }

    /**
     * Set the value of nexusAnalyzerEnabled.
     *
     * @param nexusAnalyzerEnabled new value of nexusAnalyzerEnabled
     */
    public void setNexusAnalyzerEnabled(Boolean nexusAnalyzerEnabled) {
        this.nexusAnalyzerEnabled = nexusAnalyzerEnabled;
    }

    /**
     * Get the value of nexusUrl.
     *
     * @return the value of nexusUrl
     */
    public String getNexusUrl() {
        return nexusUrl;
    }

    /**
     * Set the value of nexusUrl.
     *
     * @param nexusUrl new value of nexusUrl
     */
    public void setNexusUrl(String nexusUrl) {
        this.nexusUrl = nexusUrl;
    }

    /**
     * Get the value of nexusUsesProxy.
     *
     * @return the value of nexusUsesProxy
     */
    public Boolean isNexusUsesProxy() {
        return nexusUsesProxy;
    }

    /**
     * Set the value of nexusUsesProxy.
     *
     * @param nexusUsesProxy new value of nexusUsesProxy
     */
    public void setNexusUsesProxy(Boolean nexusUsesProxy) {
        this.nexusUsesProxy = nexusUsesProxy;
    }

    /**
     * Get the value of zipExtensions.
     *
     * @return the value of zipExtensions
     */
    public String getZipExtensions() {
        return zipExtensions;
    }

    /**
     * Set the value of zipExtensions.
     *
     * @param zipExtensions new value of zipExtensions
     */
    public void setZipExtensions(String zipExtensions) {
        this.zipExtensions = zipExtensions;
    }

    /**
     * Get the value of pathToMono.
     *
     * @return the value of pathToMono
     */
    public String getPathToMono() {
        return pathToMono;
    }

    /**
     * Set the value of pathToMono.
     *
     * @param pathToMono new value of pathToMono
     */
    public void setPathToMono(String pathToMono) {
        this.pathToMono = pathToMono;
    }

    @Override
    public void execute() throws BuildException {
        dealWithReferences();
        validateConfiguration();
        populateSettings();
        Engine engine = null;
        try {
            engine = new Engine(Check.class.getClassLoader());
            if (isUpdateOnly()) {
                log("Deprecated 'UpdateOnly' property set; please use the UpdateTask instead", Project.MSG_WARN);
                try {
                    engine.doUpdates();
                } catch (UpdateException ex) {
                    if (this.isFailOnError()) {
                        throw new BuildException(ex);
                    }
                    log(ex.getMessage(), Project.MSG_ERR);
                }
            } else {
                for (Resource resource : getPath()) {
                    final FileProvider provider = resource.as(FileProvider.class);
                    if (provider != null) {
                        final File file = provider.getFile();
                        if (file != null && file.exists()) {
                            engine.scan(file);
                        }
                    }
                }

                try {
                    engine.analyzeDependencies();
                } catch (ExceptionCollection ex) {
                    if (this.isFailOnError()) {
                        throw new BuildException(ex);
                    }
                }
                engine.writeReports(getProjectName(), new File(reportOutputDirectory), reportFormat);

                if (this.failBuildOnCVSS <= 10) {
                    checkForFailure(engine.getDependencies());
                }
                if (this.showSummary) {
                    showSummary(engine.getDependencies());
                }
            }
        } catch (DatabaseException ex) {
            final String msg = "Unable to connect to the dependency-check database; analysis has stopped";
            if (this.isFailOnError()) {
                throw new BuildException(msg, ex);
            }
            log(msg, ex, Project.MSG_ERR);
        } catch (ReportException ex) {
            final String msg = "Unable to generate the dependency-check report";
            if (this.isFailOnError()) {
                throw new BuildException(msg, ex);
            }
            log(msg, ex, Project.MSG_ERR);
        } finally {
            Settings.cleanup(true);
            if (engine != null) {
                engine.cleanup();
            }
        }
    }

    /**
     * Validate the configuration to ensure the parameters have been properly
     * configured/initialized.
     *
     * @throws BuildException if the task was not configured correctly.
     */
    private void validateConfiguration() throws BuildException {
        if (getPath() == null) {
            throw new BuildException("No project dependencies have been defined to analyze.");
        }
        if (failBuildOnCVSS < 0 || failBuildOnCVSS > 11) {
            throw new BuildException("Invalid configuration, failBuildOnCVSS must be between 0 and 11.");
        }
    }

    /**
     * Takes the properties supplied and updates the dependency-check settings.
     * Additionally, this sets the system properties required to change the
     * proxy server, port, and connection timeout.
     *
     * @throws BuildException thrown when an invalid setting is configured.
     */
    @Override
    protected void populateSettings() throws BuildException {
        super.populateSettings();
        Settings.setBooleanIfNotNull(Settings.KEYS.AUTO_UPDATE, autoUpdate);
        Settings.setStringIfNotEmpty(Settings.KEYS.SUPPRESSION_FILE, suppressionFile);
        Settings.setStringIfNotEmpty(Settings.KEYS.HINTS_FILE, hintsFile);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_EXPERIMENTAL_ENABLED, enableExperimental);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_JAR_ENABLED, jarAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_PYTHON_DISTRIBUTION_ENABLED, pyDistributionAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_PYTHON_PACKAGE_ENABLED, pyPackageAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_RUBY_GEMSPEC_ENABLED, rubygemsAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_OPENSSL_ENABLED, opensslAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_CMAKE_ENABLED, cmakeAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_SWIFT_PACKAGE_MANAGER_ENABLED, swiftPackageManagerAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_COCOAPODS_ENABLED, cocoapodsAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_BUNDLE_AUDIT_ENABLED, bundleAuditAnalyzerEnabled);
        Settings.setStringIfNotNull(Settings.KEYS.ANALYZER_BUNDLE_AUDIT_PATH, bundleAuditPath);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_AUTOCONF_ENABLED, autoconfAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_COMPOSER_LOCK_ENABLED, composerAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_NODE_PACKAGE_ENABLED, nodeAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_NUSPEC_ENABLED, nuspecAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_CENTRAL_ENABLED, centralAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_NEXUS_ENABLED, nexusAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_ARCHIVE_ENABLED, archiveAnalyzerEnabled);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_ASSEMBLY_ENABLED, assemblyAnalyzerEnabled);
        Settings.setStringIfNotEmpty(Settings.KEYS.ANALYZER_NEXUS_URL, nexusUrl);
        Settings.setBooleanIfNotNull(Settings.KEYS.ANALYZER_NEXUS_USES_PROXY, nexusUsesProxy);
        Settings.setStringIfNotEmpty(Settings.KEYS.ADDITIONAL_ZIP_EXTENSIONS, zipExtensions);
        Settings.setStringIfNotEmpty(Settings.KEYS.ANALYZER_ASSEMBLY_MONO_PATH, pathToMono);
    }

    /**
     * Checks to see if a vulnerability has been identified with a CVSS score
     * that is above the threshold set in the configuration.
     *
     * @param dependencies the list of dependency objects
     * @throws BuildException thrown if a CVSS score is found that is higher
     * than the threshold set
     */
    private void checkForFailure(List<Dependency> dependencies) throws BuildException {
        final StringBuilder ids = new StringBuilder();
        for (Dependency d : dependencies) {
            for (Vulnerability v : d.getVulnerabilities()) {
                if (v.getCvssScore() >= failBuildOnCVSS) {
                    if (ids.length() == 0) {
                        ids.append(v.getName());
                    } else {
                        ids.append(", ").append(v.getName());
                    }
                }
            }
        }
        if (ids.length() > 0) {
            final String msg = String.format("%n%nDependency-Check Failure:%n"
                    + "One or more dependencies were identified with vulnerabilities that have a CVSS score greater than '%.1f': %s%n"
                    + "See the dependency-check report for more details.%n%n", failBuildOnCVSS, ids.toString());
            throw new BuildException(msg);
        }
    }

    /**
     * Generates a warning message listing a summary of dependencies and their
     * associated CPE and CVE entries.
     *
     * @param dependencies a list of dependency objects
     */
    private void showSummary(List<Dependency> dependencies) {
        final StringBuilder summary = new StringBuilder();
        for (Dependency d : dependencies) {
            boolean firstEntry = true;
            final StringBuilder ids = new StringBuilder();
            for (Vulnerability v : d.getVulnerabilities()) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    ids.append(", ");
                }
                ids.append(v.getName());
            }
            if (ids.length() > 0) {
                summary.append(d.getFileName()).append(" (");
                firstEntry = true;
                for (Identifier id : d.getIdentifiers()) {
                    if (firstEntry) {
                        firstEntry = false;
                    } else {
                        summary.append(", ");
                    }
                    summary.append(id.getValue());
                }
                summary.append(") : ").append(ids).append(NEW_LINE);
            }
        }
        if (summary.length() > 0) {
            final String msg = String.format("%n%n"
                    + "One or more dependencies were identified with known vulnerabilities:%n%n%s"
                    + "%n%nSee the dependency-check report for more details.%n%n", summary.toString());
            log(msg, Project.MSG_WARN);
        }
    }

    /**
     * An enumeration of supported report formats: "ALL", "HTML", "XML", "CSV",
     * "JSON", "VULN", etc..
     */
    public static class ReportFormats extends EnumeratedAttribute {

        /**
         * Returns the list of values for the report format.
         *
         * @return the list of values for the report format
         */
        @Override
        public String[] getValues() {
            int i = 0;
            final Format[] formats = Format.values();
            final String[] values = new String[formats.length];
            for (Format format : formats) {
                values[i++] = format.name();
            }
            return values;
        }
    }
}
