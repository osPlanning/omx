import os
import tempfile

import numpy as np
import numpy.testing as npt
import tables

import openmatrix as omx

import nose.tools as nt

TEST_FILE = None


def ones5x5():
    return np.ones((5, 5))


def add_m1_node(f):
    f.createMatrix('m1', obj=ones5x5())


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

    assert os.path.isfile(TEST_FILE)

    with omx.openFile(TEST_FILE, 'r'):
        pass


@nt.with_setup(setup_func, teardown_func)
def test_set_get_del():
    with omx.openFile(TEST_FILE, 'w') as f:
        add_m1_node(f)
        npt.assert_array_equal(f['m1'], ones5x5())
        assert f.shape() == (5, 5)
        del f['m1']
        assert 'm1' not in f


@nt.with_setup(setup_func, teardown_func)
def test_add_numpy_matrix_using_brackets():
    with omx.openFile(TEST_FILE, 'w') as f:
        f['m1'] = ones5x5()
        npt.assert_array_equal(f['m1'], ones5x5())
        assert f.shape() == (5, 5)

        # test check for shape matching
        with nt.assert_raises(omx.Exceptions.ShapeError):
            f.createMatrix('m2', obj=np.ones((8, 8)))


@nt.with_setup(setup_func, teardown_func)
def test_add_numpy_matrix_using_create_matrix():
    with omx.openFile(TEST_FILE, 'w') as f:
        f.createMatrix('m1', obj=ones5x5())
        npt.assert_array_equal(f['m1'], ones5x5())
        assert f.shape() == (5, 5)


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


@nt.with_setup(setup_func, teardown_func)
def test_len_list_iter():
    names = ['m{}'.format(x) for x in range(5)]
    with omx.openFile(TEST_FILE, 'w') as f:
        for m in names:
            f[m] = ones5x5()

        for mat in f:
            npt.assert_array_equal(mat, ones5x5())

        assert len(f) == len(names)
        assert f.listMatrices() == names


@nt.with_setup(setup_func, teardown_func)
def test_contains():
    with omx.openFile(TEST_FILE, 'w') as f:
        add_m1_node(f)
        assert 'm1' in f


@nt.with_setup(setup_func, teardown_func)
def test_list_all_attrs():
    with omx.openFile(TEST_FILE, 'w') as f:
        add_m1_node(f)
        f['m2'] = ones5x5()

        nt.assert_equal(f.listAllAttributes(), [])

        f['m1'].attrs['a1'] = 'a1'
        f['m1'].attrs['a2'] = 'a2'
        f['m2'].attrs['a2'] = 'a2'
        f['m2'].attrs['a3'] = 'a3'

        nt.assert_equal(f.listAllAttributes(), ['a1', 'a2', 'a3'])


@nt.with_setup(setup_func, teardown_func)
def test_matrices_by_attr():
    with omx.openFile(TEST_FILE, 'w') as f:
        f['m1'] = ones5x5()
        f['m2'] = ones5x5()
        f['m3'] = ones5x5()

        for m in f:
            m.attrs['a1'] = 'a1'
            m.attrs['a2'] = 'a2'
        f['m3'].attrs['a2'] = 'a22'
        f['m3'].attrs['a3'] = 'a3'

        gmba = f._getMatricesByAttribute

        assert gmba('zz', 'zz') == []
        assert gmba('a1', 'a1') == [f['m1'], f['m2'], f['m3']]
        assert gmba('a2', 'a2') == [f['m1'], f['m2']]
        assert gmba('a2', 'a22') == [f['m3']]
        assert gmba('a3', 'a3') == [f['m3']]


@nt.with_setup(setup_func, teardown_func)
def test_set_with_carray():
    with omx.openFile(TEST_FILE, 'w') as f:
        f['m1'] = ones5x5()
        f['m2'] = f['m1']
        npt.assert_array_equal(f['m2'], f['m1'])
