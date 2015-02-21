#=====
#omx.r
#=====

#Read and Write Open Matrix Files
#Ben Stabler, stabler@pbworld.com, 08/21/13
#Brian Gregor, gregor@or-analytics.com, 12/18/13
#Requires the rhdf5 v2.5.1+ package from bioconductor
#Transposes matrix when writing it to file to be in row major order like C/Python
################################################################################

#Load the rhdf5 library
library( rhdf5 )

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

#Utility function to read the SHAPE and VERSION attributes
#---------------------------------------------------------
#This function reads the SHAPE and VERSION attributes of an OMX file. This is called by several other functions
#Arguments:
#OMXFileName = full path name of the OMX file being read
#Return: List containing the SHAPE  and VERSION attributes. 
#SHAPE component is a vector of number of rows and columns
#VERSION component is the version number
getRootAttrOMX <- function( OMXFileName ) {
	H5File <- H5Fopen( OMXFileName )
  H5Attr <- H5Aopen( H5File, "SHAPE" )
  RootAttr <- list()
  RootAttr$SHAPE <- H5Aread( H5Attr )
  H5Aclose( H5Attr )
  H5Attr <- H5Aopen( H5File, "OMX_VERSION" )
  RootAttr$VERSION <- H5Aread( H5Attr )
  H5Aclose( H5Attr )
  H5Fclose( H5File )
  RootAttr
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
	RootAttr <- getRootAttrOMX( OMXFileName )
	Shape <- RootAttr$SHAPE
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
    
    #Write matrix to file, set chunking and compression
    ItemName <- paste( "data", MatrixSaveName, sep="/" )
    h5createDataset(OMXFileName, ItemName, dim(Matrix), chunk=c(nrow(Matrix),1), level=7)
    h5write( Matrix, OMXFileName, ItemName)
    
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
    if( !( LookupDim %in% c( "row", "col" ) ) ) {
      stop( "LookupDim argument must be 'row', 'col', or NULL." )
    }
  }
  Len <- length( LookupVector )
	RootAttr <- getRootAttrOMX( OMXFileName )
	Shape <- RootAttr$SHAPE
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
#Return: A list with 5 components:
#Version - the OMX version number
#Rows - number of rows in the matrix
#Columns - number of columns in the matrix
#Matrices - a dataframe identifying the matrices and all their attributes
#Lookups - a dataframe identifying the lookups and all their attributes 
listOMX <- function( OMXFileName ) {
  #Get the version and shape information
	RootAttr <- getRootAttrOMX( OMXFileName )
	Version <- RootAttr$VERSION
	Shape <- RootAttr$SHAPE
  #Use the h5ls function to read the contents of the file
  Contents <- h5ls( OMXFileName )
  MatrixContents <- Contents[ Contents$group == "/data", ]
  LookupContents <- Contents[ Contents$group == "/lookup", ]
  #Read the matrix attribute information
  Names <- MatrixContents$name
  Types <- MatrixContents$dclass
  H5File <- H5Fopen( OMXFileName )
  H5Group <- H5Gopen( H5File, "data" )
  MatAttr <- list()
  for( i in 1:length(Names) ) {
    Attr <- list(type="matrix")
    H5Data <- H5Dopen( H5Group, Names[i] )
    if(H5Aexists(H5Data, "NA")) {
      H5Attr <- H5Aopen( H5Data, "NA" )
      Attr$navalue <- H5Aread( H5Attr )
      H5Aclose( H5Attr )
    }
    if(H5Aexists(H5Data, "Description")) {
      H5Attr <- H5Aopen( H5Data, "Description" )
      Attr$description <- H5Aread( H5Attr )
      H5Aclose( H5Attr )
    }
    MatAttr[[Names[i]]] <- Attr
    H5Dclose( H5Data )
    rm( Attr )
  }
  H5Gclose( H5Group )
  H5Fclose( H5File )
  MatAttr <- do.call( rbind, lapply( MatAttr, function(x) data.frame(x) ) )
  rm( Names, Types )        
  #Read the lookup attribute information
  H5File <- H5Fopen( OMXFileName )
  H5Group <- H5Gopen( H5File, "lookup" )
  Names <- LookupContents$name
  Types <- LookupContents$dclass
  LookupAttr <- list()
  if(length(Names)>0) {
    for( i in 1:length(Names) ) {
      Attr <- list(type="lookup")
      H5Data <- H5Dopen( H5Group, Names[i] )
      if( H5Aexists( H5Data, "DIM" ) ) {
        H5Attr <- H5Aopen( H5Data, "DIM" )
        Attr$lookupdim <- H5Aread( H5Attr )
        H5Aclose( H5Attr )
      } else {
        Attr$lookupdim <- ""
      }
      if( H5Aexists( H5Data, "Description" ) ) {
        H5Attr <- H5Aopen( H5Data, "Description" )
        Attr$description <- H5Aread( H5Attr )
        H5Aclose( H5Attr )
      } else {
        Attr$description <- ""
      }
      LookupAttr[[Names[i]]] <- Attr
      H5Dclose( H5Data )
      rm( Attr )
    }
    H5Gclose( H5Group )
    H5Fclose( H5File )
    LookupAttr <- do.call( rbind, lapply( LookupAttr, function(x) data.frame(x) ) )    
    rm( Names, Types )
  }
    #Combine the results into a list
  if(length(MatAttr)>0) {
    MatInfo <- cbind( MatrixContents[,c("name","dclass","dim")], MatAttr )
  } else {
    MatInfo <- MatrixContents[,c("name","dclass","dim")]
  }
  if(length(LookupAttr)>0) {
    LookupInfo <- cbind( LookupContents[,c("name","dclass","dim")], LookupAttr )
  } else {
    LookupInfo <- LookupContents[,c("name","dclass","dim")]
  }
  list( OMXVersion=Version, Rows=Shape[1], Columns=Shape[2], Matrices=MatInfo, Lookups=LookupInfo )
}

