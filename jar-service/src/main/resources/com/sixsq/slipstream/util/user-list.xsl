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

	<xsl:template match="list">
		<html>
			<head>
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Users'" />
				</xsl:call-template>

				<xsl:call-template name="head" />
				<script language="javascript" type="text/javascript" src="js/common.js" />
			</head>

			<xsl:variable name="actions">
				<form action="{$resourcePath}/new" method="get">
					<input type="submit" value="New User" />
				</form>
			</xsl:variable>

			<body>
				<div>
					<xsl:call-template name="top">
						<xsl:with-param name="crumbName" select="'Users'" />
					</xsl:call-template>

					<div class="page">
						<div class="page_head">
							<xsl:call-template name="addPageHead">
								<xsl:with-param name="headerName" select="'Users'" />
								<xsl:with-param name="actions" select="$actions" />
							</xsl:call-template>
						</div>
						<table>
							<thead>
								<tr>
									<th>Username</th>
									<th>First Name</th>
									<th>Last Name</th>
									<th>State</th>
								</tr>
							</thead>
							<xsl:apply-templates select="item"/>
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


	<xsl:template match="item">
		<xsl:variable name='position'>
			<xsl:number />
		</xsl:variable>
		<tr>
			<xsl:choose>
				<xsl:when test="$position mod 2 != 1">
					<xsl:attribute name="class">
						<xsl:value-of select="'even'" />
					</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="class">
						<xsl:value-of select="'odd'" />
					</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<td class="values">
				<a href="{@resourcePath}{@resourceUri}">
					<xsl:value-of select="@name" />
				</a>
			</td>
			<td class="values">
				<xsl:value-of select="@firstName" />
			</td>
			<td class="values">
				<xsl:value-of select="@lastName" />
			</td>
			<td class="values">
				<xsl:value-of select="@state" />
			</td>
		</tr>
	</xsl:template>

</xsl:stylesheet>
