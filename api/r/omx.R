
#Read and Write Open Matrix Files
#Ben Stabler, stabler@pbworld.com, 08/21/13
#Brian Gregor, gregorb@meritel.net, 10/28/13
#Requires the rhdf5 v2.5.1+ package from bioconductor
#Transposes matrix when writing it to file to be in row major order like C/Python
################################################################################

#Function to create an OMX file that is ready for writing data
#-------------------------------------------------------------
#This function creates an OMX file, establishes the shape attribute (number of rows and columns) and version attribute, and creates the data and lookup groups.
#Arguments:
#OMXFileName = full path name of the OMX file to create
#Numrows = number of rows that all matrices in the file will have
#Numcols = number of columns that all matrices in the file will have
#Level = compression level
#Return: TRUE

createFileOMX <- function( Filename, Numrows, Numcols, Level=1 ) {
  if(file.exists(Filename)) { file.remove(Filename) }
  Shape <- c( Numrows, Numcols )
  H5File <- H5Fcreate( Filename )
  h5writeAttribute( 0.2, H5File, "OMX_VERSION" )
  h5writeAttribute( Shape, H5File, "SHAPE" )
  h5createGroup(Filename,"data")
  h5createGroup(Filename,"lookup")
  H5Fclose( H5File )
  TRUE
}


#Function to write OMX matrix data
#---------------------------------
#This function writes OMX matrix data. A full matrix can be written or just portions of an existing matrix. It allows overwriting existing matrix values, but only if the "Replace" argument is set to TRUE. If only portions of the matrix are to be written to, the full matrix must already exist.
#Arguments:
#OMXFileName = full path name of the OMX file to store the matrix in
#Matrix = matrix object to be stored
#MatrixSaveName = name under which the matrix will be saved in the OMX file
#RowIndex = vector of positional indexes that rows of matrix object are written to. NULL value means that all rows are written to.
#ColIndex = vector of positional indexes that columns of matrix object are written to. NULL value means that all columns are written to.
#NaValue = value that will be used to replace NA values in the matrix (NA is not a value that can be stored in OMX)
#Replace = TRUE or FALSE value to determine whether an existing matrix of the same name should be replaced by new matrix
#Description = String that describes the matrix
#Function returns TRUE if completed successfully
#Return: TRUE

writeMatrixOMX <- function( OMXFileName, Matrix, MatrixSaveName, RowIndex=NULL, ColIndex=NULL, NaValue=-1 , 
                            Replace=FALSE, Description="" ) {
  #Get names of matrices in the file vc
	Contents <- h5ls( OMXFileName )
	MatrixNames <- Contents$name[ Contents$group == "/data" ]
	MatrixExists <- MatrixSaveName %in% MatrixNames
	# Get the matrix dimensions specified in the file
	Shape <- attr(h5read(OMXFileName, "/", read.attribute=T), "SHAPE")
  #Check whether there is matrix of that name already in the file
  if( MatrixExists & Replace == FALSE ){
    stop( paste("A matrix named '", MatrixSaveName, "' already exists. Value of 'Replace' argument must be TRUE in order to overwrite.", sep="") )
  }
  #Allow indexed writing (if RowIndex and ColIndex are not NULL) only if the matrix already exists
  if( !( is.null( RowIndex ) & is.null( ColIndex ) ) ){
    if( !MatrixExists ){
      stop( "Indexed writing to a matrix only allowed if a full matrix of that name already exists." )
    }
  }
  #If neither dimension will be written to indexes, write the full matrix and add the NA attribute
  if( is.null( RowIndex ) & is.null( ColIndex ) ){
    #Check conformance of matrix dimensions with OMX file
	  if( !all( dim( Matrix ) == Shape ) ){
      stop( paste( "Matrix dimensions not consistent with", OMXFileName, ":", Shape[1], "Rows,", Shape[2], "Cols" ) )
    }
    #Transpose matrix and convert NA to designated storage value
    Matrix <- t( Matrix )
    Matrix[ is.na( Matrix ) ] <- NaValue
    #Write matrix to file
    ItemName <- paste( "data", MatrixSaveName, sep="/" )
    h5write( Matrix, OMXFileName, ItemName )
    #Add the NA storage value and matrix descriptions as attributes to the matrix
    H5File <- H5Fopen( OMXFileName )
    H5Group <- H5Gopen( H5File, "data" )
    H5Data <- H5Dopen( H5Group, MatrixSaveName )
    h5writeAttribute( NaValue, H5Data, "NA" )
    h5writeAttribute( Description, H5Data, "Description" )
    #Close everything up before exiting
    H5Dclose( H5Data )
    H5Gclose( H5Group )
    H5Fclose( H5File )
  #Otherwise write only to the indexed positions
  } else {
    if( is.null( RowIndex ) ) RowIndex <- 1:Shape[1]
    if( is.null( ColIndex ) ) ColIndex <- 1:Shape[2]
    #Check that indexes are within matrix dimension ranges
    if( any( RowIndex <= 0 ) | ( max( RowIndex ) > Shape[1] ) ){
      stop( "One or more values of 'RowIndex' are outside the index range of the matrix." )
    }
    if( any( ColIndex <= 0 ) | ( max( ColIndex ) > Shape[2] ) ){
      stop( "One or more values of 'ColIndex' are outside the index range of the matrix." )
    }
    #Check that there are no duplicated indices
    if( any( duplicated( RowIndex ) ) ){
      stop( "Duplicated index values in 'RowIndex'. Not permitted." )
    }
    if( any( duplicated( ColIndex ) ) ){
      stop( "Duplicated index values in 'ColIndex'. Not permitted." )
    }
    #Combine the row and column indexes into a list
    #Indices are reversed since matrix is stored in transposed form
    Indices <- list( RowIndex, ColIndex )
    #Transpose matrix and convert NA to designated storage value
    Matrix <- t( Matrix )
    Matrix[ is.na( Matrix ) ] <- NaValue
    # Write the matrix to the indexed positions
    ItemName <- paste( "data", MatrixSaveName, sep="/" )
    h5write( Matrix, OMXFileName, ItemName, index=Indices )
  }
  TRUE
}


