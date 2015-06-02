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

	<xsl:template match="/">
		<xsl:for-each select="node()">
			<xsl:element name="{name(.)}">
				<xsl:attribute name="name">
					<xsl:choose>
						<xsl:when test="@parentUri = 'module/'">
							<xsl:value-of select="@shortName" />
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(substring-after(@parentUri, 'module/'), '/', @shortName)" />
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				
				<xsl:attribute name="id">
					<xsl:choose>
						<xsl:when test="@parentUri = 'module/'">
							<xsl:value-of select="concat(@parentUri, @shortName, '/', @version)" />
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="concat(@parentUri, '/', @shortName, '/', @version)" />
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>

				<xsl:for-each select="@*">
					<xsl:attribute name="{name(.)}">
						<xsl:value-of select="." />
					</xsl:attribute>
				</xsl:for-each>
	
				<xsl:apply-templates select="*" />
			</xsl:element>
		</xsl:for-each>
	</xsl:template>

 	<xsl:template match="children" />

 	<xsl:template match="image" />

   	<xsl:template match="node()">
  		<xsl:copy>
			<xsl:if test="name(.) = 'cloudImageIdentifier'">
	  			<xsl:attribute name="id">
	  				<xsl:value-of select="concat(/*/@parentUri, /*/@shortName, '/', @version, '/', @cloudServiceName)" />
	  			</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates select="*|@*|text()" />
  		</xsl:copy>
  	</xsl:template>

  	<xsl:template match="@*|text()">
		<xsl:copy-of select="." />
  	</xsl:template>

</xsl:stylesheet>
