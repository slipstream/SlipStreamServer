#!/bin/env python
"""
 SlipStream Client
 =====
 Copyright (C) 2018 SixSq Sarl (sixsq.com)
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

import os
#import sys
import json
import codecs
import logging
import argparse

from slipstream.api import Api
from slipstream.api.api import _mod_url


default_endpoint = os.environ.get('SLIPSTREAM_ENDPOINT') or 'https://nuv.la'


def parse_arguments():
    parser = argparse.ArgumentParser(description='Convert SlipStream modules into CIMI resources')

    parser.add_argument('--ss-endpoint', dest='endpoint', metavar='URL',
                        help='SlipStream server endpoint. Default: {}'.format(default_endpoint),
                        default=default_endpoint)

    parser.add_argument('--ss-username', dest='username',
                        help='Cloud username', metavar='USERNAME',
                        default=os.environ.get('SLIPSTREAM_USERNAME', ''))

    parser.add_argument('--ss-password', dest='password',
                        help='Cloud password', metavar='PASSWORD',
                        default=os.environ.get('__SLIPSTREAM_PASSWORD', ''))

    parser.add_argument('--ss-insecure', dest='insecure', action='store_true', default=False,
                        help='Do not check server certificate')

    parser.add_argument('--recurse', dest='recurse', action='store_true', default=False,
                        help='Recurse into sub-modules (only with source slipstream)')

    parser.add_argument('--all-versions', dest='versions', action='store_true', default=False,
                        help='Migrate all versions of module (only with source slipstream)')

    parser.add_argument('-v', '--verbose', dest='verbose_level',
                        help='Verbose level. Add more to get more details.',
                        action='count', default=0)

    #    parser.add_argument('--source', dest='source', choices=['files', 'slipstream'],
    #                        help='Where to take the source module(s). Default: slipstream'.format(default_endpoint),
    #                        default='slipstream')

    parser.add_argument('path', nargs='*',
                        help='Path of a module on SlipStream or on the file system')

    return parser.parse_args()


def patch_api():
    def _json_get(self, url, **params):
        print('Get {}'.format(url))
        response = self.session.get('%s%s' % (self.endpoint, url),
                                    headers={'Accept': 'application/json'},
                                    params=params)
        response.raise_for_status()
        return response.json()

    def get_module(self, path='/'):
        root_paths = ['', '/', '/module', '/module/']
        url = _mod_url(path) if (path and path not in root_paths) else '/module'
        module = self._json_get(url)
        return module[module.keys()[0]]

    def get_module_versions(self, path):
        url =_mod_url(path) + '/'
        module_versions = self._json_get(url)
        return module_versions

    Api._json_get = _json_get
    Api.get_module = get_module
    Api.get_module_versions = get_module_versions


def get_api(config):
    patch_api()

    api = Api(endpoint=config.endpoint, insecure=config.insecure)
    api.login_internal(config.username, config.password)

    return api


def get_modules(config):
    #    if config.source == 'file':
    #        return get_modules_from_files(config)
    #    else:
    return get_modules_from_slipstream(config)


def get_modules_from_files(config):
    for path in config.path:
        with open(path, 'r') as f:
            module = json.load(f)
            if len(module) == 1:
                module = module[module.keys()[0]]
            yield module


def get_modules_from_slipstream(config):
    api = get_api(config)

    paths = config.path if config.path else _get_root_modules(api)

    return _get_modules_from_slipstream(api, paths, config.recurse, config.versions)


def _get_root_modules(api):
    root_module = api.get_module()
    return [module['name'] for module in root_module['item']]


def _to_str(x):
    if x is None:
        return None
    try:
        return str(x)
    except UnicodeEncodeError as e:
        print('warning: character encoding error: {}'.format(e))
        return str(codecs.encode(x, 'utf-8', 'ignore'))


def _to_int(x):
    try:
        return int(x)
    except (TypeError, ValueError):
        return None


def _to_float(x):
    try:
        return float(x)
    except (TypeError, ValueError):
        return None

def _get_dict(x):
    if not x:
        return {}
    return x


def _get_list(x):
    if not x:
        return []
    return x if isinstance(x, list) else [x]


def _get_modules_from_slipstream(api, paths, recurse=False, get_versions=False):
    for path in paths:
        try:
            module = api.get_module(path)
        except Exception as e:
            print('Failed to get module "{}": {}'.format(path, e))
            continue

        if get_versions:
            #import code; code.interact(local=locals())
            module['versions'] = []
            versions = _get_list(_get_dict(api.get_module_versions(path).get('versionList', {})).get('item', []))
            for version in versions:
                path_version = '{}/{}'.format(path, version['version'])
                try:
                    module['versions'].append(api.get_module(path_version))
                except Exception as e:
                    print('Failed to get module version "{}": {}'.format(path_version, e))
                    continue
        else:
            module['versions'] = [module]

        yield module

        if recurse and module['category'] == 'Project' and module.get('children'):
            try:
                children = _get_list(_get_dict(module.get('children', {})).get('item', []))
                children_paths = ['{}/{}'.format(path, child['name']) for child in children]
            except Exception as e:
                print('Failed to get children of "{}": {}'.format(path, e))
                import code; code.interact(local=locals())
                raise

            for m in _get_modules_from_slipstream(api, children_paths, recurse, get_versions):
                yield m


def get_module_path(module):
    return '{}/{}'.format(module.get('parentUri', ''), module['shortName'])


def convert_module(module):
    return module


def convert_date(date):
    d = date.split(' ')
    return '{}T{}Z'.format(d[0], d[1])


def category_to_type(category):
    return {'Project': 'project',
            'Image': 'component',
            'Deployment': 'application'}.get(category)


def get_cimi_acl(module):
    owner = module.get('authz', {}).get('owner')

    type = 'USER' if owner else 'ROLE'
    principal = owner if owner else 'ADMIN'

    return {'owner': {
        'type': type,
        'principal': principal},
        'rules': [
            {
                'right': 'ALL',
                'type': 'ROLE',
                'principal': 'ADMIN'
            }, {
                'right': 'VIEW',
                'type': type,
                'principal': principal
            }]
    }


def get_path(module):
    name = module.get('shortName')
    parent = module.get('parentUri','')[7:]
    return parent + '/' + name if parent else name


def get_cimi_module_common_attributes(module):
    name = module['shortName']
    parent = module['parentUri'][7:]

    return {'name': name,
            'path': get_path(module),
            'parent': parent,
            'description': module.get('description', ''),
            'logo': module.get('logoLink'),
            'type': category_to_type(module['category']),
            'created': convert_date(module['creation']),
            'updated': convert_date(module['lastModified']),
            'acl': get_cimi_acl(module)}


def get_cimi_module(module, versions_hrefs):
    cimi_module = get_cimi_module_common_attributes(module)
    cimi_module.update(
        {'versions': versions_hrefs}
    )
    return cimi_module


def get_cloud_image_ids(version):
    ids = _get_list(_get_dict(version.get('cloudImageIdentifiers', {})).get('cloudImageIdentifier', []))

    if ids:
        return {i['cloudImageIdentifier']: i['cloudServiceName'] for i in ids}
    return {}


def split_parameters(version):
    version_parameters = _get_list(_get_dict(version.get('parameters', {})).get('entry'))
    resources_requirements = {}
    cloud_parameters = {}
    parameters = {'input': [], 'output': []}

    if version_parameters:
        for p in version_parameters:
            param = p['parameter']
            name = param['name']
            value = param.get('value')
            category = param.get('category')
            description = param.get('description')

            if category == 'Cloud':
                resources_requirements[name] = value

            elif category in ['Input', 'Output']:
                parameters.setdefault(category.lower(), []).append({
                    'name': name,
                    'description': description,
                    'value': _to_str(value)
                })

            elif not category:
                print('warning: parameter category null or empty for "{}": {}. Skipping this parameter'.format(get_path(version), param))

            else:
                cloud_parameters.setdefault(category, []).append({
                    'name': name,
                    'description': description,
                    'type': param['type'],
                    'value': _to_str(value)
                })

    return resources_requirements, cloud_parameters, parameters


def get_targets_by_name(version):
    targets = _get_list(_get_dict(version.get('targets', {})).get('target', []))
    return {t['name']: t['content'] for t in targets if 'content' in t}


def get_packages(version):
    packages = _get_list(_get_dict(version.get('packages', {})).get('package', []))
    return [p['name'] for p in packages]


def get_cimi_version_for_component(version):
    res_req, cloud_params, params = split_parameters(version)
    targets = get_targets_by_name(version)
    packages = get_packages(version)

    return {
        'native': version.get('isBase'),
        'parent_component': version.get('parentUri'),
        'cloud_image_ids': get_cloud_image_ids(version),
        'operating_system': {
            'platform': version.get('platform'),
            'username': version.get('loginUser')
        },
        'resources_requirements': {
            'cpu': _to_int(res_req.get('cpu.nb')),
            'ram': _to_float(res_req.get('ram.GB')),
            'disk': _to_float(res_req.get('disk.GB')),
            'extra_disk': _to_float(res_req.get('extra.disk.volatile')),
            'network': _to_str(res_req.get('network', 'Public'))
        },
        'cloud_parameters': cloud_params,
        'parameters': params,
        'workflow': {
            'prerecipe': version.get('prerecipe'),
            'packages': packages,
            'recipe': version.get('recipe'),
            'execute': targets.get('execute'),
            'report': targets.get('report'),
            'onvmadd': targets.get('onvmadd'),
            'onvmremove': targets.get('onvmremove'),
            'prescale': targets.get('prescale'),
            'postscale': targets.get('postscale')
        }

    }


def get_mappings(node):
    cimi_mappings = []
    mappings = _get_list(_get_dict(node.get('parameterMappings', {})).get('entry', []))

    for mapping in mappings:
        parameter = mapping['parameter']
        is_map = parameter['isMappedValue']
        value = _to_str(parameter['value'])

        cimi_mappings.append({
            'parameter_name': parameter['name'],
            'map': value if is_map else None,
            'value': value.strip('"'"'") if not is_map else None
        })

    return cimi_mappings


def get_cimi_version_for_application(version):
    cimi_nodes = []
    nodes = _get_list(_get_dict(version.get('nodes', {})).get('entry', []))

    for n in nodes:
        node = n['node']
        cimi_nodes.append({
            'name': node['name'],
            'component': {'href': node['imageUri'][7:]},
            'cloud': {'href': node.get('cloudService')},
            'multiplicity': _to_int(node.get('multiplicity')),
            'max_provisioning_failures': _to_int(node.get('maxProvisioningFailures')),
            'mappings': get_mappings(node)
        })

    return {
        'nodes': cimi_nodes
    }


def get_cimi_version_for_type(version_type, version):
    if version_type == 'component':
        return get_cimi_version_for_component(version)
    elif version_type == 'application':
        return get_cimi_version_for_application(version)
    return {}


def get_cimi_version(version):
    cimi_version = get_cimi_module_common_attributes(version)

    commit = version.get('commit', {})
    cimi_version.update(
        {'commit': {
            'author': commit.get('author'),
            'message': commit.get('comment')
        }}

    )

    cimi_version.update(get_cimi_version_for_type(cimi_version['type'], version))

    return cimi_version


def cimi_add(api, resource_type, element):
    try:
        return api.cimi_add(resource_type, element)
    except Exception as e:
        print('Failed to upload "{}" to CIMI resource "{}": {}'.format(element.get('name'), resource_type, e))
        return None
        #import code; code.interact(local=locals())

def upload_module(api, module):
    versions_hrefs = []


    for version in module['versions']:
        cimi_resp = cimi_add(api, 'versions', get_cimi_version(version))
        if cimi_resp is not None:
            version_href = {'href': cimi_resp.json['resource-id']}
            versions_hrefs.append(version_href)

    if versions_hrefs:
        cimi_add(api, 'modules', get_cimi_module(module, versions_hrefs))
    else:
        print('No version for module "{}". Skipping'.format(get_path(module)))


def convert_and_upload_modules(config, modules):
    api = get_api(config)

    for module in modules:
        print('{} with {} versions'.format(get_module_path(module), len(module.get('versions', []))))
        module = convert_module(module)
        upload_module(api, module)


def main():
    logging.basicConfig(level=logging.INFO)

    requests_log = logging.getLogger("requests")
    requests_log.setLevel(logging.INFO)
    requests_log.propagate = True

    config = parse_arguments()
    modules = get_modules(config)
    convert_and_upload_modules(config, modules)


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print('\nExecution interrupted by the user.')