#Function to write a lookup vector to an OMX file
#------------------------------------------------
#This function writes a lookup vector to the file. It allows the user to specify if the lookup vector applies only to rows or columns (in case the matrix is not square and/or the rows and columns don't have the same meanings.
#Arguments:
#OMXFileName = full path name of the OMX file to store the lookup vector in
#LookupVector = lookup vector object to be stored
#LookupSaveName = name under which the lookup vector will be saved in the OMX file
#LookupDim = matrix dimension that the lookup vector is associated with
#Values can be "row", "col", or NULL. A lookup dimension attribute is optional.
#Description = string that describes the matrix
#Return: TRUE
writeLookupOMX <- function( OMXFileName, LookupVector, LookupSaveName, LookupDim=NULL, Replace=FALSE, Description="" ) {
  #Check whether there is lookup of that name already in the file
  Contents = h5ls( OMXFileName )
  LookupNames <- Contents$name[ Contents$group == "/lookup" ]
  if( ( LookupSaveName %in% LookupNames ) & ( Replace == FALSE ) ){
    stop( paste("A lookup named '", LookupSaveName, "' already exists. 'Replace' must be TRUE in order to overwrite.", sep="") )
  }
  #Error check lookup dimension arguments
  if( !is.null( LookupDim ) ){
    if( !( LookupDim %in% c( "row", "col", "both" ) ) ) {
      stop( "LookupDim argument must be 'row', 'col', or NULL." )
    }
  }
  Len <- length( LookupVector )
  Shape = attr( h5read( OMXFileName, "/", read.attribute=T ), "SHAPE" )
  if( is.null( LookupDim ) ) {
    if( Shape[1] != Shape[2] ) {
      stop( "Matrix is not square. You must specify the 'LookupDim'" )
    }
    if( Len != Shape[1] ) {
      stop( paste( OMXFileName, " has ", Shape[1], " rows and columns. LookupVector has ", Len, " positions.", sep="" ) )
    }
  }
  if( !is.null( LookupDim ) ){
    if( LookupDim == "row" ){
      if( Len != Shape[1] ){
        stop( paste( "Length of 'LookupVector' does not match row dimension of", OMXFileName ) )
      }
    }
    if( LookupDim == "col" ){
      if( Len != Shape[2] ){
        stop( paste( "Length of 'LookupVector' does not match column dimension of", OMXFileName ) )
      }
    }
  }
  #Write lookup vector to file
  ItemName <- paste( "lookup", LookupSaveName, sep="/" )
  h5write( LookupVector, OMXFileName, ItemName )
  #Write attributes
  H5File <- H5Fopen( OMXFileName )
  H5Group <- H5Gopen( H5File, "lookup" )
  H5Data <- H5Dopen( H5Group, LookupSaveName )
  h5writeAttribute( Description, H5Data, "Description" )
  if( !is.null( LookupDim ) ) {
    h5writeAttribute( LookupDim, H5Data, "DIM" )
  }
  #Close everything up before exiting
  H5Dclose( H5Data )
  H5Gclose( H5Group )
  H5Fclose( H5File )
  TRUE
}


