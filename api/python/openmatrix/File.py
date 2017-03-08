import numpy as np
import tables  # requires pytables >= 3.1

from .Exceptions import *


class File(tables.File):
    """
    OMX File class, which contains all the methods for adding, removing, manipulating tables 
    and mappings in an OMX file.
    """

    def __init__(self, f,m,t,r,f1, **kwargs):
        tables.File.__init__(self,f,m,t,r,f1,**kwargs)
        self._shape = None

    def version(self):
        """
        Return the OMX file format of this OMX file, embedded in the OMX_VERSION file attribute.
        Returns None if the OMX_VERSION attribute is not set.
        """
        if 'OMX_VERSION' in self.root._v_attrs:
            return self.root._v_attrs['OMX_VERSION']
        else:
            return None


    def create_matrix(self, name, atom=None, shape=None, title='', filters=None,
                     chunkshape=None, byteorder=None, createparents=False, obj=None,
                     attrs=None):
        """
        Create an OMX Matrix (CArray) at the root level. User must pass in either
        an existing numpy matrix, or a shape and an atom type.

        Parameters
        ----------
        name : string
            The name of this matrix. Stored in HDF5 as the leaf name.
        title : string
            Short description of this matrix. Default is ''.
        obj : numpy.CArray
            Existing numpy array from which to create this OMX matrix. If obj is passed in,
            then shape and atom can be left blank. If obj is not passed in, then a shape and
            atom must be specified instead. Default is None.
        shape : numpy.array
            Optional shape of the matrix. Shape is an int32 numpy array of format (rows,columns).
            If shape is not specified, an existing numpy CArray must be passed in instead, 
            as the 'obj' parameter. Default is None.
        atom : atom_type
            Optional atom type of the data. Can be int32, float32, etc. Default is None.
            If None specified, then obj parameter must be passed in instead.
        filters : tables.Filters
            Set of HDF5 filters (compression, etc) used for creating the matrix. 
            Default is None. See HDF5 documentation for details. Note: while the default here
            is None, the default set of filters set at the OMX parent file level is 
            zlib compression level 1. Those settings usually trickle down to the table level.
        attrs : dict
            Dictionary of attribute names and values to be attached to this matrix.
            Default is None.

        Returns
        -------
        matrix : tables.carray
            HDF5 CArray matrix
        """

        # If object was passed in, make sure its shape is correct
        if self.shape() is not None and obj is not None and obj.shape != self.shape():
            raise ShapeError('%s has shape %s but this file requires shape %s' %
                (name, obj.shape, self.shape()))

        matrix = self.create_carray(self.root.data, name, atom, shape, title, filters,
                                    chunkshape, byteorder, createparents, obj)

        # Store shape if we don't have one yet
        if self._shape is None:
            storeshape = np.array([matrix.shape[0],matrix.shape[1]], dtype='int32')
            self.root._v_attrs['SHAPE'] = storeshape
            self._shape = matrix.shape

        # attributes
        if attrs:
            for key in attrs:
                matrix.attrs[key] = attrs[key]

        return matrix

    def shape(self):
        """
        Get the one and only shape of all matrices in this File
        
        Returns
        -------
        shape : tuple
            Tuple of (rows,columns) for this matrix and file.
        """

        # If we already have the shape, just return it
        if self._shape:
            return self._shape

        # If shape is already set in root node attributes, grab it
        if 'SHAPE' in self.root._v_attrs:
            # Shape is stored as a numpy.array:
            arrayshape = self.root._v_attrs['SHAPE']
            # which must be converted to a tuple:
            realshape = (arrayshape[0],arrayshape[1])
            self._shape = realshape
            return self._shape

        # Inspect the first CArray object to determine its shape
        if len(self) > 0:
            self._shape = self.iter_nodes(self.root.data,'CArray').next().shape

            # Store it if we can
            if self._iswritable():
                storeshape = np.array(
                    [self._shape[0],self._shape[1]],
                    dtype='int32')
                self.root._v_attrs['SHAPE'] = storeshape

            return self._shape

        else:
            return None


    def list_matrices(self):
        """
        List the matrix names in this File

        Returns
        -------
        matrices : list
            List of all matrix names stored in this OMX file.
        """
        return [node.name for node in self.list_nodes(self.root.data,'CArray')]


    def list_all_attributes(self):
        """
        Return set of all attributes used for any Matrix in this File
        
        Returns
        -------
        all_attributes : set
            The combined set of all attribute names that exist on any matrix in this file.
        """
        all_tags = set()
        for m in self.iter_nodes(self.root.data, 'CArray'):
            all_tags.update(m.attrs._v_attrnamesuser)
        return sorted(all_tags)


    # MAPPINGS -----------------------------------------------
    def list_mappings(self):
        """
        List all mappings in this file

        Returns:
        --------
        mappings : list
            List of the names of all mappings in the OMX file. Mappings 
            are stored internally in the 'lookup' subset of the HDF5 file
            structure. Returns empty list if there are no mappings.
        """
        try:
            return [m.name for m in self.list_nodes(self.root.lookup)]
        except:
            return []


    def delete_mapping(self, title):
        """
        Remove a mapping.

        Raises:
        -------
        LookupError : if the specified mapping does not exist.
        """

        try:
            self.remove_node(self.root.lookup, title)
        except:
            raise LookupError('No such mapping: '+title)


    def mapping(self, title):
        """
        Return dict containing key:value pairs for specified mapping. Keys
        represent the map item and value represents the array offset.

        Parameters:
        -----------
        title : string
            Name of the mapping to be returned

        Returns:
        --------
        mapping : dict
            Dictionary where each key is the map item, and the value 
            represents the array offset.

        Raises:
        -------
        LookupError : if the specified mapping does not exist.
        """

        try:
            # fetch entries
            entries = []
            entries.extend(self.get_node(self.root.lookup, title)[:])

            # build reverse key-lookup
            keymap = {}
            for i in range(len(entries)):
                keymap[entries[i]] = i

            return keymap

        except:
            raise LookupError('No such mapping: '+title)

    def map_entries(self, title):
        """Return a list of entries for the specified mapping.
           Throws LookupError if the specified mapping does not exist.
        """
        try:
            # fetch entries
            entries = []
            entries.extend(self.get_node(self.root.lookup, title)[:])

            return entries

        except:
            raise LookupError('No such mapping: '+title)


    def create_mapping(self, title, entries, overwrite=False):
        """
        Create an equivalency index, which maps a raw data dimension to
        another integer value. Once created, mappings can be referenced by
        offset or by key.
        
        Parameters:
        -----------
        title : string
            Name of this mapping
        entries : list
            List of n equivalencies for the mapping. n must match one data
            dimension of the matrix.
        overwrite : boolean
            True to allow overwriting an existing mapping, False will raise
            a LookupError if the mapping already exists. Default is False.

        Returns:
        --------
        mapping : tables.array
            Returns the created mapping.

        Raises:
            LookupError : if the mapping exists and overwrite=False
        """

        # Enforce shape-checking
        if self.shape():
            if not len(entries) in self._shape:
                raise ShapeError('Mapping must match one data dimension')

        # Handle case where mapping already exists:
        if title in self.list_mappings():
            if overwrite:
                self.delete_mapping(title)
            else:
                raise LookupError(title+' mapping already exists.')

        # Create lookup group under root if it doesn't already exist.
        if 'lookup' not in self.root:
            self.create_group(self.root, 'lookup')

        # Write the mapping!
        mymap = self.create_array(self.root.lookup, title, atom=tables.UInt32Atom(),
                                 shape=(len(entries),) )
        mymap[:] = entries

        return mymap


    # The following functions implement Python list/dictionary lookups. ----
    def __getitem__(self,key):
        """Return a matrix by name, or a list of matrices by attributes"""

        if isinstance(key, str):
            return self.get_node(self.root.data, key)

        if 'keys' not in dir(key):
            raise LookupError('Key %s not found' % key)

        # Loop through key/value pairs
        mats = self.list_nodes(self.root.data, 'CArray')
        for a in key.keys():
            mats = self._getMatricesByAttribute(a, key[a], mats)

        return mats


    def _getMatricesByAttribute(self, key, value, matrices=None):

        answer = []

        if matrices is None:
            matrices = self.list_nodes(self.root.data,'CArray')

        for m in matrices:
            if m.attrs is None:
                continue

            # Only test if key is present in matrix attributes
            if key in m.attrs._v_attrnames and m.attrs[key] == value:
                answer.append(m)

        return answer


    def __len__(self):
        return len(self.list_nodes(self.root.data, 'CArray'))


    def __setitem__(self, key, dataset):
        # We need to determine atom and shape from the object that's been passed in.
        # This assumes 'dataset' is a numpy object.
        atom = tables.Atom.from_dtype(dataset.dtype)
        shape = dataset.shape

        #checks to see if it is already a tables instance, and if so, just copies it
        if dataset.__class__.__name__ == 'CArray':
            return dataset.copy(self.root.data, key)
        else:
            return self.create_matrix(key, atom, shape, obj=dataset)


    def __delitem__(self, key):
        self.remove_node(self.root.data, key)


    def __iter__(self):
        """Iterate over the keys in this container"""
        return self.iter_nodes(self.root.data, 'CArray')


    def __contains__(self, item):
        return item in self.root.data._v_children

    # BACKWARD COMPATIBILITY:
    # PyTables switched from camelCaseMethods to camel_case_methods
    # We follow suit, and keep old methods for backward compat:
    createMapping = create_mapping
    createMatrix = create_matrix
    deleteMapping = delete_mapping
    listMatrices = list_matrices
    listAllAttributes = list_all_attributes
    listMappings = list_mappings
    mapentries = map_entries
    mapEntries = map_entries

