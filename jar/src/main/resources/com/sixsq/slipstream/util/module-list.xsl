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
	<xsl:param name="chooserType" />

	<xsl:include href="common.xsl" />

	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="list">
		<html>
			<head>
				<xsl:call-template name="head" />
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Modules'" />
				</xsl:call-template>
			</head>

			<xsl:variable name="actions">
				<xsl:if test="not($chooserType)">
					<form action="{$resourcePath}/new" method="get">
						<input type="hidden" name="category" value="Project" />
						<input type="submit" value="New Project" />
					</form>
				</xsl:if>
				<xsl:if test="$chooserType">
					<form>
						<button name="CancelChooser" onclick="cancelChooser();">Cancel</button>
					</form>
				</xsl:if>
			</xsl:variable>

			<body>
				<div>
					<xsl:variable name="chooserUrlAttribute">
						<xsl:call-template name="extractChooserType" />
					</xsl:variable>
					<xsl:call-template name="top">
						<xsl:with-param name="chooserType" select="$chooserType" />
						<xsl:with-param name="crumbName" select="'Modules'" />
					</xsl:call-template>

					<div class="page">
						<div class="page_head">
							<xsl:call-template name="addPageHead">
								<xsl:with-param name="headerName" select="'Modules'" />
								<xsl:with-param name="actions" select="$actions" />
							</xsl:call-template>
						</div>
						<table>
							<thead>
								<tr>
									<th>Name</th>
									<th>Category</th>
									<th>Version</th>
								</tr>
							</thead>
							<xsl:apply-templates />
						</table>
					</div>
					<p />

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

	<xsl:template match="versionList">
		<html>
			<head>
				<xsl:call-template name="head" />
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Versions'" />
				</xsl:call-template>
			</head>

			<body>
				<div>
					<xsl:variable name="chooserUrlAttribute">
						<xsl:call-template name="extractChooserType" />
					</xsl:variable>
					<xsl:call-template name="top">
						<xsl:with-param name="chooserType" select="$chooserType" />
						<xsl:with-param name="crumbName" select="'Modules'" />
					</xsl:call-template>
					<div class="page">
						<div class="page_head">
							<xsl:call-template name="addPageHead">
								<xsl:with-param name="headerName" select="'Versions'" />
							</xsl:call-template>
						</div>
						<table>
							<thead>
								<tr>
									<th>Version</th>
									<th>Date</th>
									<th>Comment</th>
								</tr>
							</thead>
							<xsl:apply-templates mode="version">
								<xsl:sort data-type="number" select="@version" />
							</xsl:apply-templates>
						</table>
					</div>
					<p />

					<div class="page_head">
						<div class="page_head">
							<xsl:call-template name="addPageHead">
								<xsl:with-param name="headerName" select="" />
							</xsl:call-template>
						</div>
					</div>

					<xsl:call-template name="bottom" />

				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="item">
		<xsl:call-template name="item">
			<xsl:with-param name="chooserType" select="$chooserType" />
		</xsl:call-template>
	</xsl:template>

	<xsl:template match="item" mode="version">
		<xsl:variable name="chooserUrlAttribute">
			<xsl:call-template name="extractChooserType" />
		</xsl:variable>
		<tr>
			<td class="values">
				<a href="{@resourceUri}{$chooserUrlAttribute}">
					<xsl:value-of select="@version" />
				</a>
			</td>
			<td class="values">
				<xsl:value-of select="@lastModified" />
			</td>
			<td class="values">
				<xsl:value-of select="comment" />
			</td>
		</tr>
	</xsl:template>

	<xsl:template name="extractChooserType">
		<xsl:if test="$chooserType">
			<xsl:text>?chooser=</xsl:text>
			<xsl:value-of select="$chooserType" />
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
