from __future__ import print_function

import tables
import numpy as np

from .File import *
from .Exceptions import *

# GLOBAL VARIABLES -----------
__version__ = '0.3.3'
__omx_version__ = '0.2'

# GLOBAL FUNCTIONS -----------
def open_file(filename, mode='r', title='', root_uep='/',
             filters=tables.Filters(complevel=1, shuffle=True, fletcher32=False, complib='zlib'),
             shape=None, **kwargs):
    """
    Open or create a new OMX file. New files will be created with default
    zlib compression enabled.
    
    Parameters
    ----------
    filename : string
        Name or path and name of file
    mode : string
        'r' for read-only; 
        'w' to write (erases existing file); 
        'a' to read/write an existing file (will create it if doesn't exist).
        Ignored in read-only mode.
    title : string
        Short description of this file, used when creating the file. Default is ''.
        Ignored in read-only mode.
    filters : tables.Filters
        HDF5 default filter options for compression, shuffling, etc. Default for
        OMX standard file format is: zlib compression level 1, and shuffle=True. 
        Only specify this if you want something other than the recommended standard 
        HDF5 zip compression.
        'None' will create enormous uncompressed files.
        Only 'zlib' compression is guaranteed to be available on all HDF5 implementations.
        See HDF5 docs for more detail.
    shape: numpy.array
        Shape of matrices in this file. Default is None. Specify a valid shape 
        (e.g. (1000,1200)) to enforce shape-checking for all added objects. 
        If shape is not specified, the first added matrix will not be shape-checked 
        and all subsequently added matrices must match the shape of the first matrix.
        All tables in an OMX file must have the same shape.

    Returns
    -------
    f : openmatrix.File
        The file object for reading and writing.
    """
    f = File(filename, mode, title, root_uep, filters, **kwargs);

    # add omx structure if file is writable
    if mode != 'r':
        # version number
        if 'OMX_VERSION' not in f.root._v_attrs:
            f.root._v_attrs['OMX_VERSION'] = __omx_version__
        if 'OMX_CREATED_WITH' not in f.root._v_attrs:
            f.root._v_attrs['OMX_CREATED_WITH'] = 'python omx ' + __version__

        # shape
        if shape:
            storeshape = numpy.array([shape[0],shape[1]], dtype='int32')
            f.root._v_attrs['SHAPE'] = storeshape

        # /data and /lookup folders
        if 'data' not in f.root:
            f.create_group(f.root,"data")
        if 'lookup' not in f.root:
            f.create_group(f.root,"lookup")

    return f


if __name__ == "__main__":
    print('OMX!')

