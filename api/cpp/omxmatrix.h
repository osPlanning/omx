/* omxmatrix.h
 *
 * OMX/HDF5 Matrix helper routines
 *
 * @author Billy Charlton, PSRC
 * @author Ben Stabler, RSG
 */
#include <iostream>
#include <string>
#include <vector>
#include <queue>
#include <map>

#include <hdf5.h>
#include <hdf5_hl.h>

using namespace std;

//--------------------------------------------------------------------
#ifndef OMXMATRIX_H
#define OMXMATRIX_H

#define  MODE_READWRITE    0
#define  MODE_CREATE  1

#define  MAX_TABLES  500

bool     isOMX(char*);

class OMXMatrix {
public:
    OMXMatrix();

    virtual  ~OMXMatrix();

    void     openFile(string fileName);
    void     closeFile();

    //Read/Open operations
    int      getRows();
    int      getCols();
    int      getTables();
    void     getRow(string table, int row, void *rowptr);  // throws InvalidOperationException, MatrixReadException
	void     getCol(string table, int col, void *colptr);  // throws InvalidOperationException, MatrixReadException
    string   getTableName(int table);

    //Write/Create operations
    void     createFile(int tables, int rows, int cols, vector<string> &matNames, string fileName);
    void     writeRow(string table, int row, double* rowptr);

    //Nested exception classes
    class    FileOpenException { };
    class    MatrixReadException { };
    class    InvalidOperationException { };
    class    OutOfMemoryException {};
    class    NoSuchTableException {};

//--------------------------------------------------------------------
    //Data

    hid_t    _h5file;
    int      _nRows;
    int      _nCols;
    int      _nTables;
    int      _mode;
    bool     _fileOpen;

    string   _tableName[MAX_TABLES+1];
    
    map<string,int> _tableLookup;
    map<string,hid_t> _dataset;
    map<string,hid_t> _dataspace;

private:

    hid_t    _memspace;

    //Methods
    void    readTableNames();
    void    printErrorCode(int error);
    void    init_tables (vector<string> &tableNames);
    hid_t   openDataset(string table);  // throws InvalidOperationException
};

#endif /* OMXMATRIX_H */

