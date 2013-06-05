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

	<xsl:param name="baseUrl" select="''" />
	<xsl:param name="version" />

	<xsl:variable name="breadcrumb-separator" select="' &#187; '" />
	<xsl:variable name="bar-separator" select="' | '" />

	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template name="head">
		<base href="{$baseUrl}" />

		<!-- jquery -->
		<script language="javascript" type="text/javascript" src="external/jquery/js/jquery.min.js" />
		<script language="javascript" type="text/javascript" src="external/jquery/js/jquery.cookie.js" />

		<!-- jquery-ui -->
		<script language="javascript" type="text/javascript" src="external/jquery-ui/js/jquery-ui.custom.min.js" />
		<link rel="stylesheet" type="text/css" href="external/jquery-ui/css/jquery-ui.css" />
		<link rel="stylesheet" type="text/css" href="external/jquery-ui/css/custom-theme/jquery-ui.custom.css" />

		<!-- ui.watermark -->
		<script language="javascript" type="text/javascript" src="external/ui.watermark/js/ui.watermark.js" />

		<!-- custom -->
		<script language="javascript" type="text/javascript" src="js/common.js" />
		<script language="javascript" type="text/javascript" src="js/actions.js" />
		<link rel="stylesheet" type="text/css" href="css/style.css" />

		<!--
			<meta http-equiv="Cache-Control" content="no-cache"/> <meta
			http-equiv="Pragma" content="no-cache"/>
		-->
		
    		<span style="display: none"> <xsl:copy-of select="/"/> </span>

	</xsl:template>

	<xsl:template name="top">
		<xsl:param name="chooserType" />
		<xsl:param name="embedded" />
		<xsl:param name="crumbName" />
				
		<xsl:if test="not($chooserType) and not($embedded)">
			<xsl:call-template name="userinfo" />

			<div id="banner" class="banner">
				<img src="images/common/banner.gif" alt="SlipStream Banner" />
			</div>
		</xsl:if>

		<xsl:call-template name="breadcrumbs">
			<xsl:with-param name="chooserType" select="$chooserType" />
			<xsl:with-param name="embedded" select="$embedded" />
			<xsl:with-param name="crumbName" select="$crumbName" />
		</xsl:call-template>

		<hr />

		<div id="messages" class="messages">
			<div id="message">
				<span />
			</div>
			<div id="error">
				<span />
			</div>
		</div>

	</xsl:template>

	<xsl:template name="breadcrumbs">

		<xsl:param name="embedded" />
		<xsl:param name="chooserType" />
		<xsl:param name="crumbName" />

		<div id="breadcrumbs" class="breadcrumbs">
			<xsl:choose>
				<xsl:when test="$embedded or $chooserType">
					<xsl:text>Home</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<a href="" >Home</a>
				</xsl:otherwise>
			</xsl:choose>

			<xsl:for-each select="breadcrumbs/crumb">

				<span>
					<xsl:value-of select="$breadcrumb-separator" />
				</span>

				<xsl:choose>
					<xsl:when test="position()!=last()">
						<a>
							<xsl:attribute name="href">
								<xsl:value-of select="//breadcrumbs/@path" />
								<xsl:value-of select="@path" />
								<xsl:if test="$chooserType">
									<xsl:text>?chooser=</xsl:text>
									<xsl:value-of select="$chooserType"/>
								</xsl:if>
							</xsl:attribute>
							<xsl:value-of select="@name" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="@name" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>

		</div>
	</xsl:template>

	<xsl:template name="users">
		<xsl:param name="separator" />

		<xsl:variable name="issuper">
			<xsl:call-template name="issuper" />
		</xsl:variable>
		<xsl:if test="$issuper = 'true'">
			<a href="user">users</a>
			<xsl:value-of select="$separator" />
		</xsl:if>

	</xsl:template>

	<xsl:template name="configuration">
		<xsl:param name="separator" />

		<xsl:variable name="issuper">
			<xsl:call-template name="issuper" />
		</xsl:variable>
		<xsl:if test="$issuper = 'true'">
			<a href="configuration">configuration</a>
			<xsl:value-of select="$separator" />
		</xsl:if>

	</xsl:template>

	<xsl:template name="issuper">

		<xsl:choose>
			<xsl:when test="/*/user/@issuper = 'true'">true</xsl:when>
			<xsl:otherwise>false</xsl:otherwise>
		</xsl:choose>

	</xsl:template>

	<xsl:template name="userinfo">
		<div id="userinfo" class="userinfo">
			<xsl:choose>
				<xsl:when test="/*/user">
					<a href="dashboard">dashboard</a>
					<xsl:value-of select="$bar-separator" />
					<a href="documentation">documentation</a>
					<xsl:value-of select="$bar-separator" />
					<xsl:call-template name="users">
						<xsl:with-param name="separator" select="$bar-separator" />
					</xsl:call-template>
					<xsl:call-template name="configuration">
						<xsl:with-param name="separator" select="$bar-separator" />
					</xsl:call-template>
					<a href="{/*/user/@resourceUri}">
						<xsl:value-of select="/*/user/@username" />
					</a>
					<xsl:text> (</xsl:text>
					<a href="logout?redirectURL={//breadcrumbs/@path}">logout</a>
					<xsl:text>)</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<a href="login/?redirectURL={//breadcrumbs/@path}">login</a>
					<xsl:value-of select="$bar-separator" />
					<a href="register">register</a>
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>

	<xsl:template name="title">
		<xsl:param name="subtitle" />
		<title>
			<xsl:text>SlipStream&#x2122;</xsl:text>
			<xsl:value-of select="$bar-separator" />
			<xsl:call-template name="replace-spaces">
				<xsl:with-param name="text" select="$subtitle" />
			</xsl:call-template>
		</title>
	</xsl:template>

	<xsl:template name="bottom">
		<div class="footer">
			<span>Powered by SlipStream&#8482;</span>
			<xsl:text> | </xsl:text>
			<span>Copyright &#x00A9; 2008-2013 SixSq S&#x00E0;rl</span>
			<xsl:text> | </xsl:text>
			<xsl:value-of select="$version" />
			<br />
			<a href="https://bit.ly/cwCMxW">
				<img alt="Swiss Made Software" src="images/common/sms-logo-small-footer.png" />
			</a>
		</div>
