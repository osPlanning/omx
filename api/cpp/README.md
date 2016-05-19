```
/* declarations */
OMXMatrix *omxMfs;
double omxDataBuffer[MAXZONES + 1]; //OMX is doubles
char* omxMfsFileName = "mfs.omx";

/* open omx file as read/write */
omxMfs = new OMXMatrix();
if (isOMX(omxMfsFileName)) {
   omxMfs->openFile(omxMfsFileName);
} else {
   printf("error: cannot open OMX mfs file.\n");
}

/* read and write a row */
int zones = omxMfs->getRows();
omxMfs->getRow("mf1", 1, &omxDataBuffer);
omxMfs->writeRow("mf1", 1, omxDataBuffer);
```
