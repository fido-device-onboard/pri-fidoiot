//Configures AIO database when a new device is initalized
//configure service info specific to the device
//%1$s is replaced with the guid


//you can load content from files using FILE_READ('path',NULL) for default encoding of text
//FILE_READ('path') for binary

//if running on windows
//INSERT INTO SYSTEM_MODULE_RESOURCE (CONTENT,GUID_TAG,FILE_NAME_TAG,CONTENT_TYPE_TAG,PRIORITY ) VALUES(FILE_READ('./resources/hello-world.bat',NULL),'%1$s','hello-world.bat','fdo_sys:filedesc',1  );
//INSERT INTO SYSTEM_MODULE_RESOURCE (CONTENT,GUID_TAG,CONTENT_TYPE_TAG,PRIORITY ) VALUES(X'8363636D64622F436F68656C6C6F2D776F726C642E626174','%1$s','fdo_sys:exec',2  );


//if running on linux
INSERT INTO SYSTEM_MODULE_RESOURCE (CONTENT,GUID_TAG,FILE_NAME_TAG,CONTENT_TYPE_TAG,PRIORITY ) VALUES(FILE_READ('./resources/payload.txt',NULL),'%1$s','payload.txt','fdo_sys:filedesc',1  );
INSERT INTO SYSTEM_MODULE_RESOURCE (CONTENT,GUID_TAG,FILE_NAME_TAG,CONTENT_TYPE_TAG,PRIORITY ) VALUES(FILE_READ('./resources/linux64.sh',NULL),'%1$s','linux64.sh','fdo_sys:filedesc',2  );
INSERT INTO SYSTEM_MODULE_RESOURCE (CONTENT,GUID_TAG,CONTENT_TYPE_TAG,PRIORITY ) VALUES(X'82672F62696E2F73686A6C696E757836342E7368','%1$s','fdo_sys:exec',3  );

//INSERT INTO SYSTEM_MODULE_RESOURCE (CONTENT,GUID_TAG,CONTENT_TYPE_TAG,PRIORITY ) VALUES(X'F4','%1$s','fdo_sys:ismore',4  );
INSERT INTO SYSTEM_MODULE_RESOURCE (CONTENT,GUID_TAG,CONTENT_TYPE_TAG,PRIORITY ) VALUES(X'F5','%1$s','fdo_sys:isdone',5  );
