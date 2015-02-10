import os
import tempfile

import numpy as np
import tables

import openmatrix as omx

import nose.tools as nt

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


@nt.with_setup(setup_func, teardown_func)
def test_create_file():
    with omx.openFile(TEST_FILE, 'w'):
        pass
    assert os.path.isfile(TEST_FILE)


@nt.with_setup(setup_func, teardown_func)
def test_open_readonly_hdf5_file():
    with tables.openFile(TEST_FILE, 'w'):
        pass
    with omx.openFile(TEST_FILE, 'r'):
        pass


@nt.with_setup(setup_func, teardown_func)
def test_add_numpy_matrix_using_brackets():
    with omx.openFile(TEST_FILE, 'w') as f:
        f['m1'] = np.ones((5, 5))


@nt.with_setup(setup_func, teardown_func)
def test_add_numpy_matrix_using_create_matrix():
    with omx.openFile(TEST_FILE, 'w') as f:
        f.createMatrix('m1', obj=np.ones((5, 5)))


@nt.with_setup(setup_func, teardown_func)
@nt.raises(tables.FileModeError)
def test_add_matrix_to_readonly_file():
    with omx.openFile(TEST_FILE, 'w') as f:
        f['m2'] = np.ones((5, 5))

    with omx.openFile(TEST_FILE, 'r') as f:
        f.createMatrix('m1', obj=np.ones((5, 5)))


@nt.with_setup(setup_func, teardown_func)
@nt.raises(tables.NodeError)
def test_add_matrix_with_same_name():
    with omx.openFile(TEST_FILE, 'w') as f:
        add_m1_node(f)
        # now add m1 again:
        add_m1_node(f)


@nt.with_setup(setup_func, teardown_func)
def test_get_length_of_file():
    with omx.openFile(TEST_FILE, 'w') as f:
        f['m1'] = np.ones((5, 5))
        f['m2'] = np.ones((5, 5))
        f['m3'] = np.ones((5, 5))
        f['m4'] = np.ones((5, 5))
        f['m5'] = np.ones((5, 5))
        assert(len(f) == 5)
        assert(len(f.listMatrices()) == 5)


def add_m1_node(f):
    f.createMatrix('m1', obj=np.ones((7, 7)))
