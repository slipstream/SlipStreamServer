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

	<xsl:template match="user">

		<html>
			<head>
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="@name" />
				</xsl:call-template>

				<xsl:call-template name="head" />
				<script language="javascript" type="text/javascript" src="js/common.js" />
			</head>

			<body>

				<div>

					<xsl:call-template name="top" />

					<xsl:variable name="headerName">
						User
					</xsl:variable>

					<xsl:variable name="actions">
						<form action="{$resourcePath}" method="get">
							<input type="hidden" name="edit" value="true" />
							<input type="submit" value="Edit" />
						</form>
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
							<xsl:if test="parameters/entry">
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Properties'" />
									<xsl:with-param name="id" select="'parametersection'" />
								</xsl:call-template>
							</xsl:if>
						</div>

						<div class="visiblesection" id="summarysection">
							<div class="subsection">
								<table id="summary-info">
									<tr>
										<th class="row" width="20%">Username</th>
										<td class="values">
											<xsl:value-of select="@name" />
										</td>
									</tr>
									<tr>
										<th class="row">First Name</th>
										<td class="values">
											<xsl:value-of select="@firstName" />
										</td>
									</tr>
									<tr>
										<th class="row">Last Name</th>
										<td class="values">
											<xsl:value-of select="@lastName" />
										</td>
									</tr>
									<tr>
										<th class="row">Email</th>
										<td class="values">
											<xsl:value-of select="@email" />
										</td>
									</tr>
									<xsl:if test="@issuper = 'true'">
										<tr>
											<th class="row">Administrator (super user)</th>
											<td class="values">
												<form>
													<input type="checkbox" disabled="true">
														<xsl:if test="@issuper='true'">
															<xsl:attribute name="checked" />
														</xsl:if>
													</input>
												</form>
											</td>
										</tr>
										<tr>
											<th class="row">State</th>
											<td class="values">
												<xsl:value-of select="@state" />
											</td>
										</tr>
									</xsl:if>
								</table>
							</div>
						</div>
					</div>

					<div class="section" id="parametersection">
						<xsl:apply-templates select="parameters" />
					</div>

					<div class="page_head">
						<div class="page_head">
							<xsl:call-template name="addPageHead">
								<xsl:with-param name="headerName" select="" />
								<xsl:with-param name="actions" select="$actions" />
							</xsl:call-template>
						</div>
					</div>

					<xsl:call-template name="bottom" />

				</div>
			</body>
		</html>
	</xsl:template>

</xsl:stylesheet>