#Function to read an OMX matrix
#------------------------------     
#This function reads an entire matrix in an OMX file or portions of a matrix using indexing.
#Arguments:
#OMXFileName = Path name of the OMX file where the matrix resides.
#MatrixName = Name of the matrix in the OMX file
#RowIndex = Vector containing numerical indices of the rows to be read.
#ColIndex = Vector containing numerical indices of the columns to be read
#Return: a matrix 
readMatrixOMX <- function( OMXFileName, MatrixName, RowIndex=NULL, ColIndex=NULL ) {
	#Get the matrix dimensions specified in the file
	RootAttr <- getRootAttrOMX( OMXFileName )
	Shape <- RootAttr$SHAPE
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
  if(!is.null(NAValue)) {
    Result[ Result == NAValue ] <- NA
  } 
  #Return the result
  Result
}

#Function to read an OMX lookup
#------------------------------     
#This function reads a lookup and its attributes.
#Arguments:
#OMXFileName = Path name of the OMX file where the lookup resides.
#LookupName = Name of the lookup in the OMX file
#Return: a list having 2 components
#Lookup = The lookup vector
#LookupDim = The name of the matrix dimension the lookup corresponds to
readLookupOMX <- function( OMXFileName, LookupName ) {
  #Identify the item to be read
  ItemName <- paste( "lookup", LookupName, sep="/" )
  #Read the lookup
  Lookup <- h5read( OMXFileName, ItemName )
  #Read the name of the dimension the lookup corresponds
  H5File <- H5Fopen( OMXFileName )
  H5Group <- H5Gopen( H5File, "lookup" )
  H5Data <- H5Dopen( H5Group, LookupName )
  if( H5Aexists( H5Data, "DIM" ) ) {
    H5Attr <- H5Aopen( H5Data, "DIM" )
    Dim <- H5Aread( H5Attr )
    H5Aclose( H5Attr )
  } else {
    Dim <- ""
  }
  H5Dclose( H5Data )
  H5Gclose( H5Group )
  H5Fclose( H5File )
  #Return the lookup and the corresponding dimension
  list( Lookup=Lookup, LookupDim=Dim )
}

