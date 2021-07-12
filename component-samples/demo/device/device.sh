#!/bin/bash

# The shell variable that is passed to the JAR file as argument.
# NOTE: Ensure that a blank space is added before closing the double-quotes for subsequent
# properties definitions.

# LOG4J2 configuration file
ARGS+=" -Dlog4j.configurationFile=log4j2.xml"

# The file containing the device credential.
#
# A device without a credential is considered uninitialized and must run Device Initialization (DI)
# or its equivalent before ownership can be transferred.
ARGS+=" -Dfidoalliance.fdo.device.credential=credential.bin"

# The PEM-encoded file containing the device's keys, both public and private.
#
ARGS+=" -Dfidoalliance.fdo.pem.dev=device.pem"

# The URL at which the Device Initialization server may be contacted, if there is no credential.
#
ARGS+=" -Dfidoalliance.fdo.url.di=http://localhost:8039/"

# The JCE SecureRandom sources to be used, in a comma-separated list in order of preference.
#
ARGS+=" -Dfidoalliance.fdo.randoms=\"NativePRNG,Windows-PRNG\""

# The ServiceInfo MTU to be advertised from device to owner.
#
ARGS+=" -Dfidoalliance.fdo.device.service.info.mtu=1300"

# Set to true to enable the credential reuse protocol in the device.
#
ARGS+=" -Dfidoalliance.fdo.device.cred.reuse=true"

# The cipher algorithm to use during TO2.
#
# This may be one of:
#
# AES128/CTR/HMAC-SHA256
# AES256/CTR/HMAC-SHA384
# AES128/CBC/HMAC-SHA256
# AES256/CBC/HMAC-SHA384
# AES128GCM
# AES256GCM
# AES-CCM-64-128-128
# AES-CCM-64-128-256
#
ARGS+=" -Dfidoalliance.fdo.cipher=AES256GCM"

# Execute the JAR file
java $ARGS -jar device.jar