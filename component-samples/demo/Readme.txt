#####################
# 
# 
#

CERTIFICATES

---------------------
customer name

+ int        int           clob      char[]  unique
+----------------------------------------------
PUB_TYPE | CUSTOMER ID | CHAIN  | PUBKEY_THUMB   


Private key
clob		key or HSM info 
---------------------------------------------
PUB_THUMB | private key


@Column(unique = true)
@ManyToOne(optional = false, fetch = FetchType.EAGER)
private ProductSerialMask mask;

@Column(unique = true)
@ManyToOne(optional = false, fetch = FetchType.EAGER)
private Group group;

@Table(
   name = "product_serial_group_mask", 
   uniqueConstraints = {@UniqueConstraint(columnNames = {"mask", "group"})}
)