#Function to return portion of OMX matrix based using selection statements
#-------------------------------------------------------------------------
#This function reads a portion of an OMX matrix using selection statements to define the portion
#Multiple selection selection statements can be used for each dimension
#Each selection statement is a logical expression represented in a double-quoted string
#The left operand is the name of a lookup vector
#The operator can be any logical operator including %in%
#The right operand is the value or values to check against. This can be the name of a vector defined in the calling environment
#If the right operand contains literal string values, those values must be single-quoted
#Multiple selection conditions may be used as argument by including in a vector
#Multiple selection conditions are treated as intersections
#Arguments:
#OMXFileName = Path name of the OMX file where the lookup resides.
#MatrixName = Name of the matrix in the OMX file
#RowSelection = Row selection statement or vector of row selection statements (see above)
#ColSelection = Column selection statement or vector of column selection statements (see above
#RowLabels = Name of lookup to use for labeling rows
#ColLabels = Name of lookup to use for labeling columns
#Return: The selected matrix

readSelectedOMX <- function( OMXFileName, MatrixName, RowSelection=NULL, ColSelection=NULL, RowLabels=NULL, ColLabels=NULL ) {
	#Get the matrix dimensions specified in the file
	RootAttr <- getRootAttrOMX( OMXFileName )
	Shape <- RootAttr$SHAPE
	#Define function to parse a selection statement and return corresponding data indices
	findIndex <- function( SelectionStmt ) {
    StmtParse <- unlist( strsplit( SelectionStmt, " " ) )
    IsBlank <- sapply( StmtParse, nchar ) == 0
    StmtParse <- StmtParse[ !IsBlank ]
    LookupName <- StmtParse[1]
    Lookup <- readLookupOMX( OMXFileName, LookupName )
    assign( LookupName, Lookup[[1]]  )
    which( eval( parse( text=SelectionStmt ) ) )
  }
  #Make index for row selection
  if( !is.null( RowSelection ) ) {
    RowIndex <- 1:Shape[1]
    for( Stmt in RowSelection ) {
      Index <- findIndex( Stmt )
      RowIndex <- intersect( RowIndex, Index )
      rm( Index )
    }
  } else {
    RowIndex <- NULL
  }
  #Make index for column selection
  if( !is.null( ColSelection ) ) {
    ColIndex <- 1:Shape[2]
    for( Stmt in ColSelection ) {
      Index <- findIndex( Stmt )
      ColIndex <- intersect( ColIndex, Index )
      rm( Index )
    }
  } else {
    ColIndex <- NULL
  }
  #Extract the matrix meeting the selection criteria
  Result <- readMatrixOMX( OMXFileName, MatrixName, RowIndex=RowIndex, ColIndex=ColIndex )
  #Label the rows and columns
  if( !is.null( RowLabels ) ) {
    rownames( Result ) <- readLookupOMX( OMXFileName, RowLabels )[[1]][ RowIndex ]
  }                
  if( !is.null( ColLabels ) ) {
    colnames( Result ) <- readLookupOMX( OMXFileName, ColLabels )[[1]][ ColIndex ]
  }
  #Return the matrix
  Result                
}  

#Function to write matrix attribute
#-------------------------------------------------------------------------
#Arguments:
#OMXFileName = Path name of the OMX file where the matrix resides
#MatrixName = Name of the matrix in the OMX file
#AttributeName = Name of attribute
#Value = Attribute value
writeMatrixAttribute <- function( OMXFileName, MatrixSaveName, AttributeName, Value) {
    H5File <- H5Fopen( OMXFileName )
    H5Group <- H5Gopen( H5File, "data" )
    H5Data <- H5Dopen( H5Group, MatrixSaveName )
    h5writeAttribute( Value, H5Data, AttributeName )
    #Close everything up before exiting
    H5Dclose( H5Data )
    H5Gclose( H5Group )
    H5Fclose( H5File )
}
