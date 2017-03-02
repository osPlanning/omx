from __future__ import print_function
import openmatrix as omx
import numpy as np

# Create some data
ones = np.ones((100,100))
twos = 2.0*ones


# Create an OMX file (will overwrite existing file!)
print('Creating myfile.omx')
myfile = omx.open_file('myfile.omx','w')   # use 'a' to append/edit an existing file


# Write to the file.
myfile['m1'] = ones
myfile['m2'] = twos
myfile['m3'] = ones + twos           # numpy array math is fast
myfile.close()


# Open an OMX file for reading only
print('Reading myfile.omx')
myfile = omx.open_file('myfile.omx')

print ('Shape:', myfile.shape())                 # (100,100)
print ('Number of tables:', len(myfile))         # 3
print ('Table names:', myfile.list_matrices())   # ['m1','m2',',m3']


# Work with data. Pass a string to select matrix by name:
# -------------------------------------------------------
m1 = myfile['m1']
m2 = myfile['m2']
m3 = myfile['m3']

# halves = m1 * 0.5  # CRASH!  Don't modify an OMX object directly.
#                    # Create a new numpy array, and then edit it.
halves = np.array(m1) * 0.5

first_row = m2[0]
first_row[:] = 0.5 * first_row[:]

my_very_special_zone_value = m2[10][25]


# FANCY: Use attributes to find matrices
# --------------------------------------
myfile.close()                            # was opened read-only, so let's reopen.
myfile = omx.open_file('myfile.omx','a')  # append mode: read/write existing file

myfile['m1'].attrs.timeperiod = 'am'
myfile['m1'].attrs.mode = 'hwy'

myfile['m2'].attrs.timeperiod = 'md'

myfile['m3'].attrs.timeperiod = 'am'
myfile['m3'].attrs.mode = 'trn'

print('attributes:', myfile.list_all_attributes())       # ['mode','timeperiod']

# Use a DICT to select matrices via attributes:

all_am_trips = myfile[ {'timeperiod':'am'} ]                    # [m1,m3]
all_hwy_trips = myfile[ {'mode':'hwy'} ]                        # [m1]
all_am_trn_trips = myfile[ {'mode':'trn','timeperiod':'am'} ]   # [m3]

print('sum of some tables:', np.sum(all_am_trips))


# SUPER FANCY: Create a mapping to use TAZ numbers instead of matrix offsets
# --------------------------------------------------------------------------
# (any mapping would work, such as a mapping with large gaps between zone
#  numbers. For this simple case we'll just assume TAZ numbers are 1-100.)

taz_equivs = np.arange(1,101)                  # 1-100 inclusive

myfile.create_mapping('taz', taz_equivs)
print('mappings:', myfile.list_mappings())                 # ['taz']

tazs = myfile.mapping('taz')          # Returns a dict:  {1:0, 2:1, 3:2, ..., 100:99}

m3 = myfile['m3']

print('cell value:', m3[tazs[100]][tazs[100]])      # 3.0  (taz (100,100) is cell [99][99])

myfile.close()

