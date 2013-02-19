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

	<xsl:template match="list">
		<html>
			<head>
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Runs'" />
				</xsl:call-template>

				<xsl:call-template name="head" />
				<script language="javascript" type="text/javascript" src="js/common.js" />
			</head>

			<body>
				<div>
					<xsl:if test="not($isembedded)">
						<xsl:call-template name="top">
							<xsl:with-param name="crumbName" select="'Runs'" />
							<xsl:with-param name="isembedded" select="$isembedded" />
						</xsl:call-template>
					</xsl:if>
					<div class="page">
						<xsl:if test="not($isembedded)">
							<div class="page_head">
								<xsl:call-template name="addPageHead">
									<xsl:with-param name="headerName" select="'Runs'" />
								</xsl:call-template>
							</div>
						</xsl:if>
						<table>
							<thead>
								<tr>
									<th>UUID</th>
									<th>Module</th>
									<th>Status</th>
									<th>Start time</th>
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
							</xsl:call-template>
						</div>
					</div>

					<xsl:if test="not($isembedded)">
						<xsl:call-template name="bottom" />
					</xsl:if>
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
				<a href="{@resourceUri}" target="_parent">
					<xsl:value-of select="@uuid" />
				</a>
			</td>
			<td class="values">
				<a href="{@moduleResourceUri}" target="_parent">
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
