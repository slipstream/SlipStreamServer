<?xml version="1.0" encoding="UTF-8"?>
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

	<xsl:template match="run">

		<xsl:variable name='decodedmodulename'>
			<xsl:call-template name='replace-spaces'>
				<xsl:with-param name="text" select="@refqname" />
			</xsl:call-template>
		</xsl:variable>

		<html>
			<head>
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Run'" />
				</xsl:call-template>

				<xsl:call-template name="head" />
				<script language="javascript" type="text/javascript" src="js/dashboard.js" />
				<link rel="stylesheet" type="text/css" href="css/dashboard.css" />

				<!-- JIT Files -->
				<link type="text/css" href="external/jit/css/base.css" rel="stylesheet" />
				<link type="text/css" href="external/jit/css/Spacetree.css" rel="stylesheet" />
				<!--[if IE]><script language="javascript" type="text/javascript" src="external/jit/js/excanvas.js"></script><![endif]-->
				<script language="javascript" type="text/javascript" src="external/jit/js/jit.js"></script>
				<script language="javascript" type="text/javascript" src="js/run-jit.js"></script>
			</head>

			<body onload="init();">

				<iframe id="loggeriframe" class="floating" />

				<xsl:call-template name="waitingMessageOverlay">
					<xsl:with-param name="message" select="'Terminating Virtual Machine(s)...'" />
				</xsl:call-template>

				<div id="terminaterundialog" title="Terminate Run">
					<p>Are you sure you want to terminate this run? All associated VMs will also be terminated.</p>
					<form id="terminateform" action="{@resourceUri}?method=delete" method="post" />
				</div>

				<div id="topdiv">
					<xsl:call-template name="top" />

					<!-- Set the buttons for the page head -->
					<xsl:variable name="actions">
						<input type="button" value="Terminate" />
					</xsl:variable>

					<div class="page">
						<div class="page_head">
							<xsl:call-template name="addPageHead">
								<xsl:with-param name="headerName" select="'Run'" />
								<xsl:with-param name="actions" select="$actions" />
							</xsl:call-template>
						</div>

						<div class="tabs">
							<xsl:call-template name="addTab">
								<xsl:with-param name="headerName" select="'Summary'" />
								<xsl:with-param name="id" select="'summarysection'" />
								<xsl:with-param name="isVisible" select="'true'" />
							</xsl:call-template>
							<xsl:call-template name="addTab">
								<xsl:with-param name="headerName" select="'Runtime Parameters'" />
								<xsl:with-param name="id" select="'runtimeparametersection'" />
							</xsl:call-template>
						</div>

						<div class="visiblesection" id="summarysection">
							<table>
								<tr>
									<xsl:variable name="moduleResource">
										<xsl:call-template name="module-resource-to-name">
											<xsl:with-param name="name" select="@moduleResourceUri" />
										</xsl:call-template>
									</xsl:variable>
									<th class="row">Module</th>
									<td class="values">
										<a href="{@moduleResourceUri}" id="moduleReference">
											<xsl:value-of select="$moduleResource" />
										</a>
									</td>
								</tr>
								<tr>
									<th class="row">Category</th>
									<td class="values">
										<xsl:value-of select="@category" />
									</td>
								</tr>
								<tr>
									<th class="row">Description</th>
									<td class="values">
										<xsl:value-of select="@description" />
									</td>
								</tr>
								<tr>
									<th class="row">User</th>
									<td class="values">
										<xsl:value-of select="@user" />
									</td>
								</tr>
								<tr>
									<th class="row">Start</th>
									<td class="values">
										<xsl:value-of select="@startTime" />
									</td>
								</tr>
								<tr>
									<th class="row">End</th>
									<td class="values" id="end">
										<xsl:value-of select="@endTime" />
									</td>
								</tr>
								<tr>
									<th class="row">Status</th>
									<td class="values" id="status">
										<xsl:value-of select="@status" />
									</td>
								</tr>
								<tr>
									<th class="row">Run type</th>
									<td class="values" id="runtype">
										<xsl:value-of select="@type" />
									</td>
								</tr>
								<tr>
									<th class="row">UUID</th>
									<td class="values" id="uuid">
										<xsl:value-of select="@uuid" />
									</td>
								</tr>
								<tr>
									<td>
										<a href="/reports/{@uuid}">Results</a>
									</td>
									<td>
										<a href="javascript:void(null);" onclick='toggleRefresh();'
											id='refresh'>Disable auto refresh</a>
									</td>
								</tr>
							</table>

							<div class="subsection">
								<div class="subsection_head">
									<span>Machines</span>
								</div>
								<div id="container">								
									<div id="center-container">
									    <div id="infovis"></div>
									</div>
								</div>
							</div>

						</div>
					</div>

					<xsl:if test="parameters/entry">
						<div class="section" id="parametersection">
							<xsl:apply-templates select="parameters" />
						</div>
					</xsl:if>
					
					<div class="section" id="runtimeparametersection">
						<xsl:apply-templates select="runtimeParameters" />
					</div>

					<xsl:call-template name="bottom" />

				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="runtimeParameters">
		<div id="runtimeParameters">

			<xsl:call-template name="runtimeParameterSection">
				<xsl:with-param name="group" select="'Global'" />
				<xsl:with-param name="isExpanded" select="'true'" />
			</xsl:call-template>

			<xsl:for-each select="entry/runtimeParameter/@group[not(.=preceding::entry/runtimeParameter/@group)][not(. = 'Global')][not(starts-with(. ,'orchestrator'))]">
				<xsl:sort select="." />
				<xsl:call-template name="runtimeParameterSection">
					<xsl:with-param name="group" select="." />
				</xsl:call-template>
			</xsl:for-each>
      
      <xsl:for-each select="entry/runtimeParameter/@group[not(.=preceding::entry/runtimeParameter/@group)][starts-with(. ,'orchestrator')]">
				<xsl:call-template name="runtimeParameterSection">
					<xsl:with-param name="group" select="." />
				</xsl:call-template>
      </xsl:for-each>

		</div>
	</xsl:template>

	<xsl:template name="runtimeParameterSection">
		<xsl:param name="group" />
		<xsl:param name="isExpanded" select="'false'" />

		<xsl:call-template name="addSectionHead">
			<xsl:with-param name="headerName" select="$group" />
			<xsl:with-param name="id" select="$group" />
			<xsl:with-param name="toggle" select="'true'" />
			<xsl:with-param name="isExpanded" select="$isExpanded" />
		</xsl:call-template>
		<div id="{$group}">
			<xsl:if test="$isExpanded = 'false'">
				<xsl:attribute name="style">display: none</xsl:attribute>
			</xsl:if>
			<table>
				<thead>
					<tr>
						<th class="row">Name</th>
						<th class="row">Value</th>
						<th class="row">Description</th>
					</tr>
				</thead>
		
				<xsl:for-each select="//runtimeParameter[@group=$group]">			
					<xsl:sort select="@key" />
					<tr>
						<xsl:apply-templates select="."/>
					</tr>
				</xsl:for-each>
	
			</table>
		</div>
	</xsl:template>


	<xsl:template match="runtimeParameter">
		<td width="30%">
			<xsl:value-of select="@key" />
		</td>
		<td width="30%" id="{@key}">
			<xsl:value-of select="." />
		</td>
		<td width="40%">
			<xsl:value-of select="@description" />
		</td>
	</xsl:template>

</xsl:stylesheet>
