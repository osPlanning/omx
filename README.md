# OMX:  The Open Matrix data file format

An OMX matrix file is a structured collection of two-dimensional array objects and associated metadata.  OMX is built on top of the well-established [HDF5](https://www.hdfgroup.org/solutions/hdf5/) scientific data storage standard. An OMX file has a specific layout that is intended to ensure that complete and consistent information about the matrix data is stored and that the data can be retrieved correctly and efficiently.  We hope for the modeling industry to adopt the OMX standard, and we will periodically review the specification to make revisions as necessary.  

[![ZephyrTransportBadge](https://zephyrtransport.org/img/badging/project_pages/omx/omx.png)](https://zephyrtransport.org/)

# OMX APIs for your language 

Each API is now developed in its own repository, to keep history cleaner and more focused:

* Python: https://github.com/osPlanning/omx-python
* R: https://github.com/osPlanning/omx-r
* C#: https://github.com/osPlanning/omx-csharp
* C++: https://github.com/osPlanning/omx-cpp
* Java: https://github.com/osPlanning/omx-java
* Freepascal: https://github.com/jpn--/omx-freepascal

# More Information

Check out the [OMX wiki](https://github.com/osPlanning/omx/wiki) for more information, including API user guides, how to import and export OMX matrices from EMME, VISUM, TransCAD, and Cube, where to get the OMX validator, OMX viewer, and other background information on how OMX was created.  

Our [previous site](https://sites.google.com/site/openmodeldata/) and [listserv](https://groups.google.com/forum/?fromgroups#!forum/openmodeldata-discuss) are no longer maintained.

# License

All code written in the OMX project, including all API implementations, is licensed under the Apache License, version 2.0.  All code (c) by its respective authors.  See [LICENSE.TXT](LICENSE.TXT) for the full Apache 2.0 license text.
