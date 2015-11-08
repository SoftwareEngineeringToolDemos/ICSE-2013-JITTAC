Details on vm built using Vagrant
------

Environment
------
__Virtual Box used:__ [box-cutter/ubuntu1404-desktop](https://atlas.hashicorp.com/box-cutter/boxes/ubuntu1404-desktop)

Folder content
-----
This build-vm folder contains following things:
- __README.md file__
- __Vagrantfile__
  This script will create and configure the VM.


Steps to create a VM using this build
-----

1. Download [Vagrant](https://www.vagrantup.com/)
2. Install Vagrant
3. Download [VirtualBox](https://www.virtualbox.org/wiki/Downloads)
4. Install VirtualBox
5. Download all files from [build-vm](https://github.com/SoftwareEngineeringToolDemos/ICSE-2013-JITTAC/tree/master/build-vm) to local machine.
6. Open a command line/teminal window and go to build-vm folder
7. Run "__vagrant up__"
8. In VirtualBox GUI, enter credentials 
    username: __vagrant__ 
    password: __vagrant__

**Note:** It may take several minutes to download and configure the VM for use. Please wait until "__vagrant up__" finishes execution before working on GUI.