<!--		<script type="text/javascript">
		var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
		document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
		</script>
		<script type="text/javascript">
		try {
		var pageTracker = _gat._getTracker("UA-8411267-1");
		pageTracker._trackPageview();
		} catch(err) {}</script> -->
	</xsl:template>	
	
	<!-- Replace '+'s in a string (e.g. url) with ' ' -->
	<xsl:template name="replace-spaces">
		<xsl:param name="text" />
		<xsl:call-template name='replace-string'>
			<xsl:with-param name="text" select="$text"/>
			<xsl:with-param name="from" select="'+'" />
			<xsl:with-param name="to" select="' '" />
		</xsl:call-template>
	</xsl:template>

	<!-- Replace '_'s in a string with ' ' -->
	<xsl:template name="replace-underscores">
		<xsl:param name="text" />
		<xsl:call-template name='replace-string'>
			<xsl:with-param name="text" select="$text"/>
			<xsl:with-param name="from" select="'_'" />
			<xsl:with-param name="to" select="' '" />
		</xsl:call-template>
	</xsl:template>

	<!-- Replace '.'s in a string with ' ' -->
	<xsl:template name="replace-dots">
		<xsl:param name="text" />
		<xsl:call-template name='replace-string'>
			<xsl:with-param name="text" select="$text"/>
			<xsl:with-param name="from" select="'.'" />
			<xsl:with-param name="to" select="' '" />
		</xsl:call-template>
	</xsl:template>

	<!-- reusable replace-string function from cookbook ("Search and Replace")-->
	<xsl:template name="replace-string">
		<xsl:param name="text" />
		<xsl:param name="from" />
		<xsl:param name="to" />
		<xsl:choose>
			<xsl:when test="contains($text, $from)">

				<xsl:variable name="before"
					select="substring-before($text, $from)" />
				<xsl:variable name="after"
					select="substring-after($text, $from)" />
				<xsl:variable name="prefix"
					select="concat($before, $to)" />

				<xsl:value-of select="$before" />
				<xsl:value-of select="$to" />
				<xsl:call-template name="replace-string">
					<xsl:with-param name="text" select="$after" />
					<xsl:with-param name="from" select="$from" />
					<xsl:with-param name="to" select="$to" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="setTRClassAttribute">
		<xsl:variable name='position'>
			<xsl:number />
		</xsl:variable><!--
		<xsl:choose>
			<xsl:when test="$position mod 2 != 1">
				<xsl:attribute name="class">even</xsl:attribute>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="class">odd</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
	--></xsl:template>
	
	<!-- Attempts to translate category and object types into more meaningful names
		 for the UI -->
	<xsl:template name="translateCategory">
		<xsl:param name="category" />
		<xsl:choose>
			<xsl:when test="$category='Image'">
				<span><xsl:text>Machine Image</xsl:text></span>
			</xsl:when>
			<xsl:when test="$category='BlockStore'">
				<span><xsl:text>Disk Image</xsl:text></span>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$category" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="addPageHead">
		<xsl:param name="headerName" />
		<xsl:param name="id" />
		<xsl:param name="toggle" select="'false'" />
		<xsl:param name="isExpanded" select="'true'" />
		<xsl:param name="actions" select="" />
		<xsl:param name="isVisible" select="'true'" />

		<div class="page_head">
			<h1>
				<xsl:value-of select="$headerName" />
				<span class="actions">
				<!-- copy the content of the variable, to force its display 
					 (value-of won't work) -->
				<xsl:copy-of select="$actions" />
			</span>
			</h1>
		</div>
		
	</xsl:template>

	<xsl:template name="togglableSection">
		<xsl:param name="headerName" />
		<xsl:param name="id" />
		<xsl:param name="toggle" select="'false'" />
		<xsl:param name="isExpanded" select="'true'" />
		<xsl:param name="isVisible" select="'true'" />
		<span>
			<xsl:if test="$isVisible = 'true'">
				<xsl:attribute name="class">sectionheadtitle</xsl:attribute>					
			</xsl:if>
			<xsl:if test="$isVisible != 'true'">
				<xsl:attribute name="class">sectionheadtitleinactive</xsl:attribute>					
			</xsl:if>
			<xsl:value-of select="$headerName" />
		</span>
		<xsl:if test="$toggle = 'true'">
			<span class="toggle">
				<xsl:if test="$isExpanded = 'true'">
					<img id="toggle-{$id}" src="images/common/reduce" />
				</xsl:if>
				<xsl:if test="$isExpanded != 'true'">
					<img id="toggle-{$id}" src="images/common/expand" />
				</xsl:if>
			</span>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="addSectionHead">
		<xsl:param name="headerName" />
		<xsl:param name="id" />
		<xsl:param name="toggle" select="'false'" />
		<xsl:param name="isExpanded" select="'true'" />
		<xsl:param name="isVisible" select="'true'" />

		<div class="subsection">
			<div class="subsection_head" onclick="toggleVisibility('{$id}','toggle-{$id}')" >
				
				<xsl:call-template name="togglableSection">
					<xsl:with-param name="headerName" select="$headerName"/>
					<xsl:with-param name="id" select="$id"/>
					<xsl:with-param name="toggle" select="$toggle" />
					<xsl:with-param name="isExpanded" select="$isExpanded" />
					<xsl:with-param name="isVisible" select="$isVisible" />
				</xsl:call-template>
				
			</div>
		</div>
		
	</xsl:template>
		
	<xsl:template name="addTab">
		<xsl:param name="headerName" />
		<xsl:param name="id" />
		<xsl:param name="isVisible" select="'false'" />

		<div id="{$id}Tab">
			<xsl:if test="$isVisible = 'true'">
				<xsl:attribute name="class">tab_head</xsl:attribute>					
			</xsl:if>
			<xsl:if test="$isVisible != 'true'">
				<xsl:attribute name="class">tab_head_inactive</xsl:attribute>					
			</xsl:if>
			<a href="javascript:activateTab('{$id}');">
				<xsl:value-of select="$headerName" />
			</a> 
		</div>
		
	</xsl:template>
	

	<!-- ********** -->
	<!-- PARAMETERS -->
	<!-- ********** -->

	<xsl:template match="parameters" mode="edit">
		<xsl:for-each select="entry/parameter/@category[not(.=preceding::entry/parameter/@category)]">
			<xsl:sort select="." /> 
	  		<xsl:call-template name="addParameterSubsection">
				<xsl:with-param name="category" select="." />
				<xsl:with-param name="mode" select="'edit'" />
			</xsl:call-template>
		</xsl:for-each>
	</xsl:template>

	<xsl:template match="parameters">
		<xsl:for-each select="entry/parameter/@category[not(.=preceding::entry/parameter/@category)][not(../@type='Dummy')]">
			<xsl:sort select="." /> 
	  		<xsl:call-template name="addParameterSubsection">
				<xsl:with-param name="category" select="." />
				<xsl:with-param name="mode" select="'view'" />
			</xsl:call-template>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="addParameterSubsection">
		<xsl:param name="category" />
		<xsl:param name="mode" />
		<div class="subsection">
			<div class="subsection_head">
				<span>
					<xsl:call-template name="replace-underscores">
						<xsl:with-param name="text" select="$category" />
					</xsl:call-template>
				</span>
			</div>
			<table id="parameter-{$category}">
				<col width="30%" />
				<col width="30%" />
				<col width="40%" />
				<thead>
					<tr>
						<th>Name</th>
						<th>Description</th>
						<th>Value</th>
						<xsl:if test="$mode = 'edit'">
							<th></th> <!-- Will hold instructions column -->
						</xsl:if>
					</tr>
				</thead>
				<xsl:for-each select="//parameter[@category=$category][@type!='Dummy']">
					<xsl:sort select="@name" />
					<xsl:choose>
						<xsl:when test="$mode = 'edit'">
							<xsl:apply-templates select="." mode="edit" />
						</xsl:when>
						<xsl:otherwise>
						  <xsl:choose>
    						<xsl:when test = "(value = '' or not(value)) and $category = 'Cloud'">
    						  <!-- Don't display anything. -->
  						  </xsl:when>
  						  <xsl:otherwise>
							     <xsl:apply-templates select="." />
							   </xsl:otherwise>
	  					</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:for-each>
			</table>
			<xsl:if test="($mode = 'edit') and ($category = 'Input' or $category = 'Output')">
				<xsl:call-template name="addParameterButton">
					<xsl:with-param name="LabelName">Add Parameter</xsl:with-param>
					<xsl:with-param name="tbodyId">
						<xsl:text>parameter-</xsl:text>
						<xsl:value-of select="$category" />
					</xsl:with-param>
				</xsl:call-template>
			</xsl:if>
		</div>
	</xsl:template>

	<xsl:template name="addParameterButton">
		<xsl:param name="LabelName" />
		<xsl:param name="tbodyId" />
		<input type="button" onclick="addParameter('{$tbodyId}');" value="{$LabelName}" />
	</xsl:template>

	<xsl:template match="parameter">
		<tr>
			<td>
				<xsl:call-template name="replace-underscores">
					<xsl:with-param name="text" select="@name" />
				</xsl:call-template>
			</td>
			<td>
				<xsl:value-of select="@description" />
			</td>
			<td>
				<xsl:choose>
					<xsl:when test="@type = 'Boolean'">
						<input type="checkbox" disabled="true">
							<xsl:if test="value = 'on'">
								<xsl:attribute name="checked">true</xsl:attribute>
							</xsl:if>
						</input>
					</xsl:when>
					<xsl:when test="@type = 'Text' or @type = 'RestrictedText'">
						<textarea disabled="true">
							<xsl:value-of select="value" />
						</textarea>
					</xsl:when>
					<xsl:when test="@type = 'Password'">&#9679;&#9679;&#9679;&#9679;&#9679;&#9679;&#9679;&#9679;</xsl:when>
					<xsl:otherwise>
						<xsl:choose>
							<xsl:when test="@isSet = 'true' or not(@isSet)">
								<xsl:value-of select="value" />
							</xsl:when>
							<xsl:otherwise>
								<xsl:attribute name="class">inherited</xsl:attribute>
								<xsl:value-of select="defaultValue" />
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</td>
		</tr>
	</xsl:template>

	<xsl:template match="parameter" mode="edit">
		<xsl:variable name='position'>
			<xsl:number level="any"/>
		</xsl:variable>
		<!--
			set the id to the parent (input or output Parameters) + the position,
			such that it's unique
		-->
		<xsl:variable name="id">
			<xsl:value-of select="name()" />
			<xsl:text>-</xsl:text>
			<xsl:value-of select="@category" />
			<xsl:text>--</xsl:text>
			<xsl:value-of select="$position" />
		</xsl:variable>
		<xsl:variable name="hasInstructions">
			<xsl:choose>
				<xsl:when test="instructions[. != '']">
					<xsl:value-of select="true()"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="false()"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<tr id="{$id}">
			<td>
				<xsl:if test="@mandatory = 'true'">
					<xsl:call-template name="replace-dots">
						<xsl:with-param name="text" select="@name" />
					</xsl:call-template>
				</xsl:if>
				<input type="text" name="{$id}--name" value="{@name}">
					<xsl:if test="@mandatory = 'true'">
						<xsl:attribute name="type">hidden</xsl:attribute>
					</xsl:if>
				</input>
				<input type="hidden" name="{$id}--category" value="{@category}" />
				<input type="hidden" name="{$id}--type" value="{@type}" />
			</td>
			<td>
				<xsl:choose>
					<xsl:when test="@mandatory = 'true' or @type = 'RestrictedText'">
						<xsl:value-of select="@description" />
						<input type="hidden" name="{$id}--description" value="{@description}" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:choose>
							<xsl:when test="@type = 'Text'">
								<textarea cols="80" rows="20" name="{$id}--description">
									<xsl:value-of select="@name" />
								</textarea>
							</xsl:when>
							<xsl:otherwise>
								<input type="text" name="{$id}--description" value="{@description}" />
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<td>
				<xsl:choose>
					<xsl:when test="@type = 'Text' or @type = 'RestrictedText'">
						<textarea cols="80" rows="20" name="{$id}--value">
							<xsl:value-of select="value" />
						</textarea>
					</xsl:when>
					<xsl:otherwise>
						<xsl:choose>
							<xsl:when test="@type = 'Enum'">
								<select name="{$id}--value">
									<xsl:for-each select="enumValues/string">
										<option>
											<xsl:if test="../../value = .">
												<xsl:attribute name="selected"/>
											</xsl:if>
											<xsl:value-of select=".">
											</xsl:value-of>
										</option>
									</xsl:for-each>
								</select>
							</xsl:when>
							<xsl:otherwise>
								<input name="{$id}--value">
									<xsl:choose>
										<xsl:when test="@type = 'Boolean'">
											<xsl:attribute name="type">checkbox</xsl:attribute>
											<xsl:if test="value">
												<xsl:attribute name="checked">true</xsl:attribute>
											</xsl:if>
										</xsl:when>
										<xsl:when test="@type = 'Password'">
											<xsl:attribute name="type">password</xsl:attribute>
											<xsl:attribute name="value">
												<xsl:value-of select="value" />
											</xsl:attribute>
										</xsl:when>
										<xsl:when test="@readonly = 'true'">
											<xsl:attribute name="disabled">disabled</xsl:attribute>
											<xsl:attribute name="value">
												<xsl:value-of select="value" />
											</xsl:attribute>
										</xsl:when>
										<xsl:otherwise>
											<xsl:attribute name="value">
 												<xsl:if test="@isSet = 'true' or not(@isSet)">
 													<xsl:value-of select="value" />
 												</xsl:if>
 											</xsl:attribute>
										</xsl:otherwise>
									</xsl:choose>
									<xsl:attribute name="placeholder">
										<xsl:value-of select="defaultValue" />
									</xsl:attribute>
								</input>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</td>
			<xsl:if test="@mandatory != 'true'">
				<td>
					<input type="button" name="remove" value="Remove"
						onClick="removeElement('{$id}')" />
				</td>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="$hasInstructions = 'true'">
					<td>
						<a class="instructions" href='javascript:void(0)'>
							<img src="images/common/info.png" alt="information" />
						</a>
					</td>
				</xsl:when>
				<xsl:otherwise>
					<td/>
				</xsl:otherwise>
			</xsl:choose>
		</tr>
		<xsl:if test="$hasInstructions = 'true'">
			<tr class="instructions">
				<td colspan="4">
					<xsl:value-of select="instructions" />
				</td>
			</tr>
		</xsl:if>
	</xsl:template>	
	
	<xsl:template match="nodeParameter">
		<tr>	
			<td width="30%">
				<xsl:value-of select="@name" />
			</td>
			<td width="40%">
				<xsl:value-of select="@description" />
			</td>
			<td width="30%">
				<xsl:value-of select="value" />
			</td>
		</tr>
	</xsl:template>
	
	<xsl:template name="isnew">
		<xsl:choose>
			<xsl:when test="substring-before(@name, '/new') or (@name = 'new')">
				<xsl:value-of select="'true'" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="'false'" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="modulename">
		<xsl:call-template name='replace-spaces'>
			<xsl:with-param name="text">
				<xsl:with-param name='name' select="@moduleName" />
			</xsl:with-param>
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="item">
		<xsl:param name="chooserType" />

		<xsl:variable name="chooserUrlAttribute">
			<xsl:if test="$chooserType">
				<xsl:text>?chooser=</xsl:text>
				<xsl:value-of select="$chooserType" />
			</xsl:if>
		</xsl:variable>

		<tr>
			<td class="values">
				<a href="{@resourceUri}{$chooserUrlAttribute}">
					<xsl:value-of select="@name" />
				</a>
			</td>
			<td class="values">
				<xsl:value-of select="@category" />
			</td>
			<td class="values">
				<xsl:value-of select="@customVersion" />
			</td>
		</tr>
	</xsl:template>

	<xsl:template name="module-resource-to-name">
		<xsl:param name="name" />
		<xsl:call-template name="replace-string">
			<xsl:with-param name="text" select="$name"/>
			<xsl:with-param name="from" select="'module/'" />
			<xsl:with-param name="to" select="''" />
		</xsl:call-template>
	</xsl:template>


	<xsl:template name="platform">
		<xsl:param name="disabled" select="false" />
	    <select name="platform" id="platform">
			<xsl:choose>
				<xsl:when test="not(@isBase = 'true')">
		        	<xsl:attribute name="disabled">true</xsl:attribute>
				</xsl:when>
				<xsl:when test="$disabled = 'true'">
		        	<xsl:attribute name="disabled">true</xsl:attribute>
				</xsl:when>
			</xsl:choose>
			<option value='centos'>
				<xsl:if test="@platform = 'centos'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
			<xsl:text>Cent OS</xsl:text>
			</option>
			<option value='debian'>
				<xsl:if test="@platform = 'debian'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:text>Debian</xsl:text>
			</option>
			<option value='fedora'>
				<xsl:if test="@platform = 'fedora'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:text>Fedora</xsl:text>
			</option>
			<!--
			<option value='gentoo'>
			  <xsl:if test="@platform = 'gentoo'">
			    <xsl:attribute name="selected">selected</xsl:attribute>
			  </xsl:if>
			  <xsl:text>Gentoo</xsl:text>
			</option>
			<option value='opensolaris'>
			  <xsl:if test="@platform = 'opensolaris'">
			    <xsl:attribute name="selected">selected</xsl:attribute>
			  </xsl:if>
			  <xsl:text>OpenSolaris</xsl:text>
			</option>
			-->
			<option value='opensuse'>
				<xsl:if test="@platform = 'opensuse'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:text>openSuSE</xsl:text>
			</option>
			<option value='redhat'>
				<xsl:if test="@platform = 'redhat'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:text>Red Hat</xsl:text>
			</option>
			<option value='suselinux'>
				<xsl:if test="@platform = 'suselinux'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:text>SuSE Linux</xsl:text>
			</option>
			<option value='ubuntu'>
				<xsl:if test="@platform = 'ubuntu'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:text>Ubuntu</xsl:text>
			</option>
		<!--
			<option value='windows'>
				<xsl:if test="@platform = 'windows'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:text>Windows</xsl:text>
			</option>
		-->
			<option value='other'>
				<xsl:if test="@platform = 'other'">
					<xsl:attribute name="selected">selected</xsl:attribute>
				</xsl:if>
				<xsl:text>Other</xsl:text>
			</option>
		</select>
	</xsl:template>
  
  	<xsl:template name="waitingMessageOverlay">
  		<xsl:param name="message" select="'Starting VMs, please wait...'"/>
		<div id="submitMessage" class="ui-overlay hidden">
			<div class="ui-widget-overlay">
			</div>
			<div class="ui-widget-shadow ui-corner-all starting-overlay-shadow" >
			</div>
			<div class="ui-widget ui-widget-content ui-corner-all starting-overlay">
				<div class="ui-dialog-content ui-widget-content center">
					<p/>
					<img src="images/common/spinner.gif" alt="spinner"></img>
					<p><xsl:value-of select="$message" /></p>
				</div>
			</div>
		</div>
	</xsl:template>
  
</xsl:stylesheet>
