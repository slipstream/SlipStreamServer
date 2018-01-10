#!/usr/bin/env python
"""
 SlipStream Client
 =====
 Copyright (C) 2014 SixSq Sarl (sixsq.com)
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
"""
from __future__ import print_function

import sys
import hashlib
import os
from lxml import etree
import subprocess
import codecs
import re

from slipstream.command.CommandBase import CommandBase
from slipstream.HttpClient import HttpClient
import slipstream.util as util
from slipstream.exceptions.Exceptions import NotFoundError


class MainProgram(CommandBase):
    """A command-line program to migrate users for SS v3.41->3.42."""

    def __init__(self, argv=None):
        self.user = None
        self.username = None
        self.password = None
        self.cookie = None
        self.endpoint = None
        super(MainProgram, self).__init__(argv)

    def parse(self):
        usage = '''usage: %prog [options] <folder>

<folder> Directory store and get per user XML files.'''

        self.parser.usage = usage
        self.add_authentication_options()
        self.addEndpointOption()
        self.parser.add_option('--get', dest='getusers',
                               help='Get users into <folder>.',
                               default=False, action='store_true')

        self.parser.add_option('--put', dest='putusers',
                               help='Upload users from <folder> to CIMI server.',
                               default=False, action='store_true')

        self.options, self.args = self.parser.parse_args()

        if self.options.endpoint == 'https://nuv.la' and self.options.putusers:
            raise SystemExit('Not allowed against https://nuv.la')

        self._checkArgs()

    def _checkArgs(self):
        if len(self.args) == 1:
            self.folder = self.args[0]
            if self.options.putusers and not os.path.exists(self.folder):
                raise Exception('Folder %s with per user xml files should exist.' % self.folder)
            else:
                self.mk_working_dir()
        else:
            self.usageExitWrongNumberOfArguments()

    def update_attrs(self, user_dom):
        try:
            user_dom.attrib.pop('authnToken')
        except KeyError:
            pass
        params = {'firstName': 'Your first name',
                  'lastName': 'Your second name',
                  'email': 'a@b.c'}
        for k, v in params.items():
            vc = user_dom.get(k)
            if not vc or len(vc.strip()) == 0:
                user_dom.set(k, v)
        user_dom.set('password', 'tmppass')
        return etree.tostring(user_dom, encoding='utf8', method='xml')

    def hash_username(self, username):
        return hashlib.md5(username).hexdigest()

    def uname_to_fname(self, username):
        return '%s/%s.xml' % (self.folder, self.hash_username(username))

    def mk_working_dir(self):
        if not os.path.exists(self.folder):
            os.makedirs(self.folder)

    def doWork(self):

        client = HttpClient(self.options.username, self.options.password)
        client.verboseLevel = self.verboseLevel

        # get all users
        if self.options.getusers:
            self.get_users(client)
        elif self.options.putusers:
            self.put_users(client)

    def to_path(self, fn):
        return os.path.join(self.folder, fn)

    def find_users_files(self):
        return [self.to_path(f) for f in os.listdir(self.folder)
                if (os.path.isfile(self.to_path(f)) and
                    self.to_path(f).endswith('.xml'))]

    def get_user_pass(self, uname):
        cmd = ['/usr/bin/java', '-jar', '/opt/hsqldb/lib/sqltool.jar',
               '--inlineRc="url=jdbc:hsqldb:hsql://localhost:9001/slipstream,user=sa,password="',
               '--sql', '"SELECT password from USER where RESOURCEURI=\'user/%s\';"' % uname]
        cmd = ' '.join(cmd)
        return subprocess.check_output(cmd, shell=True)

    def update_user_pass(self, username, client):
        print('->>> update user pass for %s' % username)
        pswd = self.get_user_pass(username).strip()
        client.put(self.options.endpoint + '/api/user/%s' % username,
                   '{"password": "%s", "id": "user/%s"}' % (pswd, username),
                   contentType='application/json', retry=False)

    def xml_to_dom(self, xml):
        return etree.XML(xml, etree.XMLParser(strip_cdata=False))

    def put_users(self, client):
        print('Uploading users.')
        for ufn in self.find_users_files():
            print('=' * 80)
            print(':: processing file %s' % ufn)
            with codecs.open(ufn, 'r', 'utf-8') as fn:
                uxml = self.update_user_xml(fn.read())
            user_dom = self.xml_to_dom(uxml)
            url = self.options.endpoint.strip('/') + '/' + user_dom.attrib.get('resourceUri')
            print('->>> put user - ' + url)
            client.put(url, self.update_attrs(user_dom), retry=False)

            # Update password in CIMI
            self.update_user_pass(user_dom.attrib.get('name'), client)
            os.rename(ufn, ufn + '.done')

    def update_user_xml(self, uxml):
        # Rename renamed connectors.
        uxml = re.sub('nuvlabox-christiane-nusslein-volhard',
                      'nuvlabox-christiane-n-volhard',
                      uxml)
        # Remove old connectors.
        old_cs = ['ultimum-cz1', 'cyfronet-pl1', 'cyclone-fr1', 't-systems-de1', 'nuvlabox-demo']
        old_cs_dcs = old_cs + ['softlayer-it-mil01']
        udom = self.xml_to_dom(uxml)
        for e in udom.xpath("//parameters/entry/string"):
            if re.match('|'.join(old_cs), e.text):
                e.getparent().getparent().remove(e.getparent())
            elif e.text == 'General.default.cloud.service':
                for ce in e.xpath('//parameter/enumValues/string'):
                    if ce.text in old_cs_dcs:
                        ce.getparent().remove(ce)
                        clouds = ce.xpath("//entry/parameter[@name='General.default.cloud.service']/enumValues/string")
                        for val in e.xpath('//parameter/value'):
                            if re.search('|'.join(old_cs_dcs), val.text):
                                if len(clouds) > 0:
                                    val.text = clouds[0].text
                                else:
                                    val.text = ""
        return etree.tostring(udom)

    def get_users(self, client):
        print("Getting users")
        _, users = client.get(self.options.endpoint + '/users/', accept='application/xml',
                              retry=False)
        users_dom = self.xml_to_dom(users)
        for user in users_dom.findall('item'):
            username = user.attrib.get("name")
            if username:
                url = self.options.endpoint + util.USER_RESOURCE_PATH + '/' + username
                fn = self.uname_to_fname(username)
                print(':: storing user %s into %s' % (url, fn))
                try:
                    _, uxml = client.get(url)
                    with codecs.open(fn, 'w', 'utf-8') as fp:
                        fp.write(uxml)
                except NotFoundError:
                    print('WARNING: user not found - ' + username)
                    continue


if __name__ == "__main__":
    try:
        MainProgram()
    except KeyboardInterrupt:
        print('\n\nExecution interrupted by the user... goodbye!')
        sys.exit(-1)
