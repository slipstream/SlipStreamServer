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

	<xsl:template match="/">
		<html>
			<head>
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Reports'" />
				</xsl:call-template>

				<xsl:call-template name="head" />
				<script language="javascript" type="text/javascript" src="js/common.js" />
			</head>

			<body>
				<div>
					<xsl:for-each select="html">
						<xsl:call-template name="top">
							<xsl:with-param name="crumbName" select="'Reports'" />
						</xsl:call-template>
					</xsl:for-each>
					<xsl:variable name="runId">
						<xsl:call-template name="replace-string">
							<xsl:with-param name="text" select="substring-after(//body/h2, '/reports/')" />
							<xsl:with-param name="from" select="'quote'" />
							<xsl:with-param name="to" select="" />
						</xsl:call-template>
					</xsl:variable>
					<div class="page">
						<div class="page_head">
							<xsl:call-template name="addPageHead">
								<xsl:with-param name="headerName" select="concat('Reports for: ',$runId)" />
							</xsl:call-template>
						</div>
					</div>
					
					<xsl:apply-templates select="//body/a[position() != 1]" />

					<xsl:call-template name="bottom" />
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="a">
		<ul>
			<li>
				<xsl:copy-of select="." />
			</li>
		</ul>
	</xsl:template>

</xsl:stylesheet>
