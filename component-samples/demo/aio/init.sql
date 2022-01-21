//Configures AIO database on inital first run the container 
//configure rv instructions,customer keys, owner blobs and service info 




//set owner tranfer customer keys
INSERT INTO MT_CUSTOMERS (CUSTOMER_ID,NAME ,KEYS) VALUES (1,'owner',FILE_READ('./resources/owner_customer1.pem',NULL) );

//set the autoenroll customer id to match a MT_CUSTOMER
//comment out below line if want to explicity require customer to be set through Api or db call
UPDATE MT_SETTINGS SET AUTO_ASSIGN_CUSTOMER_ID = 1 WHERE ID = 1;

//set owner to2 replacement keys
INSERT INTO OWNER_CUSTOMERS (CUSTOMER_ID,NAME ,KEYS) VALUES (1,'owner',FILE_READ('./resources/owner_customer1.pem',NULL) );


// Make sure to encode the SYSTEM_MODULE_RESOURCE.CONTENT in Hex Byte format before manual SQL insertion. Use X'' or any inbuilt function to encode the SYSTEM_MODULE_RESOURCE.CONTENT in byte stream format.
//INSERT INTO SYSTEM_MODULE_RESOURCE (CONTENT,CONTENT_TYPE_TAG,PRIORITY ) VALUES(X'F5','fdo_sys:active',0);
//INSERT INTO SYSTEM_MODULE_RESOURCE (CONTENT,GUID_TAG,FILE_NAME_TAG,CONTENT_TYPE_TAG,PRIORITY ) VALUES('68656c6c6f2066646f20776f726c64','2e1418eb-c810-49c4-a0db-91c4d0a58306','test.txt','fdo_sys:filedesc',1  )
//INSERT INTO SYSTEM_MODULE_RESOURCE (CONTENT,GUID_TAG,CONTENT_TYPE_TAG,PRIORITY ) VALUES('8363636D64622F636B737461727475702E626174','2e1418eb-c810-49c4-a0db-91c4d0a58306','fdo_sys:exec',2  )
          
//INSERT INTO TO2_SETTINGS  (ID,DEVICE_SERVICE_INFO_MTU_SIZE, OWNER_MTU_THRESHOLD  ) VALUES(1,1300,10400  );


//the sets the to2 owner blob returned during to1
UPDATE TO2_CONFIG SET RV_BLOB = 'http://localhost:8080?ipaddress=127.0.0.1';

// the manufaturing rv instructions
//localhost:8080 127.0.0.1 
UPDATE MT_SETTINGS SET RENDEZVOUS_INFO = '81858205696C6F63616C686F73748203191F90820C018202447F00000182041920FB' WHERE ID=1;
