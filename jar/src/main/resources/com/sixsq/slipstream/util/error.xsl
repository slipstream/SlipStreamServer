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

	<xsl:include href="common.xsl" />

	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="error">

		<html>
			<head>
				<xsl:call-template name="head" />
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Error'" />
				</xsl:call-template>
				<script language="javascript" type="text/javascript" src="js/error.js" />
			</head>

			<body>

				<xsl:call-template name="top" />

				<div class="error">
					<div>
						<span id="errorMessage">
							Error:
							<xsl:value-of select="." />
						</span>
					</div>
					<div>
						<span>
							Code:
							<xsl:value-of select="@code" />
						</span>
					</div>
				</div>

				<xsl:call-template name="bottom" />

			</body>
		</html>

	</xsl:template>

</xsl:stylesheet>
