#This script copies OnDie ECDSA artifacts from the cloud to a specified local directory.
#Typically, the FDO components (Mfg, RV, Owner) will be configured to load OnDie certs from this
#local directory.
#
#When files are updated in the local directory, they will have a file type of ".new". In addition,
#a touch file is created after copying to indicate to the component than an update has been made.
#The component will trigger an update when this touch file is detected and will rename the .new
#files to the original names as the cache files are read. 
#
#The use of the .new mechanism is employed to prevent any conflict between reading a file while
#a new version is copied.
#
# This script requires python 3 to run.

import sys, getopt
import urllib.request
import argparse
import os.path
import zipfile
import glob
import shutil

def artifact_copy(source_pathname, destdir):
    print(source_pathname)
    filename = os.path.basename(source_pathname)
    fi = open(source_pathname, "+rb")
    fo = open(os.path.join(destdir, filename + ".new"), "+wb")
    fo.write(fi.read())
    fi.close
    fo.close

parser = argparse.ArgumentParser(description='Update local OnDieCache.')
parser = argparse.ArgumentParser(prog='ondieCache', usage='%(prog)s --cachedir CACHEDIR -f')
parser.add_argument('-c', '--cachedir', required=True, help='local directory to store cache artifacts')
parser.add_argument('-f', required=False, action='store_true', help='force update when previous update not yet processed')

args = parser.parse_args()

cache_touch_file = os.path.join(args.cachedir, "cache_updated")

#make sure that there is not a current update in process
if (args.f == False and os.path.exists(cache_touch_file)):
    print ("previous update not yet processed (touch file still exists), exiting.", flush=True)
    exit()

print ("downloading zip file... ")
fi = urllib.request.urlopen("https://tsci.intel.com/content/csme.zip")
fo = open(os.path.join(args.cachedir, "csme.zip"), "+wb")
fo.write(fi.read())
fi.close
fo.close
print ("downloaded zip file")

# unzip the files
cwd = os.getcwd()

with zipfile.ZipFile(os.path.join(args.cachedir, "csme.zip"), mode='r', compression=zipfile.ZIP_DEFLATED) as zip_file:
    os.chdir(args.cachedir)
    zip_file.extractall()
    zip_file.close()
os.chdir(cwd)

# copy downloaded files into cachedir and append .new to each file name
print ("copying certs")
sourcedir = os.path.join(args.cachedir, "content/OnDieCA/certs/*.cer")
for file in glob.glob(sourcedir):
    artifact_copy(file, args.cachedir)

print ("copying crls")
sourcedir = os.path.join(args.cachedir, "content/OnDieCA/crls/*.crl")
for file in glob.glob(sourcedir):
    artifact_copy(file, args.cachedir)

# clean up:
# delete the zip file
# delete the unzipped files
shutil.rmtree(os.path.join(args.cachedir, "content"), ignore_errors=False, onerror=None)


# create the touch file to indicate that the cache has been updated
fo = open(os.path.join(cache_touch_file), "+wb")
fo.close

