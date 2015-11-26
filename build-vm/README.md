Details on vm built using Vagrant
------

Environment
------
__Virtual Box used:__ [box-cutter/ubuntu1404-desktop](https://atlas.hashicorp.com/box-cutter/boxes/ubuntu1404-desktop)  
This will be an Ubuntu 14.04 64 bit box with guest additions.

Folder content
-----
This build-vm folder contains following things:
- __README.md__ file
- __Vagrantfile__  This script will create and configure the VM with OpenJdk7 and Eclipse Indigo with JITTAC plugin and dependencies already installed.
- __vm-files__  The necessary files for the Vagrant script to run


Steps to create a VM using this build
-----

1. Download [Vagrant](https://www.vagrantup.com/)
2. Install Vagrant
3. Download [VirtualBox](https://www.virtualbox.org/wiki/Downloads)
4. Install VirtualBox
5. Download __Vagrantfile__ from [build-vm](https://github.com/SoftwareEngineeringToolDemos/ICSE-2013-JITTAC/tree/master/build-vm) to local machine.
6. Open a command line/teminal window and go to folder where __Vagrantfile__ is located
7. Run "__vagrant up__"
8. If prompted in VirtualBox GUI, enter credentials:  
username: __vagrant__  
password: __vagrant__

**Note:** It may take several minutes to download and configure the VM for use. Please wait until "__vagrant up__" finishes execution and the box reboots before working in the GUI.

Verification
-----
1. Wait for box to configure and reboot
2. Eclipse will automatically start upon reboot

Acknowledgements
-----
Thanks to [Priyadarshini Rajagopal](https://github.com/PriyadarshiniRajagopal) for assistance with removing unused launcher icons!

