import os
import tempfile

import numpy as np
import tables

import openmatrix as omx

from nose.tools import *

TEST_FILE = None


def setup_func():
    global TEST_FILE

    if TEST_FILE is not None and os.path.isfile(TEST_FILE):
        os.remove(TEST_FILE)

    with tempfile.NamedTemporaryFile(suffix='.omx') as tmp:
        TEST_FILE = tmp.name


def teardown_func():
    if TEST_FILE is not None and os.path.isfile(TEST_FILE):
        os.remove(TEST_FILE)


@with_setup(setup_func, teardown_func)
def test_create_file():
    f = omx.openFile(TEST_FILE, 'w')
    f.close()
    assert os.path.isfile(TEST_FILE)


@with_setup(setup_func, teardown_func)
def test_open_readonly_hdf5_file():
    f = tables.openFile(TEST_FILE, 'w')
    f.close()
    f = omx.openFile(TEST_FILE, 'r')
    f.close()


@with_setup(setup_func, teardown_func)
def test_add_numpy_matrix_using_brackets():
    f = omx.openFile(TEST_FILE, 'w')
    f['m1'] = np.ones((5,5))
    f.close()


@with_setup(setup_func, teardown_func)
def test_add_numpy_matrix_using_create_matrix():
    f = omx.openFile(TEST_FILE, 'w')
    f.createMatrix('m1', obj=np.ones((5,5)))
    f.close()


@with_setup(setup_func, teardown_func)
@raises(tables.FileModeError)
def test_add_matrix_to_readonly_file():
    f = omx.openFile(TEST_FILE, 'w')
    f['m2'] = np.ones((5,5))
    f.close()
    f = omx.openFile(TEST_FILE, 'r')
    f.createMatrix('m1', obj=np.ones((5, 5)))
    f.close()


@with_setup(setup_func, teardown_func)
def test_add_matrix_with_same_name():
    f = omx.openFile(TEST_FILE, 'w')
    add_m1_node(f)
    # now add m1 again:
    assert_raises(tables.NodeError, add_m1_node, f)
    f.close()


@with_setup(setup_func, teardown_func)
def test_get_length_of_file():
    f = omx.openFile(TEST_FILE, 'w')
    f['m1'] = np.ones((5,5))
    f['m2'] = np.ones((5,5))
    f['m3'] = np.ones((5,5))
    f['m4'] = np.ones((5,5))
    f['m5'] = np.ones((5,5))
    assert(len(f)==5)
    assert(len(f.listMatrices())==5)
    f.close()

def add_m1_node(f):
    f.createMatrix('m1', obj=np.ones((7,7)))

