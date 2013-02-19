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
				<xsl:call-template name="head" />
				<xsl:call-template name="title">
					<xsl:with-param name="subtitle" select="'Welcome'" />
				</xsl:call-template>
			</head>

			<body>
				<div>
					<xsl:call-template name="top">
						<xsl:with-param name="crumbName" select="'Modules'" />
					</xsl:call-template>
					<xsl:if test="item">
						<h1>Marketplace</h1>
						<p>Here you will find published cloud images and pre-configured
						systems you can self-provision. These are provided by SixSq and our
						Marketplace partners. Want to deliver your images and systems
						in the Marketplace? Get in touch.</p>
	
						<xsl:call-template name="marketplace-category">
							<xsl:with-param name="category" select="'Image'" />
							<xsl:with-param name="title" select="'Images: pre-configured virtual machines'" />
						</xsl:call-template>
						<xsl:call-template name="marketplace-category">
							<xsl:with-param name="category" select="'Deployment'" />
							<xsl:with-param name="title" select="'Systems: pre-configured applications and systems'" />
						</xsl:call-template>
						<xsl:call-template name="marketplace-category">
							<xsl:with-param name="category" select="'Project'" />
							<xsl:with-param name="title" select="'Project: projects containing sets of images and deployment (i.e. systems)'" />
						</xsl:call-template>

						<h1>Want to do more? Start developing your own images and systems</h1>
					</xsl:if>

					<table>
			
						<tr>
							<td class="home-icon">
								<a href="module">
									<img src="images/common/home-start.png" alt="start" />
								</a>
							</td>
							<td class="home-text">
								<a href="module">Start</a>
							</td>
							<td class="home-text">Automation engine for the cloud. Build images or deploy applications and systems in a click!</td>
						</tr>
			
						<tr>
							<td class="home-icon">
								<a href="documentation">
									<img src="images/common/home-docs.png" alt="documentation" />
								</a>
							</td>
							<td class="home-text">
								<a href="documentation">Documentation</a>
							</td>
							<td class="home-text">Reference material to help you get the most
								from the
								system.</td>
						</tr>

						<tr>
							<td class="home-icon">
								<a href="support">
									<img src="images/common/home-support.png" alt="support" />
								</a>
							</td>
							<td class="home-text">
								<a href="support">Support</a>
							</td>
							<td class="home-text">Help when you are having problems with the system
								or
								with the documentation.</td>
						</tr>

						<tr>
							<td class="home-icon">
								<a href="http://www.sixsq.com">
									<img src="images/common/home-sixsq.png" alt="SixSq" />
								</a>
							</td>
							<td class="home-text">
								<a href="http://www.sixsq.com">SixSq</a>
							</td>
							<td class="home-text">Learn more about the company's products and
								services.
							</td>
						</tr>

					</table>

				</div>

				<div>
					<h2>SixSq's latest news on <a href="http://www.twitter.com/sixsq">Twitter</a></h2>
			
					<div class="twitters" id="sixsq">
						<p>
							Please wait while SixSq's news loads
							<img src="images/common/spinner.gif" alt="" />
						</p>
						<p>
							<a href="http://twitter.com/sixsq">If you can't wait - check out the news on Twitter</a>
						</p>
					</div>

				</div>
			<xsl:call-template name="bottom" />
			<script 
				src="https://twitterjs.googlecode.com/svn/trunk/src/twitter.min.js" 
				type="text/javascript">
			</script>
			<script
				type="text/javascript"
				charset="utf-8">
			
				jQuery(function() {
					getTwitters('sixsq', { 
						id: 'sixsq', 
						count: 10,
						enableLinks: true, 
						ignoreReplies: false, 
						template: '<span class="twitterPrefix"><span class="twitterStatus"> %text%</span><em class="twitterTime"> - <a href="http://twitter.com/%user_screen_name%/statuses/%id_str%">%time%</a></em></span>'
					});
				})

			</script>
		</body>
	</html>
	</xsl:template>

	<xsl:template name="marketplace-category">
		<xsl:param name="category" />
		<xsl:param name="title" />
		<xsl:if test="item[@category=$category]">
			<h2><xsl:value-of select="$title" /></h2>
			<table>
				<thead>
					<tr>
						<th>Name</th>
						<th>Description</th>
						<th>Version</th>
						<th>Owner</th>
					</tr>
				</thead>
				<xsl:for-each select="item[@category=$category]">
					<xsl:apply-templates select="." />
				</xsl:for-each>
			</table>
		</xsl:if>
		
	</xsl:template>

	<xsl:template match="item">
		<tr>
			<td class="values">
				<a href="{@resourceUri}">
					<xsl:value-of select="@name" />
				</a>
			</td>
			<td class="values">
				<xsl:value-of select="@description" />
			</td>
			<td class="values">
				<xsl:value-of select="@version" />
			</td>
			<td class="values">
				<xsl:value-of select="authz/@owner" />
			</td>
		</tr>
	</xsl:template>


</xsl:stylesheet>
	