#Function to list the contents of an OMX file
#--------------------------------------------
#This function lists the contents of an omx file. These include:
#OMX version
#Matrix shape
#Names, descriptions, datatypes, and NA values of all of the matrices in an OMX file.
#Names and descriptions of indices and whether each index applies to rows, columns or both
#Arguments:
#OMXFileName = full path name of the OMX file
#Return: A list with 3 components:
#File - a list with the OMX_Version and Shape attributes
#Matrix - a dataframe identifying the matrices and all their attributes
#Lookup - a dataframe identifying the lookups and all their attributes 
#Return: A list containing the OMX version, number of rows, number of columns, names and attributes of all matrices, names and attributes of all lookups
OMXFileName = "test.omx"
listOMX <- function( OMXFileName ) {
  #Get the version and shape information
 	Version <- attr(h5read(OMXFileName, "/", read.attribute=T), "OMX_VERSION")
 	Shape <- attr(h5read(OMXFileName, "/", read.attribute=T), "SHAPE")
  #Use the h5ls function to read the contents of the file
  Contents <- h5ls( OMXFileName )
  MatrixContents <- Contents[ Contents$group == "/data", ]
  LookupContents <- Contents[ Contents$group == "/lookup", ]
  #Read the matrix information
  Names <- MatrixContents$name
  Types <- MatrixContents$dclass
  MatAttrs <- list()
  for( i in 1:length(Names) ) {
    ItemName <- paste( "data", Names[i], sep="/" )
    MatAttrs[[i]] <- unlist( attributes( h5read( OMXFileName, ItemName, read.attribute=T ) ) )
  }
  MatAttrs <- do.call( rbind, MatAttrs )
  MatDF <- as.data.frame ( cbind( Names, Types, MatAttrs ) )
  rownames( MatDF ) <- NULL
  #Read the lookup information
  Names <- LookupContents$name
  Types <- LookupContents$dclass
  LookupAttrs <- list()
  for( i in 1:length(Names) ) {
    ItemName <- paste( "lookup", Names[i], sep="/" )
    LookupAttrs[[i]] <- unlist( attributes( h5read( OMXFileName, ItemName, read.attribute=T ) ) )
  }
  LookupAttrs <- do.call( rbind, LookupAttrs )    
  LookupDF <- as.data.frame( cbind( Names, Types, LookupAttrs ) )
  #Combine the results into a list
  list( OMXVersion=Version, Rows=Shape[1], Columns=Shape[2], Matrices=MatDF, Lookups=LookupDF )
}
  
        
#Function to read an OMX matrix
#------------------------------     
#This function reads an entire matrix in an OMX file or portions of a matrix using indexing.
#Arguments:
#OMXFileName = Path name of the OMX file where the matrix resides.
#MatrixName = Name of the matrix in the OMX file
#RowIndex = Vector containing numerical indices of the rows to be read.
#ColIndex = Vector containing numerical indices of the columns to be read 
#Return: Matrix containing the values of all cells read
readMatrixOMX <- function( OMXFileName, MatrixName, RowIndex=NULL, ColIndex=NULL ) {
	#Get the matrix dimensions specified in the file
	Shape <- attr(h5read(OMXFileName, "/", read.attribute=T), "SHAPE")
  #Identify the item to be read
  ItemName <- paste( "data", MatrixName, sep="/" )
  #Check that RowIndex is proper
  if( !is.null( RowIndex ) ) {
    if( any( RowIndex <= 0 ) | ( max( RowIndex ) > Shape[1] ) ){
      stop( "One or more values of 'RowIndex' are outside the index range of the matrix." )
    }
    if( any( duplicated( RowIndex ) ) ){
      stop( "Duplicated index values in 'RowIndex'. Not permitted." )
    }
  }
  #Check that ColIndex is proper
  if( !is.null( ColIndex ) ) {
    if( any( ColIndex <= 0 ) | ( max( ColIndex ) > Shape[2] ) ){
      stop( "One or more values of 'ColIndex' are outside the index range of the matrix." )
    }
    if( any( duplicated( ColIndex ) ) ){
      stop( "Duplicated index values in 'ColIndex'. Not permitted." )
    }
  }
  #Combine the row and column indexes into a list
  #Indexes are reversed since matrix is stored in transposed form
  Indices <- list( ColIndex, RowIndex )
  #Read the indexed positions of the matrix
  Result <- t( h5read( OMXFileName, ItemName, index=list(ColIndex,RowIndex) ) )
  #Replace the NA values with NA
  NAValue = as.vector( attr( h5read( OMXFileName, ItemName, read.attribute=T ), "NA" ) )
  Result[ Result == NAValue ] <- NA
  #Return the result
  Result
}


#Function to read an OMX lookup
#------------------------------     
#This function reads a lookup and its attributes.
#Arguments:
#OMXFileName = Path name of the OMX file where the lookup resides.
#LookupName = Name of the lookup in the OMX file
#Return: Vector of the lookup values
readLookupOMX <- function( OMXFileName, LookupName ) {
  #Identify the item to be read
  ItemName <- paste( "lookup", LookupName, sep="/" )
  #Read the lookup
  h5read( OMXFileName, ItemName )
}
       