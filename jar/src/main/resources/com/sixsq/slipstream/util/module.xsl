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
	version="1.0" xmlns:str="http://exslt.org/strings">

	<xsl:output method="html" indent="yes" version="4.01"
		encoding="utf-8" media-type="text/html" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" />

	<xsl:param name="resourcePath" />
	<xsl:param name="chooserType" />

	<xsl:include href="common.xsl" />

	<xsl:template
		match="deploymentModule | imageModule">

		<html>
			<head>
				<xsl:call-template name="head" />
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="@name" />
				</xsl:call-template>
				<script language="javascript" type="text/javascript" src="js/module.js" />
			</head>

			<xsl:variable name="headerName">
				<xsl:call-template name="translateCategory">
					<xsl:with-param name="category" select="@category" />
				</xsl:call-template>
			</xsl:variable>

			<body>

				<iframe id="loggeriframe" class="floating" />

				<xsl:call-template name="copydialog" />
				<xsl:call-template name="waitingMessageOverlay" />

				<div id="runOptions" class="dialog" title="Launch a new run">
					<form method="post" action="run" name="runwithoptions">
						<input type="hidden" name="refqname" value="{@resourceUri}" />
						<xsl:apply-templates select="nodes" mode="runOptions" />
					</form>
				</div>

				<div id="topdiv">
				
					<xsl:variable name="chooserUrlAttribute">
						<xsl:if test="$chooserType">
							<xsl:text>?chooser=</xsl:text>
							<xsl:value-of select="$chooserType" />
						</xsl:if>
					</xsl:variable>
					<xsl:call-template name="top">
						<xsl:with-param name="chooserType" select="$chooserType" />
					</xsl:call-template>

					<!-- Set the buttons for the page head -->
					<xsl:variable name="actions">
						<xsl:if test="not($chooserType)">
							<xsl:if test="name() != 'projectModule'">
									<xsl:choose>
										<xsl:when test="@category = 'Deployment'">
											<form method="post" action="run" name="formexec">
												<input type="hidden" name="refqname" value="{@resourceUri}" />
												<input type="submit" value="Run">
													<xsl:call-template name="isDisabled">
														<xsl:with-param name="action" select="'Post'" />
													</xsl:call-template>
												</input>
												<input type="button" value="Run..." name="showRunWithOptions">
													<xsl:call-template name="isDisabled">
														<xsl:with-param name="action" select="'Post'" />
													</xsl:call-template>
												</input>
											</form>
										</xsl:when>
										<xsl:otherwise>
											<form method="post" action="run" name="formexec">
												<input type="hidden" name="refqname" value="{@resourceUri}" />
												<input type="submit" value="Build">
													<xsl:call-template name="isDisabled">
														<xsl:with-param name="action" select="'Post'" />
													</xsl:call-template>
												</input>
											</form>
											<form method="post" action="run" name="formexec">
												<input type="hidden" name="refqname" value="{@resourceUri}" />
												<input type="hidden" name="type" value="Machine" />
												<input type="submit" value="Run">
													<xsl:call-template name="isDisabled">
														<xsl:with-param name="action" select="'Post'" />
													</xsl:call-template>
												</input>
											</form>
										</xsl:otherwise>
									</xsl:choose>
							</xsl:if>
							<form action="{$resourcePath}" method="get">
								<input type="hidden" name="edit" value="true" />
								<input type="submit" value="Edit">
									<xsl:call-template name="isDisabled">
										<xsl:with-param name="action" select="'Put'" />
									</xsl:call-template>
								</input>
							</form>
							<input type="button" value="Copy..." id="copybutton" />
							<xsl:choose>
								<xsl:when test="published">
									<form action="{$resourcePath}/publish?method=delete" method="post">
										<input type="submit" value="Unpublish">
											<xsl:call-template name="isDisabled">
												<xsl:with-param name="action" select="'Put'" />
											</xsl:call-template>
										</input>
									</form>
								</xsl:when>
								<xsl:otherwise>
									<form action="{$resourcePath}/publish?method=put" method="post">
										<input type="submit" value="Publish">
											<xsl:call-template name="isDisabled">
												<xsl:with-param name="action" select="/*/user/@issuper = 'true'" />
											</xsl:call-template>
										</input>
									</form>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:if>
						<xsl:if test="$chooserType">
							<form>
								<xsl:choose>
									<xsl:when test="@category = $chooserType">
										<button name="SelectChooser" onclick="selectChooser();">Select</button>
										<button name="SelectChooserWithVersion" onclick="selectChooserWithVersion();">Select Exact Version</button>
									</xsl:when>
									<xsl:otherwise>
										<button disabled="on">Select</button>
										<button disabled="on">Select With Version</button>
									</xsl:otherwise>
								</xsl:choose>
								<button name="CancelChooser" onclick="cancelChooser();">Cancel</button>
							</form>
						</xsl:if>
					</xsl:variable>

					<div class="page">

						<xsl:call-template name="addPageHead">
							<xsl:with-param name="headerName" select="$headerName" />
							<xsl:with-param name="actions" select="$actions" />
						</xsl:call-template>

						<div class="tabs">
							<xsl:call-template name="addTab">
								<xsl:with-param name="headerName" select="'Summary'" />
								<xsl:with-param name="id" select="'summarysection'" />
								<xsl:with-param name="isVisible" select="'true'" />
							</xsl:call-template>
							<xsl:if test="name(.) = 'imageModule' or name(.) = 'blockStoreModule'">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Reference'" />
									<xsl:with-param name="id" select="'referencesection'" />
								</xsl:call-template>
							</xsl:if>
							<xsl:if test="prerecipe/text() | packages/* | recipe/text()">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Creation'" />
									<xsl:with-param name="id" select="'recipessection'" />
								</xsl:call-template>
							</xsl:if>
							<xsl:if test="nodes/entry">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Nodes'" />
									<xsl:with-param name="id" select="'nodessection'" />
								</xsl:call-template>
							</xsl:if>
							<xsl:if test="parameters/entry">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Parameters'" />
									<xsl:with-param name="id" select="'parametersection'" />
								</xsl:call-template>
							</xsl:if>
							<xsl:if test="targets/target">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Deployment'" />
									<xsl:with-param name="id" select="'deploymentsection'" />
								</xsl:call-template>
							</xsl:if>
							<xsl:if test="not($chooserType) and not(name(.) = 'projectModule')">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Runs'" />
									<xsl:with-param name="id" select="'runsection'" />
								</xsl:call-template>
							</xsl:if>
							<xsl:call-template name="addTab">
								<xsl:with-param name="headerName" select="'Authorization'" />
								<xsl:with-param name="id" select="'authzsection'" />
							</xsl:call-template>
						</div>
						<div class="visiblesection" id="summarysection">
							<div class="subsection">
								<table>
									<tr>
										<th class="row">Module</th>
										<td class="values">
											<a id="modulename"
												href="{@resourceUri}{$chooserUrlAttribute}">
												<xsl:value-of select="@name" />
											</a>
										</td>
									</tr>
									<tr>
										<th class="row">Version</th>
										<td class="values">
											<span id="moduleversion">
												<xsl:value-of select="@version" />
											</span>
											<xsl:text> (</xsl:text>
											<a
												href="module/{@name}/{$chooserUrlAttribute}">other versions</a>
											<xsl:text>)</xsl:text>
										</td>
									</tr>
									<tr>
										<th class="row">Description</th>
										<td class="values">
											<xsl:value-of select="@description" />
										</td>
									</tr>
									<tr>
										<th class="row">Comment</th>
										<td class="values">
											<xsl:value-of select="comment" />
										</td>
									</tr>
									<tr>
										<th class="row">Category</th>
										<td class="values">
											<xsl:call-template name="translateCategory">
												<xsl:with-param name="category" select="@category" />
											</xsl:call-template>
										</td>
									</tr>
									<tr>
										<th class="row">Created</th>
										<td class="values">
											<xsl:value-of select="@creation" />
										</td>
									</tr>
									<tr>
										<th class="row">Modified</th>
										<td class="values">
											<xsl:value-of select="@lastModified" />
										</td>
									</tr>
									<tr>
										<th class="row">Owner</th>
										<td class="values">
											<xsl:value-of select="authz/@owner" />
										</td>
									</tr>
									<xsl:if test="@published">
										<tr>
											<th class="row">Published</th>
											<td class="values">
												<xsl:call-template name="checkbox">
													<xsl:with-param select="'published'" name="attribute" />
												</xsl:call-template>
											</td>
										</tr>
									</xsl:if>
								</table>
							</div>
						</div>
						<div class="section" id="referencesection">
							<div class="subsection">
								<table>
									<xsl:if test="@moduleReferenceUri">
										<xsl:variable name="moduleReference">
											<xsl:call-template name='replace-spaces'>
												<xsl:with-param name="text" select="@moduleReferenceUri" />
											</xsl:call-template>
										</xsl:variable>
										<xsl:variable name="moduleReferenceName">
											<xsl:call-template name='module-resource-to-name'>
												<xsl:with-param name="name" select="$moduleReference" />
											</xsl:call-template>
										</xsl:variable>
										<tr>
											<th class="row">Machine Image Reference</th>
											<td class="values">
												<a href="{@moduleReferenceUri}{$chooserUrlAttribute}">
													<xsl:value-of select="$moduleReferenceName" />
												</a>
											</td>
										</tr>
									</xsl:if>
									<xsl:if test="//cloudImageIdentifiers/cloudImageIdentifier">
										<tr>
											<th class="row">Machine Image IDs</th>
												<td class="values">
													<xsl:for-each select="//cloudImageIdentifiers/cloudImageIdentifier">
														<div>
															<xsl:value-of select="@cloudServiceName" />: 
															<xsl:value-of select="@cloudImageIdentifier" />
														</div>
													</xsl:for-each>
												</td>
										</tr>
									</xsl:if>
									<xsl:if test="@volumeId">
										<tr>
											<th class="row">Disk Image ID</th>
											<td class="values">
												<xsl:value-of select="@volumeId" />
											</td>
										</tr>
									</xsl:if>
									<xsl:if test="@snapshotId">
										<tr>
											<th class="row">Disk Image Snapshot ID</th>
											<td class="values">
												<xsl:value-of select="@snapshotId" />
											</td>
										</tr>
									</xsl:if>
									<xsl:if test="@blockStoreReference">
										<xsl:variable name="blockStoreReference">
											<xsl:call-template name='replace-spaces'>
												<xsl:with-param name="text" select="@blockStoreReference" />
											</xsl:call-template>
										</xsl:variable>
										<tr>
											<th class="row">Disk Image Reference</th>
											<td class="values">
												<a
													href="{services/service[@name='navigator']/@url}/{@blockStoreReference}{$chooserUrlAttribute}">
													<xsl:value-of select="$blockStoreReference" />
												</a>
											</td>
										</tr>
									</xsl:if>
									<xsl:if test="@isBase = 'true'">
										<tr>
											<th class="row">Is a base image (i.e. not created by SlipStream)</th>
											<td class="values">
												<input type="checkbox" name="isbase" disabled="true">
													<xsl:if test="@isBase = 'true'">
														<xsl:attribute name='checked'>on</xsl:attribute>
													</xsl:if>
												</input>
											</td>
										</tr>
										<tr>
											<th class="row">Platform</th>
											<td class="values">
						                        <xsl:call-template name="platform">
						                        	<xsl:with-param name="disabled" select="'true'" />
						                        </xsl:call-template>
											</td>
										</tr>
										<tr>
											<th class="row">Login username</th>
											<td class="values">
												<input id="loginUser" name="loginUser" type="text" value="{@loginUser}" disabled="true" />
											</td>
										</tr>
									</xsl:if>
								</table>
							</div>
						</div>
						<xsl:if test="prerecipe/text() | packages/* | recipe/text()">
							<div class="section" id="recipessection">
								<xsl:apply-templates select="prerecipe" />
								<xsl:apply-templates select="packages" />
								<xsl:apply-templates select="recipe" />
							</div>
						</xsl:if>
						<xsl:if test="nodes">
							<xsl:apply-templates select="nodes" />
						</xsl:if>
						<xsl:if test="parameters">
							<div class="section" id="parametersection">
								<xsl:apply-templates select="parameters" />
							</div>
						</xsl:if>
						<xsl:if test="targets">
							<div class="section" id="deploymentsection">
								<xsl:apply-templates select="targets" />
							</div>
						</xsl:if>

						<xsl:if test="not($chooserType) and not(name(.) = 'projectModule')">
							<xsl:call-template name="executions" />
						</xsl:if>


						<xsl:apply-templates select="authz" />

					</div>

					<div class="page_head">
						<xsl:call-template name="addPageHead">
							<xsl:with-param name="headerName" select="''" />
							<xsl:with-param name="actions" select="$actions" />
						</xsl:call-template>
					</div>

				</div>

				<xsl:if test="not($chooserType)">
					<xsl:call-template name="bottom" />
				</xsl:if>

			</body>
		</html>
	</xsl:template>
	
	<xsl:template match="projectModule">

		<html>
			<head>
				<xsl:call-template name="head" />
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="@name" />
				</xsl:call-template>
				<script language="javascript" type="text/javascript" src="js/module.js" />
			</head>

			<xsl:variable name="headerName">
				<xsl:call-template name="translateCategory">
					<xsl:with-param name="category" select="@category" />
				</xsl:call-template>
			</xsl:variable>

			<body>

				<iframe id="loggeriframe" class="floating" />

				<div id="importdialog" class="dialog" title="Import module (as xml)">
					<form id="importform" action="" enctype="multipart/form-data" method="post">
						<input type="file" id="importinputfile" name="file" size="40" />
					</form>
				</div>

				<div id="topdiv">
					<xsl:variable name="chooserUrlAttribute">
						<xsl:if test="$chooserType">
							<xsl:text>?chooser=</xsl:text>
							<xsl:value-of select="$chooserType" />
						</xsl:if>
					</xsl:variable>
					<xsl:call-template name="top">
						<xsl:with-param name="chooserType" select="$chooserType" />
					</xsl:call-template>

					<!-- Set the buttons for the page head -->
					<xsl:variable name="actions">
						<xsl:if test="not($chooserType)">
							<form action="{$resourcePath}" method="get">
								<input type="hidden" name="edit" value="true" />
								<input type="submit" value="Edit">
									<xsl:call-template name="isDisabled">
										<xsl:with-param name="action" select="'Put'" />
									</xsl:call-template>
								</input>
							</form>
							<input type="button" name="Project" value="New Project" >
								<xsl:call-template name="isDisabled">
									<xsl:with-param name="action" select="'CreateChildren'" />
								</xsl:call-template>
							</input>
							<input type="button" name="Image" value="New Machine Image" >
								<xsl:call-template name="isDisabled">
									<xsl:with-param name="action" select="'CreateChildren'" />
								</xsl:call-template>
							</input>
							<input type="button" name="Deployment" value="New Deployment" >
								<xsl:call-template name="isDisabled">
									<xsl:with-param name="action" select="'CreateChildren'" />
								</xsl:call-template>
							</input>
							<input type="button" value="Import..." id="importbutton" />
						</xsl:if>
						<xsl:if test="$chooserType">
							<form>
								<xsl:choose>
									<xsl:when test="@category = $chooserType">
										<button name="SelectChooser" onclick="selectChooser();">Select</button>
										<button name="SelectChooserWithVersion" onclick="selectChooserWithVersion();">Select Exact Version</button>
									</xsl:when>
									<xsl:otherwise>
										<button disabled="on">Select</button>
										<button disabled="on">Select With Version</button>
									</xsl:otherwise>
								</xsl:choose>
								<button name="CancelChooser" onclick="cancelChooser();">Cancel</button>
							</form>
						</xsl:if>
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
								<xsl:with-param name="headerName" select="'Children'" />
								<xsl:with-param name="id" select="'childrensection'" />
								<xsl:with-param name="isVisible" select="'true'" />
							</xsl:call-template>
							<xsl:call-template name="addTab">
								<xsl:with-param name="headerName" select="'Summary'" />
								<xsl:with-param name="id" select="'summarysection'" />
							</xsl:call-template>
							<xsl:if test="parameters/entry">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Parameters'" />
									<xsl:with-param name="id" select="'parametersection'" />
								</xsl:call-template>
							</xsl:if>
							<xsl:call-template name="addTab">
								<xsl:with-param name="headerName" select="'Authorization'" />
								<xsl:with-param name="id" select="'authzsection'" />
							</xsl:call-template>
						</div>
						<div class="visiblesection" id="childrensection">
							<xsl:call-template name="children" />
						</div>
						<div class="section" id="summarysection">
							<div class="subsection">
								<table>
									<tr>
										<th class="row">Module</th>
										<td class="values">
											<a id="modulename"
												href="{@resourceUri}{$chooserUrlAttribute}">
												<xsl:value-of select="@name" />
											</a>
										</td>
									</tr>
									<tr>
										<th class="row">Version</th>
										<td class="values">
											<span id="moduleversion">
												<xsl:value-of select="@version" />
											</span>
											<xsl:text> (</xsl:text>
											<a
												href="module/{@name}/{$chooserUrlAttribute}">other versions</a>
											<xsl:text>)</xsl:text>
										</td>
									</tr>
									<tr>
										<th class="row">Description</th>
										<td class="values">
											<xsl:value-of select="@description" />
										</td>
									</tr>
									<tr>
										<th class="row">Comment</th>
										<td class="values">
											<xsl:value-of select="comment" />
										</td>
									</tr>
									<tr>
										<th class="row">Category</th>
										<td class="values">
											<xsl:call-template name="translateCategory">
												<xsl:with-param name="category" select="@category" />
											</xsl:call-template>
										</td>
									</tr>
									<tr>
										<th class="row">Created</th>
										<td class="values">
											<xsl:value-of select="@creation" />
										</td>
									</tr>
									<tr>
										<th class="row">Modified</th>
										<td class="values">
											<xsl:value-of select="@lastModified" />
										</td>
									</tr>
									<tr>
										<th class="row">Owner</th>
										<td class="values">
											<xsl:value-of select="authz/@owner" />
										</td>
									</tr>
								</table>
							</div>
						</div>
						<div class="section" id="referencesection">
							<div class="subsection">
								<table>
									<xsl:if test="@moduleReferenceUri">
										<xsl:variable name="moduleReference">
											<xsl:call-template name='replace-spaces'>
												<xsl:with-param name="text" select="@moduleReferenceUri" />
											</xsl:call-template>
										</xsl:variable>
										<xsl:variable name="moduleReferenceName">
											<xsl:call-template name='module-resource-to-name'>
												<xsl:with-param name="name" select="$moduleReference" />
											</xsl:call-template>
										</xsl:variable>
										<tr>
											<th class="row">Machine Image Reference</th>
											<td class="values">
												<a href="{@moduleReferenceUri}{$chooserUrlAttribute}">
													<xsl:value-of select="$moduleReferenceName" />
												</a>
											</td>
										</tr>
									</xsl:if>
									<xsl:if test="@imageId">
										<tr>
											<th class="row">Machine Image ID</th>
											<td class="values">
												<xsl:value-of select="@imageId" />
											</td>
										</tr>
									</xsl:if>
									<xsl:if test="@volumeId">
										<tr>
											<th class="row">Disk Image ID</th>
											<td class="values">
												<xsl:value-of select="@volumeId" />
											</td>
										</tr>
									</xsl:if>
									<xsl:if test="@snapshotId">
										<tr>
											<th class="row">Disk Image Snapshot ID</th>
											<td class="values">
												<xsl:value-of select="@snapshotId" />
											</td>
										</tr>
									</xsl:if>
									<xsl:if test="@blockStoreReference">
										<xsl:variable name="blockStoreReference">
											<xsl:call-template name='replace-spaces'>
												<xsl:with-param name="text" select="@blockStoreReference" />
											</xsl:call-template>
										</xsl:variable>
										<tr>
											<th class="row">Disk Image Reference</th>
											<td class="values">
												<a
													href="{services/service[@name='navigator']/@url}/{@blockStoreReference}{$chooserUrlAttribute}">
													<xsl:value-of select="$blockStoreReference" />
												</a>
											</td>
										</tr>
									</xsl:if>
									<xsl:if test="@isBase = 'true'">
										<tr>
											<th class="row">Is a base image (i.e. not created by SlipStream)</th>
											<td class="values">
												<input type="checkbox" name="isbase" disabled="true">
													<xsl:if test="@isBase = 'true'">
														<xsl:attribute name='checked'>on</xsl:attribute>
													</xsl:if>
												</input>
											</td>
										</tr>
									</xsl:if>
								</table>
							</div>
						</div>
						<xsl:if test="prerecipe/text() | packages/* | recipe/text()">
							<div class="section" id="recipessection">
								<xsl:apply-templates select="prerecipe" />
								<xsl:apply-templates select="packages" />
								<xsl:apply-templates select="recipe" />
							</div>
						</xsl:if>
						<xsl:if test="nodes">
							<xsl:apply-templates select="nodes" />
						</xsl:if>
						<xsl:if test="parameters">
							<div class="section" id="parametersection">
								<xsl:apply-templates select="parameters" />
							</div>
						</xsl:if>
						<xsl:if test="targets">
							<div class="section" id="deploymentsection">
								<xsl:apply-templates select="targets" />
							</div>
						</xsl:if>

						<xsl:if test="not($chooserType) and not(name(.) = 'projectModule')">
							<xsl:call-template name="executions" />
						</xsl:if>


						<xsl:apply-templates select="authz" />

					</div>

					<div class="page_head">
						<xsl:call-template name="addPageHead">
							<xsl:with-param name="headerName" select="''" />
							<xsl:with-param name="actions" select="$actions" />
						</xsl:call-template>
					</div>

				</div>

				<xsl:if test="not($chooserType)">
					<xsl:call-template name="bottom" />
				</xsl:if>

			</body>
		</html>
	</xsl:template>

	<xsl:template match="nodes">
		<div class="section" id="nodessection">
			<div class="subsection">
				<table id="nodestable">
					<thead>
						<tr>
							<th>Name</th>
							<th>Mapping</th>
						</tr>
					</thead>
					<tbody>
						<xsl:apply-templates select="entry" mode="node">
							<xsl:sort select="string" />
						</xsl:apply-templates>
					</tbody>
				</table>
			</div>
		</div>
	</xsl:template>

	<!-- the options the user can provide using the 'Run...' button  -->
	<xsl:template match="nodes" mode="runOptions">
		<xsl:apply-templates select="entry" mode="runOptions">
			<xsl:sort select="string" />
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template match="packages">
		<xsl:if test="*">
			<div class="subsection">
				<div class="subsection_head">
					<span>Packages</span>
				</div>
				<table>
					<thead>
						<tr>
							<tr>
								<th>Name</th>
								<th>Repository</th>
								<th>Key</th>
							</tr>
						</tr>
					</thead>
					<xsl:apply-templates />
				</table>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="package">
		<tr>
			<td>
				<xsl:value-of select="@name" />
			</td>
			<td>
				<xsl:value-of select="@repository" />
			</td>
			<td>
				<xsl:value-of select="@key" />
			</td>
		</tr>
	</xsl:template>

	<xsl:template match="prerecipe">
		<xsl:if test='. != ""'>
			<div class="subsection">
				<div class="subsection_head">
					<span>Bootstrap recipe (SlipStream client installation)</span>
				</div>
				<pre>
					<xsl:value-of select="." />
				</pre>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="recipe">
		<xsl:if test='. != ""'>
			<div class="subsection">
				<div class="subsection_head">
					<span>Recipe</span>
				</div>
				<pre>
					<xsl:value-of select="." />
				</pre>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="parameterMappings">
		<xsl:if test="entry">
			<div class="subsection">
				<div class="subsection_head">
					<span>Parameter mappings</span>
				</div>
				<table>
					<thead>
						<tr>
							<th>Input parameter</th>
							<th>Output parameter</th>
						</tr>
					</thead>
					<xsl:apply-templates select="entry" />
				</table>
			</div>
		</xsl:if>
	</xsl:template>

	<xsl:template match="targets">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="authz">
		<div class="section" id='authzsection'>
			<div class="subsection" id="authorizationsubsection">
				<table id="authorizationtable">
					<thead>
						<tr>
							<th />
							<th>Owner</th>
							<th>Group</th>
							<th>Public</th>
						</tr>
					</thead>
					<tr>
						<td>Read</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'ownerGet'" name="attribute" />
							</xsl:call-template>
						</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'groupGet'" name="attribute" />
							</xsl:call-template>
						</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'publicGet'" name="attribute" />
							</xsl:call-template>
						</td>
					</tr>
					<tr>
						<td>Write</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'ownerPut'" name="attribute" />
							</xsl:call-template>
						</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'groupPut'" name="attribute" />
							</xsl:call-template>
						</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'publicPut'" name="attribute" />
							</xsl:call-template>
						</td>
					</tr>
					<tr>
						<td>Delete</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'ownerDelete'" name="attribute" />
							</xsl:call-template>
						</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'groupDelete'" name="attribute" />
							</xsl:call-template>
						</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'publicDelete'" name="attribute" />
							</xsl:call-template>
						</td>
					</tr>
					<tr>
						<td>Execute</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'ownerPost'" name="attribute" />
							</xsl:call-template>
						</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'groupPost'" name="attribute" />
							</xsl:call-template>
						</td>
						<td>
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'publicPost'" name="attribute" />
							</xsl:call-template>
						</td>
					</tr>
					<xsl:if test="../@category = 'Project'">
						<tr>
							<td>Create Children</td>
							<td>
								<xsl:call-template name="checkbox">
									<xsl:with-param select="'ownerCreateChildren'" name="attribute" />
								</xsl:call-template>
							</td>
							<td>
								<xsl:call-template name="checkbox">
									<xsl:with-param select="'groupCreateChildren'" name="attribute" />
								</xsl:call-template>
							</td>
							<td>
								<xsl:call-template name="checkbox">
									<xsl:with-param select="'publicCreateChildren'" name="attribute" />
								</xsl:call-template>
							</td>
						</tr>
					</xsl:if>
				</table>
				<table>
					<thead>
						<tr>
							<th>Group members</th>
						</tr>
					</thead>
					<tr>
						<td>Inherited from parent group members:
							<xsl:call-template name="checkbox">
								<xsl:with-param select="'inheritedGroupMembers'" name="attribute" />
							</xsl:call-template>
						</td>
					</tr>
					<xsl:if test="@inheritedGroupMembers != 'true'">
						<xsl:for-each select="groupMembers/string">
							<tr>
								<td>
									<xsl:value-of select="." />
								</td>
							</tr>
						</xsl:for-each>
					</xsl:if>
				</table>
			</div>
		</div>
	</xsl:template>

	<xsl:template name="checkbox">
		<xsl:param name="attribute" />
		<input type="checkbox" name="{$attribute}" disabled="true">
			<xsl:if test="@*[name()=$attribute] = 'true'">
				<xsl:attribute name='checked'>on</xsl:attribute>
			</xsl:if>
		</input>
	</xsl:template>

	<xsl:template match="target">
		<div class="subsection">
			<div class="subsection_head">
				<span>
					<xsl:value-of select="@name" />
				</span>
			</div>

			<xsl:if test="@runInBackground = 'true'">
				<xsl:text> (run in the background: </xsl:text>
				<input name="{@name}--checkbox" type="checkbox" checked="true"
					disabled="true" />
				<xsl:text>)</xsl:text>
			</xsl:if>
			<pre>
				<xsl:value-of select="." />
			</pre>
		</div>

	</xsl:template>

	<xsl:template name="checkBox">
		<input type="checkbox" disabled="true" name="checkbox-{@name}">
			<xsl:if test=".='on'">
				<xsl:attribute name="checked" />
			</xsl:if>
		</input>
	</xsl:template>

	<xsl:template match="entry">
		<tr>
			<td>
				<xsl:value-of select="string" />
			</td>
			<td>
				<xsl:value-of select="nodeParameter" />
			</td>
		</tr>
	</xsl:template>

	<!-- 'Run...' button -->
	<xsl:template match="entry" mode="runOptions">
		<div class="subsection">
			<div class="subsection_head" id="runoptionsnodessection">
				<span>
					<xsl:value-of select="string" />
				</span>
			</div>
			<table>
				<tr>
					<td>
						<xsl:text>Multiplicity</xsl:text>
					</td>
					<td>
						<input type="text"
						       name="parameter--node--{string}--multiplicity"
						       value="{node/@multiplicity}" />
					</td>
				</tr>
				<tr>
					<td>
						<xsl:text>Cloud service</xsl:text>
					</td>
					<td>
						<select name="parameter--node--{string}--cloudservice">
							<xsl:variable name="node" select="node"/>
							<xsl:for-each select="../../cloudNames/string">
								<option>
									<xsl:if test="$node/@cloudService = .">
										<xsl:attribute name="selected"/>
									</xsl:if>
									<xsl:value-of select=".">
									</xsl:value-of>
								</option>
							</xsl:for-each>
						</select>
					</td>
				</tr>
				<xsl:for-each select="node/parameters/entry/parameter">
					<xsl:if test="@isMappedValue = 'false'">
						<tr>
							<td>
								<xsl:value-of select="@name" />
							</td>
							<td>
								<input type="text"
								       name="parameter--node--{../../../../string}--{@name}"
								       value="{value}" />
							</td>
						</tr>
					</xsl:if>
				</xsl:for-each>
			</table>
		</div>
	</xsl:template>

	<xsl:template match="entry" mode="node">
		<xsl:variable name="chooserUrlAttribute">
			<xsl:if test="$chooserType">
				<xsl:text>?chooser=</xsl:text>
				<xsl:value-of select="$chooserType" />
			</xsl:if>
		</xsl:variable>
		<tr>
			<td>
				<xsl:value-of select="string" />
			</td>
			<td>
				<table>
					<tr>
						<td>
							Reference image:
							<a
								href="{node/@imageUri}{$chooserUrlAttribute}">
								<xsl:value-of select="node/@imageUri" />
							</a>
						</td>
						<xsl:if
							test="node/@multiplicity > '1'">
							<td align="right">
								<xsl:text>Multiplicity: </xsl:text>
								<xsl:value-of
									select="node/@multiplicity" />
							</td>
						</xsl:if>
						<td align="right">
							<xsl:text>Cloud service: </xsl:text>
							<xsl:value-of select="node/@cloudService" />
						</td>
					</tr>
					<tr>
						<td colspan="2">
							<xsl:apply-templates select="node/parameterMappings" />
						</td>
						<td />
					</tr>
				</table>
			</td>
		</tr>
	</xsl:template>

	<xsl:template name="chooserUrlFragment">
		<xsl:variable name="chooserUrlFragment">
			<xsl:if test="$chooserType">
				?chooser=
				<xsl:value-of select="$chooserType" />
			</xsl:if>
		</xsl:variable>
		<xsl:value-of select="'$chooserUrlFragment'" />
	</xsl:template>

	<xsl:template name="isDisabled">
		<xsl:param name="action" />
		<xsl:variable name="disabled">
			<xsl:choose>
				<xsl:when test="/*/authz/@*[name()=concat('public',$action)] = 'true'">
					<xsl:value-of select="false()" />
				</xsl:when>
				<xsl:when
					test="(/*/user/@username = /*/authz/groupMembers/string) and (/*/authz/@*[name()=concat('group',$action)] = 'true')">
					<xsl:value-of select="false()" />
				</xsl:when>
				<xsl:when
					test="(/*/user/@username = /*/authz/@owner) and (/*/authz/@*[name()=concat('owner',$action)] = 'true')">
					<xsl:value-of select="false()" />
				</xsl:when>
				<xsl:when test="/*/user/@issuper = 'true'">
					<xsl:value-of select="false()" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="true()" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:if test="$disabled = 'true'">
			<xsl:attribute name="disabled">on</xsl:attribute>
		</xsl:if>
	</xsl:template>

	<xsl:template name="children">
		<div class="subsection">
			<xsl:if test="children/item">
				<table id="childrentable">
					<thead>
						<tr>
							<th width="250">Name</th>
							<th width="80">Category</th>
							<th width="350">Description</th>
						</tr>
					</thead>
					<xsl:apply-templates select="children/item" />
				</table>
			</xsl:if>
		</div>
	</xsl:template>

	<xsl:template match="item">
		<xsl:call-template name="item">
			<xsl:with-param name="chooserType" select="$chooserType" />
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="executions">
		<xsl:variable name="qname">
			<xsl:value-of select="@name" />
		</xsl:variable>
		<xsl:variable name="moduleQName">
			<xsl:value-of select="@parent/@name" />
		</xsl:variable>
		<div class="section" id="runsection">
			<div class="subsection">
				<iframe name="execiframe" id="execiframe"
					src="run/?query={@resourceUri}&amp;isembedded=true" width="100&#38;" />
			</div>
		</div>
	</xsl:template>

	<xsl:template name="copydialog">
	
		<iframe id="chooseriframe" class="floating" style="z-index:10000" />

		<div id="copydialog" title="Copy Module?">
			<div id="copydialogerror" class="error hidden">
				<span />
			</div>
			<form method="post" action="" id="copyform" name="copyform">
				<input type="hidden" name="source_uri" value="{@resourceUri}" />
				Select a project where to create the copy:
				<input disabled="true" type="text" id="target_project_uri" name="target_project_uri" value="" size="50" />
				<xsl:variable name='moduleReference'>
					<xsl:call-template name='replace-spaces'>
						<xsl:with-param name="text" select="@moduleReferenceUri" />
					</xsl:call-template>
				</xsl:variable>
				<xsl:variable name="moduleReferenceName">
					<xsl:call-template name='module-resource-to-name'>
						<xsl:with-param name="name" select="$moduleReference" />
					</xsl:call-template>
				</xsl:variable>
				<input class="button" type="button" name="moduleReferenceChooser" id="moduleReferenceChooser"
					onclick="showChooser('target_project_uri','Project');"
					value="Choose Reference">
				</input>
				<p/>
				Name of the new module:
				<input type="text" name="target_name" value="{@shortName}" size="50" />
			</form>
		</div>
	</xsl:template>

</xsl:stylesheet>
