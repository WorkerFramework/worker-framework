# worker-store-fs

---

 This is an implementation of a `DataStore` that reads and writes data from
 and to a local disk.


## Configuration

 The configuration source for this module is `FileSystemDataStoreConfiguration`.
 The following configuration options are present:

 - datastore: the relative path of the directory to be used to store and read
  data. If the directory does not exist, it will be created. This parameter
  must not be null and must not be empty.


## Usage

 This is a simple module (and as such may be suitable for unit or integration
 tests) which will simply be looking for files in the datastore directory.


## Failure modes

 The following scenarios will prevent the module from initialising:

 - The datastore directory was not specified
 - The datastore directory does not exist and cannot be created

 The following scenarios have been identified as possible runtime failure modes
 for this module:

 - Disk read failures including file system corruption
 - Insufficient disk space
 - Blocking I/O layers causing read or write requests to hang
 - Attempting to read very large files into memory
 - Exceeding file system limits (including maximum file handles)

## Maintainers

The following people are responsible for maintaining this code:

- Andy Reid (Belfast, UK, andrew.reid@microfocus.com)
- Dermot Hardy (Belfast, UK, dermot.hardy@microfocus.com)
- Anthony Mcgreevy (Belfast, UK, anthony.mcgreevy@microfocus.com)
- Davide Giorgio Picchione (Belfast, UK, davide-giorgio.picchione@microfocus.com)
- Thilagavathi Santhoshkumar (Belfast, UK, thilagavathi.santhoshkumar@microfocus.com)
- Radoslav Straka (Belfast, UK, radoslav.straka@microfocus.com)
- Michael Bryson (Belfast, UK, michael.bryson@microfocus.com)
- Rahul Kulkarni (Chicago, USA, rahul.kulkarni@microfocus.com)
- Kusuma Ghosh Dastidar (Pleasanton, USA, vgkusuma@microfocus.com)
- Om Mariappan (Pleasanton, USA, omkumar.mariappan@microfocus.com)
- Morvin Shah (Pleasanton, USA, morivn.pan.shah@microfocus.com)
