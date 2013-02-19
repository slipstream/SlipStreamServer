<!--
  +=================================================================+
  SlipStream Server (WAR)
  =====
  Copyright (C) 2013 SixSq Sarl (sixsq.com)
  =====
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
       http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -=================================================================-
  -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">
	<xsl:output method="html" indent="yes" version="4.01"
		encoding="utf-8" media-type="text/html" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" />

	<xsl:param name="resourcePath" />

	<xsl:include href="common.xsl" />

	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="parameters">

		<xsl:variable name='position'>
			<xsl:number />
		</xsl:variable>

		<html>
			<head>
				<xsl:call-template name="head" />
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Configuration'" />
				</xsl:call-template>
				<script language="javascript" type="text/javascript" src="js/configuration.js" />
			</head>

			<body onload="activateTab('summarysection');return 0;">
				<div id="floaterdiv" class="floating" />
				<div id="topdiv">

					<div id="reloadconfigurationfile" title="Reload Configuration File?">
						<p>Are you sure you want to re-load all parameters from the configuration file?</p>
						<p>All parameters set through the interface will be lost.</p>
					</div>

					<xsl:for-each select="/*">
						<xsl:call-template name="top" />
					</xsl:for-each>

					<form name="mainform" method="post" action="{$resourcePath}?method=put">

						<xsl:variable name="headerName">
							Configuration
						</xsl:variable>

						<xsl:variable name="actions">
							<input type="submit" value="Save" />

							<input id="reloadconfigurationfilebutton" type="button" value="Reload Configuration File" />
						</xsl:variable>

						<div class="page">
							<div class="page_head">
								<xsl:call-template name="addPageHead">
									<xsl:with-param name="headerName" select="$headerName" />
									<xsl:with-param name="actions" select="$actions" />
								</xsl:call-template>
							</div>

							<div class="tabs">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Summary'" />
									<xsl:with-param name="id" select="'summarysection'" />
									<xsl:with-param name="isVisible" select="'true'" />
								</xsl:call-template>
							</div>

							<div class="visiblesection" id="summarysection">
								<div class="subsection">
									<table>
										<xsl:for-each select="entry/parameter/@category[not(.=preceding::entry/parameter/@category)]">
											<xsl:sort select="."/>
									  		<xsl:call-template name="addParameterSubsection">
												<xsl:with-param name="category" select="." />
												<xsl:with-param name="mode" select="'edit'" />
											</xsl:call-template>
										</xsl:for-each>
									</table>
								</div>
							</div>
						</div>

						<div class="page_head">
							<div class="page_head">
								<xsl:call-template name="addPageHead">
									<xsl:with-param name="headerName" select="" />
									<xsl:with-param name="actions" select="$actions" />
								</xsl:call-template>
							</div>
						</div>

					</form>

					<xsl:call-template name="bottom" />

				</div>
			</body>
		</html>
	</xsl:template>

</xsl:stylesheet>
