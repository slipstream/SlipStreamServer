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

	<xsl:include href="common.xsl" />

	<xsl:param name="isembedded" />

	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="dashboard">
		<html>
			<head>
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Dashboard'" />
				</xsl:call-template>

				<xsl:call-template name="head" />
			</head>

			<body>
				<div id="topdiv">
					<xsl:call-template name="top" />

					<div class="page">

						<xsl:call-template name="addPageHead">
							<xsl:with-param name="headerName" select="'Dashboard'" />
						</xsl:call-template>

						<div class="tabs">
							<xsl:call-template name="addTab">
								<xsl:with-param name="headerName" select="'Runs'" />
								<xsl:with-param name="id" select="'summarysection'" />
								<xsl:with-param name="isVisible" select="'true'" />
							</xsl:call-template>
							<xsl:call-template name="addTab">
								<xsl:with-param name="headerName" select="'Running Virtual Machines'" />
								<xsl:with-param name="id" select="'vmssection'" />
							</xsl:call-template>
						</div>
						
						<div class="visiblesection" id="summarysection">
							<div class="subsection">
								<xsl:apply-templates select="runs"/>
							</div>
						</div>
						<div class="section" id="vmssection">
							<div class="subsection">
								<xsl:apply-templates select="vms"/>
							</div>
						</div>
					</div>
				</div>

				<xsl:call-template name="bottom" />
			</body>
		</html>
	</xsl:template>

	<xsl:template match="runs">
		<table>
			<thead>
				<tr>
					<th>UUID</th>
					<th>Module</th>
					<th>Status</th>
					<th>Start time</th>
				</tr>
			</thead>
			<xsl:apply-templates select="item" mode="run" />
		</table>
	</xsl:template>

	<xsl:template match="vms">
		<table id="vms">
			<thead>
				<tr>
					<th>Instance Id</th>
					<th>Run</th>
					<th>Status</th>
				</tr>
			</thead>
			<xsl:apply-templates select="item" mode="vm"/>
		</table>
	</xsl:template>

	<xsl:template match="item" mode="vm">
		<tr>
			<td><xsl:value-of select="@instanceId" /></td>
			<td>
				<xsl:if test="@runUuid = 'Unknown'">
					<xsl:text>Unknown</xsl:text>
				</xsl:if>
				<xsl:if test="@runUuid != 'Unknown'">
					<a href="/run/{@runUuid}">
						<xsl:value-of select="@runUuid" />
					</a>
				</xsl:if>
			</td>			
			<td><xsl:value-of select="@status" /></td>
		</tr>
	</xsl:template>

	<xsl:template match="item" mode="run">
		<tr>
			<td class="values">
				<a href="{@resourceUri}">
					<xsl:value-of select="@uuid" />
				</a>
			</td>
			<td class="values">
				<a href="{@moduleResourceUri}">
					<xsl:value-of select="@moduleResourceUri" />
				</a>
			</td>
			<td class="values">
				<xsl:value-of select="@status" />
			</td>
			<td class="values">
				<xsl:value-of select="@startTime" />
			</td>
		</tr>
	</xsl:template>

</xsl:stylesheet>
