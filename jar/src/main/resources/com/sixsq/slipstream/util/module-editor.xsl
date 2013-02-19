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

	<xsl:include href="common.xsl" />
	<xsl:include href="messages.xsl" />

	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="deploymentModule">

		<xsl:variable name="modulename">
			<xsl:call-template name="modulename" />
		</xsl:variable>

		<html>
			<head>
				<xsl:call-template name="head" />
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Deployment'" />
				</xsl:call-template>
				<xsl:call-template name="add-scripts" />
				<script language="javascript" type="text/javascript" src="js/deployment.js" />
			</head>

			<body>
			
				<div class="hidden">
					<select id="cloudServiceNamesList">
						<xsl:variable name="node" select="node"/>
						<xsl:for-each select="cloudNames/string">
							<option>
								<xsl:if test="'default' = .">
									<xsl:attribute name="selected"/>
								</xsl:if>
								<xsl:value-of select=".">
								</xsl:value-of>
							</option>
						</xsl:for-each>
					</select>
				</div>

				<iframe id="chooseriframe" class="floating" />
				<iframe id="loggeriframe" class="floating" />
				<xsl:call-template name="savedialog" />

				<div id="topdiv">
					<xsl:call-template name="top" />

					<form action="{$resourcePath}?method=put" method="post"
						enctype="x-www-form-urlencoded" accept-charset="utf-8" id="formsave">

						<div class="page">
							<xsl:call-template name="MainHeader">
								<xsl:with-param name="headerName" select="'Deployment'" />
							</xsl:call-template>

							<div class="tabs">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Summary'" />
									<xsl:with-param name="id" select="'summarysection'" />
									<xsl:with-param name="isVisible" select="'true'" />
								</xsl:call-template>
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Nodes'" />
									<xsl:with-param name="id" select="'nodessection'" />
								</xsl:call-template>
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Post-processing'" />
									<xsl:with-param name="id" select="'postprocessingsection'" />
								</xsl:call-template>
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Authorization'" />
									<xsl:with-param name="id" select="'authzsection'" />
								</xsl:call-template>
							</div>

							<xsl:call-template name="topSummary" />

							<xsl:call-template name="nodes" />

							<div class="section" id="postprocessingsection">
								<div class="section_head">
									<span>Post-processing script (e.g. to process reports)
									</span>
								</div>
								<textarea name="post-processing" id="post-processing"
									rows="20" cols="120">
									<xsl:value-of select="parameters/entry/parameter[@name='post-processing']" />
								</textarea>
							</div>

							<xsl:call-template name="authz" />

							<div class="page_head">
								<xsl:call-template name="MainHeader">
									<xsl:with-param name="headerName" select="''" />
								</xsl:call-template>
							</div>
						</div>
					</form>

					<xsl:call-template name="bottom" />

				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="imageModule">

		<xsl:variable name="modulename">
			<xsl:call-template name="modulename" />
		</xsl:variable>

		<html>
			<head>
				<xsl:call-template name="head" />

				<xsl:variable name="subtitle">
					<xsl:if test="@category='Image'">
						<xsl:text>Machine Image</xsl:text>
					</xsl:if>
				</xsl:variable>
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="$subtitle" />
				</xsl:call-template>
				<xsl:call-template name="add-scripts" />
				<script language="javascript" type="text/javascript" src="js/module.js" />
			</head>

			<body>

				<iframe id="chooseriframe" class="floating" />
				<iframe id="loggeriframe" class="floating" />
				<xsl:call-template name="savedialog" />

				<div id="topdiv">
					<xsl:call-template name="top" />

					<form action="{$resourcePath}?method=put" method="post"
						enctype="x-www-form-urlencoded" accept-charset="utf-8" id="formsave">

						<div class="page">
							<div class="page_head">
								<xsl:choose>
									<xsl:when test="@category='Image'">
										<xsl:call-template name="MainHeader">
											<xsl:with-param name="headerName" select="'Machine Image'" />
										</xsl:call-template>
									</xsl:when>
								</xsl:choose>
							</div>
							<div class="tabs">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Summary'" />
									<xsl:with-param name="id" select="'summarysection'" />
									<xsl:with-param name="isVisible" select="'true'" />
								</xsl:call-template>
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Reference'" />
									<xsl:with-param name="id" select="'referencesection'" />
								</xsl:call-template>
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Creation'" />
									<xsl:with-param name="id" select="'recipessection'" />
								</xsl:call-template>
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Parameters'" />
									<xsl:with-param name="id" select="'propertiessection'" />
								</xsl:call-template>
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Deployment'" />
									<xsl:with-param name="id" select="'targetssection'" />
								</xsl:call-template>
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Authorization'" />
									<xsl:with-param name="id" select="'authzsection'" />
								</xsl:call-template>
							</div>

							<xsl:call-template name="topSummary" />

							<xsl:variable name="isBase">
								<xsl:value-of select="@isBase" />
							</xsl:variable>

							<div class="section" id="referencesection">
								<table>
									<tr>
										<th class="row">Machine Image Reference</th>
										<td class="values" colspan="2">
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
											<input name="moduleReference" id="moduleReferenceInput"
												type="text" value="{$moduleReferenceName}" readonly="true" />
											<input class="button" type="button" name="moduleReferenceChooser" id="moduleReferenceChooser"
												onclick="showChooser('moduleReferenceInput','Image',updateParameterDefaults);"
												value="Choose Reference">
												<xsl:if test="$isBase = 'true'">
													<xsl:attribute name="disabled">true</xsl:attribute>
												</xsl:if>
											</input>
										</td>
									</tr>
									<tr>
										<th class="row">Machine Image ID(s)</th>
										<td class="values" colspan="2" id="cloudimages">
											<xsl:for-each select="cloudNames/string[not(. = 'default')]">
												<div>
													<xsl:variable name="cloudName" select="." />
													<xsl:value-of select="$cloudName"/>
													<xsl:text>: </xsl:text>
													<input name="cloudimageid_imageid_{.}" type="input" value="{//cloudImageIdentifier[@cloudServiceName=$cloudName]/@cloudImageIdentifier}">
														<xsl:if test="$isBase != 'true'">
															<xsl:attribute name="disabled">true</xsl:attribute>
														</xsl:if>
													</input>
												</div>
											</xsl:for-each>
										</td>
									</tr>
									<tr>
										<th class="row">Is a base image (created by hand)</th>
										<td class="values">
											<input id="isbase" name="isbase" type="checkbox">
												<xsl:if test="@isBase = 'true'">
													<xsl:attribute name='checked'>on</xsl:attribute>
												</xsl:if>
											</input>
										</td>
										<td>
											<a class="instructions" href='javascript:void(0)'>
												<img src="images/common/info.png" alt="information" />
											</a>
										</td>
									</tr>
									<tr class="instructions">
										<td colspan="3">
											<xsl:call-template name="image-module-base-image-checkbox"/>
										</td>
									</tr>
									<tr>
										<th class="row">Platform</th>
										<td class="values" colspan="2">
											<xsl:call-template name="platform" />
										</td>
									</tr>
									<tr>
										<th class="row">Login username</th>
										<td class="values" colspan="2">
											<input id="loginUser" name="loginUser" type="text" value="{@loginUser}">
												<xsl:if test="not(@isBase = 'true')">
													<xsl:attribute name="disabled">true</xsl:attribute>
												</xsl:if>
											</input>
										</td>
									</tr>
								</table>
							</div>
						</div>

						<div class="section" id="recipessection">
							<xsl:call-template name="prerecipe" />
							<xsl:call-template name="packages" />
							<xsl:call-template name="recipe" />
						</div>

						<div class="section" id="propertiessection">
							<xsl:for-each select="parameters">
								<xsl:apply-templates select="." mode="edit" />
							</xsl:for-each>
						</div>

						<div class="section" id="targetssection">
							<xsl:call-template name="targets" />
						</div>

						<xsl:call-template name="authz" />

						<div class="page_head">
							<xsl:call-template name="MainHeader">
								<xsl:with-param name="headerName" select="''" />
							</xsl:call-template>
						</div>
					</form>

					<xsl:call-template name="bottom" />

				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="projectModule">

		<xsl:variable name="modulename">
			<xsl:call-template name="modulename" />
		</xsl:variable>

		<html>
			<head>
				<xsl:call-template name="head" />
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Project'" />
				</xsl:call-template>
				<xsl:call-template name="add-scripts" />
			</head>

			<body>

				<iframe id="loggeriframe" class="floating" />
				<xsl:call-template name="savedialog" />

				<div id="topdiv">
					<xsl:call-template name="top" />

					<form action="{$resourcePath}?method=put" method="post"
						enctype="x-www-form-urlencoded" accept-charset="utf-8" id="formsave">
						<div class="page">
							<div class="page_head">
								<xsl:call-template name="MainHeader">
									<xsl:with-param name="headerName" select="'Project'" />
								</xsl:call-template>
							</div>

							<div class="tabs">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Summary'" />
									<xsl:with-param name="id" select="'summarysection'" />
									<xsl:with-param name="isVisible" select="'true'" />
								</xsl:call-template>
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Authorization'" />
									<xsl:with-param name="id" select="'authzsection'" />
								</xsl:call-template>
							</div>

							<xsl:call-template name="topSummary" />

							<xsl:call-template name="authz" />

							<div class="page_head">
								<xsl:call-template name="MainHeader">
									<xsl:with-param name="headerName" select="''" />
								</xsl:call-template>
							</div>

							<xsl:call-template name="bottom" />

						</div>
					</form>
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template name="add-scripts">
		<script language="javascript" type="text/javascript" src="js/module.js" />
	</xsl:template>

	<xsl:template match="list">

		<xsl:variable name="empty" />

		<xsl:variable name='name'>
			<xsl:choose>
				<xsl:when test="@name = $empty">
					<xsl:text>/</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name='replace-spaces'>
						<xsl:with-param name="text" select="@name" />
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<html>
			<head>
				<xsl:call-template name="head" />
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Projects'" />
				</xsl:call-template>

				<script language="javascript" type="text/javascript" src="js/common.js" />
			</head>

			<body>
				<div id="topdiv">
					<xsl:call-template name="top" />

					<div class="page_head">
						<span>
							<xsl:value-of select="$name" />
						</span>
					</div>

					<div class="section_head">
						<span>Modules</span>
					</div>
					<table>
						<thead>
							<tr>
								<th width="250">Name</th>
								<th width="80">Type</th>
								<th width="80">Category</th>
								<th width="350">Description</th>
							</tr>
						</thead>
						<tbody>
							<xsl:for-each select="item">
								<xsl:apply-templates select="." />
							</xsl:for-each>
						</tbody>
					</table>

					<xsl:call-template name="bottom" />

				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="item">
		<xsl:variable name='module'>
			<xsl:call-template name='replace-spaces'>
				<xsl:with-param name="text" select="@shortModule" />
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name='position'>
			<xsl:number />
		</xsl:variable>
		<!-- set the id to the parent (targets) + the position, such that it's 
			unique -->
		<xsl:variable name="id">
			<xsl:value-of select="name(..)" />
			<xsl:text>--</xsl:text>
			<xsl:value-of select="$position" />
		</xsl:variable>
		<tr id="{$id}">
			<td>
				<a href="{@resourceUri}">
					<input type="hidden" name="child--{$position}" value="{@name}" />
					<xsl:value-of select="$module" />
				</a>
			</td>
			<td>
				<xsl:value-of select="@category" />
			</td>
			<xsl:if test="not(@description)">
				<td />
			</xsl:if>
			<xsl:if test="@description">
				<td>
					<xsl:value-of select="@description" />
				</td>
			</xsl:if>
		</tr>
	</xsl:template>

	<xsl:template name="packages">
		<div class="subsection">
			<div class="subsection_head">
				<span>Packages </span>
			</div>
			<table id="packages">
				<thead>
					<tr>
						<th>Name</th>
						<th>Repository</th>
						<th>key</th>
						<th width="1%"></th>
					</tr>
				</thead>
				<xsl:for-each select="packages/package">
					<xsl:variable name="id">
						<xsl:text>package--</xsl:text>
						<xsl:value-of select="count(preceding-sibling::node())" />
					</xsl:variable>
					<tr id="{$id}">
						<td>
							<input name="{$id}--name" value="{@name}" type="text" />
						</td>
						<td>
							<input name="{$id}--repository" value="{@repository}" type="text" />
						</td>
						<td>
							<input name="{$id}--key" value="{@key}" type="text" />
						</td>
						<td>
							<input class="button" type="button" onclick="removeElement('{$id}');"
								value="Remove" />
						</td>
					</tr>
				</xsl:for-each>
			</table>
			<xsl:call-template name="addPackageButton" />
		</div>
	</xsl:template>

	<xsl:template name="prerecipe">
		<xsl:variable name="isDefined">
			<xsl:choose>
				<xsl:when test="not(prerecipe = '')">true</xsl:when>
				<xsl:otherwise>false</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:call-template name="addSectionHead">
			<xsl:with-param name="headerName"
				select="'Prerecipe (before package installation)'" />
			<xsl:with-param name="id" select="'precipesubsection'" />
			<xsl:with-param name="toggle" select="'true'" />
			<xsl:with-param name="isExpanded" select="$isDefined" />
		</xsl:call-template>

		<div id="precipesubsection">
			<xsl:attribute name="style">
				<xsl:if test="$isDefined = 'false'">display: none</xsl:if>
			</xsl:attribute>
			<textarea name="prerecipe--script" rows="20" cols="120">
				<xsl:value-of select="prerecipe" />
			</textarea>
		</div>
	</xsl:template>

	<xsl:template name="recipe">
		<div class="subsection">
			<div class="subsection_head">
				<span>Recipe</span>
			</div>
			<div>
				<textarea name="recipe--script" rows="20" cols="120">
					<xsl:value-of select="recipe" />
				</textarea>
			</div>
		</div>
	</xsl:template>

	<xsl:template name="publishing">
		<div class="subsection">
			<div class="subsection_head">
				<span>Publishing</span>
			</div>
			<div>
				<input type="checkbox" name="publishing-make-public">
					<xsl:if test="parameters/entry/parameter[@name='publishing-make-public']">
						<xsl:value-of select="recipe" />
					</xsl:if>
				</input>
			</div>
		</div>
	</xsl:template>

	<xsl:template name="nodes">
		<div class="section" id="nodessection">
			<xsl:call-template name="addNodeButton" />
			<div class="subsection">
				<table>
					<thead>
						<tr>
							<th>Name</th>
							<th>Image link</th>
							<th width="1%"></th>
						</tr>
					</thead>
					<tbody id="nodes">
						<xsl:for-each select="nodes/entry">
							<xsl:sort select="string" />
							<xsl:apply-templates select="." mode="node" />
						</xsl:for-each>
					</tbody>
				</table>
			</div>
		</div>
	</xsl:template>

	<xsl:template name="addTargetButton">
		<input class="button" type="button" onclick="addTarget('targets');" value="Add Target" />
	</xsl:template>

	<xsl:template name="addPackageButton">
		<input class="button" type="button" onclick="addPackage();" value="Add Package" />
	</xsl:template>

	<xsl:template name="addNodeButton">
		<xsl:variable name='counter'>
			<!-- Count the number of nodes and set the counter. This will be used 
				by when dynamically adding nodes -->
			<xsl:value-of select="count(nodes/entry)" />
		</xsl:variable>
		<input type="hidden" value="{$counter}" id="nodeCounter" />
		<input type="hidden" value="" id="nodeSelection" />
		<input class="button" type="button" id="moduleReferenceChooser"
			onclick="showChooser('nodeSelection','Image',addNode); nodeParametersAutoCompleter.clear();" value="Add Node" />
	</xsl:template>

	<xsl:template name="addParameterMappingButton">
		<xsl:param name="targetid" />
		<xsl:variable name='count'>
			<xsl:value-of select="count(node/parameterMappings/entry)" />
		</xsl:variable>
		<input class="button" type="button" onclick="addParameterMapping('{$targetid}');"
			value="Add Parameter Mapping" />
	</xsl:template>

	<xsl:template name="parameterMappings">
		<xsl:param name="nodeid" />
		<xsl:variable name="id">
			<xsl:value-of select="$nodeid" />
			<xsl:text>--mappingtable</xsl:text>
		</xsl:variable>
		<div>
			<div class="section_head">
				<span>Parameter mappings </span>
				<xsl:call-template name="addParameterMappingButton">
					<xsl:with-param name="targetid" select="$id" />
				</xsl:call-template>
			</div>
			<table id="{$id}" class="full">
				<thead>
					<tr>
						<th>Input parameter</th>
						<th>Linked to</th>
					</tr>
				</thead>
				<xsl:for-each select="node/parameterMappings/entry">
					<xsl:variable name="mappingid">
						<xsl:value-of select="$id" />
						<xsl:text>--</xsl:text>
						<xsl:value-of select="count(preceding-sibling::node())" />
					</xsl:variable>
					<tr id="{$mappingid}">
						<td>
							<input name="{$mappingid}--input" value="{string}" type="text" />
						</td>
						<td>
							<input id="nodeparametermappingoutput" name="{$mappingid}--output" value="{nodeParameter/value}"
								type="text" />
						</td>
						<td width="1%">
							<input class="button" type="button" onclick="removeElement('{$mappingid}');"
								value="Remove" />
						</td>
					</tr>
				</xsl:for-each>
			</table>
		</div>
	</xsl:template>

	<xsl:template name="targets">
		<div class="subsection">
			<xsl:call-template name="addTargetButton" />
			<table>
				<tbody id="targets">
					<xsl:apply-templates select="targets/target" />
				</tbody>
			</table>
		</div>
	</xsl:template>

	<xsl:template name="authz">
		<div class="section" id="authzsection">
			<div class="subsection">
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
					<xsl:if test="@category = 'Project'">
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
					<tr>
						<td>
							<input type="input" name="groupmembers" id="groupmembers">
								<xsl:attribute name="value">
									<xsl:for-each select="authz/groupMembers/string">
										<xsl:value-of select="." />									
										<xsl:text>, </xsl:text>
									</xsl:for-each>				
								</xsl:attribute>
							</input>
						</td>
					</tr>
				</table>
			</div>
		</div>
	</xsl:template>

	<xsl:template name="checkbox">
		<xsl:param name="attribute" />
		<input type="checkbox" name="{$attribute}" id="{$attribute}">
			<xsl:if test="authz/@*[name()=$attribute] = 'true'">
				<xsl:attribute name='checked'>on</xsl:attribute>
			</xsl:if>
		</input>
	</xsl:template>

	<xsl:template name="addParameter">
		<xsl:param name="prefix" />
		<xsl:param name="name" />
		<xsl:param name="description" />
		<xsl:param name="default" />
		<tr>
			<xsl:variable name="uniqueId">
				parameters--
				<xsl:value-of select="concat($prefix,'_',$name)" />
				--
			</xsl:variable>
			<xsl:variable name="value">
				<xsl:if test="not(parameters/entry/parameter[@name=concat($prefix,'_',$name)])">
					<xsl:value-of select="$default" />
				</xsl:if>
				<xsl:if test="parameters/entry/parameter[@name=concat($prefix,'_',$name)]">
					<xsl:value-of
						select="parameters/entry/parameter[@name=concat($prefix,'_',$name)]" />
				</xsl:if>
			</xsl:variable>
			<td>
				<xsl:value-of select="$name" />
				<input type="hidden" name="{$uniqueId}name" value="{$prefix}_{$name}" />
			</td>
			<td width="50%">
				<xsl:value-of select="$description" />
				<input type="hidden" name="{$uniqueId}description" value="{$description}" />
			</td>
			<td>
				<input type="text" name="{$uniqueId}value" value="{$value}" />
			</td>
		</tr>
	</xsl:template>

	<xsl:template match="target">
		<xsl:variable name='position'>
			<xsl:number />
		</xsl:variable>
		<!-- set the id to the parent (targets) + the position, such that it's 
			unique -->
		<xsl:variable name="id">
			<xsl:value-of select="name(..)" />
			<xsl:text>--</xsl:text>
			<xsl:value-of select="$position" />
		</xsl:variable>
		<tr id="{$id}">
			<td>
				<table>
					<tr>
						<th class="row">
							<xsl:text>Name: </xsl:text>
							<select name="{$id}--name">
								<option>
									<xsl:if test="@name = 'execute'">
										<xsl:attribute name="selected" />
									</xsl:if>
									<xsl:text>execute</xsl:text>
								</option>
								<option>
									<xsl:if test="@name = 'report'">
										<xsl:attribute name="selected" />
									</xsl:if>
									<xsl:text>report</xsl:text>
								</option>
							</select>
						</th>
						<th class="row">
							<xsl:text>Run in the background:</xsl:text>
							<input type="checkbox" name="{$id}--runinbackground">
								<xsl:if test="@runInBackground='true'">
									<xsl:attribute name="checked">on</xsl:attribute>
								</xsl:if>
							</input>
						</th>
						<th align="right">
							<input class="button" type="button" name="remove" value="Remove"
								onClick="removeElement('{$id}')" />
						</th>
					</tr>
					<tr>
						<th class="row">Script:</th>
					</tr>
					<tr>
						<td colspan="3">
							<textarea name="{$id}--script" rows="20" cols="110">
								<xsl:value-of select="." />
							</textarea>
						</td>
					</tr>
				</table>
			</td>
		</tr>
	</xsl:template>

	<!-- These entries contain the nodes -->
	<xsl:template match="entry" mode="node">
		<xsl:variable name='position'>
			<xsl:number />
		</xsl:variable>
		<xsl:variable name="id">
			<xsl:text>node--</xsl:text>
			<xsl:value-of select="$position" />
		</xsl:variable>
		<tr id="{$id}">
			<xsl:call-template name="setTRClassAttribute" />
			<td>
				<input id="nodename" name="{$id}--shortname" type="text" value="{string}" />
			</td>
			<td>
				<table>
					<tr>
						<td>
							Reference image:
							<xsl:variable name="imagelink">
								<xsl:value-of select="node/@imageUri" />
							</xsl:variable>
							<xsl:variable name="imageResourceUri">
								<xsl:value-of select="node/@imageUri" />
							</xsl:variable>
							<a href="{$imageResourceUri}">
								<xsl:value-of select="$imagelink" />
							</a>
							<input name="{$id}--imagelink" type="hidden" value="{$imageResourceUri}" />
						</td>
						<td style="align:right">
							<xsl:variable name="isMultiplicity">
								<xsl:choose>
									<xsl:when test="node/@multiplicity[.>'1']">
										<xsl:value-of select="true()" />
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="false()" />
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<span>
								Multiplicity:
								<input type="text" name="{$id}--multiplicity--value" id="{$id}--multiplicity--value"
									size="3" value="{node/@multiplicity}" />
							</span>
						</td>
						<td style="align:right">
							<span>
								Cloud service:
								<select name="{$id}--cloudservice--value">
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
							</span>
						</td>
					</tr>
				</table>
				<xsl:call-template name="parameterMappings">
					<xsl:with-param name="nodeid" select="$id" />
				</xsl:call-template>
			</td>
			<td>
				<input class="button" type="button" onclick="removeElement('{$id}'); nodeParametersAutoCompleter.clear();" value="Remove" />
			</td>
		</tr>
	</xsl:template>

	<!-- Generate the top part of the summary. Creates a separate table. -->
	<xsl:template name="topSummary">

		<xsl:variable name="isnew">
			<xsl:call-template name="isnew" />
		</xsl:variable>

		<xsl:variable name='parentmodulename'>
			<xsl:call-template name='replace-spaces'>
				<xsl:with-param name="text"
					select="substring-before(substring-after(@resourceUri,'module/'),'/new')" />
			</xsl:call-template>
		</xsl:variable>

		<xsl:variable name='shortmodulename'>
			<xsl:call-template name='replace-spaces'>
				<xsl:with-param name="text" select="@shortModule" />
			</xsl:call-template>
		</xsl:variable>

		<div class="visiblesection" id="summarysection">
			<div class="subsection">

				<table>
					<tr>
						<th class="row">Module</th>
						<td class="values">
							<xsl:choose>
								<xsl:when test="$isnew = 'true'">
									<xsl:value-of select="$parentmodulename" />
									<xsl:text>/</xsl:text>
									<input name="parentmodulename" id="parentmodulename"
										type="hidden" value="{$parentmodulename}" />
									<input class="notfull" name="name" id="modulename" type="text" />
									<xsl:text>*</xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="@name" />
									<input name="name" id="modulename" type="hidden" value="{@name}" />
								</xsl:otherwise>
							</xsl:choose>
						</td>
					</tr>
					<xsl:if test="$isnew != 'true'">
						<tr>
							<th class="row">Version</th>
							<td class="values">
								<xsl:value-of select="@version" />
							</td>
						</tr>
					</xsl:if>

					<tr>
						<th class="row">Description</th>
						<td class="values">
							<xsl:variable name='value'>
								<xsl:value-of select="@description" />
							</xsl:variable>
							<input name="description" type="text" value="{$value}" />
						</td>
					</tr>

					<input id="commentinput" name="comment" type="hidden" value="{comment}" />

					<tr>
						<th class="row">Category</th>
						<td>
							<xsl:variable name="category">
								<xsl:call-template name="translateCategory">
									<xsl:with-param name="category" select="@category" />
								</xsl:call-template>
							</xsl:variable>
							<xsl:value-of select="$category" />
							<input name="category" type="hidden" value="{@category}" />
						</td>
					</tr>

					<xsl:if test="$isnew != 'true'">
						<tr>
							<th class="row">Created</th>
							<td class="values">
								<xsl:value-of select="@creation" />
								<input type="hidden" name="creation" value="{@creation}" />
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
					</xsl:if>
				</table>
			</div>
		</div>
	</xsl:template>

	<!-- Generate the main header, including top buttons with generic actions -->
	<xsl:template name="MainHeader">
		<xsl:param name="headerName" />

		<xsl:variable name="actions">

			<input id="savemodulebutton" type="button" value="Save" />

			<xsl:variable name="isnew">
				<xsl:call-template name="isnew" />
			</xsl:variable>

			<xsl:if test="$isnew = 'true'">
				<input type="hidden" name="new" value="true" />
			</xsl:if>

			<button onclick="_refactor();return false;">
				<xsl:if test="$isnew = 'true'">
					<xsl:attribute name="disabled">disabled</xsl:attribute>
				</xsl:if>
				<xsl:text>Refactor</xsl:text>
			</button>

			<input type="button" value="Cancel" />

			<input type="button" value="Delete">
				<xsl:if test="$isnew = 'true'">
					<xsl:attribute name="disabled">disabled</xsl:attribute>
				</xsl:if>
			</input>

		</xsl:variable>

		<div class="page_head">
			<xsl:call-template name="addPageHead">
				<xsl:with-param name="headerName" select="$headerName" />
				<xsl:with-param name="actions" select="$actions" />
			</xsl:call-template>
		</div>

	</xsl:template>

	<xsl:template name="savedialog">
		<div id="savemoduledialog" title="Save Module?">
			<p>Save comment: </p>
			<textarea id="savecomment" rows="3" />
		</div>
	</xsl:template>

</xsl:stylesheet>
