import openmatrix as omx
import numpy

# Create some data
ones = numpy.ones((100,100))
twos = 2.0*ones


# Create an OMX file (will overwrite existing file!)
myfile = omx.openFile('myfile.omx','w')   # use 'a' to append/edit an existing file


# Write to the file.
myfile['m1'] = ones
myfile['m2'] = twos
myfile['m3'] = ones + twos           # numpy array math is fast
myfile.close()


# Open an OMX file for reading only
myfile = omx.openFile('myfile.omx')

print (myfile.shape())               # (100,100)
len(myfile)                          # 3
myfile.listMatrices()                # ['m1','m2',',m3']


# Work with data. Pass a string to select matrix by name:
# -------------------------------------------------------
m1 = myfile['m1']
m2 = myfile['m2']
m3 = myfile['m3']

# halves = m1 * 0.5  # CRASH!  Don't modify an OMX object directly.
#                    # Create a new numpy array, and then edit it.
halves = numpy.array(m1) * 0.5

first_row = m2[0]
first_row[:] = 0.5 * first_row[:]

my_very_special_zone_value = m2[10][25]


# FANCY: Use tags to find matrices
# -----------------------------------
myfile.close()                           # was opened read-only, so let's reopen.
myfile = omx.openFile('myfile.omx','a')  # append mode: read/write existing file

myfile['m1'].attrs.tags = ['trips','am','hwy']
myfile['m2'].attrs.tags = ['trips','md','hwy']
myfile['m3'].attrs.tags = ['trips','md','trn']

try:
	myfile.listAllTags()                 # ['am','hwy','md','trips','trn']

	# Use a TUPLE to select matrices via tags:
	all_hwy_trips = myfile[ ('trips','hwy') ]   # [m1,m2]
	all_md_trips = myfile[ ('md',) ]            # [m2,m3]

	print (numpy.sum(all_md_trips))
except:
	print("oops, these commands don't currently work with this package")

# SUPER FANCY: Create a mapping to use TAZ numbers instead of matrix offsets
# --------------------------------------------------------------------------
# (any mapping would work, such as a mapping with large gaps between zone
#  numbers. For this simple case we'll just assume TAZ numbers are 1-100.)

taz_equivs = numpy.arange(1,101)                  # 1-100 inclusive

myfile.createMapping('taz', taz_equivs)
myfile.listMappings()                 # ['taz']

tazs = myfile.mapping('taz')          # Returns a dict:  {1:0, 2:1, 3:2, ..., 100:99}

m3 = myfile['m3']

print (m3[tazs[100]][tazs[100]])      # 3.0  (taz (100,100) is cell [99][99])

myfile.close()

