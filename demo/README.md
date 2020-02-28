# Secure Device Onboard (SDO) out-of-the-box demo

The out-of-the-box demo contains scripts and configuration files to run SDO services.

The SDO out-of-the-box demo is organized according to the following directory structure:

    demo
     └──|
        ├── device       : Demo Device
        ├── owner        : Demo Owner
        └── rendezvous   : Demo Rendezvous service

## Packages required for the Ubuntu* 18.04 system

    $ sudo apt install openjdk-11-jdk-headless

## Running the demo on a single machine

The demo consists of running a Rendezvous service, an Ownership service, and
a simulated device in three separate terminal windows.

To begin the demo, open four separate terminal windows:

Terminal #1: Start the Rendezvous Server.

    $ cd rendezvous
    $ ./rendezvous

Terminal #2: Register the voucher for TO0.

    $ cd owner
    $ ./to0client vouchers/1fae14fb-deca-405a-abdd-b25391b9d932.json

The client would perform TO0 registration for the ownership vouchers present
within 'vouchers' folder. The TO0 registration has a timeout value, hence
to0client needs to be run periodically.

Terminal #3: Start the Ownership service.

    $ cd owner
    $ ./owner

Terminal #4: Start the simulated device.

    $ cd device
    $ ./device

When the demo finishes, the log files would be avaialble in respective folders.

## Default key-types in the demo

* ECDSA NIST-256

# Configuring the demo

Different properties used in the demo are specified in respective
application.properties file. A brief description for each of these fields are
provided within the configuration file. For more details, please refer to the
file application.properties.sample provided under 'cri' folder in source
package.

# Working with new Ownership Voucher/Ownership Credentials pairs

New Ownership Vouchers/Ownership Credential pair can be generated using
'SupplyChainTools'. Follow the related user guide to generate the new SDO
credentials.

* The generated Ownership voucher file should be copied to owner/vouchers
  folder.
* In device/application.properties, com.intel.sdo.device.credentials field
  should be updated with the path of generated Ownership Credential file.
* While running to0client, path of generated Ownership voucher should be
  provided as arguement.

