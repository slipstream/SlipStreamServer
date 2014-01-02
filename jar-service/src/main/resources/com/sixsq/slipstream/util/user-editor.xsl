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

	<xsl:template match="user">

		<html>
			<head>
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="@name" />
				</xsl:call-template>

				<xsl:call-template name="head" />
			</head>

			<body>

				<div>

					<xsl:call-template name="top" />

					<xsl:variable name="headerName">
						User
					</xsl:variable>

					<xsl:variable name="isnew">
						<xsl:call-template name="isnew" />
					</xsl:variable>

					<form action="{$resourcePath}?method=put" method="post" enctype="x-www-form-urlencoded"
						accept-charset="utf-8" id="formsave">

						<xsl:variable name="actions">
							<input type="submit" value="Save" />
							<input type="button" value="Delete">
								<xsl:if test="$isnew = 'true'">
									<xsl:attribute name="disabled">true</xsl:attribute>
								</xsl:if>
							</input>
							<input type="button" value="Cancel" />
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
								<xsl:call-template name="addTab">
									<xsl:with-param name="headerName" select="'Properties'" />
									<xsl:with-param name="id" select="'parametersection'" />
								</xsl:call-template>
							</div>

							<xsl:variable name="issuper">
								<xsl:call-template name="issuper" />
							</xsl:variable>

							<div class="visiblesection" id="summarysection">
								<div class="subsection">
									<table id="summary-info">
										<tr>
											<th class="row" width="20%">Username</th>
											<td class="values">
												<xsl:if test="$isnew != 'true'">
													<xsl:value-of select="@name"/>
												</xsl:if>
												<input id="modulename" name="name">
													<xsl:choose>
														<xsl:when test="$isnew != 'true'">
															<xsl:attribute name="value">
																<xsl:value-of select="@name"/>
															</xsl:attribute>
															<xsl:attribute name="type">hidden</xsl:attribute>
														</xsl:when>
														<xsl:otherwise>
														</xsl:otherwise>
													</xsl:choose> 
												</input>
											</td>
										</tr>
										<tr>
											<th class="row">First Name</th>
											<td class="values">
												<input name="firstname" type="text" value="{@firstName}" />
											</td>
										</tr>
										<tr>
											<th class="row">Last Name</th>
											<td class="values">
												<input name="lastname" type="text" value="{@lastName}" />
											</td>
										</tr>
										<tr>
											<th class="row">Email</th>
											<td class="values">
												<input name="email" type="text" value="{@email}" />
											</td>
										</tr>
										<tr colspan="2">
											<th class="row">Change Password</th>
											<td class="values">
												<input name="password1" type="password" />
												<input name="password2" type="password" />
											</td>
										</tr>
										<tr>
											<th class="row">Old Password</th>
											<td class="values">
												<input name="oldPassword" type="password" />
											</td>
										</tr>
										<xsl:if test="user/@issuper = 'true'">
											<tr>
												<th class="row">Administrator (super user)</th>
												<td class="values">
													<input name="super" type="checkbox">
														<xsl:if test="@issuper = 'true'">
															<xsl:attribute name="checked" />
															<xsl:attribute name="value">on</xsl:attribute>
														</xsl:if>
													</input>
												</td>
											</tr>
											<tr>
												<th class="row">State</th>
												<td class="values">
													<select name="state">
														<option></option>
														<option>
															<xsl:if test="@state = 'NEW'">
																<xsl:attribute name="selected">selected</xsl:attribute>
															</xsl:if>
															<xsl:text>NEW</xsl:text>
														</option>
														<option>
															<xsl:if test="@state = 'ACTIVE'">
																<xsl:attribute name="selected">selected</xsl:attribute>
															</xsl:if>
															<xsl:text>ACTIVE</xsl:text>
														</option>
														<option>
															<xsl:if test="@state = 'DELETED'">
																<xsl:attribute name="selected">selected</xsl:attribute>
															</xsl:if>
															<xsl:text>DELETED</xsl:text>
														</option>
														<option>
															<xsl:if test="@state = 'SUSPENDED'">
																<xsl:attribute name="selected">selected</xsl:attribute>
															</xsl:if>
															<xsl:text>SUSPENDED</xsl:text>
														</option>
													</select>
												</td>
											</tr>
										</xsl:if>
									</table>
								</div>
							</div>
						</div>

						<div class="section" id="parametersection">
							<xsl:apply-templates select="parameters" mode="edit"/>
						</div>

						<div class="page_head">
							<xsl:call-template name="addPageHead">
								<xsl:with-param name="headerName" select="" />
								<xsl:with-param name="actions" select="$actions" />
							</xsl:call-template>
						</div>
						<p />

					</form>

					<xsl:call-template name="bottom" />

				</div>
			</body>
		</html>
	</xsl:template>
	
</xsl:stylesheet>
