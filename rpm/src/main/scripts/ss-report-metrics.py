#!/usr/bin/env python
import collections
import optparse
import socket
import time
import unittest
import sys

try:
    from lxml import etree
except ImportError:
    try:
        import xml.etree.cElementTree as etree
    except ImportError:
        import xml.etree.ElementTree as etree

CARBON_HOST = 'localhost'
CARBON_PORT = 2003

UNKNOWN_STATUS = 'unknown'
COLLECTED_STATUS = (
    'running',
)

METADATA_KEYS = (
    'image_id', 'name', 'index', 'type',
    'created_at', 'deleted_at', 'vmstate'
)

Metric = collections.namedtuple('Metric', [
    'name',
    'unit',
    'volume',
    'user_id',
    'run_id',
    'instance_id',
    'metadata',
    'source',
])


def can_process(vm):
    vm_status = vm.get('vmstate', None)
    if not (vm_status and vm_status.lower() in COLLECTED_STATUS):
        return False

    vm_instance = vm.get('instance_id', None)
    if not (vm_instance and vm_instance.lower() != UNKNOWN_STATUS):
        return False

    return True


def get_instances(xml_str):
    root = etree.fromstring(xml_str)
    iter_func = getattr(root, 'iter',      # Python 2.7 and above
                        root.getiterator)  # Python 2.6 compatibility
    for vm in iter_func('vm'):
        if can_process(vm):
            yield dict(vm.items())


def get_metrics(instance, collectors):
    for name, collector_func in collectors:
        yield collector_func(instance)


def _get_metadata_from_object(instance):
    return dict([(k, v) for k, v in instance.items() if k in METADATA_KEYS])


def make_metric_from_instance(instance, name, unit, volume,
                              additional_metadata={}):
    metadata = _get_metadata_from_object(instance)
    metadata.update(additional_metadata)
    return Metric(
        name=name,
        unit=unit,
        volume=volume,
        user_id=instance['user_id'],
        run_id=instance['run_id'],
        instance_id=instance['instance_id'],
        metadata=metadata,
        source=instance['cloud']
    )


def get_cpu_metric(instance, timestamp=None):
    return make_metric_from_instance(
        instance,
        name='vcpus',
        unit='vcpu',
        volume=int(instance['cpu'])
    )


def get_disk_metric(instance, timestamp=None):
    return make_metric_from_instance(
        instance,
        name='disk',
        unit='Gb',
        volume=int(instance['disk'])
    )


def get_instance_metric(instance, timestamp=None):
    return make_metric_from_instance(
        instance,
        name='instance',
        unit='instance',
        volume=1
    )


def get_memory_metric(instance, timestamp=None):
    return make_metric_from_instance(
        instance,
        name='memory',
        unit='GB',
        volume=int(instance['ram'])
    )


COLLECTORS = (
    # (name, collector function)
    ('cpu', get_cpu_metric),
    ('disk', get_disk_metric),
    ('instance', get_instance_metric),
    ('memory', get_memory_metric),
)


def collect_metrics(xml_str, collectors=COLLECTORS, base_name='slipstream'):
    metrics = collections.defaultdict(int)
    for instance in get_instances(xml_str):
        for metric in get_metrics(instance, collectors):
            # metric/user/source
            key = '{0}.{1}.{2}.{3}'.format(base_name, metric.user_id,
                                           metric.name, metric.source)
            metrics[key] += metric.volume
    return metrics


def report_metrics(metrics, host=CARBON_HOST, port=CARBON_PORT):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((host, port))
    now = int(time.time())
    buffer = "\n".join(["{0} {1} {2}".format(name, value, now)
                        for name, value in metrics.items()])
    s.sendall(buffer)


# TESTS #######################################################################

TEST_COLLECTORS = (
    # (name, collector function)
    ('cpu', get_cpu_metric),
    ('instance', get_instance_metric),
)

TEST_STATS_XML = """<?xml version="1.0" encoding="utf-8"?>
<vms>
  <vm instance_id="1f422ae5-a6b2-427a-9529-b23b944786ae" name="apache1.1" index="1"
    created_at="2013-10-09T07:38:44" deleted_at="" vmstate="running"
    run_id="791c3b221a2346668e07b5e1c19b4b75"
    image_id="2c3c8d1a-30b7-11e3-aeba-080027880ca6"
    user_id="22bb7e29-c888-45b8-aa0d-695c324c7f92" cpu="2" ram="4" type="m1.small"
    disk="100" cloud="my-cloud" />
  <vm instance_id="Unknown" name="test1.1" index="1"
    created_at="2013-10-09T07:38:44" deleted_at="" vmstate="running"
    run_id="791c3b221a2346668e07b5e1c19b4b75"
    image_id="32fb56ce-6f50-4e98-baa7-0ba2ac7d69a2"
    user_id="22bb7e29-c888-45b8-aa0d-695c324c7f92" cpu="1" ram="1" type="m1.tiniy"
    disk="10" cloud="my-other-cloud" />
  <vm instance_id="57f8f2a0-dd8e-4fa8-a844-820464575c31" name="apache1.1" index="1"
    created_at="2013-10-09T07:38:44" deleted_at=""
    run_id="791c3b221a2346668e07b5e1c19b4b75"
    image_id="2c3c8d1a-30b7-11e3-aeba-080027880ca6"
    user_id="c0d3c50f-f751-444c-b633-792794ffd041" cpu="2" ram="4" type="m1.small"
    disk="100" cloud="my-cloud" />
</vms>"""

