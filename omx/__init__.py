# OMX package
# release 1

import tables
from File import *
from Exceptions import *

# GLOBAL VARIABLES -----------
__version__ = '0.1'

# GLOBAL FUNCTIONS -----------
def openFile(filename, mode='r', title='', root_uep='/',
             filters=tables.Filters(complevel=1,shuffle=True,fletcher32=False,complib='zlib'),
             **kwargs):
    """Open or create a new OMX file. New files will be created with default
       zlib compression enabled."""
    return File(filename, mode, title, root_uep, filters, **kwargs);


if __name__ == "__main__":
    print 'OMX!'


