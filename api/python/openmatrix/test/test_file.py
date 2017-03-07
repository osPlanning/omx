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
    f.create_matrix('m1', obj=ones5x5())


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
    with omx.open_file(TEST_FILE, 'w'):
        pass
    assert os.path.isfile(TEST_FILE)


@nt.with_setup(setup_func, teardown_func)
def test_open_readonly_hdf5_file():
    with tables.open_file(TEST_FILE, 'w'):
        pass

    assert os.path.isfile(TEST_FILE)

    with omx.open_file(TEST_FILE, 'r'):
        pass


@nt.with_setup(setup_func, teardown_func)
def test_set_get_del():
    with omx.open_file(TEST_FILE, 'w') as f:
        add_m1_node(f)
        npt.assert_array_equal(f['m1'], ones5x5())
        nt.assert_equal(f.shape(), (5, 5))
        del f['m1']
        nt.assert_not_in('m1', f)


@nt.with_setup(setup_func, teardown_func)
def test_add_numpy_matrix_using_brackets():
    with omx.open_file(TEST_FILE, 'w') as f:
        f['m1'] = ones5x5()
        npt.assert_array_equal(f['m1'], ones5x5())
        nt.assert_equal(f.shape(), (5, 5))

        # test check for shape matching
        with nt.assert_raises(omx.Exceptions.ShapeError):
            f.create_matrix('m2', obj=np.ones((8, 8)))


@nt.with_setup(setup_func, teardown_func)
def test_add_numpy_matrix_using_create_matrix():
    with omx.open_file(TEST_FILE, 'w') as f:
        f.create_matrix('m1', obj=ones5x5())
        npt.assert_array_equal(f['m1'], ones5x5())
        nt.assert_equal(f.shape(), (5, 5))


@nt.with_setup(setup_func, teardown_func)
@nt.raises(tables.FileModeError)
def test_add_matrix_to_readonly_file():
    with omx.open_file(TEST_FILE, 'w') as f:
        f['m2'] = np.ones((5, 5))

    with omx.open_file(TEST_FILE, 'r') as f:
        f.create_matrix('m1', obj=np.ones((5, 5)))


@nt.with_setup(setup_func, teardown_func)
@nt.raises(tables.NodeError)
def test_add_matrix_with_same_name():
    with omx.open_file(TEST_FILE, 'w') as f:
        add_m1_node(f)
        # now add m1 again:
        add_m1_node(f)


@nt.with_setup(setup_func, teardown_func)
def test_get_length_of_file():
    with omx.open_file(TEST_FILE, 'w') as f:
        f['m1'] = np.ones((5, 5))
        f['m2'] = np.ones((5, 5))
        f['m3'] = np.ones((5, 5))
        f['m4'] = np.ones((5, 5))
        f['m5'] = np.ones((5, 5))
        nt.assert_equal(len(f), 5)
        nt.assert_equal(len(f.list_matrices()), 5)


@nt.with_setup(setup_func, teardown_func)
def test_len_list_iter():
    names = ['m{}'.format(x) for x in range(5)]
    with omx.open_file(TEST_FILE, 'w') as f:
        for m in names:
            f[m] = ones5x5()

        for mat in f:
            npt.assert_array_equal(mat, ones5x5())

        nt.assert_equal(len(f), len(names))
        nt.assert_equal(f.list_matrices(), names)


@nt.with_setup(setup_func, teardown_func)
def test_contains():
    with omx.open_file(TEST_FILE, 'w') as f:
        add_m1_node(f)
        nt.assert_in('m1', f)
        # keep this here to be sure we're actually running
        # File.__contains__
        assert 'm1' in f


@nt.with_setup(setup_func, teardown_func)
def test_list_all_attrs():
    with omx.open_file(TEST_FILE, 'w') as f:
        add_m1_node(f)
        f['m2'] = ones5x5()

        nt.assert_equal(f.list_all_attributes(), [])

        f['m1'].attrs['a1'] = 'a1'
        f['m1'].attrs['a2'] = 'a2'
        f['m2'].attrs['a2'] = 'a2'
        f['m2'].attrs['a3'] = 'a3'

        nt.assert_equal(f.list_all_attributes(), ['a1', 'a2', 'a3'])


@nt.with_setup(setup_func, teardown_func)
def test_matrices_by_attr():
    with omx.open_file(TEST_FILE, 'w') as f:
        f['m1'] = ones5x5()
        f['m2'] = ones5x5()
        f['m3'] = ones5x5()

        for m in f:
            m.attrs['a1'] = 'a1'
            m.attrs['a2'] = 'a2'
        f['m3'].attrs['a2'] = 'a22'
        f['m3'].attrs['a3'] = 'a3'

        gmba = f._getMatricesByAttribute

        nt.assert_equal(gmba('zz', 'zz'), [])
        nt.assert_equal(gmba('a1', 'a1'), [f['m1'], f['m2'], f['m3']])
        nt.assert_equal(gmba('a2', 'a2'), [f['m1'], f['m2']])
        nt.assert_equal(gmba('a2', 'a22'), [f['m3']])
        nt.assert_equal(gmba('a3', 'a3'), [f['m3']])


@nt.with_setup(setup_func, teardown_func)
def test_set_with_carray():
    with omx.open_file(TEST_FILE, 'w') as f:
        f['m1'] = ones5x5()
        f['m2'] = f['m1']
        npt.assert_array_equal(f['m2'], f['m1'])

@nt.with_setup(setup_func, teardown_func)
def test_mappings():
    with omx.open_file(TEST_FILE, 'w') as f:
        taz_equivs = np.arange(1,4)
        f.create_mapping('taz', taz_equivs)

        tazs = f.mapping('taz')
        nt.assert_equal(tazs, {1:0, 2:1, 3:2})
        nt.assert_raises(LookupError, f.mapping, 'missing')
        nt.assert_raises(TypeError, f.mapping)

        entries = f.map_entries('taz')
        nt.assert_equal(entries, [1, 2, 3])
        nt.assert_raises(LookupError, f.map_entries, 'missing')
        nt.assert_raises(TypeError, f.map_entries)