TEST_INSTANCE = {
    'instance_id': "1f422ae5-a6b2-427a-9529-b23b944786ae",
    'name': "apache1.1",
    'index': "1",
    'created_at': "2013-10-09T07:38:44",
    'deleted_at': "",
    'vmstate': "running",
    'run_id': "791c3b221a2346668e07b5e1c19b4b75",
    'image_id': "2c3c8d1a-30b7-11e3-aeba-080027880ca6",
    'user_id': "22bb7e29-c888-45b8-aa0d-695c324c7f92",
    'cpu': "2",
    'ram': "4",
    'type': "m1.small",
    'disk': "100",
    'cloud': "my-cloud",
}

TEST_METRIC_0 = Metric(
    name='vcpus',
    unit='vcpu',
    volume=2,
    user_id="22bb7e29-c888-45b8-aa0d-695c324c7f92",
    run_id="791c3b221a2346668e07b5e1c19b4b75",
    instance_id="1f422ae5-a6b2-427a-9529-b23b944786ae",
    metadata={
        'image_id': "2c3c8d1a-30b7-11e3-aeba-080027880ca6",
        'name': "apache1.1",
        'index': "1",
        'type': "m1.small",
        'created_at': "2013-10-09T07:38:44",
        'deleted_at': "",
        'vmstate': "running",
    },
    source='my-cloud'
)

TEST_METRIC_1 = Metric(
    name='instance',
    unit='instance',
    volume=1,
    user_id="22bb7e29-c888-45b8-aa0d-695c324c7f92",
    run_id="791c3b221a2346668e07b5e1c19b4b75",
    instance_id="1f422ae5-a6b2-427a-9529-b23b944786ae",
    metadata={
        'image_id': "2c3c8d1a-30b7-11e3-aeba-080027880ca6",
        'name': "apache1.1",
        'index': "1",
        'type': "m1.small",
        'created_at': "2013-10-09T07:38:44",
        'deleted_at': "",
        'vmstate': "running",
    },
    source='my-cloud'
)


class TestMetricReporter(unittest.TestCase):

    def test_get_instances(self):
        instances = list(get_instances(TEST_STATS_XML))
        assert instances == [TEST_INSTANCE]

    def test_get_metrics(self):
        metrics = list(get_metrics(TEST_INSTANCE, TEST_COLLECTORS))
        assert metrics == [TEST_METRIC_0, TEST_METRIC_1]

    def test_collect_metrics(self):
        metrics = collect_metrics(TEST_STATS_XML, TEST_COLLECTORS)
        assert metrics == {
            'slipstream.22bb7e29-c888-45b8-aa0d-695c324c7f92.vcpus.my-cloud': 2,
            'slipstream.22bb7e29-c888-45b8-aa0d-695c324c7f92.instance.my-cloud': 1,
        }


def runtests():
    suite = unittest.TestLoader().loadTestsFromTestCase(TestMetricReporter)
    unittest.TextTestRunner().run(suite)


# MAIN ########################################################################

class Option(optparse.Option):

    ACTIONS = optparse.Option.ACTIONS + ("test",)

    def take_action(self, action, dest, opt, value, values, parser):
        if action == "test":
            runtests()
            parser.exit()
        else:
            optparse.Option.take_action(self, action, dest, opt, value, values,
                                        parser)


class OptionParser(optparse.OptionParser):
    def error(self, msg):
        """error(msg : string)

        Print a usage message incorporating 'msg' to stderr and exit.
        """
        self.exit(2, 'error: {1}\nTry "{0} --help" for more information.\n'.format(
            self.get_prog_name(), msg))


def main():
    usage = "usage: %prog [options] [file]"
    description = """Collects metrics from XML data and reports the collected
    metrics to carbon daemon. If <file> is a single dash ('-') or absent, it reads
    from the standard input. See the SlipStream /stats resource response
    for an example XML document.""".replace('    ', '')
    parser = OptionParser(usage=usage, option_class=Option,
                          description=description, add_help_option=False)
    parser.add_option('--help', action='help',
                      help="show this help message and exit")
    parser.add_option('--test', action='test',
                      help="run the test suite and exit")
    parser.add_option('-h', '--host', metavar='HOSTNAME', default=CARBON_HOST,
                      help="carbon deamon host (default: {})".format(CARBON_HOST))
    parser.add_option('-p', '--port', type='int', default=CARBON_PORT,
                      help="carbon daemon port (default: {})".format(CARBON_PORT))
    options, args = parser.parse_args()

    if not args or args[0] == "-":
        xml_str = sys.stdin.read()
    else:
        with open(args[0]) as fp:
            xml_str = fp.read()

    metrics = collect_metrics(xml_str)
    if metrics:
        report_metrics(metrics, host=options.host, port=options.port)


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        pass
    except AssertionError:
        raise  # don't mess with test errors

    except Exception as e:
        sys.stderr.write(str(e) + "\n")
        sys.exit(1)
